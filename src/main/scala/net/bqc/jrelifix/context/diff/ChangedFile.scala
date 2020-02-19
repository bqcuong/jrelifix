package net.bqc.jrelifix.context.diff

import net.bqc.jrelifix.context.parser.JavaParser
import org.eclipse.jdt.core.dom.CompilationUnit

import scala.collection.mutable.ArrayBuffer

case class ChangedFile(filePath: String,
                       oldVersion: String,
                       newVersion: String) {

  val changedSnippets: ArrayBuffer[ChangedSnippet] = ArrayBuffer[ChangedSnippet]()
  val oldCUnit: CompilationUnit = JavaParser.parseAST(oldVersion).asInstanceOf[CompilationUnit]
  val newCUnit: CompilationUnit = JavaParser.parseAST(newVersion).asInstanceOf[CompilationUnit]
}