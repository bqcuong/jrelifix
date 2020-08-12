package net.bqc.jrelifix.context.validation

import java.lang.reflect.Modifier

import org.apache.log4j.Logger
import org.junit.Test

case class TestCaseFilter(ignoredTestCases: Seq[String]) {
  private val logger: Logger = Logger.getLogger(this.getClass)

  def acceptTestCase(testCase: TestCase, clazz: Class[_]): Boolean = {
    if (ignoredTestCases != null) {
      for (ignoredTC <- this.ignoredTestCases) {
        if (testCase.getFullName.contains(ignoredTC)) return false
      }
    }
    true
  }

  def acceptClass(clazz: Class[_]): Boolean = {
    if (ignoredTestCases != null) {
      for (ignoredTC <- this.ignoredTestCases) {
        if (ignoredTC.contains(clazz.getCanonicalName)) return false
      }
    }

    if (isAbstractClass(clazz)) {
//      logger.debug(clazz.getCanonicalName + " is abstract test case class!")
      return false
    }
    for (method <- clazz.getMethods) {
      if (method.getAnnotation(classOf[Test])!= null) return true
    }

//    logger.debug("No test methods found in " + clazz.getCanonicalName + "!")
    false
  }

  private def isAbstractClass(clazz: Class[_]) = (clazz.getModifiers & Modifier.ABSTRACT) != 0
}
