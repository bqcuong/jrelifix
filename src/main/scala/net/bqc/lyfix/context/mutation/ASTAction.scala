package net.bqc.lyfix.context.mutation

import org.eclipse.jdt.core.dom.{ASTNode, TryStatement}

object ASTActionFactory {
  def generateReplaceAction(currentNode: ASTNode, newNode: ASTNode): ASTAction = {
    ASTReplace(currentNode, newNode)
  }

  def generateRemoveAction(node: ASTNode): ASTAction = {
    ASTRemove(node)
  }

  def generateAppendAction(parentNode: ASTNode, newChildNode: ASTNode): ASTAction = {
    ASTReplace(parentNode, newChildNode)
  }

  def generateInsertAction(pivotNode: ASTNode, newNode: ASTNode, insertAfter: Boolean = true): ASTAction = {
    ASTInsert(pivotNode, newNode, insertAfter)
  }

  def generateAddCatchClauseAction(tryStmtNode: TryStatement, exceptionClassName: String): ASTAction = {
    ASTAddCatchClause(tryStmtNode, exceptionClassName)
  }
}

abstract class ASTAction {}
case class ASTReplace(currentNode: ASTNode, newNode: ASTNode) extends ASTAction
case class ASTRemove(node: ASTNode) extends ASTAction
case class ASTAppend(parentNode: ASTNode, newChildNode: ASTNode) extends ASTAction
case class ASTInsert(pivotNode: ASTNode, newNode: ASTNode, insertAfter: Boolean) extends ASTAction
case class ASTAddCatchClause(tryStmtNode: TryStatement, exceptionClassName: String) extends ASTAction
