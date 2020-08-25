package net.bqc.lyfix.context.validation

import java.io.File
import java.net.{URL, URLClassLoader}
import java.util
import java.util.concurrent._

import br.usp.each.saeg.jaguar.core.utils.FileUtils
import org.junit.Test
import org.junit.runner.RunWith

import scala.collection.mutable.ArrayBuffer

case class TestCaseFinder(classpath: Array[URL], testFolder: String, testFilter: TestCaseFilter) extends Callable[ArrayBuffer[TestCase]] {
  override def call(): ArrayBuffer[TestCase] = {
    val classes = TestCaseFinderUtils.findTestClasses(new File(testFolder), testFilter)
    val testCases = ArrayBuffer[TestCase]()

    for (c <- classes) {
      // Treat the whole class as a single testcase if it is annotated with @RunWith
      if (c.getAnnotation(classOf[RunWith]) != null) {
        val tc = new TestCase(c.getCanonicalName)
        testCases += tc
      }
      else {
        // find testcase by traversal all the method inside
        for (method <- c.getMethods) {
          if (method.getAnnotation(classOf[Test]) != null) {
            val methodName = method.getName
            val tc = new TestCase(c.getCanonicalName, methodName)
            if (testFilter.acceptTestCase(tc, c)) {
              testCases += tc
            }
          }
        }
      }
    }
    testCases
  }

  def find(): ArrayBuffer[TestCase] = {
    val classLoader = new URLClassLoader(this.classpath, Thread.currentThread().getContextClassLoader)
    val executor = Executors.newSingleThreadExecutor(new CustomClassLoaderThreadFactory(classLoader))
    try {
      executor
        .submit(TestCaseFinder(this.classpath, this.testFolder, this.testFilter))
        .get(10, TimeUnit.MINUTES)
    }
    catch {
      case _: TimeoutException => {
        throw new TimeoutException("Fault Localization exceeds time limit!")
      }
      case e: Exception => {
        e.printStackTrace()
        ArrayBuffer[TestCase]()
      }
    }
    finally {
      executor.shutdown()
    }
  }
}

class CustomClassLoaderThreadFactory(val classLoader: ClassLoader) extends ThreadFactory {
  override def newThread(r: Runnable): Thread = {
    val thread = new Thread(r)
    thread.setDaemon(true)
    thread.setContextClassLoader(classLoader)
    thread
  }
}

object TestCaseFinderUtils {
  def findTestClasses(testDir: File, testFilter: TestCaseFilter): Array[Class[_]] = {
    val  testClassFiles: util.List[File] = FileUtils.findFilesEndingWith(testDir, Array[String]("Test.class", "TestCase.class", "Tests.class"))
    val classes: util.List[Class[_]] = convertToClasses(testClassFiles, testDir, testFilter)
    val classesArr = new Array[Class[_]](classes.size)
    for(i <- 0 until classes.size()) {
      classesArr(i) = classes.get(i)
    }
    classesArr
  }

  def convertToClasses(classFiles: util.List[File], classesDir: File, testFilter: TestCaseFilter): util.List[Class[_]] = {
    val classes: util.List[Class[_]] = new util.ArrayList[Class[_]]()
    val var3: util.Iterator[java.io.File] = classFiles.iterator()
    while (var3.hasNext) {
      val file = var3.next()
      val c = {
        Class.forName(FileUtils.getClassNameFromFile(classesDir, file), false, Thread.currentThread().getContextClassLoader)
      }
      if (testFilter.acceptClass(c)) classes.add(c)
    }
    classes
  }
}