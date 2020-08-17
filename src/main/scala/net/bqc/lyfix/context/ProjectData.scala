package net.bqc.lyfix.context

import java.io.{File, FileNotFoundException}
import java.util

import net.bqc.lyfix.config.Config
import net.bqc.lyfix.context.compiler.DocumentASTRewrite
import net.bqc.lyfix.context.diff.ChangedFile
import net.bqc.lyfix.engine.LyFixEngine
import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.identifier.fault.Faulty
import net.bqc.lyfix.identifier.seed.Seedy
import net.bqc.lyfix.utils.ASTUtils
import org.apache.commons.io.FileUtils
import org.eclipse.jdt.core.dom.{ASTNode, CompilationUnit}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

case class ProjectData() {
  private val TEMP_POSTFIX = "_temp"

  val RANDOM_SEED: Long = 1586793494000L
  val randomizer = new Random()
  randomizer.setSeed(RANDOM_SEED)

  private var configData: Config = _
  private var engine: LyFixEngine = _
  val compilationUnitMap: mutable.HashMap[String, CompilationUnit] = new mutable.HashMap[String, CompilationUnit]
  val class2FilePathMap: mutable.HashMap[String, String] = new mutable.HashMap[String, String]
  val sourceFilesArray: ArrayBuffer[String] = new ArrayBuffer[String]
  val sourceFileContents: java.util.HashMap[String, DocumentASTRewrite] = new util.HashMap[String, DocumentASTRewrite]
  val changedSourcesMap: mutable.HashMap[String, ChangedFile] = new mutable.HashMap[String, ChangedFile]
  val changedConditions: mutable.HashMap[String, ArrayBuffer[Identifier]] = new mutable.HashMap[String, ArrayBuffer[Identifier]]()
  val originalFaultFiles: mutable.HashSet[String] = new mutable.HashSet[String]
  val seedsMap: mutable.HashMap[String, mutable.HashSet[Identifier]] = new mutable.HashMap[String, mutable.HashSet[Identifier]]()
  val allSeeds: mutable.HashSet[Identifier] = new mutable.HashSet[Identifier]()

  def class2CU(className: String): CompilationUnit = compilationUnitMap(class2FilePathMap(className))
  def filePath2CU(relativeFilePath: String): CompilationUnit = compilationUnitMap(relativeFilePath)

  def identifier2ASTNode(identifier: Identifier): ASTNode = {
    val cu = filePath2CU(identifier.getFileName())
    ASTUtils.searchNodeByIdentifier(cu, identifier)
  }

  def lineNumber2StmtNode(filePath: String, lineNumber: Int): ASTNode = {
    val cu = filePath2CU(filePath)
    ASTUtils.searchStmtNodeByLineNumber(cu, lineNumber)
  }

  def class2Path(className: String): String = {
    class2FilePathMap(className)
  }

  def updateDocument(faultFile: String): Unit = {
    sourceFileContents.get(faultFile).resetModifiedDocument()
  }

  def resetDocument(faultFile: String): Unit = {
    sourceFileContents.get(faultFile).resetModifiedDocument()
  }

  def mergeSeeds(): mutable.HashSet[Identifier] = {
    for (seedSet <- seedsMap.values) {
      for (seed <- seedSet) {
        if (allSeeds.contains(seed)) { // this seed code occur before
          val seedOpt = allSeeds.find(_.equals(seed))
          val foundSeed = seedOpt.orNull
          assert(foundSeed != null)
          foundSeed.asInstanceOf[Seedy].addChangeTypes(seed.asInstanceOf[Seedy].getChangeTypes())
        }
        else {
          allSeeds.add(seed)
        }
      }
    }
    allSeeds
  }

  def initChangedSourcesMap(changedSources: ArrayBuffer[ChangedFile]): Unit = {
    for (changedSource <- changedSources) {
      changedSourcesMap.put(changedSource.filePath, changedSource)
    }
  }

  def setConfig(cfg: Config): Unit = this.configData = cfg
  def config(): Config = configData

  /**
   * Clone a new temporary classes folder for the repair process
   */
  def makeTemp(): Unit = {
    if (config().BugSwarmValidation) return // no need to use temp when use BugSwarm Validation
    val srcClass = new File(config().sourceClassFolder)
    if (!srcClass.exists()) throw new FileNotFoundException("Source Classes: %s not found!".format(config().sourceClassFolder))
    val srcClassTemp = new File(config().sourceClassFolder + TEMP_POSTFIX)
    if (srcClassTemp.exists()) FileUtils.deleteDirectory(srcClassTemp)
    FileUtils.copyDirectory(srcClass, srcClassTemp)
    this.configData.sourceClassFolder = srcClassTemp.getCanonicalPath
  }

  /**
   * clean temp folder after repair
   */
  def cleanTemp(): Unit = {
    if (config().BugSwarmValidation) return // no need to use temp when use BugSwarm Validation
    val srcClassTemp = new File(config().sourceClassFolder)
    FileUtils.deleteDirectory(srcClassTemp)
  }

  def backupFaultFileSource(faults: ArrayBuffer[Identifier]): Unit = {
    val faultFiles = faults.foldLeft(mutable.HashSet[String]()) {
      (faultFs, f) => {
        f match {
          case faulty: Faulty =>
            faultFs.addOne(class2Path(faulty.getClassName()))
          case _ =>
        }
      }
      faultFs
    }
    originalFaultFiles.addAll(faultFiles)
  }

  def setEngine(e: LyFixEngine): Unit = this.engine = e
  def getEngine: LyFixEngine = this.engine

  def updateChangedSourceFiles(): Unit = {
    for (f <- originalFaultFiles) {
      val changedSource = sourceFileContents.get(f).modifiedDocument.get()
      FileUtils.writeStringToFile(new File(f), changedSource, "utf-8")
    }
  }

  var bugId: String = ""

}
