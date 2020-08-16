package net.bqc.lyfix.identifier.fault

import net.bqc.lyfix.identifier.PositionBasedIdentifier

case class PredefinedFaultIdentifier(beginLine: Int,
                                     endLine: Int,
                                     beginColumn: Int,
                                     endColumn: Int,
                                     className: String,
                                     var suspiciousness: Double = 1.0f)

  extends PositionBasedIdentifier(beginLine, endLine, beginColumn, endColumn) with Faulty {

  var fileName: String = _

  override def getSuspiciousness(): Double = suspiciousness

  override def getFileName(): String = fileName

  override def getClassName(): String = className

  override def setFileName(fileName: String): Unit = this.fileName = fileName

  override def toString: String = {
    super.toString + " -> " + this.javaNode.toString.trim
  }

  override def setSuspiciousness(sus: Double): Unit = {
    this.suspiciousness = sus
  }
}
