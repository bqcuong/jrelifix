package net.bqc.lyfix.identifier.seed

import net.bqc.lyfix.identifier.node.StatementIdentifier


class StatementSeedIdentifier(beginLine: Int,
                              endLine: Int,
                              beginColumn: Int,
                              endColumn: Int,
                              fileName: String)
extends StatementIdentifier(beginLine, endLine, beginColumn, endColumn, fileName)
with Seedy {

  override def equals(obj: Any): Boolean = Seedy._equals(this, obj)
  override def hashCode(): Int = Seedy._hashCode(this)
  override def toString: String = Seedy._toString(this)
}