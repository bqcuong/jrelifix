package net.bqc.jrelifix.identifier

case class JaguarFaultIdentifier(lineNumber: Int,
                                 className: String,
                                 suspiciousness: Double)

  extends Identifier with Faulty {

  var fileName: String = _

  def getSuspiciousness(): Double = suspiciousness

  override def getBeginLine(): Int = lineNumber

  override def getEndLine(): Int = lineNumber

  override def getLine(): Int = lineNumber

  override def getBeginColumn(): Int = -1

  override def getEndColumn(): Int = -1

  override def getFileName(): String = fileName

  override def getClassName(): String = className

  override def setFileName(fileName: String): Unit = this.fileName = fileName
}
