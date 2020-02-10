package net.bqc.jrelifix.config

import java.net.URL

import net.bqc.jrelifix.utils.ClassPathUtils

case class Config(
                   var depClasspath: String = "",

                   var sourceFolder: String = "src/main/java",
                   var testFolder: String = "test/main/java",
                   var sourceClassFolder: String = "target/classes",
                   var testClassFolder: String = "target/test-classes",

                   projFolder: String = "",

                   passingTests: Seq[String] = null,
                   failingTests: Seq[String] = null,
                   onlyFailTests: Boolean = false,

                   testTimeout: Int = 100,
                   testsIgnored: Seq[String] = null,

                   isDataFlow: Boolean = false,
                   locHeuristic: String = "Ochiai",
                   faultLines: String = null,
                   topNFaults: Int = 100,

                   javaHome: String = null,

                   iterationPeriod: Int = 1, // Specific for JRelifix Engine
                   bugInducingCommits: Seq[String] = Array[String]("HEAD"),
                 ) {

  def classpath(): String = {
    String.join(ClassPathUtils.CP_DELIMITER, sourceClassFolder, testClassFolder, depClasspath)
  }

  def classpathURLs(): Array[URL] = {
    ClassPathUtils.parseClassPaths(String.join(ClassPathUtils.CP_DELIMITER, sourceClassFolder, testClassFolder, depClasspath))
  }
}
