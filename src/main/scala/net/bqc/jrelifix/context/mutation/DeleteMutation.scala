package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.search.Searcher
import net.bqc.jrelifix.search.cs.AddedSnippetCondition
import net.bqc.jrelifix.utils.{ASTUtils, DiffUtils}
import org.eclipse.jdt.core.dom.{ASTNode, Block}

import scala.collection.mutable.ArrayBuffer

/**
 * To delete incorrectly added statement/expression in previous version
 * @param faultStatement
 * @param projectData
 */
case class DeleteMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  override def mutate(paramSeeds: ArrayBuffer[Identifier]): Boolean = {
    if (isParameterizable) assert(paramSeeds != null)
    // delete only when the fault line is added in previous commit
    if (!faultStatement.isStatement()) return false
    val faultFile = faultStatement.getFileName()
    val cs = Searcher.searchChangeSnippets(projectData.changedSourcesMap(faultFile), AddedSnippetCondition(faultStatement))
    if (cs.nonEmpty) {
      // Modify source code on ASTRewrite
      val patch = new Patch(this.document)
      val astAction = ASTActionFactory.generateRemoveAction(faultStatement.getJavaNode())
      patch.addAction(astAction)
      addPatch(patch)
      true
    }
    else false
  }

  override def applicable(): Boolean = ???

  override def isParameterizable: Boolean = false
}
