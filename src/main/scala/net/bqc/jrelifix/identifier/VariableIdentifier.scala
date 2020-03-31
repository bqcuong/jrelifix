package net.bqc.jrelifix.identifier

import org.eclipse.jdt.core.dom.{ASTNode, Type}

/**
 * Just use to store information about variable node, declaration type, and initializer
 */
class VariableIdentifier(beginLine: Int,
                         endLine: Int,
                         beginColumn: Int,
                         endColumn: Int,
                         declType: ASTNode,
                         initializer: ASTNode)

  extends PositionBasedIdentifier(beginLine, endLine, beginColumn, endColumn) {

  def getDefaultValue(): String = {
    declType match {
      case value: Type => null
    }
  }
}
