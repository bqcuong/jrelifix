package net.bqc.jrelifix.identifier.seed

import net.bqc.jrelifix.identifier.{Identifier}
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

  /**
   * The same source code string (if javaNode exists), OR same location
   * @param obj
   * @return
   */
  override def equals(obj: Any): Boolean =
    obj match {
      case that: Identifier => {
        ((that.getJavaNode() != null && this.getJavaNode() != null && that.getJavaNode().toString.equals(this.getJavaNode().toString)) ||
          that.sameLocation(this))
      }
      case _ => false
    }

  override def hashCode(): Int = {
    if (javaNode != null) {
      javaNode.toString.hashCode + 31
    }
    else {
      locationHashCode()
    }
  }

  override def toString: String = {
    "[%s] %s".format(this.getClass.getSimpleName, javaNode.toString)
  }
}
