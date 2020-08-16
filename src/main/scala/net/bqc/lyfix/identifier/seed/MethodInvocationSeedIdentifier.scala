package net.bqc.lyfix.identifier.seed

import net.bqc.lyfix.identifier.node.MethodInvocationIdentifier
import org.eclipse.jdt.core.dom.ITypeBinding

class MethodInvocationSeedIdentifier(beginLine: Int,
                                     endLine: Int,
                                     beginColumn: Int,
                                     endColumn: Int,
                                     fileName: String,
                                     returnType: ITypeBinding)
  extends MethodInvocationIdentifier(beginLine, endLine, beginColumn, endColumn, fileName, returnType)
  with Seedy {

  override def equals(obj: Any): Boolean = Seedy._equals(this, obj)
  override def hashCode(): Int = Seedy._hashCode(this)
  override def toString: String = Seedy._toString(this)
}
