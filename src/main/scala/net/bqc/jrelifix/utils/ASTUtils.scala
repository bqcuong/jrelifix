package net.bqc.jrelifix.utils

import net.bqc.jrelifix.identifier.{Identifier, PositionBasedIdentifier, PredefinedFaultIdentifier}
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom._
import org.eclipse.jdt.core.dom.rewrite.{ASTRewrite, ListRewrite}
import org.eclipse.jface.text.Document



object ASTUtils {
  private val logger: Logger = Logger.getLogger(ASTUtils.getClass)

  /**
   * ============================
   * AST Manipulation
   * ============================
   */
  def replaceNode(rew: ASTRewrite, node: ASTNode, replacement: ASTNode): ASTRewrite = {
    var rep: ASTNode = null
    rep = ASTNode.copySubtree(node.getAST(), replacement)
    rew.replace(node, rep, null)
    rew
  }

  def removeNode(rewriter: ASTRewrite, toRemoved: ASTNode): ASTRewrite = {
    this.replaceNode(rewriter, toRemoved, rewriter.getAST.createInstance(classOf[Block]))
  }

  def insertNode(rewriter: ASTRewrite, currentNode: ASTNode, newNode: ASTNode, insertAfter: Boolean = true): ASTRewrite = {
    if (newNode == null) throw new Exception("This should never happen")
    val to_add: ASTNode = ASTNode.copySubtree(currentNode.getAST, newNode)
    val bl: Block = currentNode.getParent.asInstanceOf[Block]
    val rewrite: ListRewrite = rewriter.getListRewrite(bl, Block.STATEMENTS_PROPERTY)
    if (insertAfter) rewrite.insertAfter(to_add, currentNode, null)
    else rewrite.insertBefore(to_add, currentNode, null)
    rewriter
  }

  /**
   * ============================
   * AST Creators
   * ============================
   */
  def createFaultIdentifierNoClassName(node: ASTNode): PositionBasedIdentifier = {
    val cu: CompilationUnit = node.getRoot.asInstanceOf[CompilationUnit]
    val nodeLength: Int = node.getLength

    val bl: Int = cu.getLineNumber(node.getStartPosition)
    val el: Int = cu.getLineNumber(node.getStartPosition + nodeLength)
    val bc: Int = cu.getColumnNumber(node.getStartPosition) + 1
    val ec: Int = cu.getColumnNumber(node.getStartPosition + nodeLength) + 1
    PredefinedFaultIdentifier(bl, el, bc, ec, null)
  }

  def createNodeFromString(toRep: String): ASTNode = {
    // Note that we need to create stub code for JDT to parse appropriately and then get back
    // the JDT ASTNode we want to create from the return statement
    val document = new Document("public class X{ public void replace(){return "+toRep+";}}")
    val parser = ASTParser.newParser(8)
    parser.setSource(document.get().toCharArray)
    val cu = parser.createAST(null).asInstanceOf[CompilationUnit]
    val visitor = new ASTVisitor() {
      var toRepNode : ASTNode = _
      override def visit(node: ReturnStatement): Boolean = {
        toRepNode = node.getExpression
        false
      }
    }
    cu.accept(visitor)
    visitor.toRepNode
  }

//  def getExpansionParentNode(astNode: ASTNode): ASTNode = {
//    while (astNode.isInstanceOf[Statement])
//  }

  def findNode(cu: CompilationUnit, to_find: Identifier): ASTNode = {
    if (cu == null) return null
    val find = new FindASTNodeForIdentifier(cu, to_find)
    cu.accept(find)
    find.found
  }

  private class FindASTNodeForIdentifier(cu: CompilationUnit, to_find: Identifier) extends ASTVisitor {
    var found: ASTNode = _

    override def preVisit2(node: ASTNode): Boolean = {
      if (to_find.sameLocation(node)) {
        found = node
        return false
      }
      true
    }

    def getCompilationUnit: CompilationUnit = {
      this.cu
    }
  }

  /**
   * ============================
   * AST Filters
   * ============================
   */
  def isConditionalStatement(astNode: ASTNode): Boolean = {
    astNode.isInstanceOf[IfStatement] ||
    astNode.isInstanceOf[WhileStatement] ||
    astNode.isInstanceOf[ForStatement]
  }
}
