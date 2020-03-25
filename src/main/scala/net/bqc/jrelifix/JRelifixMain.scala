package net.bqc.jrelifix

import java.io.File

import net.bqc.jrelifix.config.OptParser
import net.bqc.jrelifix.context.compiler.{DocumentASTRewrite, JavaJDKCompiler}
import net.bqc.jrelifix.context.diff.DiffCollector
import net.bqc.jrelifix.context.faultlocalization.{JaguarConfig, JaguarLocalizationLibrary, PredefinedFaultLocalization}
import net.bqc.jrelifix.context.mutation.MutationGenerator
import net.bqc.jrelifix.context.parser.JavaParser
import net.bqc.jrelifix.context.validation.TestCaseValidator
import net.bqc.jrelifix.context.{EngineContext, ProjectData}
import net.bqc.jrelifix.engine.{APREngine, JRelifixEngine}
import net.bqc.jrelifix.identifier.{Faulty, Identifier}
import net.bqc.jrelifix.utils.{ClassPathUtils, SourceUtils}
import org.apache.log4j.Logger

import scala.collection.mutable.ArrayBuffer

object JRelifixMain {

  val logger: Logger = Logger.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    val cfg = OptParser.parseOpts(args)
    val projectData = ProjectData()
    projectData.setConfig(cfg)
    projectData.makeTemp()

    logger.info("Parsing AST ...")
    val astParser = JavaParser(projectData.config().projFolder, projectData.config().sourceFolder, projectData.config().classpath())
    val (path2CuMap, class2PathMap) = astParser.batchParse()
    projectData.compilationUnitMap.addAll(path2CuMap)
    projectData.class2FilePathMap.addAll(class2PathMap)
    logger.info("Done parsing AST!")

    logger.info("Initializing Diff Collector...")
    val differ = DiffCollector(projectData)
    val changedSources = differ.collectChangedSources()
    projectData.initChangedSourcesMap(changedSources)
    logger.info("Done Initializing Diff Collector!")

    logger.info("Building source file contents (ASTRewriter) ...")
    val sourcePath: Array[String] = Array[String](projectData.config().sourceFolder)
    projectData.sourceFilesArray.addAll(SourceUtils.getSourceFiles(sourcePath))
    projectData.sourceFileContents.putAll(SourceUtils.buildSourceDocumentMap(projectData.sourceFilesArray, projectData))
    logger.info("Done building source file contents!")

    logger.info("Initializing Compiler/TestCases Invoker ...")
    val compiler = initializeCompiler(projectData.sourceFileContents, projectData)
    val compilable = compiler.compile() == JavaJDKCompiler.Status.COMPILED
    if (!compilable) {
      logger.error("Please make sure your project compilable first!")
      System.exit(1)
    }
    val testValidator = TestCaseValidator(projectData)
    testValidator.loadTestsCasesFromOpts()
    logger.info("Done initializing!")

    logger.info("Initializing Mutation Generator ...")
    val mutationGenerator = new MutationGenerator(projectData)
    logger.info("Done initializing!")

    logger.info("Trying to set up fault localization ...")
    val topNFaults = faultLocalization(projectData)
    logger.info("Finished fault localization!")
    logger.info("Transforming faults to Java Nodes ...")
    topNFaults.foreach {
      case f@(fault: Faulty) =>
        fault.setFileName(projectData.class2FilePathMap(fault.getClassName()))
        f.setJavaNode(projectData.identifier2ASTNode(f))
    }
    logger.info("Done Transforming!")
    logger.info("Faults after transforming to Java Nodes:")
    topNFaults.take(projectData.config().topNFaults).foreach(logger.info(_))
    projectData.backupFaultFileSource(topNFaults)

    logger.info("Running Repair Engine ...")
    val context = new EngineContext(astParser, differ, compiler, testValidator, mutationGenerator)
    val engine: APREngine = JRelifixEngine(topNFaults, projectData, context)
    engine.repair()
    logger.info("Done Repair!")
    projectData.cleanTemp()
  }

  def initializeCompiler(sourceFileContents: java.util.HashMap[String, DocumentASTRewrite], projectData: ProjectData): JavaJDKCompiler  = {
    val cpArr = projectData.config().classpathURLs().map(_.toString)
    val srcArr = Array[String] {projectData.config().sourceFolder}
    val copyIncludes: Array[String] = Array[String]{""}
    val copyExcludes: Array[String] = Array[String]{""}

    val compiler = new JavaJDKCompiler(
      projectData.config().sourceClassFolder,
      cpArr,
      sourceFileContents,
      srcArr,
      copyIncludes,
      copyExcludes
    )
    compiler
  }

  def faultLocalization(projectData: ProjectData): ArrayBuffer[Identifier] = {
    var rankedList: ArrayBuffer[Identifier] = null

    if (projectData.config().faultLines != null) { // Fault information is given manually
      // Note: fault lines are not provided with suspiciousness score
      // but with exact location of fault components (startLine, endLine, startColumn, endColumn)
      // because the given fault lines are the absolute correct ones, the repair engine needs only focus on these lines
      logger.info("Doing localization with predefined faults...")
      val locLib = PredefinedFaultLocalization(projectData.config().faultLines)
      locLib.run()
      rankedList = locLib.rankedList
    }
    else { // using Jaguar Localization Library
      logger.info("Doing localization with Jaguar, heuristic: %s ...".format(projectData.config().locHeuristic))
      try {
        import br.usp.each.saeg.jaguar.core.heuristic.Heuristic

        // check existing of heuristic in classpath
        val locHeuristic = Class.forName("br.usp.each.saeg.jaguar.core.heuristic.%sHeuristic".format(projectData.config().locHeuristic))
          .newInstance.asInstanceOf[Heuristic]

        val locConfig: JaguarConfig = JaguarConfig(locHeuristic,
          new File(projectData.config().projFolder),
          new File(projectData.config().sourceClassFolder),
          new File(projectData.config().testClassFolder),
          projectData.config().testsIgnored,
          projectData.config().isDataFlow)

        val locLib = JaguarLocalizationLibrary(locConfig, ClassPathUtils.parseClassPaths(projectData.config().classpath()))
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
    logger.info("Considering top %d fault locations".format(projectData.config().topNFaults))
    val topNFaults = rankedList.take(projectData.config().topNFaults)

    topNFaults
  }
}
