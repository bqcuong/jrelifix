package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.search.{AddedSnippetCondition, Searcher}
import net.bqc.jrelifix.utils.{ASTUtils, DiffUtils}
import org.eclipse.jdt.core.dom.{ASTNode, Block}

/**
 * To delete incorrectly added statement/expression in previous version
 * @param faultStatement
 * @param projectData
 */
case class DeleteMutation(faultStatement: Identifier, projectData: ProjectData, doc: DocumentASTRewrite)
  extends Mutation(faultStatement, projectData, doc) {

  private var emptyBlock: ASTNode = _

  override def mutate(conditionExpr: Identifier = null): Boolean = {
    if (isParameterizable) assert(conditionExpr != null)
    // delete only when the fault line is added in previous commit
    if (!faultStatement.isStatement()) return false
    val faultFile = faultStatement.getFileName()
    val cs = Searcher.searchChangeSnippets(projectData.changedSourcesMap(faultFile), AddedSnippetCondition(faultStatement))
    if (cs.nonEmpty) {
      // Modify source code on ASTRewrite
      this.emptyBlock = this.astRewrite.getAST.createInstance(classOf[Block])
      ASTUtils.replaceNode(this.astRewrite, faultStatement.getJavaNode(), this.emptyBlock)
      doMutating()
      true
    }
    else false
  }

  override def applicable(): Boolean = ???

  override def isParameterizable: Boolean = false
}
