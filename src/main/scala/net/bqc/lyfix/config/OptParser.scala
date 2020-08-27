package net.bqc.lyfix.config

import java.io.File

import scopt.OptionParser

object OptParser {
  val builder: OptionParser[Config] = new scopt.OptionParser[Config]("scopt") {

    head("scopt", "4.x")

    help("help").text("prints this usage text")

    opt[String]( "depClasspath")
      .action((cp, c) => c.copy(depClasspath = cp))
      .text("Dependencies Classpath (external libs,...). Accept both folder and jar path. e.g., /libs1/:/lib2/common.jar:...")

    opt[String]( "javaHome")
      .action((cp, c) => c.copy(javaHome = cp))
      .text("Specify the path to Java home used to execute test cases")

    opt[String]( "testDriver")
      .action((cp, c) => c.copy(testDriver = cp.toLowerCase()))
      .text("Test Driver. e.g., JUnit, TestNG. Default: JUnit")

    opt[String]( "sourceFolder")
      .action((source, c) => c.copy(sourceFolder = source))
      .text("Folder of source code, e.g., src/main/java")

    opt[String]( "sourceClassFolder")
      .action((source, c) => c.copy(sourceClassFolder = source))
      .text("Folder of classes of compiled source code, e.g., target/classes")

    opt[Seq[String]]("reducedTests")
      .action((p, c) => c.copy(reducedTests = p))
      .text("List of reduced test cases to be executed first, e.g., a.b.c.TestD#methodM")

    opt[String]( "testFolder")
      .action((t, c) => c.copy(testFolder = t))
      .text("Folder of tests, e.g., src/main/test")

    opt[String]( "testClassFolder")
      .action((t, c) => c.copy(testClassFolder = t))
      .text("Folder of classes of tests, e.g., target/test-classes")

    opt[String]( "projectFolder")
      .action((t, c) => c.copy(projFolder = t))
      .text("Folder of project, if there are multiple modules, please fill the absolute path to the module")

    opt[String]( "rootProjectFolder")
      .action((t, c) => c.copy(rootProjFolder = t))
      .text("Root folder of the project if your project has multiple modules, which contains the .git folder")

    opt[Int]("testTimeout")
      .action((t, c) => c.copy(testTimeout = t))
      .text("Timeout for running tests, in seconds")

    opt[Seq[String]]("ignoredTests")
      .action((p, c) => c.copy(ignoredTests = p))

    opt[String]("locHeuristic")
      .action((o, c) => c.copy(locHeuristic = o))
      .text("Name of heuristic for fault localization, e.g., Ochiai, Tarantula, etc")

    opt[String]("faultLines")
      .action((o, c) => c.copy(faultLines = o))
      .text("Faulty lines with class names, begin line, end line, begin column, end column \"e.g., a.b.c.XYZ:10 10 1 9\"")

    opt[String]("faultFile")
      .action((o, c) => c.copy(faultFile = o))
      .text("The path to the file which contains faulty lines with class names, content inside: e.g., a.b.c.XYZ@123@1.00")

    opt[Int]("topNFaults")
      .action((o, c) => c.copy(topNFaults = o))
      .text("Top N Faults to be considered, default is 100")

    opt[Boolean]("isDataFlow")
      .action((o, c) => c.copy(isDataFlow = o))
      .text("Option for Jaguar fault localization tool")

    opt[String]("bugInducingCommit")
      .action((o, c) => c.copy(bugInducingCommit = o))
      .text("The hash of the bug-inducing commit. If not being set, it'll be the current commit.")

    opt[Boolean]("bgValidation")
      .action((o, c) => c.copy(BugSwarmValidation = o))
      .text("Specify if use bugswarm scripts to execute and validate the whole test suite")

    opt[String]("bgImageTag")
      .action((o, c) => c.copy(BugSwarmImageTag = o))
      .text("The image tag of BugSwam artifact which you want to evaluate on")

    opt[String]("externalTestCommand")
      .action((o, c) => c.copy(externalTestCommand = o))
      .text("The external command to test the whole test suite. e.g., \"mvn test -Dtest=a.b.c.ClassD\"")

    opt[String]("externalReducedTestCommand")
      .action((o, c) => c.copy(externalReducedTestCommand = o))
      .text("The external command to test the reduced test suite. e.g., \"mvn test\"")

    opt[String]("configFile")
      .action((o, c) => c.copy(configFile = o))
      .text("The path to config file which contains all run options. If providing this, none of the others is needed")
  }

  def parseOpts(args: Array[String]): Config = {
    builder.parse(args, Config()) match {
      case Some(config) =>
        if (config.rootProjFolder != null) config.rootProjFolder = new File(config.rootProjFolder).getCanonicalPath
        config.projFolder = new File(config.projFolder).getCanonicalPath
        config.sourceFolder = new File(config.projFolder + File.separator + config.sourceFolder).getCanonicalPath
        config.testFolder = new File(config.projFolder + File.separator + config.testFolder).getCanonicalPath
        config.sourceClassFolder = new File(config.projFolder + File.separator + config.sourceClassFolder).getCanonicalPath
        config.testClassFolder = new File(config.projFolder + File.separator + config.testClassFolder).getCanonicalPath
        config
      case _ => throw new RuntimeException("Parsing arguments error!")
      // arguments are bad, error message will have been displayed
    }
  }
}
