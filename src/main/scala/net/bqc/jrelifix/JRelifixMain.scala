package net.bqc.jrelifix

import java.io.File

import net.bqc.jrelifix.config.OptParser
import net.bqc.jrelifix.context.faultlocalization.{JaguarConfig, JaguarLocalizationLibrary, PredefinedFaultLocalization}
import net.bqc.jrelifix.context.parser.JavaParser
import net.bqc.jrelifix.context.validation.compiler.DocumentASTRewrite
import net.bqc.jrelifix.model.Identifier
import net.bqc.jrelifix.utils.{ClassPathUtils, SourceUtils}
import org.apache.log4j.Logger

import scala.collection.mutable.ArrayBuffer

object JRelifixMain {

  val logger: Logger = Logger.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    OptParser.parseOpts(args)

    logger.info("Trying to set up fault localization ...")
    val topNFaults = faultLocalization()
    logger.info("Finished fault localization!")
    logger.info("Parsing AST ...")
    val astParser = JavaParser(OptParser.params().projFolder, OptParser.params().sourceFolder, OptParser.params().classpath())
    astParser.batchParse()
    logger.info("Done parsing AST!")
    logger.info("Transforming faults to Java Nodes ...")
    topNFaults.foreach(f => f.setJavaNode(astParser.identifier2ASTNode(f)))
    logger.info("Done Transforming!")
    logger.info("Faults after transforming to Java Nodes:")
    topNFaults.take(OptParser.params().topNFaults).foreach(logger.info(_))

    logger.info("Building source file contents (ASTRewriter) ...")
    val sourcePath: Array[String] = Array[String](OptParser.params().sourceFolder)
    val sourceFilesArray: Array[String] = SourceUtils.getSourceFiles(sourcePath)
    val sourceFileContents: scala.collection.mutable.HashMap[String, DocumentASTRewrite] =
      SourceUtils.buildSourceDocumentMap(sourceFilesArray, OptParser.params().projFolder, astParser)
    logger.info("Done building source file contents!")
    //
    //    logger.info("Initializing Compiler/TestCases Invoker ...")
    //    val compiler = initializeCompiler(sourceFileContents)
    //    val testInvoker = JUnitTestInvoker(astParser);
    //    testInvoker.loadTestsCasesFromOpts()
    //    logger.info("Done initializing!")
    //
    //    logger.info("Initializing Mutation Generator ...")
    //    val mutationGenerator = new MutationGenerator(sourceFileContents)
    //    logger.info("Done initializing!")
    //
    //    logger.info("Running Repair Engine ...")
    //    val context = new EngineContext(astParser, compiler, testInvoker, mutationGenerator)
    //    val engine: APREngine = JRelifixEngine(topNFaults, context)
    //    engine.repair()
    //    logger.info("Done Repair!")
  }

  //  def initializeCompiler(sourceFileContents: util.HashMap[String, DocumentASTRewrite]): JavaJDKCompiler  = {
  //    val cpArr = OptParser.params().classpathURLs().map(_.toString)
  //    val cpArrayList = new util.ArrayList[String]()
  //    cpArr.foreach(cpArrayList.add)
  //    val copyIncludes: Array[String] = Array[String]{""}
  //    val copyExcludes: Array[String] = Array[String]{""}
  //
  //    val compiler = new JavaJDKCompiler(
  //      OptParser.params().sourceClassFolder,
  //      cpArrayList,
  //      sourceFileContents,
  //      copyIncludes,
  //      copyExcludes
  //    )
  //    compiler
  //  }

  def faultLocalization(): ArrayBuffer[Identifier] = {
    var rankedList: ArrayBuffer[Identifier] = null

    if (OptParser.params().faultLines != null) { // Fault information is given manually
      // Note: fault lines are not provided with suspiciousness score
      // but with exact location of fault components (startLine, endLine, startColumn, endColumn)
      // because the given fault lines are the absolute correct ones, the repair engine needs only focus on these lines
      logger.info("Doing localization with predefined faults...")
      val locLib = PredefinedFaultLocalization(OptParser.params().faultLines)
      locLib.run()
      rankedList = locLib.rankedList
    }
    else { // using Jaguar Localization Library
      logger.info("Doing localization with Jaguar, heuristic: %s ...".format(OptParser.params().locHeuristic))
      try {
        import br.usp.each.saeg.jaguar.core.heuristic.Heuristic

        // check existing of heuristic in classpath
        val locHeuristic = Class.forName("br.usp.each.saeg.jaguar.core.heuristic.%sHeuristic".format(OptParser.params().locHeuristic))
          .newInstance.asInstanceOf[Heuristic]

        val locConfig = JaguarConfig(locHeuristic,
          new File(OptParser.params().projFolder),
          new File(OptParser.params().sourceClassFolder),
          new File(OptParser.params().testClassFolder),
          OptParser.params().isDataFlow)

        val locLib = new JaguarLocalizationLibrary(locConfig.asInstanceOf[JaguarConfig],
          ClassPathUtils.parseClassPaths(OptParser.params().classpath()))
        locLib.run()
        rankedList = locLib.rankedList
      }
      catch {
        case e: ClassNotFoundException => {
          logger.error("ClassNotFoundException: " + e.getMessage)
          return null
        }
        case e: Exception => {
          e.printStackTrace()
          return null
        }
      }
    }

    logger.info("Done localization!")

    // Warning: can be a lot of fault Files if the topNFaultLoc from rankedList is large enough. May need improvement later ...
    logger.info("Considering top %d fault locations".format(OptParser.params().topNFaults))
    val topNFaults = rankedList.take(OptParser.params().topNFaults)

    topNFaults
  }
}
