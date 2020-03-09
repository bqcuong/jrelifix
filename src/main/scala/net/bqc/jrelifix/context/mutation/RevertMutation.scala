package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.diff.ChangedType
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.{ASTUtils, DiffUtils}
import org.apache.log4j.Logger
import org.eclipse.text.edits.TextEdit

/**
 * To revert the modified statement/expression to old ones in previous version:
 * Change actions covered:
 * - remove ADDED (already supported by Delete Mutation)
 * - add REMOVED
 * - revert MODIFIED
 * - re-swap MOVED
 * @param faultStatement
 * @param projectData
 */
case class RevertMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  override def mutate(): Unit = {
    // try to revert modified expressions which are faulty lines
    val faultFile = faultStatement.getFileName()
    val faultLineNumber = faultStatement.getLine()
    var applied = false

    val changedSnippet = DiffUtils.getChangedSnippet(projectData.changedSourcesMap, faultStatement)
    if (changedSnippet != null) {
      if (changedSnippet.changedType == ChangedType.REMOVED) {

        applied = true
      }

      if (changedSnippet.changedType == ChangedType.MODIFIED) {
        val prevCode = changedSnippet.srcSource
        val currCode = changedSnippet.dstSource
        assert(prevCode != null)
        assert(currCode != null)
        val currASTNodeOnDocument = ASTUtils.findNode(document.cu, currCode)
        ASTUtils.replaceNode(this.astRewrite, currASTNodeOnDocument, prevCode.getJavaNode())
        applied = true
      }

      if (changedSnippet.changedType == ChangedType.MOVED) {
        applied = true
      }

      if (applied) {
        // Apply changes to the document object
        val edits = this.astRewrite.rewriteAST(this.document.modifiedDocument, null)
        edits.apply(this.document.modifiedDocument, TextEdit.NONE)
      }
      return
    }
  }

  override def unmutate(): Unit = ???

  override def applicable(): Boolean = ???
}
