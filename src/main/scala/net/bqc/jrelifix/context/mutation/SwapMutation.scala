package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.diff.ChangeType
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.{ASTUtils, DiffUtils}
import org.eclipse.jdt.core.dom.{ASTNode, Block}

/**
 * Swap changed statement with siblings
 * @param faultStatement
 * @param projectData
 */
case class SwapMutation(faultStatement: Identifier, projectData: ProjectData, swapDirection: Int)
  extends Mutation(faultStatement, projectData) {

  private var emptyBlock: ASTNode = _
  private var chosenSibNode: ASTNode = _
  private var swapUp: Boolean = false

  override def mutate(conditionExpr: Identifier = null): Boolean = {
    if (isParameterizable) assert(conditionExpr != null)
    if (!faultStatement.isSwappableStatement()) return false

    swapUp = swapDirection == SwapMutation.SWAP_UP

    var changedSnippet = DiffUtils.searchChangedSnippetOutside(projectData.changedSourcesMap, faultStatement)
    // current faulty line is the changed line => try to swap with its siblings
    if (changedSnippet != null) {
      val currCode = changedSnippet.dstSource
      assert(currCode != null)
      // pick the sibling statement to swap with faulty statement
      val prevSibNode: ASTNode = ASTUtils.getSiblingNode(faultStatement.getJavaNode(), after = false)
      val nextSibNode: ASTNode = ASTUtils.getSiblingNode(faultStatement.getJavaNode(), after = true)

      if (prevSibNode == null) {
        chosenSibNode = nextSibNode
        swapUp = false
      }
      else if (nextSibNode == null) {
        chosenSibNode = prevSibNode
        swapUp = true
      }
      else {
        chosenSibNode = if (swapUp) prevSibNode else nextSibNode
      }
    }
    else {
      changedSnippet = DiffUtils.searchChangedSnippetOutside(projectData.changedSourcesMap, faultStatement, MAX_LINE_DISTANCE)
      if (changedSnippet != null && changedSnippet.changeType != ChangeType.REMOVED) {
        val currCode = changedSnippet.dstSource
        assert(currCode != null)

        chosenSibNode = ASTUtils.searchNodeByIdentifier(this.document.cu, currCode)
        swapUp = faultStatement.getLine() > currCode.getLine()
        val actualSibNode: ASTNode = ASTUtils.getSiblingNode(faultStatement.getJavaNode(), after = !swapUp)
        // validate if the changed statement is indeed a sibling of the faulty statement
        if (actualSibNode == null) return false
      }
    }

    if (chosenSibNode != null) {
      // Step 1: Remove the current block of code
      this.emptyBlock = this.astRewrite.getAST.createInstance(classOf[Block])
      ASTUtils.replaceNode(document.rewriter, faultStatement.getJavaNode(), emptyBlock)
      // Step 2: Put it before/after the chosen sibling statement
      ASTUtils.insertNode(document.rewriter, chosenSibNode, faultStatement.getJavaNode(), !swapUp)
      doMutating()
      true
    }
    else false
  }

  override def unmutate(): Unit = {
    ASTUtils.removeNode(document.rewriter, faultStatement.getJavaNode())
    ASTUtils.replaceNode(document.rewriter, emptyBlock, faultStatement.getJavaNode())
    doMutating()
  }

  override def applicable(): Boolean = ???

  override def isParameterizable: Boolean = false
}

object SwapMutation {
  def SWAP_UP = 0
  def SWAP_DOWN = 1
}