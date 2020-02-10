package net.bqc.jrelifix.utils

import java.io.File
import java.util

import net.bqc.jrelifix.validation.compiler.{DocumentASTRewrite, Utilities}
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.{SuffixFileFilter, TrueFileFilter}
import org.eclipse.jdt.core.dom.CompilationUnit
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite
import org.eclipse.jface.text.Document

object SourceUtils {

  @throws(classOf[Exception])
  def getSourceFiles(sourcePaths: Array[String], ext: String = ".java"): Array[String] = {
    val sourceFiles: util.Collection[File] = new util.LinkedList[File]
    var sourceFilesArray: Array[String] = null
    for (sourcePath <- sourcePaths) {
      val sourceFile: File = new File(sourcePath)
      if (sourceFile.isDirectory) {
        sourceFiles.addAll(FileUtils.listFiles(sourceFile, new SuffixFileFilter(ext), TrueFileFilter.INSTANCE))
      }
      else {
        sourceFiles.add(sourceFile)
      }
      sourceFilesArray = new Array[String](sourceFiles.size)
      var i: Int = 0
      import scala.jdk.CollectionConverters._
      for (file <- sourceFiles.asScala) {
        sourceFilesArray(i) = file.getCanonicalPath
        i += 1
      }
    }
    sourceFilesArray
  }

  @throws[Exception]
  def buildSourceDocumentMap(sourceFilesArray: Array[String], sourcePath: String, globalASTs: scala.collection.mutable.HashMap[String, CompilationUnit]): util.HashMap[String, DocumentASTRewrite] = {
    val map = new util.HashMap[String, DocumentASTRewrite]
    for (sourceFile <- sourceFilesArray) {
      val classPackageName = FileFolderUtils.relativePath(sourcePath, sourceFile).split("\\.")(0)
      val backingFile = new File(sourceFile)
      val encoded = Utilities.readFromFile(backingFile)
      val contents = new Document(new String(encoded))
      val cu = globalASTs(classPackageName.replace(File.separator, "."))
      val docrw = new DocumentASTRewrite(contents, backingFile, ASTRewrite.create(cu.getAST), cu)
      map.put(classPackageName, docrw)
    }
    map
  }
}
