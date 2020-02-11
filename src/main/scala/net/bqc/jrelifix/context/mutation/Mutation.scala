package net.bqc.jrelifix.context.mutation

import java.io.File
import java.util

import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.context.parser.JavaParser
import net.bqc.jrelifix.identifier.Identifier
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite

abstract class Mutation(sourceFileContents: util.HashMap[String, DocumentASTRewrite],
                        faultStatement: Identifier, astParser: JavaParser) {

  protected val document: DocumentASTRewrite = sourceFileContents.get(astParser.class2Path(faultStatement.getClassName()))
  protected val astRewrite: ASTRewrite = document.rewriter

  def mutate(): Unit
  def unmutate(): Unit
  def applicable(): Boolean
}
