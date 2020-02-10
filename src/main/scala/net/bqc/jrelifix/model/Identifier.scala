package net.bqc.jrelifix.model

import net.bqc.jrelifix.utils.ASTUtils
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{ASTNode, CompilationUnit}

abstract class Identifier {

  private val logger: Logger = Logger.getLogger(this.getClass)
  protected var javaNode: ASTNode = _
  private var triedTransform: Boolean = false

  def getBeginLine(): Int
  def getEndLine(): Int
  def getLine(): Int
  def getBeginColumn(): Int
  def getEndColumn(): Int
  def getClassName(): String
  def getJavaNode(): ASTNode = javaNode

  def transformJavaNode(globalASTs: scala.collection.mutable.HashMap[String, CompilationUnit]): ASTNode = {
    if(triedTransform) return javaNode
    triedTransform = true

    if(javaNode == null) {
      val desCUnit = globalASTs.getOrElse(getClassName(), null)
      this.javaNode = ASTUtils.findNode(desCUnit, this)
    }

    this.javaNode
  }

  def sameLocation(node: ASTNode): Boolean = {
    val id = ASTUtils.createFaultIdentifierNoClassName(node)
     this.getBeginLine() == id.getBeginLine() &&
       this.getBeginColumn() == id.getBeginColumn() &&
       this.getEndLine() == id.getEndLine() &&
       this.getEndColumn() == id.getEndColumn()
  }
}