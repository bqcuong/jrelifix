package net.bqc.jrelifix.model

class LineBasedIdentifier(lineNumber: Int, className: String) extends Identifier {

  override def getBeginLine(): Int = lineNumber

  override def getEndLine(): Int = lineNumber

  override def getLine(): Int = lineNumber

  override def getBeginColumn(): Int = -1

  override def getEndColumn(): Int = -1

  override def getClassName(): String = className
}
