package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.{ASTUtils, DiffUtils}

/**
 * To delete incorrectly added statement/expression in previous version
 * @param faultStatement
 * @param projectData
 */
case class DeleteMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  override def mutate(): Unit = {
    // delete only when the fault line is modified in examining commit
    if (DiffUtils.isChanged(projectData.changedSourcesMap, faultStatement)) {
      // Modify source code on ASTRewrite
      ASTUtils.removeNode(this.astRewrite, faultStatement.getJavaNode())
      doMutating()
    }
  }

  override def unmutate(): Unit = ???

  override def applicable(): Boolean = ???
}
