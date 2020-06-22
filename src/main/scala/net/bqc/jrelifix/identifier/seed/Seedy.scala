package net.bqc.jrelifix.identifier.seed

import net.bqc.jrelifix.context.diff.ChangeType
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
    "[%s] %s".format(i.getClass.getSimpleName, i.getJavaNode().toString.trim)
  }
}

trait Seedy {
  protected val changeTypes: mutable.HashSet[ChangeType.Value] = new mutable.HashSet[ChangeType.Value]()

  def addChangeType(changeType: ChangeType.Value): Unit = {
    this.changeTypes.add(changeType)
  }

  def addChangeTypes(changeTypes: mutable.HashSet[ChangeType.Value]): Unit = {
    this.changeTypes.addAll(changeTypes)
  }

  def getChangeTypes(): mutable.HashSet[ChangeType.Value] = this.changeTypes

  def containsChangeType(changeType: ChangeType.Value): Boolean = {
    this.changeTypes.contains(changeType)
  }

  def equals(obj: Any): Boolean
  def hashCode(): Int
}
