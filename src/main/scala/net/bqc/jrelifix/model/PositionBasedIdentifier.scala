package net.bqc.jrelifix.model


class PositionBasedIdentifier(beginLine: Int,
                              endLine: Int,
                              beginColumn: Int,
                              endColumn: Int,
                              className: String) extends Identifier {

  override def getBeginLine(): Int = beginLine

  override def getEndLine(): Int = endLine

  override def getLine(): Int = beginLine

  override def getBeginColumn(): Int = beginColumn

  override def getEndColumn(): Int = endColumn

  override def getClassName(): String = className
}