package net.bqc.jrelifix.identifier.node

import org.eclipse.jdt.core.dom.Expression

class AssignmentIdentifier(beginLine: Int,
                           endLine: Int,
                           beginColumn: Int,
                           endColumn: Int,
                           val lhs: Expression,
                           val rhs: Expression)
  extends ExpressionIdentifier(beginLine, endLine, beginColumn, endColumn) {

}
