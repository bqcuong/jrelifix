package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.ASTUtils
import org.eclipse.jdt.core.dom.{ASTNode, CatchClause, MethodDeclaration, TryStatement}

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

    val parentTryStmt = tryToGetTryCatchBlock(faultAST)
    if (parentTryStmt != null) {
      val catchClause = parentTryStmt.catchClauses().get(0).asInstanceOf[CatchClause]
      val ast = this.document.ast
      val newCatchClause = ast.newCatchClause()
      val decl = ast.newSingleVariableDeclaration
      decl.setName(ast.newSimpleName("e"))
      decl.setType(ast.newSimpleType(ast.newName("java.lang.Throwable")))
      newCatchClause.setException(decl)
      val listRewrite = this.astRewrite.getListRewrite(parentTryStmt, TryStatement.CATCH_CLAUSES_PROPERTY)
      listRewrite.insertAfter(newCatchClause, catchClause, null)
    }
    else {
      val faultStr = faultAST.toString
      val tryCatchBlockStr = "try {\n  %s\n} catch(Throwable e){}".format(faultStr)
      val tryCatchBlock = ASTUtils.createStmtNodeFromString(tryCatchBlockStr)
      ASTUtils.replaceNode(this.astRewrite, faultAST, tryCatchBlock)
    }
    doMutating()
    true
  }

  private def tryToGetTryCatchBlock(node: ASTNode): TryStatement = {
    var parentNode = node.getParent
    while (parentNode != null && !parentNode.isInstanceOf[MethodDeclaration]) {
      parentNode match {
        case statement: TryStatement =>
          return statement
        case _ =>
      }
      parentNode = parentNode.getParent
    }
    null
  }

  override def applicable(): Boolean = ???
}
