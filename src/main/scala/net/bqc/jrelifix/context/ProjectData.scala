package net.bqc.jrelifix.context

import java.io.{File, FileNotFoundException}
import java.util

import net.bqc.jrelifix.config.Config
import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.context.diff.ChangedFile
import net.bqc.jrelifix.identifier.{Faulty, Identifier}
import net.bqc.jrelifix.utils.ASTUtils
import org.apache.commons.io.FileUtils
import org.eclipse.jdt.core.dom.{ASTNode, CompilationUnit}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class ProjectData() {
  private val TEMP_POSTFIX = "_temp"

  private var configData: Config = _
  val compilationUnitMap: mutable.HashMap[String, CompilationUnit] = new mutable.HashMap[String, CompilationUnit]
  val class2FilePathMap: mutable.HashMap[String, String] = new mutable.HashMap[String, String]
  val sourceFilesArray: ArrayBuffer[String] = new ArrayBuffer[String]
  val sourceFileContents: java.util.HashMap[String, DocumentASTRewrite] = new util.HashMap[String, DocumentASTRewrite]
  val changedSourcesMap: mutable.HashMap[String, ChangedFile] = new mutable.HashMap[String, ChangedFile]
  val originalFaultFiles: mutable.HashSet[String] = new mutable.HashSet[String]

  def class2CU(className: String): CompilationUnit = compilationUnitMap(class2FilePathMap(className))
  def filePath2CU(relativeFilePath: String): CompilationUnit = compilationUnitMap(relativeFilePath)

  def identifier2ASTNode(identifier: Identifier): ASTNode = {
    val cu = filePath2CU(identifier.getFileName())
    ASTUtils.searchNodeByIdentifier(cu, identifier)
  }

  def class2Path(className: String): String = {
    class2FilePathMap(className)
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
    val srcClass = new File(config().sourceClassFolder)
    if (!srcClass.exists()) throw new FileNotFoundException("Source Classes: %s not found!".format(config().sourceClassFolder))
    val srcClassTemp = new File(config().sourceClassFolder + TEMP_POSTFIX)
    if (srcClassTemp.exists()) FileUtils.deleteDirectory(srcClassTemp)
    FileUtils.copyDirectory(srcClass, srcClassTemp)
    this.configData.sourceClassFolder = srcClassTemp.getAbsolutePath
  }

  /**
   * clean temp folder after repair
   */
  def cleanTemp(): Unit = {
    val srcClassTemp = new File(config().sourceClassFolder + TEMP_POSTFIX)
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
}
