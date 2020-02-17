package net.bqc.jrelifix.identifier

import net.bqc.jrelifix.utils.ASTUtils
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{ASTNode, Assignment, BooleanLiteral, CharacterLiteral, CompilationUnit, ConditionalExpression, InfixExpression, Name, NullLiteral, NumberLiteral, PostfixExpression, PrefixExpression, StringLiteral, TypeLiteral}

abstract class Identifier {

  private val logger: Logger = Logger.getLogger(this.getClass)
  protected var javaNode: ASTNode = _
  private var triedTransform: Boolean = false

  def getBeginLine(): Int
  def getEndLine(): Int
  def getLine(): Int
  def getBeginColumn(): Int
  def getEndColumn(): Int
  def getClassName(): String

  def getJavaNode(): ASTNode = javaNode
  def setJavaNode(javaNode: ASTNode): Unit = this.javaNode = javaNode

  def sameLocation(node: ASTNode): Boolean = {
    val id = ASTUtils.createFaultIdentifierNoClassName(node)
     this.getBeginLine() == id.getBeginLine() &&
       this.getBeginColumn() == id.getBeginColumn() &&
       this.getEndLine() == id.getEndLine() &&
       this.getEndColumn() == id.getEndColumn()
  }

  def getAstType(): ASTType.Value = {
    if (javaNode == null) return ASTType.NONE

    javaNode match {
      case _: Name => ASTType.NAME

      case _: CharacterLiteral => ASTType.LITERAL
      case _: BooleanLiteral => ASTType.LITERAL
      case _: TypeLiteral => ASTType.LITERAL
      case _: NumberLiteral => ASTType.LITERAL
      case _: NullLiteral => ASTType.LITERAL
      case _: StringLiteral => ASTType.LITERAL

      case _: Assignment => ASTType.ASSIGNMENT

      case _: InfixExpression => ASTType.STAR_FIX_EXPRESSION
      case _: PrefixExpression => ASTType.STAR_FIX_EXPRESSION
      case _: PostfixExpression => ASTType.STAR_FIX_EXPRESSION

      case _: ConditionalExpression => ASTType.CONDITIONAL_EXPRESSION

      case _ => ASTType.NONE
    }
  }
}