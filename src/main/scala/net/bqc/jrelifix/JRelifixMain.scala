package net.bqc.jrelifix

import java.io.File

import net.bqc.jrelifix.config.OptParser
import net.bqc.jrelifix.context.collector.{ChangedSeedsCollector, SeedsCollector}
import net.bqc.jrelifix.context.compiler.inmemory.JavaJDKCompiler
import net.bqc.jrelifix.context.compiler.{BugSwarmCompiler, DocumentASTRewrite, ICompiler}
import net.bqc.jrelifix.context.diff.DiffCollector
import net.bqc.jrelifix.context.faultlocalization.{JaguarConfig, JaguarLocalizationLibrary, PredefinedFaultLocalization}
import net.bqc.jrelifix.context.mutation.MutationGenerator
import net.bqc.jrelifix.context.parser.JavaParser
import net.bqc.jrelifix.context.validation.{BugSwarmTestCaseValidator, TestCaseValidator}
import net.bqc.jrelifix.context.{EngineContext, ProjectData}
import net.bqc.jrelifix.engine.{APREngine, JRelifixEngine}
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.identifier.fault.{Faulty, PredefinedFaultIdentifier}
import net.bqc.jrelifix.utils.{ClassPathUtils, FileFolderUtils, SourceUtils}
import org.apache.log4j.Logger

import scala.collection.mutable.ArrayBuffer

object JRelifixMain {

  val logger: Logger = Logger.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    val cfg = OptParser.parseOpts(args)
    val projectData = ProjectData()
    projectData.setConfig(cfg)

    logger.info("Trying to set up fault localization ...")
    val topNFaults = faultLocalization(projectData)
    logger.info("Finished fault localization!")

    val FL_THRESHOLD = 0.1f
    val bug = "Bears-98"
    val fileName = "SusFiles/NormalFL/" + bug + ".txt"
    val fileName2 = "SusFiles/PerfectFL/" + bug + ".txt"
//    FileFolderUtils.writeFile(fileName2, "")
    val content: StringBuffer = new StringBuffer()
    topNFaults.foreach {
      case f@(fault: Faulty) => {
        if (fault.getSuspiciousness() >= FL_THRESHOLD) {
          val faultStr = "%s@%d@%.2f".format(fault.getClassName(), fault.getLine(), fault.getSuspiciousness())
          println(faultStr)
          content.append(faultStr)
          content.append("\n")
        }
      }
    }
    FileFolderUtils.writeFile(fileName, content.toString)
    println("Done!")
    System.exit(0)
  }

  def initializeCompiler(sourceFileContents: java.util.HashMap[String, DocumentASTRewrite], projectData: ProjectData): ICompiler  = {
    if (projectData.config().BugSwarmValidation) {
      return new BugSwarmCompiler(projectData.config().BugSwarmImageTag)
    }

    val cpArr = projectData.config().classpathURLs().map(_.toString)
    val srcArr = Array[String] {projectData.config().sourceFolder}
    val copyIncludes: Array[String] = Array[String]{""}
    val copyExcludes: Array[String] = Array[String]{""}

    val compiler: ICompiler = new JavaJDKCompiler(
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
          projectData.config().ignoredTests,
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

    rankedList
  }
}
