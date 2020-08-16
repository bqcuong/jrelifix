package net.bqc.lyfix.identifier.seed

import net.bqc.lyfix.identifier.node.VariableIdentifier
import org.eclipse.jdt.core.dom.ASTNode

class VariableSeedIdentifier(beginLine: Int,
                            endLine: Int,
                            beginColumn: Int,
                            endColumn: Int,
                            fileName: String,
                            declType: ASTNode,
                            initializer: ASTNode)
  extends VariableIdentifier(beginLine, endLine, beginColumn, endColumn, fileName, declType, initializer)
  with Seedy {

  override def equals(obj: Any): Boolean = Seedy._equals(this, obj)
  override def hashCode(): Int = Seedy._hashCode(this)
  override def toString: String = Seedy._toString(this)
}
