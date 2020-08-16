package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.Identifier
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.Statement

import scala.collection.mutable.ArrayBuffer

case class AddStmtMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)
  override def isParameterizable: Boolean = true

  override def mutate(paramSeeds: ArrayBuffer[Identifier]): Boolean = {
    if (isParameterizable) assert(paramSeeds != null)
    for (seed <- paramSeeds) {
      val paramASTNode = seed.getJavaNode()
      assert(paramASTNode.isInstanceOf[Statement])
      val faultyASTNode = faultStatement.getJavaNode()
      val patch = new Patch(document)
      val insertAction = ASTActionFactory.generateInsertAction(faultyASTNode, paramASTNode)
      patch.addAction(insertAction)
      addPatch(patch)
    }
    true
  }

  override def applicable(): Boolean = ???
}
