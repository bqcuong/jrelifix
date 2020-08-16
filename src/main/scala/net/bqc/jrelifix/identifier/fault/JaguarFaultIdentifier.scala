package net.bqc.jrelifix.identifier.fault

import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.ASTUtils
import org.eclipse.jdt.core.dom.ASTNode

case class JaguarFaultIdentifier(lineNumber: Int,
                                 className: String,
                                 var suspiciousness: Double)

  extends Identifier with Faulty {

  var fileName: String = _

  def getSuspiciousness(): Double = suspiciousness

  def setSuspiciousness(sus: Double): Unit = {
    this.suspiciousness = sus
  }

  override def getBeginLine(): Int = lineNumber

  override def getEndLine(): Int = lineNumber

  override def getLine(): Int = lineNumber

  override def getBeginColumn(): Int = -1

  override def getEndColumn(): Int = -1

  override def getFileName(): String = fileName

  override def getClassName(): String = className

  override def setFileName(fileName: String): Unit = this.fileName = fileName

  override def sameLocation(node: ASTNode): Boolean = {
    val id = ASTUtils.createFaultIdentifierNoClassName(node)
    this.getLine() == id.getBeginLine()
  }
}
