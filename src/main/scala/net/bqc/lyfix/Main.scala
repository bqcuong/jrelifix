package net.bqc.lyfix

import java.io.File
import java.util.Properties

import net.bqc.lyfix.config.OptParser
import net.bqc.lyfix.context.collector.{ChangedSeedsCollector, SeedsCollector}
import net.bqc.lyfix.context.compiler.inmemory.JavaJDKCompiler
import net.bqc.lyfix.context.compiler.{BugSwarmCompiler, DocumentASTRewrite, ICompiler}
import net.bqc.lyfix.context.diff.DiffCollector
import net.bqc.lyfix.context.faultlocalization.{JaguarConfig, JaguarLocalizationLibrary, PredefinedFaultLocalization}
import net.bqc.lyfix.context.mutation.MutationGenerator
import net.bqc.lyfix.context.parser.JavaParser
import net.bqc.lyfix.context.validation.{BugSwarmTestCaseValidator, TestCaseValidator}
import net.bqc.lyfix.context.{EngineContext, ProjectData}
import net.bqc.lyfix.engine.{APREngine, LyFixEngine}
import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.identifier.fault.{Faulty, JaguarFaultIdentifier, PredefinedFaultIdentifier}
import net.bqc.lyfix.utils.{ClassPathUtils, FileFolderUtils, SourceUtils}
import org.apache.log4j.{Logger, PropertyConfigurator}

import scala.collection.mutable.ArrayBuffer

object Main {

  val logger: Logger = Logger.getLogger(this.getClass)
  val projectData: ProjectData = ProjectData()

  def main(args: Array[String]): Unit = {
//    projectData.bugId = "Bears-139"
//    projectData.bugId = "Bears-127"
//    projectData.bugId = "Bears-98"
//    projectData.bugId = "Bears-121"
//    projectData.bugId = "tananaev-traccar-68883949"
//    projectData.bugId = "tananaev-traccar-82839755"
//    projectData.bugId = "Bears-251"
//    projectData.bugId = "sannies-mp4parser-79111320"
//    projectData.bugId = "stagemonitor-stagemonitor-145477129"
//    projectData.bugId = "puniverse-capsule-78565048"
//    projectData.bugId = "tananaev-traccar-64783123"
//    projectData.bugId = "yamcs-yamcs-186324159"
//    projectData.bugId = "openpnp-openpnp-130246850"
//    projectData.bugId = "Bears-203"
//    projectData.bugId = "Bears-201"
//    projectData.bugId = "apache-commons-lang-224267191"
//    projectData.bugId = "openpnp-openpnp-213669200"
//    projectData.bugId = "petergeneric-stdlib-292030904"
//    projectData.bugId = "Bears-102"
//    projectData.bugId = "Bears-188"
//    projectData.bugId = "Bears-217"
//    projectData.bugId = "Bears-56"
    projectData.bugId = "Bears-74"

//    configLog4J(projectData.bugId)
    val predefinedArgs = FileFolderUtils.readFile("ArgFiles/%s.txt".format(projectData.bugId))
      .split("\n")
      .map(_.replaceAll("\"", ""))
    repair(predefinedArgs)
  }

  def configLog4J(bugId: String): Unit = {
    val configStream = this.getClass.getResourceAsStream("/log4j.properties")
    val props = new Properties();
    props.load(configStream);
    configStream.close();
    props.setProperty("log4j.appender.FILE.File", "logs/%s.log".format(bugId))
    PropertyConfigurator.configure(props)
  }

