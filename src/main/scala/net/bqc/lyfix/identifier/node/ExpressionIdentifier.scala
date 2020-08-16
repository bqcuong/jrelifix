package net.bqc.lyfix.identifier.node

import net.bqc.lyfix.identifier.PositionBasedIdentifier

class ExpressionIdentifier(beginLine: Int,
                           endLine: Int,
                           beginColumn: Int,
                           endColumn: Int,
                           val fileName: String)
  extends PositionBasedIdentifier(beginLine, endLine, beginColumn, endColumn) {

  protected var bool = false

  def isBool(): Boolean = bool
  def setBool(bool: Boolean): Unit = this.bool = bool
  override def getFileName(): String = this.fileName
}
