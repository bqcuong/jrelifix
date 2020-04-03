package net.bqc.jrelifix.identifier.node

import net.bqc.jrelifix.identifier.PositionBasedIdentifier
import org.eclipse.jdt.core.dom._

/**
 * Just use to store information about variable node, declaration type, and initializer
 */
class VariableIdentifier(beginLine: Int,
                         endLine: Int,
                         beginColumn: Int,
                         endColumn: Int,
                         val declType: ASTNode,
                         val initializer: ASTNode)
  extends PositionBasedIdentifier(beginLine, endLine, beginColumn, endColumn) {

  def getDefaultInitializer(): String = {
    var initStr = javaNode.toString
    val defaultVal = getDefaultValue()
    if (defaultVal != null) initStr += " = " + defaultVal
    initStr
  }

  def getInitializerAssignment(): String = {
    if (initializer == null) return null
    val assignStr = "%s = %s;".format(javaNode.toString, initializer.toString)
    assignStr
  }

  def getDefaultValue(): String = {
    val typeStr = declType.toString
    declType match {
      case _: PrimitiveType =>
        typeStr match {
          case "byte" | "short" | "char" | "int" | "long" => "0"
          case "float" | "double" => "0.0"
          case "boolean" => "false"
        }
      case _: SimpleType =>
        typeStr match { // check if it is a boxed primitive type
          case "Byte" | "Short" | "Character" | "Integer" | "Long" => "0"
          case "Float" | "Double" => "0.0"
          case "Boolean" => "false"
          case _ => "null"
        }
      case _: ArrayType =>
        "new %s[%s]".format(typeStr, VariableIdentifier.MAX_DECLARATION_ELEMENT_SIZE)
      case _: ParameterizedType => // List<String> = new ArrayList<>()
        "null" // haven't supported yet
      case _ =>
        "null"
    }
  }

  override def toString: String = "%s[%s,%s]".format(javaNode.toString, declType.toString,initializer.toString)
}

object VariableIdentifier {
  val MAX_DECLARATION_ELEMENT_SIZE = 1000
}
