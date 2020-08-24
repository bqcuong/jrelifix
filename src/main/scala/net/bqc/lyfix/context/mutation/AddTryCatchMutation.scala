package net.bqc.lyfix.context.mutation

import net.bqc.lyfix.context.ProjectData
import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.utils.ASTUtils
import org.eclipse.jdt.core.dom.{ASTNode, CatchClause, MethodDeclaration, TryStatement}

import scala.collection.mutable.ArrayBuffer

case class AddTryCatchMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  override def isParameterizable: Boolean = false

  /**
   * Handle the mutating actions
   *
   * @param paramSeeds if not null, this operator is parameterizable
   */
  override def mutate(paramSeeds: ArrayBuffer[Identifier]): Boolean = {
    if (isParameterizable) assert(paramSeeds != null)
    val faultAST = faultStatement.getJavaNode()

    val parentTryStmt = tryToGetTryCatchBlock(faultAST)
    // special case
    if (parentTryStmt != null) {
      val patch = new Patch(this.document)
      val addCatchAction = ASTActionFactory.generateAddCatchClauseAction(parentTryStmt, "java.lang.Throwable")
      patch.addAction(addCatchAction)
      addPatch(patch)
    }

    val faultStr = faultAST.toString
    val tryCatchBlockStr = "try {\n  %s\n} catch(Throwable e){}".format(faultStr)
    val tryCatchBlock = ASTUtils.createStmtNodeFromString(tryCatchBlockStr)
    val replaceAction = ASTActionFactory.generateReplaceAction(faultAST, tryCatchBlock)
    val patch2 = new Patch(this.document)
    patch2.addAction(replaceAction)
    addPatch(patch2)
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
