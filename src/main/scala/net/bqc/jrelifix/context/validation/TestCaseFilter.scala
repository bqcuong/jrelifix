package net.bqc.jrelifix.context.validation

import java.lang.reflect.Modifier

import org.junit.Test

case class TestCaseFilter(ignoredTestCases: Seq[String]) {

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
        if (clazz.getCanonicalName.contains(ignoredTC)) return false
      }
    }

    if (isAbstractClass(clazz)) return false
    for (method <- clazz.getMethods) {
      if (method.getAnnotation(classOf[Test])!= null) return true
    }
    false
  }

  private def isAbstractClass(clazz: Class[_]) = (clazz.getModifiers & Modifier.ABSTRACT) != 0
}