  def repair(args: Array[String]): Unit = {
    val cfg = OptParser.parseOpts(args)
    projectData.setConfig(cfg)
    projectData.makeTemp()
    logger.debug("classpath: " + cfg.classpath())

    logger.info("Parsing AST ...")
    val astParser = JavaParser(projectData.config().projFolder, projectData.config().sourceFolder, projectData.config().classpath())
    val (path2CuMap, class2PathMap) = astParser.batchParse()
    projectData.compilationUnitMap.addAll(path2CuMap)
    projectData.class2FilePathMap.addAll(class2PathMap)
    logger.info("Done parsing AST!")

    logger.info("Building source file contents (ASTRewriter) ...")
    val sourcePath: Array[String] = Array[String](projectData.config().sourceFolder)
    projectData.sourceFilesArray.addAll(SourceUtils.getSourceFiles(sourcePath))
    projectData.sourceFileContents.putAll(SourceUtils.buildSourceDocumentMap(projectData.sourceFilesArray, projectData))
    logger.info("Done building source file contents!")

    logger.info("Initializing Compiler/TestCases Invoker ...")
    val compiler = initializeCompiler(projectData.sourceFileContents, projectData)
    if (!projectData.config().BugSwarmValidation) {
      val compilable = compiler.compile() == ICompiler.Status.COMPILED
      if (!compilable) {
        logger.error("Please make sure your project compilable first!\n" +
          "----------------COMPILATION LOG----------------\n" +
          compiler.dequeueCompileError())
        System.exit(1)
      }
    }
    var testValidator: TestCaseValidator = null
    if (projectData.config().BugSwarmValidation) {
      if (projectData.config().BugSwarmImageTag != null) {
        testValidator = new BugSwarmTestCaseValidator(projectData)
      }
      else {
        logger.error("Please provide the BugSwarm image tag!")
        System.exit(1)
      }
    }
    else {
      testValidator = new TestCaseValidator(projectData)
    }
    testValidator.loadTestsCasesFromOpts()
//    testValidator.validateReducedTestCases()
//    testValidator.validateAllTestCases()
    logger.info("Done initializing!")

    logger.info("Initializing Collectors...")
    val differ = DiffCollector(projectData)
    val changedSources = differ.collectChangedSources()
    projectData.initChangedSourcesMap(changedSources)
    val seedsCollector = SeedsCollector(projectData)
    val changedSeedsCollector = ChangedSeedsCollector(projectData)
    seedsCollector.collect()
    changedSeedsCollector.collect()
    projectData.mergeSeeds()
    logger.info("Done Initializing Collectors!")

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
        val astNode = projectData.lineNumber2StmtNode(f.getFileName(), f.getBeginLine())
        f.setJavaNode(astNode)
        if (astNode != null) {
          f match {
            case jaguarFault: JaguarFaultIdentifier =>
              val (bl, el, bc, ec) = projectData.getNodePosition(f.getFileName(), astNode)
              jaguarFault.updatePosition(bl, el, bc, ec)
            case _ =>
          }
        }
        if (f.isInstanceOf[PredefinedFaultIdentifier] && f.getJavaNode() == null) throw new IllegalStateException("Please assure the --faultLines arguments are correct!")
    }
    logger.info("Done Transforming!")
    logger.info("Faults after transforming to Java Nodes:")
    topNFaults.foreach(f => logger.info(f + " ->\n" + f.getJavaNode()))
    projectData.backupFaultFileSource(topNFaults)

    logger.info("Running Repair Engine ...")
    val context = new EngineContext(astParser, differ, compiler, testValidator, mutationGenerator)
    val engine: APREngine = LyFixEngine(topNFaults, projectData, context)
    projectData.setEngine(engine.asInstanceOf[LyFixEngine])
    engine.repair()
    logger.info("Done Repair!")
    projectData.cleanTemp()
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

    if (projectData.config().faultFile != null) {
      logger.info("Doing localization with predefined faults in provided susp file...")
      val faultLines = FileFolderUtils.readFile(projectData.config().faultFile).split("\n")
      rankedList = ArrayBuffer[Identifier]()
      for (faultLine <- faultLines) {
        val parts: Array[String] = faultLine.split("@")
        val className = parts(0)
        val suspScore = parts(2).toDouble
        val lineNumbers = parts(1).split(",")
        for (ln <- lineNumbers) {
          val fault = JaguarFaultIdentifier(ln.toInt, className, suspScore)
          rankedList.addOne(fault)
        }
      }
      rankedList = rankedList.sortWith((i1, i2) => i1.asInstanceOf[Faulty].getSuspiciousness() > i2.asInstanceOf[Faulty].getSuspiciousness())
    }
    else if (projectData.config().faultLines != null) { // Fault information is given manually
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

    topNFaults
  }
}
