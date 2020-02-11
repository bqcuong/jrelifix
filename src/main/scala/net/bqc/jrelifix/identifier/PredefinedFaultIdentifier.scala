package net.bqc.jrelifix.identifier

case class PredefinedFaultIdentifier(beginLine: Int,
                                     endLine: Int,
                                     beginColumn: Int,
                                     endColumn: Int,
                                     className: String,
                                     suspiciousness: Double = 1.0f)
  extends PositionBasedIdentifier(beginLine, endLine, beginColumn, endColumn, className) with Faulty {

  override def getSuspiciousness(): Double = suspiciousness
}
