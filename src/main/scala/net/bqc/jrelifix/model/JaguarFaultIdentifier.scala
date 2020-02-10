package net.bqc.jrelifix.model

case class JaguarFaultIdentifier(lineNumber: Int,
                                 className: String,
                                 suspiciousness: Double)
  extends LineBasedIdentifier(lineNumber, className) with Faulty {

  override def getSuspiciousness(): Double = suspiciousness
}
