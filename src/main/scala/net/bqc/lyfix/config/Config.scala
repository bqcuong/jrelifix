package net.bqc.lyfix.config

import java.io.File
import java.net.URL

import net.bqc.lyfix.utils.{ClassPathUtils, FileFolderUtils}

import scala.collection.mutable.ArrayBuffer

case class Config(
                   var depClasspath: String = "",

                   var sourceFolder: String = "src/main/java",
                   var testFolder: String = "test/main/java",
                   var sourceClassFolder: String = "target/classes",
                   var testClassFolder: String = "target/test-classes",

                   var projFolder: String = "",
                   var rootProjFolder: String = null,

                   reducedTests: Seq[String] = null,

                   testTimeout: Int = 30,
                   ignoredTests: Seq[String] = null,

                   isDataFlow: Boolean = false,
                   locHeuristic: String = "Ochiai",
                   faultLines: String = null,
                   faultFile: String = null,
                   topNFaults: Int = 1000,

                   javaHome: String = null,
                   testDriver: String = null,

                   iterationPeriod: Int = 1, // Specific for JRelifix Engine
                   bugInducingCommit: String = "HEAD",
                   BugSwarmValidation: Boolean = false,
                   BugSwarmImageTag: String = null,
                   externalTestCommand: String = null
                 ) {

  def classpath(): String = {

    val realDepCps = parseDependencyCp(depClasspath).mkString(ClassPathUtils.CP_DELIMITER)
    String.join(ClassPathUtils.CP_DELIMITER, sourceClassFolder, testClassFolder, realDepCps)
  }

  def classpathURLs(): Array[URL] = {
    ClassPathUtils.parseClassPaths(classpath())
  }

  private def parseDependencyCp(depClasspath: String): ArrayBuffer[String] = {
    val cpsFromArgs = depClasspath.split(":")
    val realCps = ArrayBuffer[String]()

    for (cp <- cpsFromArgs) {
      val cpFile = new File(cp)
      if (cpFile.exists() && cpFile.isDirectory) { // if the given cp is a folder, try to collect all jars inside it
        val jars: java.util.List[File] = FileFolderUtils.walk(cp, ".jar", new java.util.ArrayList[File])
        import scala.jdk.CollectionConverters._
        if (!jars.isEmpty) realCps.addAll(jars.asScala.map(_.getCanonicalPath))
        realCps.addOne(cp)
      }
    }
    realCps
  }
}
