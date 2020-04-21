package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.ASTUtils
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.Statement

case class AddStmtMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)
  override def isParameterizable: Boolean = true

  override def mutate(paramSeed: Identifier): Boolean = {
    if (isParameterizable) assert(paramSeed != null)
    val paramASTNode = paramSeed.getJavaNode()
    assert(paramASTNode.isInstanceOf[Statement])
    val faultyASTNode = faultStatement.getJavaNode()
    ASTUtils.insertNode(this.astRewrite, faultyASTNode, paramASTNode)
    doMutating()
    true
  }

  override def applicable(): Boolean = ???
}
