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
    val faultLineNumber = faultStatement.getLine()
    var applied = false

    var changedSnippet = DiffUtils.getChangedSnippet(projectData.changedSourcesMap, faultStatement)
    if (changedSnippet != null) {
      val prevCode = changedSnippet.srcSource
      val currCode = changedSnippet.dstSource
      assert(prevCode != null)
      assert(currCode != null)
      if (changedSnippet.changedType == ChangedType.MODIFIED) {
        val currASTNodeOnDocument = ASTUtils.searchNodeByIdentifier(document.cu, currCode)
        ASTUtils.replaceNode(this.astRewrite, currASTNodeOnDocument, prevCode.getJavaNode())
        applied = true
      }
      else if (changedSnippet.changedType == ChangedType.MOVED && currCode.getBeginLine() == faultLineNumber) {
        val prevLine = prevCode.getBeginLine()
        val currentNodeAtPrevLine = ASTUtils.searchNodeByLineNumber(document.cu, prevLine)
        val currentNode = ASTUtils.searchNodeByIdentifier(document.cu, currCode)

        // Step 1: Remove the current block of code
        ASTUtils.removeNode(document.rewriter, currentNode)

        // Step 2: Put it again at the previous line number
        if (prevLine < currCode.getBeginLine()) { // move down
          ASTUtils.insertNode(document.rewriter, currentNodeAtPrevLine, currCode.getJavaNode(), insertAfter = false)
        }
        else { // move up
          ASTUtils.insertNode(document.rewriter, currentNodeAtPrevLine, currCode.getJavaNode())
        }

        applied = true
      }
    }
    else {
      changedSnippet = DiffUtils.getChangedSnippet(projectData.changedSourcesMap, faultStatement, MAX_LINE_DISTANCE)
      // TODO: Support removed minor expression in a statement
      if (changedSnippet != null && changedSnippet.changedType == ChangedType.REMOVED) {
        val prevCode = changedSnippet.srcSource
        assert(prevCode != null)

        if (changedSnippet.srcRange.beginLine > faultStatement.getBeginLine()) {
          // insert after fault statement
          ASTUtils.insertNode(this.astRewrite, faultStatement.getJavaNode(), prevCode.getJavaNode())
        }
        else {
          // insert before fault statement
          ASTUtils.insertNode(this.astRewrite, faultStatement.getJavaNode(), prevCode.getJavaNode(), insertAfter = false)
        }
        applied = true
      }
    }

    if (applied) {
      doMutating()
    }
  }

  override def unmutate(): Unit = ???

  override def applicable(): Boolean = ???
}
