package net.bqc.jrelifix.identifier

import net.bqc.jrelifix.context.diff.ChangedType

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

  var changedType: ChangedType.Value = ChangedType.NONE

  /**
   * The same source code string (if javaNode exists), OR same location
   * @param obj
   * @return
   */
  override def equals(obj: Any): Boolean =
    obj match {
      case that: SeedIdentifier => {
        ((that.getJavaNode() != null && this.getJavaNode() != null && that.getJavaNode().toString.equals(this.getJavaNode().toString)) ||
          that.sameLocation(this)) &&
        that.changedType == this.changedType
      }
      case _ => false
    }

  override def hashCode(): Int = {
    if (javaNode != null) {
      javaNode.toString.hashCode + 31 * changedType.hashCode()
    }
    else {
      locationHashCode()
    }
  }

  override def toString: String = {
    "[%s] %s".format(seedType, javaNode.toString)
  }

  def setChangedType(changedType: ChangedType.Value): Unit = this.changedType = changedType
}
