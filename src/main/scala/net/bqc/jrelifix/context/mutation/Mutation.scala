package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.identifier.Identifier
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite

abstract class Mutation(faultStatement: Identifier, projectData: ProjectData) {

  protected val document: DocumentASTRewrite = projectData.sourceFileContents.get(faultStatement.getFileName())
  protected val astRewrite: ASTRewrite = document.rewriter

  def mutate(): Unit
  def unmutate(): Unit
  def applicable(): Boolean
}
