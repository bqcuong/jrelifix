package net.bqc.jrelifix.utils

import net.bqc.jrelifix.context.diff.SourceRange
import net.bqc.jrelifix.context.parser.JavaParser
import net.bqc.jrelifix.identifier.{Identifier, PositionBasedIdentifier, PredefinedFaultIdentifier, SeedIdentifier, SeedType, SimpleIdentifier}
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom._
import org.eclipse.jdt.core.dom.rewrite.{ASTRewrite, ListRewrite}
import org.eclipse.jface.text.Document

import scala.collection.mutable.ArrayBuffer



object ASTUtils {
  private val logger: Logger = Logger.getLogger(ASTUtils.getClass)

  /**
   * ============================
   * AST Manipulation
   * ============================
   */
  def replaceNode(rew: ASTRewrite, node: ASTNode, replacement: ASTNode): ASTRewrite = {
    var rep: ASTNode = null
    rep = ASTNode.copySubtree(node.getAST, replacement)
    rew.replace(node, rep, null)
    rew
  }

  def removeNode(rew: ASTRewrite, toRemoved: ASTNode): ASTRewrite = {
    this.replaceNode(rew, toRemoved, rew.getAST.createInstance(classOf[Block]))
  }

  def insertNode(rew: ASTRewrite, currentNode: ASTNode, newNode: ASTNode, insertAfter: Boolean = true): ASTRewrite = {
    if (newNode == null) throw new Exception("This should never happen")
    val to_add: ASTNode = ASTNode.copySubtree(currentNode.getAST, newNode)
    val bl: Block = currentNode.getParent.asInstanceOf[Block]
    val rewrite: ListRewrite = rew.getListRewrite(bl, Block.STATEMENTS_PROPERTY)
    if (insertAfter) rewrite.insertAfter(to_add, currentNode, null)
    else rewrite.insertBefore(to_add, currentNode, null)
    rew
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

  def createIdentifierForASTNode(node: ASTNode, fileName: String = null): PositionBasedIdentifier = {
    val cu: CompilationUnit = node.getRoot.asInstanceOf[CompilationUnit]
    val nodeLength: Int = node.getLength

    val bl: Int = cu.getLineNumber(node.getStartPosition)
    val el: Int = cu.getLineNumber(node.getStartPosition + nodeLength)
    val bc: Int = cu.getColumnNumber(node.getStartPosition) + 1
    val ec: Int = cu.getColumnNumber(node.getStartPosition + nodeLength) + 1
    val p = SimpleIdentifier(bl, el, bc, ec, fileName)
    p.setJavaNode(searchNodeByIdentifier(cu, p))
    p
  }

  def createSeedIdentifierForASTNode(node: ASTNode, seedType: SeedType.Value, fileName: String = null): SeedIdentifier = {
    val cu: CompilationUnit = node.getRoot.asInstanceOf[CompilationUnit]
    val nodeLength: Int = node.getLength

    val bl: Int = cu.getLineNumber(node.getStartPosition)
    val el: Int = cu.getLineNumber(node.getStartPosition + nodeLength)
    val bc: Int = cu.getColumnNumber(node.getStartPosition) + 1
    val ec: Int = cu.getColumnNumber(node.getStartPosition + nodeLength) + 1
    val p = SeedIdentifier(bl, el, bc, ec, seedType, fileName)
    p.setJavaNode(searchNodeByIdentifier(cu, p))
    p
  }

  def createStmtNodeFromString(toTransform: String): ASTNode = {
    val document = new Document("public class X{public void replace(){" + toTransform + "}}")
    val cu = getCuFromDocument(document)
    val visitor = new ASTVisitor() {
      var toTransNode : ASTNode = _
      override def visit(node: MethodDeclaration): Boolean = {
        val methodBody = node.getBody
        toTransNode = methodBody.statements().get(0).asInstanceOf[ASTNode]
        false
      }
    }
    cu.accept(visitor)
    visitor.toTransNode
  }

  def createExprNodeFromString(toTransform: String): ASTNode = {
    val document = new Document("public class X{public void replace(){return " + toTransform + ";}}")
    val cu = getCuFromDocument(document)
    val visitor = new ASTVisitor() {
      var toTransNode : ASTNode = _
      override def visit(node: ReturnStatement): Boolean = {
        toTransNode = node.getExpression
        false
      }
    }
    cu.accept(visitor)
    visitor.toTransNode
  }

  private def getCuFromDocument(doc: Document): CompilationUnit = {
    val astNode = JavaParser.parseAST(doc.get())
    astNode.asInstanceOf[CompilationUnit]
  }

  /**
   * Get sibling statements (only applicable inside a block of code {})
   * @param currentNode
   * @param after
   * @return
   */
  def getSiblingNode(currentNode: ASTNode, after: Boolean): ASTNode = {
    val parent = currentNode.getParent()
    if (!parent.isInstanceOf[Block]) return null
    val block = parent.asInstanceOf[Block]
    val children = block.statements()
    var prev: ASTNode = null
    var currentNodeIndex = -1
    for (i <- 0 until children.size()) {
      val child = children.get(i)
      child match {
        case childAST: ASTNode =>
          if (childAST.getStartPosition == currentNode.getStartPosition) {
            if (!after) return prev
            currentNodeIndex = i
          }
          else if (currentNodeIndex > -1 && after) {
            return childAST
          }
          else {
            prev = childAST
          }
        case _ =>
      }
    }
    null
  }

  def searchNodeByIdentifier(cu: CompilationUnit, identifier: Identifier): ASTNode = {
    if (cu == null) return null
    val find = new SearchASTNodeByIdentifier(cu, identifier)
    cu.accept(find)
    find.found
  }

  def searchNodeByLineNumber(cu: CompilationUnit, lineNumber: Int): ASTNode = {
    if (cu == null) return null
    val find = new SearchASTNodeByLineNumber(cu, lineNumber)
    cu.accept(find)
    find.found
  }

  private class SearchASTNodeByLineNumber(cu: CompilationUnit, toFind: Int) extends ASTVisitor {
    var found: ASTNode = _

    override def preVisit2(node: ASTNode): Boolean = {
      val id = ASTUtils.createFaultIdentifierNoClassName(node)
      if (id.getBeginLine() == toFind) {
        found = node
        return false
      }
      true
    }

    def getCompilationUnit: CompilationUnit = {
      this.cu
    }
  }

  private class SearchASTNodeByIdentifier(cu: CompilationUnit, toFind: Identifier) extends ASTVisitor {
    var found: ASTNode = _

    override def preVisit2(node: ASTNode): Boolean = {
      if (toFind.sameLocation(node)) {
        found = node
        return false
      }
      true
    }

    def getCompilationUnit: CompilationUnit = {
      this.cu
    }
  }

  def isInRange(toCheck: Identifier, range: SourceRange, lineDistance: Int = 0) : Boolean = {
    val c1 = toCheck.getBeginLine() >= (range.beginLine - lineDistance) && toCheck.getEndLine() <= (range.endLine + lineDistance)
    val c2 = toCheck.getBeginColumn() == -1 || range.beginColumn == -1 || range.beginLine < range.endLine ||
      (range.beginLine == range.endLine && toCheck.getBeginColumn() >= range.beginColumn && toCheck.getEndColumn() <= range.endColumn)
    c1 && c2
  }

  /**
   * Check if the given node is inside the condition of a conditional statement
   * @param node
   * @return
   */
  def belongsToConditionStatement(node: ASTNode): Boolean = {
    if (node == null) false
    else if (ASTUtils.isConditionalStatement(node)) true
    else if (node.isInstanceOf[Statement]) false
    else belongsToConditionStatement(node.getParent)
  }

  def isConditionalStatement(s: ASTNode): Boolean = {
    s match {
      case _: ForStatement => true
      case _: WhileStatement => true
      case _: IfStatement => true
      case _ => false
    }
  }

  /**
   * Extract the conditional expression from a conditional statement
   * @param conditionalStatement
   * @return
   */
  def getConditionalNode(conditionalStatement: ASTNode): ASTNode = {
    conditionalStatement match {
      case s: IfStatement => s.getExpression
      case s: WhileStatement => s.getExpression
      case s: ForStatement => s.getExpression
      case s: ConditionalExpression => s.getExpression
      case _ => null
    }
  }

  /**
   * Extract atomic bool ASTNode from a condition expression
   * @param outerNode
   * @return
   */
  def getBoolNodes(outerNode: ASTNode): ArrayBuffer[ASTNode] = {
    val result = ArrayBuffer[ASTNode]()
    outerNode match {
      case o: InfixExpression =>
        val op = o.getOperator
        if (op.equals(InfixExpression.Operator.CONDITIONAL_AND) ||
            op.equals(InfixExpression.Operator.CONDITIONAL_OR) ||
            op.equals(InfixExpression.Operator.AND) ||
            op.equals(InfixExpression.Operator.OR) ||
            op.equals(InfixExpression.Operator.XOR)) {

          result.addAll(getBoolNodes(o.getLeftOperand))
          result.addAll(getBoolNodes(o.getRightOperand))
        }
        else {
          result.addOne(outerNode)
        }
      case o: ParenthesizedExpression => result.addAll(getBoolNodes(o.getExpression))
      case _ => result.addOne(outerNode)
    }
    result
  }
}
