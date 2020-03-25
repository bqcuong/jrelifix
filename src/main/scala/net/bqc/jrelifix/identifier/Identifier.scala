package net.bqc.jrelifix.identifier

import net.bqc.jrelifix.context.diff.SourceRange
import net.bqc.jrelifix.utils.ASTUtils
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom._

abstract class Identifier {
  private val logger: Logger = Logger.getLogger(this.getClass)
  protected var javaNode: ASTNode = _

  def getBeginLine(): Int
  def getEndLine(): Int
  def getLine(): Int
  def getBeginColumn(): Int
  def getEndColumn(): Int
  def getFileName(): String

  def getJavaNode(): ASTNode = javaNode
  def setJavaNode(javaNode: ASTNode): Unit = this.javaNode = javaNode

  def toSourceRange(): SourceRange = {
    new SourceRange(getBeginLine(), getEndLine(), getBeginColumn(), getEndColumn())
  }

  def sameLocation(node: ASTNode): Boolean = {
    val id = ASTUtils.createFaultIdentifierNoClassName(node)
     this.getBeginLine() == id.getBeginLine() &&
       this.getBeginColumn() == id.getBeginColumn() &&
       this.getEndLine() == id.getEndLine() &&
       this.getEndColumn() == id.getEndColumn()
  }

  def isConditionalStatement(): Boolean = {
    javaNode match {
      case _: ForStatement => true
      case _: WhileStatement => true
      case _: IfStatement => true
      case _ => false
    }
  }

  def isSwappableStatement(): Boolean = {
    !isConditionalStatement() && !javaNode.isInstanceOf[ConstructorInvocation] &&
      !javaNode.isInstanceOf[ReturnStatement] && !javaNode.isInstanceOf[VariableDeclaration]
  }
}