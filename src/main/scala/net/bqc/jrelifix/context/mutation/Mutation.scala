package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.identifier.Identifier
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite
import org.eclipse.text.edits.TextEdit

abstract class Mutation(faultStatement: Identifier, projectData: ProjectData) {
  /**
   * Line Margin for Fault Localization (±2)
   */
  protected val MAX_LINE_DISTANCE: Int = 1
  protected val document: DocumentASTRewrite = projectData.sourceFileContents.get(faultStatement.getFileName())
  protected val astRewrite: ASTRewrite = document.rewriter

  protected def doMutating(): Unit = {
    // Apply changes to the document object
    val edits = this.astRewrite.rewriteAST(this.document.modifiedDocument, null)
    edits.apply(this.document.modifiedDocument, TextEdit.NONE)
  }

  def mutate(): Unit
  def unmutate(): Unit
  def applicable(): Boolean

  def getRewriter(): ASTRewrite = this.astRewrite
}
