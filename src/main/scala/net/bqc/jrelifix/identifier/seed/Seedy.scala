package net.bqc.jrelifix.identifier.seed

import net.bqc.jrelifix.context.diff.ChangedType
import net.bqc.jrelifix.identifier.Identifier

import scala.collection.mutable

object SeedType extends Enumeration {
  val VARIABLE, STATEMENT, EXPRESSION = Value
}

object Seedy {

  /**
   * The same source code string (if javaNode exists), OR same location
   * @param obj
   * @return
   */
  def _equals(i: Identifier, obj: Any): Boolean =
    obj match {
      case that: Identifier => {
        (that.getJavaNode() != null && i.getJavaNode() != null && that.getJavaNode().toString.equals(i.getJavaNode().toString)) ||
          that.sameLocation(i)
      }
      case _ => false
    }

  def _hashCode(i: Identifier): Int = {
    if (i.getJavaNode() != null) {
      i.getJavaNode().toString.hashCode + 31
    }
    else {
      i.locationHashCode()
    }
  }

  def _toString(i: Identifier): String = {
    "[%s] %s".format(i.getClass.getSimpleName, i.getJavaNode().toString)
  }
}

trait Seedy {
  protected val changedTypes: mutable.HashSet[ChangedType.Value] = new mutable.HashSet[ChangedType.Value]()

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

  def equals(obj: Any): Boolean
  def hashCode(): Int
}
