package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.ASTUtils

case class AddTryCatchMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  override def isParameterizable: Boolean = false

  /**
   * Handle the mutating actions
   *
   * @param paramSeed if not null, this operator is parameterizable
   */
  override def mutate(paramSeed: Identifier): Boolean = {
    if (isParameterizable) assert(paramSeed != null)
    val faultAST = faultStatement.getJavaNode()
    val faultStr = faultAST.toString
    val tryCatchBlockStr = "try {\n  %s\n} catch(Exception e){}".format(faultStr)
    val tryCatchBlock = ASTUtils.createStmtNodeFromString(tryCatchBlockStr)
    ASTUtils.replaceNode(this.astRewrite, faultAST, tryCatchBlock)
    doMutating()
    true
  }

  override def applicable(): Boolean = ???
}
