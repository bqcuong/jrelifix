package net.bqc.jrelifix.context.validation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.validation.executor.BugSwarmLauncher
import org.apache.log4j.Logger

import scala.collection.mutable.ArrayBuffer

class BugSwarmTestCaseValidator(projectData: ProjectData) extends TestCaseValidator(projectData: ProjectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  override def loadTestsCasesFromOpts(): Unit = {
    // Do nothing
    // All the predefined test cases have been assigned in the docker container scripts
  }

  override def validateReducedTestCases() : (Boolean, ArrayBuffer[TestCase]) = {
    val bugSwarmImageTag = projectData.config().BugSwarmImageTag
    val timeout = projectData.config().testTimeout
    val testResult =  BugSwarmLauncher.validateReducedTS(bugSwarmImageTag, timeout)
    (testResult, ArrayBuffer[TestCase]())
  }

  override def validateAllTestCases() : (Boolean, ArrayBuffer[TestCase]) = {
    val bugSwarmImageTag = projectData.config().BugSwarmImageTag
    val timeout = projectData.config().testTimeout
    val testResult =  BugSwarmLauncher.validateAllTS(bugSwarmImageTag, timeout)
    (testResult, ArrayBuffer[TestCase]())
  }
}
