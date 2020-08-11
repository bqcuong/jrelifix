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
    val patch = new Patch(this.document)
    if (parentTryStmt != null) {
      val addCatchAction = ASTActionFactory.generateAddCatchClauseAction(parentTryStmt, "java.lang.Throwable")
      patch.addAction(addCatchAction)
    }
    else {
      val faultStr = faultAST.toString
      val tryCatchBlockStr = "try {\n  %s\n} catch(Throwable e){}".format(faultStr)
      val tryCatchBlock = ASTUtils.createStmtNodeFromString(tryCatchBlockStr)
      val replaceAction = ASTActionFactory.generateReplaceAction(faultAST, tryCatchBlock)
      patch.addAction(replaceAction)
    }
    addPatch(patch)
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
