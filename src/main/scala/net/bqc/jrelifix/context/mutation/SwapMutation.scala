package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.{ASTUtils, DiffUtils}
import org.eclipse.jdt.core.dom.ASTNode

import scala.util.Random

/**
 * Swap changed statement with siblings
 * @param faultStatement
 * @param projectData
 */
case class SwapMutation(faultStatement: Identifier, projectData: ProjectData, swapDirection: Int)
  extends Mutation(faultStatement, projectData) {


  override def mutate(): Unit = {
    if (!faultStatement.isSwappableStatement()) return

    var chosenSibNode: ASTNode = null
    var swapUp = swapDirection == SwapMutation.SWAP_UP

    var changedSnippet = DiffUtils.getChangedSnippet(projectData.changedSourcesMap, faultStatement)
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
      changedSnippet = DiffUtils.getChangedSnippet(projectData.changedSourcesMap, faultStatement, MAX_LINE_DISTANCE)
      if (changedSnippet != null) {
        val currCode = changedSnippet.dstSource
        assert(currCode != null)

        chosenSibNode = ASTUtils.searchNodeByIdentifier(this.document.cu, currCode)
        swapUp = faultStatement.getLine() > currCode.getLine()
      }
    }

    if (chosenSibNode != null) {
      // Step 1: Remove the current block of code
      ASTUtils.removeNode(document.rewriter, faultStatement.getJavaNode())
      // Step 2: Put it before/after the chosen sibling statement
      ASTUtils.insertNode(document.rewriter, chosenSibNode, faultStatement.getJavaNode(), !swapUp)
      doMutating()
    }
  }

  override def unmutate(): Unit = ???

  override def applicable(): Boolean = ???
}

object SwapMutation {
  def SWAP_UP = 0
  def SWAP_DOWN = 1
}