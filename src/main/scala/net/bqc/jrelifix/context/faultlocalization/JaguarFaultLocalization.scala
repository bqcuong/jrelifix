package net.bqc.jrelifix.context.faultlocalization
import java.io.{File, OutputStream, PrintStream}
import java.net.{URL, URLClassLoader}
import java.util.concurrent._

import br.usp.each.saeg.jaguar.core.heuristic.Heuristic
import br.usp.each.saeg.jaguar.core.model.core.requirement.LineTestRequirement
import br.usp.each.saeg.jaguar.core.runner.JaguarRunListener
import br.usp.each.saeg.jaguar.core.{JaCoCoClient, Jaguar}
import ch.qos.logback.classic.Level
import net.bqc.jrelifix.config.OptParser
import net.bqc.jrelifix.context.validation.{CustomClassLoaderThreadFactory, TestCaseFilter, TestCaseFinderUtils}
import net.bqc.jrelifix.model.{Identifier, JaguarFaultIdentifier}
import org.junit.runner.JUnitCore
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

case class JaguarConfig (heuristic: Heuristic, projectDir: File, sourceDir: File, testDir: File, isDataFlow: Boolean) {}

case class JaguarLocalizationLibrary(config: JaguarConfig, classpath: Array[URL])
  extends FaultLocalization with Callable[ArrayBuffer[Identifier]]  {

  protected lazy val junit = new JUnitCore

  override def call(): ArrayBuffer[Identifier] = {
    LoggerFactory.getLogger("JaguarLogger").asInstanceOf[ch.qos.logback.classic.Logger].setLevel(Level.OFF);
    val original = System.out
    System.setOut(new PrintStream((_: Int) => {}))

    val classes = TestCaseFinderUtils.findTestClasses(config.testDir, TestCaseFilter(OptParser.params().testsIgnored))
    execute(classes)

    System.setOut(original)
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
      val ej = JaguarFaultIdentifier(e.asInstanceOf[LineTestRequirement].getLineNumber, e.getClassName.replace("/", "."), e.getSuspiciousness)
      this.rankedList.append(ej)
    })
  }

  override def run(): Unit = {
    val classLoader = new URLClassLoader(this.classpath, Thread.currentThread().getContextClassLoader)
    val executor = Executors.newSingleThreadExecutor(new CustomClassLoaderThreadFactory(classLoader))

    try {
      this.rankedList = executor
        .submit(JaguarLocalizationLibrary(this.config, this.classpath))
        .get(5, TimeUnit.MINUTES)
    }
    catch {
      case _: TimeoutException => {
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
