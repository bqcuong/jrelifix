package net.bqc.jrelifix.identifier.seed

import net.bqc.jrelifix.identifier.node.VariableIdentifier
import org.eclipse.jdt.core.dom.ASTNode

class VariableSeedIdentifier(beginLine: Int,
                            endLine: Int,
                            beginColumn: Int,
                            endColumn: Int,
                            declType: ASTNode,
                            initializer: ASTNode)
  extends VariableIdentifier(beginLine, endLine, beginColumn, endColumn, declType, initializer)
  with Seedy {

  override def equals(obj: Any): Boolean = Seedy._equals(this, obj)
  override def hashCode(): Int = Seedy._hashCode(this)
  override def toString: String = Seedy._toString(this)
}
