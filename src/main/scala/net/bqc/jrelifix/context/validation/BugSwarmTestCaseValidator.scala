package net.bqc.jrelifix.context.validation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.validation.executor.BugSwarmTestExecutionLauncher
import org.apache.log4j.Logger

import scala.collection.mutable.ArrayBuffer

class BugSwarmTestCaseValidator(projectData: ProjectData) extends TestCaseValidator(projectData: ProjectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  override def loadTestsCasesFromOpts(): Unit = {
    predefinedTests.addAll(loadPredefinedTestCases())
    predefinedTests.foreach(tc => logger.debug("Initially Reduced Tests: " + tc.getFullName))
  }

  override def validateAllTestCases() : (Boolean, ArrayBuffer[TestCase]) = {
    val bugSwarmImageTag = projectData.config().BugSwarmImageTag
    val timeout = projectData.config().testTimeout
    val testResult =  BugSwarmTestExecutionLauncher.validate(bugSwarmImageTag, timeout)
    (testResult, ArrayBuffer[TestCase]())
  }
}
