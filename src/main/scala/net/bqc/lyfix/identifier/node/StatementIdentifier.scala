package net.bqc.lyfix.identifier.node

import net.bqc.lyfix.identifier.PositionBasedIdentifier

class StatementIdentifier(beginLine: Int,
                          endLine: Int,
                          beginColumn: Int,
                          endColumn: Int,
                          val fileName: String)

  extends PositionBasedIdentifier(beginLine, endLine, beginColumn, endColumn) {
  override def getFileName(): String = this.fileName
}
