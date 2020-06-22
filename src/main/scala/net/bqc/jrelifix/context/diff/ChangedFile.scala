package net.bqc.jrelifix.context.diff

import net.bqc.jrelifix.context.parser.JavaParser
import org.eclipse.jdt.core.dom.CompilationUnit

import scala.collection.mutable.ArrayBuffer

case class ChangedFile(filePath: String,
                       oldPath: String,
                       newPath: String,
                       oldVersion: String,
                       newVersion: String) {

  // Root changed snippets
  val rootCS: ArrayBuffer[ChangeSnippet] = ArrayBuffer[ChangeSnippet]()
  // All changed snippets
  val allCS: ArrayBuffer[ChangeSnippet] = ArrayBuffer[ChangeSnippet]()

  val oldCUnit: CompilationUnit = JavaParser.parseAST(oldVersion)
  val newCUnit: CompilationUnit = JavaParser.parseAST(newVersion)
}