package net.bqc.jrelifix.faultlocalization
import java.io.File
import java.lang.reflect.Modifier
import java.net.{URL, URLClassLoader}
import java.util
import java.util.concurrent._

import br.usp.each.saeg.jaguar.core.{JaCoCoClient, Jaguar}
import br.usp.each.saeg.jaguar.core.heuristic.Heuristic
import br.usp.each.saeg.jaguar.core.model.core.requirement.LineTestRequirement
import br.usp.each.saeg.jaguar.core.runner.JaguarRunListener
import br.usp.each.saeg.jaguar.core.utils.FileUtils
import ch.qos.logback.classic.Level
import net.bqc.jrelifix.model.{Identifier, JaguarFaultIdentifier}
import org.junit.runner.JUnitCore
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

case class JaguarConfig (heuristic: Heuristic, projectDir: File, sourceDir: File, testDir: File, isDataFlow: Boolean) {}

case class JaguarLocalizationStandaloneLibrary(config: JaguarConfig, classpath: Array[URL])
  extends FaultLocalization with Callable[ArrayBuffer[Identifier]]  {

  protected lazy val junit = new JUnitCore

  override def call(): ArrayBuffer[Identifier] = {
    LoggerFactory.getLogger("JaguarLogger").asInstanceOf[ch.qos.logback.classic.Logger].setLevel(Level.OFF);
    val classes = CustomJaguarFileUtils.findTestClasses(config.testDir)
    execute(classes)
    this.rankedList
  }

  private def execute(classes: Array[Class[_]]): Unit = {
    val jaguar = new Jaguar(config.sourceDir)
    val client = new JaCoCoClient(config.isDataFlow)
    client.connect()
    this.junit.addListener(new JaguarRunListener(jaguar, client))
    this.junit.run(classes:_*) // Super note: adding the _* tells the compiler to treat array as varargs in Java
    client.close()
    val rankedList = jaguar.generateRank(config.heuristic)
    rankedList.forEach(e => {
      val ej = JaguarFaultIdentifier(e.asInstanceOf[LineTestRequirement].getLineNumber, e.getClassName, e.getSuspiciousness)
      this.rankedList.append(ej)
    })
  }

  override def run(): Unit = {
    val classLoader = new URLClassLoader(this.classpath, Thread.currentThread().getContextClassLoader)
    val executor = Executors.newSingleThreadExecutor(new CustomClassLoaderThreadFactory(classLoader))

    try {
      this.rankedList = executor
        .submit(new JaguarLocalizationStandaloneLibrary(this.config, this.classpath))
        .get(5, TimeUnit.MINUTES)
    }
    catch {
      case e: TimeoutException => {
        throw new TimeoutException("Fault Localization exceeds time limit!")
      }
      case e: Exception => {
        e.printStackTrace()
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

object CustomJaguarFileUtils {
  def findTestClasses(testDir: File): Array[Class[_]] = {
    val  testClassFiles: util.List[File] = FileUtils.findFilesEndingWith(testDir, Array[String]("Test.class"))
    val classes: util.List[Class[_]] = convertToClasses(testClassFiles, testDir)
    val classesArr = new Array[Class[_]](classes.size)
    for(i <- 0 until classes.size()) {
      classesArr(i) = classes.get(i)
    }
    classesArr
  }

  def convertToClasses(classFiles: util.List[File], classesDir: File): util.List[Class[_]] = {
    val classes: util.List[Class[_]] = new util.ArrayList[Class[_]]()
    val var3: util.Iterator[java.io.File] = classFiles.iterator()
    while (var3.hasNext) {
      val file = var3.next()
      val c = {
        Class.forName(FileUtils.getClassNameFromFile(classesDir, file), false, Thread.currentThread().getContextClassLoader)
      }
      if (!Modifier.isAbstract(c.getModifiers)) classes.add(c)
    }
    classes
  }
}
