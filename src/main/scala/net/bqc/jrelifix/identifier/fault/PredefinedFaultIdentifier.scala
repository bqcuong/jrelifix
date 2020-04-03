package net.bqc.jrelifix.identifier.fault

import net.bqc.jrelifix.identifier.PositionBasedIdentifier

case class PredefinedFaultIdentifier(beginLine: Int,
                                     endLine: Int,
                                     beginColumn: Int,
                                     endColumn: Int,
                                     className: String,
                                     suspiciousness: Double = 1.0f)

  extends PositionBasedIdentifier(beginLine, endLine, beginColumn, endColumn) with Faulty {

  var fileName: String = _

  override def getSuspiciousness(): Double = suspiciousness

  override def getFileName(): String = fileName

  override def getClassName(): String = className

  override def setFileName(fileName: String): Unit = this.fileName = fileName
}
