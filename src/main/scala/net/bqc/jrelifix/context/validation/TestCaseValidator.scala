package net.bqc.jrelifix.context.validation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.validation.executor.{JUnitTestExecutor, TestExecutionProcessLauncher}
import net.bqc.jrelifix.utils.ClassPathUtils
import org.apache.log4j.Logger

import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks

case class TestCaseValidator(projectData: ProjectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  var predefinedNegTests: ArrayBuffer[TestCase] = ArrayBuffer[TestCase]()
  var predefinedPosTests: ArrayBuffer[TestCase] = ArrayBuffer[TestCase]()

  def loadTestsCasesFromOpts(): Unit = {
    predefinedNegTests.addAll(loadNegativeTestCases())
    predefinedNegTests.foreach(tc => logger.debug("Initially Negative Tests(-): " + tc.getFullName))

    predefinedPosTests.addAll(loadPositiveTestCases(predefinedNegTests))
    predefinedPosTests.foreach(tc => logger.debug("Initially Positive Tests(+): " + tc.getFullName))

  }

  def loadNegativeTestCases(): ArrayBuffer[TestCase] = {
    val failingTests = projectData.config().failingTests
    failingTests.foldLeft(new ArrayBuffer[TestCase]){
      (res, testName) => {
        val tc = new TestCase(testName)
        res.append(tc)
        res
      }
    }
  }

  def loadPositiveTestCases(negTests: ArrayBuffer[TestCase]): ArrayBuffer[TestCase] = {
    // only validate with failed tests
    if(projectData.config().onlyFailTests)
      return new ArrayBuffer[TestCase]()

    val allTestCases = TestCaseFinder(
      ClassPathUtils.parseClassPaths(projectData.config().classpath()),
      projectData.config().testClassFolder,
      TestCaseFilter(projectData.config().testsIgnored)).find()

    var positiveTestCases = ArrayBuffer[TestCase]()
    for (tc <- allTestCases) {
      var flag = false
      val loop = new Breaks
      loop.breakable(
        for (negTc <- negTests) {
          if (tc.getFullName.contains(negTc.getFullName)) {
            flag = true
            loop.break
          }
        }
      )
      if (!flag) positiveTestCases += tc
    }

    positiveTestCases
  }

  def validateAllTestCases(classpath: String) : (Boolean, ArrayBuffer[TestCase]) = {
    val (negAllPassed, negFailed) = validateTestCases(predefinedNegTests, classpath)
    val (posAllPassed, posFailed) = validateTestCases(predefinedPosTests, classpath)
    (negAllPassed && posAllPassed, negFailed ++ posFailed)
  }

  def validateTestCases(testCases: ArrayBuffer[TestCase], classpath: String) : (Boolean, ArrayBuffer[TestCase]) = {
    var allPassed = true
    val failedTestCases = testCases.foldLeft(ArrayBuffer[TestCase]()) {
      (failedTC, tc) => {
        val testPassed = validateTestCase(tc, classpath)
        if (!testPassed) {
          allPassed = false
          failedTC :+ tc
        }
      }
      failedTC
    }
    (allPassed, failedTestCases)
  }

  def validateTestCase(testCase: TestCase, classpath: String) : Boolean = {
    val process = new TestExecutionProcessLauncher()
    val testResult = process.execute(
      classpath,
      testCase.getFullName,
      classOf[JUnitTestExecutor],
      projectData.config().javaHome,
      projectData.config().testTimeout,
      Array[String]{""}
    )
    testResult.wasSuccessful()
  }
}
