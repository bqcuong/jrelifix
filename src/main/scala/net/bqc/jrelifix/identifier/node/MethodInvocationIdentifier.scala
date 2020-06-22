package net.bqc.jrelifix.identifier.node

import org.eclipse.jdt.core.dom.ITypeBinding

class MethodInvocationIdentifier(beginLine: Int,
                                 endLine: Int,
                                 beginColumn: Int,
                                 endColumn: Int,
                                 fileName: String,
                                 val returnType: ITypeBinding)
  extends ExpressionIdentifier(beginLine, endLine, beginColumn, endColumn, fileName) {

  def getDefaultValue(): String = {
    assert(returnType != null)

    val returnTypeStr = returnType.toString
    if (returnType.isPrimitive) { // return type is a primitive type
      returnTypeStr match {
        case "byte" | "short" | "char" | "int" | "long" => "0"
        case "float" | "double" => "0.0"
        case "boolean" => "false"
        case _ => "null"
      }
    }
    else { // other type
      "null"
    }
  }

  override def isBool(): Boolean = {
    assert(returnType != null)
    val returnTypeStr = returnType.toString
    returnType.isPrimitive && returnTypeStr == "boolean" || returnTypeStr == "Boolean"
  }
}
