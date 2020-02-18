package net.bqc.jrelifix.context

import java.util

import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.ASTUtils
import org.eclipse.jdt.core.dom.{ASTNode, CompilationUnit}

import scala.collection.mutable.ArrayBuffer

case class ProjectData() {
  val compilationUnitMap: scala.collection.mutable.HashMap[String, CompilationUnit] = new scala.collection.mutable.HashMap[String, CompilationUnit]
  val class2FilePathMap: scala.collection.mutable.HashMap[String, String] = new scala.collection.mutable.HashMap[String, String]
  val sourceFilesArray: ArrayBuffer[String] = ArrayBuffer[String]()
  val sourceFileContents: java.util.HashMap[String, DocumentASTRewrite] = new util.HashMap[String, DocumentASTRewrite]()

  def class2CU(className: String): CompilationUnit = compilationUnitMap(class2FilePathMap(className))
  def filePath2CU(relativeFilePath: String): CompilationUnit = compilationUnitMap(relativeFilePath)

  def identifier2ASTNode(identifier: Identifier): ASTNode = {
    val cu = class2CU(identifier.getClassName())
    ASTUtils.findNode(cu, identifier)
  }

  def class2Path(className: String): String = {
    class2FilePathMap(className)
  }
}
