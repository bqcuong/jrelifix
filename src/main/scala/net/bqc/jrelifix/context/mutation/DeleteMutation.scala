package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.{ASTUtils, DiffUtils}
import org.eclipse.jdt.core.dom.{ASTNode, Block}

/**
 * To delete incorrectly added statement/expression in previous version
 * @param faultStatement
 * @param projectData
 */
case class DeleteMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  private var emptyBlock: ASTNode = _

  override def mutate(conditionExpr: Identifier = null): Boolean = {
    if (isParameterizable) assert(conditionExpr != null)
    // delete only when the fault line is modified in examining commit
    if (DiffUtils.isChanged(projectData.changedSourcesMap, faultStatement)) {
      // Modify source code on ASTRewrite
      this.emptyBlock = this.astRewrite.getAST.createInstance(classOf[Block])
      ASTUtils.replaceNode(this.astRewrite, faultStatement.getJavaNode(), this.emptyBlock)
      doMutating()
      true
    }
    else false
  }

  override def unmutate(): Unit = {
    ASTUtils.replaceNode(this.astRewrite, this.emptyBlock, faultStatement.getJavaNode())
    doMutating()
  }

  override def applicable(): Boolean = ???

  override def isParameterizable: Boolean = false
}
