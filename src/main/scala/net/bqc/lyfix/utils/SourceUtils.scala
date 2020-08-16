package net.bqc.lyfix.utils

import java.io.File
import java.util

import net.bqc.lyfix.context.ProjectData
import net.bqc.lyfix.context.compiler.DocumentASTRewrite
import net.bqc.lyfix.context.compiler.inmemory.Utilities
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.{SuffixFileFilter, TrueFileFilter}
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite
import org.eclipse.jface.text.Document

import scala.collection.mutable.ArrayBuffer

object SourceUtils {

  @throws(classOf[Exception])
  def getSourceFiles(sourcePaths: Array[String], ext: String = ".java"): ArrayBuffer[String] = {
    val sourceFiles: util.Collection[File] = new util.LinkedList[File]
    val sourceFilesArray: ArrayBuffer[String] = new ArrayBuffer[String]()
    for (sourcePath <- sourcePaths) {
      val sourceFile: File = new File(sourcePath)
      if (sourceFile.isDirectory) {
        sourceFiles.addAll(FileUtils.listFiles(sourceFile, new SuffixFileFilter(ext), TrueFileFilter.INSTANCE))
      }
      else {
        sourceFiles.add(sourceFile)
      }
      import scala.jdk.CollectionConverters._
      for (file <- sourceFiles.asScala) {
        sourceFilesArray.addOne(file.getCanonicalPath)
      }
    }
    sourceFilesArray
  }

  @throws[Exception]
  def buildSourceDocumentMap(sourceFilesArray: ArrayBuffer[String], projectData: ProjectData): java.util.HashMap[String, DocumentASTRewrite] = {
    val map = new java.util.HashMap[String, DocumentASTRewrite]
    for (sourceFile <- sourceFilesArray) {
      val backingFile = new File(sourceFile)
      val encoded = Utilities.readFromFile(backingFile)
      val contents = new Document(new String(encoded))
      val cu = projectData.filePath2CU(sourceFile)
      val docrw = new DocumentASTRewrite(contents, backingFile, cu)
      map.put(sourceFile, docrw)
    }
    map
  }
}
