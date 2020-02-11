package net.bqc.jrelifix.identifier

object ModifiedType extends Enumeration {
  val ADDED, REMOVED, CHANGED, MOVED = Value
}

case class ModifiedExpression(filePath: String,
                              modifiedText: String,
                              modifiedType: ModifiedType.Value,
                              beginLine: Int,
                              endLine: Int,
                              beginColumn: Int,
                              endColumn: Int)

  extends PositionBasedIdentifier(beginLine, endLine, beginColumn, endColumn, null){
}