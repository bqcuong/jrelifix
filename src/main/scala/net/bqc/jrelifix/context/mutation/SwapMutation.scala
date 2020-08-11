package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.context.diff.ChangeType
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.{ASTUtils, DiffUtils}
import org.eclipse.jdt.core.dom.{ASTNode, Block}

/**
 * Swap changed statement with siblings
 * @param faultStatement
 * @param projectData
 */
case class SwapMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  override def mutate(paramSeed: Identifier = null): Boolean = {
    if (isParameterizable) assert(paramSeed != null)
    if (!faultStatement.isSwappableStatement()) return false

    val isChanged = DiffUtils.isChanged(projectData.changedSourcesMap, faultStatement)
    // current faulty line is the changed line => try to swap with its siblings
    if (isChanged) {
      // pick the sibling statement to swap with faulty statement
      val prevSibNode: ASTNode = ASTUtils.getSiblingNode(faultStatement.getJavaNode(), after = false)
      val nextSibNode: ASTNode = ASTUtils.getSiblingNode(faultStatement.getJavaNode(), after = true)

      if (prevSibNode != null) {
        val patch = createPatch(prevSibNode, swapDown = false)
        addPatch(patch)
      }

      if (nextSibNode != null) {
        val patch = createPatch(nextSibNode, swapDown = true)
        addPatch(patch)
      }

      prevSibNode != null || nextSibNode != null
    }

    else false
  }

  private def createPatch(siblingNode: ASTNode, swapDown: Boolean): Patch = {
    // Step 1: Remove the current block of code
    val removeAction = ASTActionFactory.generateRemoveAction(faultStatement.getJavaNode())
    // Step 2: Put it before/after the chosen sibling statement
    val insertAction = ASTActionFactory.generateInsertAction(siblingNode, faultStatement.getJavaNode(), swapDown)

    val patch = new Patch(document)
    patch.addAction(removeAction)
    patch.addAction(insertAction)
    patch
  }

  override def applicable(): Boolean = ???

  override def isParameterizable: Boolean = false
}