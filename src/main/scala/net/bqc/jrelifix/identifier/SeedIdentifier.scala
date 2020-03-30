package net.bqc.jrelifix.identifier

import net.bqc.jrelifix.context.diff.ChangedType

import scala.collection.mutable

object SeedType extends Enumeration {
  val CONDITION, VARIABLE, STATEMENT, EXPRESSION = Value
}

case class SeedIdentifier(beginLine: Int,
                          endLine: Int,
                          beginColumn: Int,
                          endColumn: Int,
                          seedType: SeedType.Value,
                          fileName: String)

  extends PositionBasedIdentifier(beginLine, endLine, beginColumn, endColumn) {

  private val changedTypes: mutable.HashSet[ChangedType.Value] = new mutable.HashSet[ChangedType.Value]()

  /**
   * The same source code string (if javaNode exists), OR same location
   * @param obj
   * @return
   */
  override def equals(obj: Any): Boolean =
    obj match {
      case that: SeedIdentifier => {
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
    "[%s] %s".format(seedType, javaNode.toString)
  }

  def addChangedType(changedType: ChangedType.Value): Unit = {
    this.changedTypes.add(changedType)
  }

  def addChangedTypes(changedTypes: mutable.HashSet[ChangedType.Value]): Unit = {
    this.changedTypes.addAll(changedTypes)
  }

  def getChangedTypes(): mutable.HashSet[ChangedType.Value] = this.changedTypes

  def containsChangedType(changedType: ChangedType.Value): Boolean = {
    this.changedTypes.contains(changedType)
  }
}
