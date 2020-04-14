package net.bqc.jrelifix.identifier.node

import net.bqc.jrelifix.identifier.PositionBasedIdentifier

class StatementIdentifier(beginLine: Int,
                          endLine: Int,
                          beginColumn: Int,
                          endColumn: Int,
                          val fileName: String)

  extends PositionBasedIdentifier(beginLine, endLine, beginColumn, endColumn) {
  override def getFileName(): String = this.fileName
}
