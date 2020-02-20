package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.{ASTUtils, DiffUtils}
import org.eclipse.text.edits.TextEdit

case class DeleteMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  override def mutate(): Unit = {
    // delete only when the fault line is modified in examining commit
    if (DiffUtils.isChanged(projectData.changedSourcesMap, faultStatement)) {
      // Modify source code on ASTRewrite
      ASTUtils.removeNode(this.astRewrite, faultStatement.getJavaNode())

      // Apply changes to the document object
      val edits = this.astRewrite.rewriteAST(this.document.modifiedDocument, null)
      edits.apply(this.document.modifiedDocument, TextEdit.NONE)
    }
  }

  override def unmutate(): Unit = ???

  override def applicable(): Boolean = ???
}
