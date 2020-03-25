package net.bqc.jrelifix.identifier

import net.bqc.jrelifix.context.diff.ChangedType

case class HistoricalIdentifier(beginLine: Int,
                                endLine: Int,
                                beginColumn: Int,
                                endColumn: Int,
                                fileName: String,
                                changeType: ChangedType.Value)
  extends PositionBasedIdentifier(beginLine, endLine, beginColumn, endColumn) {
}
