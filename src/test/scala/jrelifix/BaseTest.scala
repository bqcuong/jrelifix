package net.bqc.jrelifix

import junit.framework.TestCase
import net.bqc.jrelifix.JRelifixMain.{faultLocalization, initializeCompiler}
import net.bqc.jrelifix.config.{Config, OptParser}
import net.bqc.jrelifix.context.collector.{ChangedSeedsCollector, SeedsCollector}
import net.bqc.jrelifix.context.compiler.JavaJDKCompiler
import net.bqc.jrelifix.context.diff.DiffCollector
import net.bqc.jrelifix.context.mutation.MutationGenerator
import net.bqc.jrelifix.context.parser.JavaParser
import net.bqc.jrelifix.context.validation.TestCaseValidator
import net.bqc.jrelifix.context.{EngineContext, ProjectData}
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.identifier.fault.Faulty
import net.bqc.jrelifix.utils.SourceUtils
import org.apache.log4j.Logger

import scala.collection.mutable.ArrayBuffer

abstract class BaseTest extends TestCase {
  private val logger: Logger = Logger.getLogger(this.getClass)

  protected var config: Config = _
  protected var projectData: ProjectData = _
  protected var context: EngineContext = _
  protected var astParser: JavaParser = _
  protected var differ: DiffCollector = _
  protected var compiler: JavaJDKCompiler = _
  protected var testValidator: TestCaseValidator = _
  protected var mutationGenerator: MutationGenerator = _
  protected var topNFaults: ArrayBuffer[Identifier] = _

  override def setUp(): Unit = {
    // create config first in concrete class
    projectData = ProjectData()
    projectData.setConfig(config)
    projectData.makeTemp()
  }

  protected def createContext(): Unit = {
    setUpAllModules()
    context = new EngineContext(astParser, differ, compiler, testValidator, mutationGenerator)
  }

  protected def setUpAllModules(): Unit = {
    parseAST()
    collectDiffs()
    collectSeeds()
    buildSourceContents()
    initCompiler()
    initMutationGenerator()
    setUpFaultLocalization()
  }

  protected def buildSourceContents(): Unit = {
    val sourcePath: Array[String] = Array[String](projectData.config().sourceFolder)
    projectData.sourceFilesArray.addAll(SourceUtils.getSourceFiles(sourcePath))
    projectData.sourceFileContents.putAll(SourceUtils.buildSourceDocumentMap(projectData.sourceFilesArray, projectData))
  }

  protected def initCompiler(): Unit = {
    compiler = initializeCompiler(projectData.sourceFileContents, projectData)
    val compilable = compiler.compile() == JavaJDKCompiler.Status.COMPILED
    if (!compilable) {
      logger.error("Please make sure your project compilable first!")
      System.exit(1)
    }
  }

  protected def initTestValidator(): Unit = {
    testValidator = new TestCaseValidator(projectData)
    testValidator.loadTestsCasesFromOpts()
  }

  protected def initMutationGenerator(): Unit = {
    mutationGenerator = new MutationGenerator(projectData)
  }

  protected def setUpFaultLocalization(): Unit = {
    topNFaults = faultLocalization(projectData)
    topNFaults.foreach {
      case f@(fault: Faulty) =>
        fault.setFileName(projectData.class2FilePathMap(fault.getClassName()))
        f.setJavaNode(projectData.identifier2ASTNode(f))
    }
    logger.info("Faults after transforming to Java Nodes:")
    topNFaults.take(projectData.config().topNFaults).foreach(logger.info(_))
    projectData.backupFaultFileSource(topNFaults)
  }

  protected def parseAST(): Unit = {
    astParser = JavaParser(projectData.config().projFolder, projectData.config().sourceFolder, projectData.config().classpath())
    val (path2CuMap, class2PathMap) = astParser.batchParse()
    projectData.compilationUnitMap.addAll(path2CuMap)
    projectData.class2FilePathMap.addAll(class2PathMap)
  }

  protected def collectDiffs(): Unit = {
    differ = DiffCollector(projectData)
    val changedSources = differ.collectChangedSources()
    projectData.initChangedSourcesMap(changedSources)
  }

  protected def collectSeeds(): Unit = {
    val seedsCollector = SeedsCollector(projectData)
    val changedSeedsCollector = ChangedSeedsCollector(projectData)
    seedsCollector.collect()
    changedSeedsCollector.collect()
  }

  override def tearDown(): Unit = {
    projectData.cleanTemp()
  }

  def createConfig(args: Array[String]): Unit = {
    this.config = OptParser.parseOpts(args)
  }
}
