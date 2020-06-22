package net.bqc.jrelifix.identifier.seed

import net.bqc.jrelifix.context.diff.{ChangeType, SourceRange}
import net.bqc.jrelifix.identifier.Identifier
import org.eclipse.jdt.core.dom.{ASTNode, Expression}

import scala.collection.mutable

class AssignmentDecoratorSeedIdentifier(assignment: AssignmentSeedIdentifier) 
  extends AssignmentSeedIdentifier(assignment.getBeginLine(), assignment.getEndLine(), 
                                   assignment.getBeginColumn(), assignment.getEndColumn(),
                                   assignment.getFileName(),
                                   assignment.lhs: Expression, assignment.rhs: Expression) {

  // new method behaviour for this decorator
  override def getJavaNode(): ASTNode = this.javaNode
  override def setJavaNode(javaNode: ASTNode): Unit = this.javaNode = javaNode
  override def toString: String = "[%s] %s".format(this.getClass.getSimpleName, this.getJavaNode().toString)

  override def equals(obj: Any): Boolean = assignment.equals(obj)

  override def hashCode(): Int = assignment.hashCode()

  override def getBeginLine(): Int = assignment.getBeginLine()

  override def getEndLine(): Int = assignment.getEndLine()

  override def getLine(): Int = assignment.getLine()

  override def getBeginColumn(): Int = assignment.getBeginColumn()

  override def getEndColumn(): Int = assignment.getEndColumn()

  override def getFileName(): String = assignment.getFileName()

  override val lhs: Expression = assignment.lhs
  override val rhs: Expression = assignment.rhs

  override def toSourceRange(): SourceRange = assignment.toSourceRange()

  override def sameLocation(node: ASTNode): Boolean = assignment.sameLocation(node)

  override def sameLocation(id: Identifier): Boolean = assignment.sameLocation(id)

  override def after(id: Identifier): Boolean = assignment.after(id)

  override def locationHashCode(): Int = assignment.locationHashCode()

  override def isConditionalStatement(): Boolean = assignment.isConditionalStatement()

  override def isIfStatement(): Boolean = assignment.isIfStatement()

  override def isVariableDeclarationStatement(): Boolean = assignment.isVariableDeclarationStatement()

  override def isSwappableStatement(): Boolean = assignment.isSwappableStatement()

  override def addChangeType(changeType: ChangeType.Value): Unit = assignment.addChangeType(changeType)

  override def addChangeTypes(changeTypes: mutable.HashSet[ChangeType.Value]): Unit = assignment.addChangeTypes(changeTypes)

  override def getChangeTypes(): mutable.HashSet[ChangeType.Value] = assignment.getChangeTypes()

  override def containsChangeType(changeType: ChangeType.Value): Boolean = assignment.containsChangeType(changeType)

  override def isBool(): Boolean = assignment.isBool()

  override def setBool(bool: Boolean): Unit = assignment.setBool(bool)
}
