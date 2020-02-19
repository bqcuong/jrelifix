package net.bqc.jrelifix.identifier

case class SimpleIdentifier (beginLine: Int, endLine: Int,
                        beginColumn: Int, endColumn: Int,
                       fileName: String)
  extends PositionBasedIdentifier(beginLine, endLine, beginColumn, endColumn) {

  override def getFileName(): String = fileName
}
