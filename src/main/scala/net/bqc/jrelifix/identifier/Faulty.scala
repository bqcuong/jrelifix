package net.bqc.jrelifix.identifier

trait Faulty {
  def getSuspiciousness(): Double
  def getClassName(): String
  def setFileName(fileName: String): Unit
}
