package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.identifier.Identifier
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite
import org.eclipse.text.edits.{TextEdit, UndoEdit}

abstract class Mutation(faultStatement: Identifier, projectData: ProjectData, doc: DocumentASTRewrite) {
  /**
   * Line Margin for Fault Localization (±3)
   */
  protected val MAX_LINE_DISTANCE: Int = 3
  /**
   * If no document is provided, use the original document in sourceFileContents map
   */
  protected val document: DocumentASTRewrite = if (doc != null) doc
                                               else projectData.sourceFileContents.get(faultStatement.getFileName())
  protected val astRewrite: ASTRewrite = document.generateASTRewrite
  protected var undo: UndoEdit = _

  protected def doMutating(): Unit = {
    // Apply changes to the document object
    val edits = this.astRewrite.rewriteAST(this.document.modifiedDocument, null)
    undo = edits.apply(this.document.modifiedDocument, TextEdit.CREATE_UNDO)
  }

  def isParameterizable: Boolean

  /**
   * Handle the mutating actions
   * @param paramSeed if not null, this operator is parameterizable
   */
  def mutate(paramSeed: Identifier = null): Boolean

  def unmutate(): Unit = {
    if (undo != null) {
      undo.apply(this.document.modifiedDocument)
    }
  }

  def applicable(): Boolean
  def getRewriter(): ASTRewrite = this.astRewrite
}
