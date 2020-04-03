package net.bqc.jrelifix.identifier.seed

import net.bqc.jrelifix.identifier.node.AssignmentIdentifier
import org.eclipse.jdt.core.dom.Expression

class AssignmentSeedIdentifier(beginLine: Int,
                               endLine: Int,
                               beginColumn: Int,
                               endColumn: Int,
                               lhs: Expression,
                               rhs: Expression)
  extends AssignmentIdentifier(beginLine, endLine, beginColumn, endColumn, lhs, rhs)
  with Seedy {

  override def equals(obj: Any): Boolean = Seedy._equals(this, obj)
  override def hashCode(): Int = Seedy._hashCode(this)
  override def toString: String = Seedy._toString(this)

}
