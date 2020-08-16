package net.bqc.lyfix.context.validation

import net.bqc.lyfix.context.ProjectData
import net.bqc.lyfix.context.validation.executor.{JUnitTestExecutor, TestExecutionProcessLauncher, TestNGExecutor}
import net.bqc.lyfix.utils.ClassPathUtils
import org.apache.log4j.Logger

import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks

class TestCaseValidator(projectData: ProjectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  val predefinedTests: ArrayBuffer[TestCase] = ArrayBuffer[TestCase]()
  val remainingTests: ArrayBuffer[TestCase] = ArrayBuffer[TestCase]()

  def loadTestsCasesFromOpts(): Unit = {
    predefinedTests.addAll(loadPredefinedTestCases())
    predefinedTests.foreach(tc => logger.debug("Initially Reduced Tests: " + tc.getFullName))

    remainingTests.addAll(loadPositiveTestCases(predefinedTests))
//    predefinedPosTests.foreach(tc => logger.debug("Initially Positive Tests(+): " + tc.getFullName))

  }

  def loadPredefinedTestCases(): ArrayBuffer[TestCase] = {
    val failingTests = projectData.config().reducedTests
    failingTests.foldLeft(new ArrayBuffer[TestCase]){
      (res, testName) => {
        val tc = new TestCase(testName)
        res.append(tc)
        res
      }
    }
  }

  def loadPositiveTestCases(predefinedTests: ArrayBuffer[TestCase]): ArrayBuffer[TestCase] = {
    val allTestCases = TestCaseFinder(
      ClassPathUtils.parseClassPaths(projectData.config().classpath()),
      projectData.config().testClassFolder,
      TestCaseFilter(projectData.config().ignoredTests)).find()

    var positiveTestCases = ArrayBuffer[TestCase]()
    for (tc <- allTestCases) {
      var flag = false
      val loop = new Breaks
      loop.breakable(
        for (negTc <- predefinedTests) {
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

  def validateAllTestCases() : (Boolean, ArrayBuffer[TestCase]) = {
    val projectFolder = projectData.config().projFolder
    val classpath = projectData.config().classpath()
    val (negAllPassed, negFailed) = validateTestCases(predefinedTests, projectFolder, classpath)
    val (posAllPassed, posFailed) = validateTestCases(remainingTests, projectFolder, classpath)
    (negAllPassed && posAllPassed, negFailed ++ posFailed)
  }

  def validateReducedTestCases() : (Boolean, ArrayBuffer[TestCase]) = {
    val projectFolder = projectData.config().projFolder
    val classpath = projectData.config().classpath()
    validateTestCases(predefinedTests, projectFolder, classpath)
  }

  def validateTestCases(testCases: ArrayBuffer[TestCase], projectFolder: String, classpath: String) : (Boolean, ArrayBuffer[TestCase]) = {
    var allPassed = true
    val failedTestCases = testCases.foldLeft(ArrayBuffer[TestCase]()) {
      (failedTC, tc) => {
        val testPassed = validateTestCase(tc, projectFolder, classpath)
        if (!testPassed) {
          allPassed = false
          failedTC.addOne(tc)
        }
      }
      failedTC
    }
    (allPassed, failedTestCases)
  }

  def validateTestCase(testCase: TestCase, projectFolder: String, classpath: String) : Boolean = {
    val process = new TestExecutionProcessLauncher()
    val testDriver = if ("testng".equals(projectData.config().testDriver)) classOf[TestNGExecutor] else classOf[JUnitTestExecutor]
    val testResult = process.execute(
      projectFolder,
      classpath,
      testCase.getFullName,
      testDriver,
      projectData.config().javaHome,
      projectData.config().testTimeout,
      Array[String]()
    )
    testResult.wasSuccessful()
  }
}
