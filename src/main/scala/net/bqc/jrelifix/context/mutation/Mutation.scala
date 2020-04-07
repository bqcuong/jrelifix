package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.identifier.Identifier
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite
import org.eclipse.text.edits.{TextEdit, UndoEdit}

abstract class Mutation(faultStatement: Identifier, projectData: ProjectData) {
  /**
   * Line Margin for Fault Localization (±2)
   */
  protected val MAX_LINE_DISTANCE: Int = 1
  protected val document: DocumentASTRewrite = projectData.sourceFileContents.get(faultStatement.getFileName())
  protected val astRewrite: ASTRewrite = document.rewriter
  protected var undo: UndoEdit = _

  protected def doMutating(): Unit = {
    // Apply changes to the document object
    val edits = this.astRewrite.rewriteAST(this.document.modifiedDocument, null)
    undo = edits.apply(this.document.modifiedDocument, TextEdit.CREATE_UNDO)
  }

  def isParameterizable: Boolean

  /**
   * Handle the mutating actions
   * @param conditionExpr if not null, this operator is parameterizable
   */
  def mutate(conditionExpr: Identifier = null): Boolean

  def unmutate(): Unit = {
    if (undo != null) {
      undo.apply(this.document.modifiedDocument)
    }
  }

  def applicable(): Boolean
  def getRewriter(): ASTRewrite = this.astRewrite
}
