package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.identifier.Identifier
import org.eclipse.jdt.core.dom.rewrite.{ASTRewrite, ListRewrite}
import org.eclipse.jdt.core.dom.{ASTNode, Block}
import org.eclipse.text.edits.{TextEdit, UndoEdit}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

abstract class Mutation(faultStatement: Identifier, projectData: ProjectData) {
  /**
   * Line Margin for Fault Localization (±3)
   */
  protected val MAX_LINE_DISTANCE: Int = 3
  /**
   * If no document is provided, use the original document in sourceFileContents map
   */
  protected val document: DocumentASTRewrite = projectData.sourceFileContents.get(faultStatement.getFileName())
  protected val astRewrite: ASTRewrite = document.generateASTRewrite
  protected val editQueue: ArrayBuffer[TextEdit] = ArrayBuffer[TextEdit]()
  protected val undoStack: mutable.Stack[UndoEdit] = mutable.Stack[UndoEdit]()

  protected def doMutating(): Unit = {
    // create edit for current mutation and add it to edit queue
    val newEdit = this.astRewrite.rewriteAST(this.document.modifiedDocument, null)
    editQueue.append(newEdit)

    for (edit <- editQueue) {
      // Apply edits to the document object
      val undo = edit.apply(this.document.modifiedDocument, TextEdit.CREATE_UNDO)
      // Store undo for un-mutating purpose
      undoStack.push(undo)
    }
  }

  def unmutate(): Unit = {
    // undo all edit in the undoStack
    while (undoStack.nonEmpty) {
      val undo = undoStack.pop()
      undo.apply(this.document.modifiedDocument)
    }
  }

  def getEdits: ArrayBuffer[TextEdit] = {
    editQueue.clone()
  }

  def addEdits(edits: ArrayBuffer[TextEdit]): Unit = {
    editQueue.addAll(edits)
  }

  def isParameterizable: Boolean

  /**
   * Handle the mutating actions
   * @param paramSeed if not null, this operator is parameterizable
   */
  def mutate(paramSeed: Identifier = null): Boolean
  def applicable(): Boolean
  def getRewriter: ASTRewrite = this.astRewrite

  def replaceNode(node: ASTNode, replacement: ASTNode): Unit = {
    var rep: ASTNode = null
    rep = ASTNode.copySubtree(node.getAST, replacement)
    this.astRewrite.replace(node, rep, null)
  }

  def removeNode(toRemoved: ASTNode): Unit = {
    val emptyBlock = this.astRewrite.getAST.createInstance(classOf[Block])
    this.replaceNode(toRemoved, emptyBlock)
  }

  def appendNode(parentNode: ASTNode, newNode: ASTNode): Unit = {
    val toAppend: ASTNode = ASTNode.copySubtree(parentNode.getAST, newNode)
    val bl: Block = parentNode.getParent.asInstanceOf[Block]
    val rewrite: ListRewrite = this.astRewrite.getListRewrite(bl, Block.STATEMENTS_PROPERTY)
    rewrite.insertAfter(toAppend, parentNode, null)
  }

  def insertNode(currentNode: ASTNode, newNode: ASTNode, insertAfter: Boolean = true): Unit = {
    val toAdd: ASTNode = ASTNode.copySubtree(currentNode.getAST, newNode)
    val bl: Block = currentNode.getParent.asInstanceOf[Block]
    val rewrite: ListRewrite = this.astRewrite.getListRewrite(bl, Block.STATEMENTS_PROPERTY)
    if (insertAfter) rewrite.insertAfter(toAdd, currentNode, null)
    else rewrite.insertBefore(toAdd, currentNode, null)
  }
}
