package net.bqc.lyfix.identifier.fault

import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.utils.ASTUtils
import org.eclipse.jdt.core.dom.ASTNode

case class JaguarFaultIdentifier(lineNumber: Int,
                                 className: String,
                                 var suspiciousness: Double)

  extends Identifier with Faulty {

  var fileName: String = _
  var bLine = lineNumber
  var eLine = lineNumber
  var bCol = -1
  var eCol = -1

  def getSuspiciousness(): Double = suspiciousness

  def setSuspiciousness(sus: Double): Unit = {
    this.suspiciousness = sus
  }

  def updatePosition(bl: Int, el: Int, bc: Int, ec: Int): Unit = {
    bLine = bl
    eLine = el
    bCol = bc
    eCol = ec
  }

  override def getBeginLine(): Int = bLine

  override def getEndLine(): Int = eLine

  override def getLine(): Int = lineNumber

  override def getBeginColumn(): Int = bCol

  override def getEndColumn(): Int = eCol

  override def getFileName(): String = fileName

  override def getClassName(): String = className

  override def setFileName(fileName: String): Unit = this.fileName = fileName

  override def sameLocation(node: ASTNode): Boolean = {
    val id = ASTUtils.createFaultIdentifierNoClassName(node)
    this.getLine() == id.getBeginLine()
  }
}
