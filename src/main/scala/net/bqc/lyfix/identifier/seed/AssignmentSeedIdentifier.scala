package net.bqc.lyfix.identifier.seed

import net.bqc.lyfix.identifier.node.AssignmentIdentifier
import net.bqc.lyfix.utils.ASTUtils
import org.eclipse.jdt.core.dom.{ASTNode, Expression}

class AssignmentSeedIdentifier(beginLine: Int,
                               endLine: Int,
                               beginColumn: Int,
                               endColumn: Int,
                               fileName: String,
                               lhs: Expression,
                               rhs: Expression)
  extends AssignmentIdentifier(beginLine, endLine, beginColumn, endColumn, fileName, lhs, rhs)
  with Seedy {

  def generateEqualityExpression(): ASTNode = {
    val javaNodeAsStr = this.javaNode.toString
    ASTUtils.createExprNodeFromString(javaNodeAsStr.replace("=", "!="))
  }

  override def equals(obj: Any): Boolean = Seedy._equals(this, obj)
  override def hashCode(): Int = Seedy._hashCode(this)
  override def toString: String = Seedy._toString(this)
}
