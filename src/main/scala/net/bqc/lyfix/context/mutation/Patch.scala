package net.bqc.lyfix.context.mutation

import net.bqc.lyfix.context.compiler.DocumentASTRewrite
import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.utils.ASTUtils
import org.eclipse.jdt.core.dom.{ASTNode, Block, CatchClause, TryStatement}
import org.eclipse.jdt.core.dom.rewrite.{ASTRewrite, ListRewrite}
import org.eclipse.text.edits.{TextEdit, UndoEdit}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class Patch(document: DocumentASTRewrite) {
  private val editQueue: ArrayBuffer[TextEdit] = ArrayBuffer[TextEdit]()
  private val undoStack: mutable.Stack[UndoEdit] = mutable.Stack[UndoEdit]()
  private val astRewrite: ASTRewrite = document.generateASTRewrite()
  private val usingSeedList: ArrayBuffer[Identifier] = ArrayBuffer[Identifier]()

  /**
   * Code used as parameter for the patch.
   * If the patch causes program compile failed,
   * these code snippets will be added to tabu set and the next operators could avoid them
   */
  val paramCodes: mutable.HashSet[String] = mutable.HashSet[String]()

  def addUsingSeed(seed: Identifier): Unit = {
    usingSeedList.addOne(seed)
  }

  def getUsingSeeds(): ArrayBuffer[Identifier] = usingSeedList

  def addAction(astAction: ASTAction): Unit = {
    astAction match {
        // remove
      case action: ASTRemove =>
        ASTUtils.removeNode(astRewrite, action.node)
        // replace
      case action: ASTReplace =>
        ASTUtils.replaceNode(astRewrite, action.currentNode, action.newNode)
        // append
      case action: ASTAppend =>
        ASTUtils.appendNode(astRewrite, action.parentNode, action.newChildNode)
        // insert
      case action: ASTInsert =>
        ASTUtils.insertNode(astRewrite, action.pivotNode, action.newNode, action.insertAfter)
        // add catch clause
      case action: ASTAddCatchClause =>
        if (action.tryStmtNode.catchClauses().size() > 0) {
          val catchClause = action.tryStmtNode.catchClauses().get(0).asInstanceOf[CatchClause]
          val ast = this.document.ast
          val newCatchClause = ast.newCatchClause()
          val decl = ast.newSingleVariableDeclaration
          decl.setName(ast.newSimpleName("e"))
          decl.setType(ast.newSimpleType(ast.newName(action.exceptionClassName)))
          newCatchClause.setException(decl)
          val listRewrite = this.astRewrite.getListRewrite(action.tryStmtNode, TryStatement.CATCH_CLAUSES_PROPERTY)
          listRewrite.insertAfter(newCatchClause, catchClause, null)
        }
      case _ =>
    }

    // create edit for current mutation and add it to edit queue
    val newEdit = this.astRewrite.rewriteAST(this.document.modifiedDocument, null)
    editQueue.append(newEdit)
  }

  def applyEdits(): Unit = {
    if (editQueue.nonEmpty) {
      val undo = editQueue.last.apply(this.document.modifiedDocument, TextEdit.CREATE_UNDO)
      undoStack.push(undo)
    }
//    for (edit <- editQueue) {
//      // Apply edits to the document object
//      val undo = edit.apply(this.document.modifiedDocument, TextEdit.CREATE_UNDO)
//      // Store undo for un-mutating purpose
//      undoStack.push(undo)
//    }
  }

  def undoEdits(): Unit = {
    // undo all edit in the undoStack
    while (undoStack.nonEmpty) {
      val undo = undoStack.pop()
      undo.apply(this.document.modifiedDocument)
    }
  }
}
