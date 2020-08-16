package net.bqc.lyfix.identifier.node

import net.bqc.lyfix.utils.ASTUtils
import org.eclipse.jdt.core.dom.{ASTNode, Expression}

class AssignmentIdentifier(beginLine: Int,
                           endLine: Int,
                           beginColumn: Int,
                           endColumn: Int,
                           fileName: String,
                           val lhs: Expression,
                           val rhs: Expression)
  extends ExpressionIdentifier(beginLine, endLine, beginColumn, endColumn, fileName) {

}
