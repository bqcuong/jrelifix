package net.bqc.lyfix.identifier.fault

trait Faulty {
  def getSuspiciousness(): Double
  def setSuspiciousness(sus: Double): Unit
  def getClassName(): String
  def setFileName(fileName: String): Unit
}
