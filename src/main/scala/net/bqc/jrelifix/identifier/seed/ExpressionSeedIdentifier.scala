package net.bqc.jrelifix.identifier.seed

import net.bqc.jrelifix.identifier.node.ExpressionIdentifier

class ExpressionSeedIdentifier(beginLine: Int,
                               endLine: Int,
                               beginColumn: Int,
                               endColumn: Int)
  extends ExpressionIdentifier(beginLine, endLine, beginColumn, endColumn)
  with Seedy {

  override def equals(obj: Any): Boolean = Seedy._equals(this, obj)
  override def hashCode(): Int = Seedy._hashCode(this)
  override def toString: String = Seedy._toString(this)
}
