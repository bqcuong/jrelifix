package net.bqc.jrelifix.identifier.seed

import net.bqc.jrelifix.context.diff.ChangedType

import scala.collection.mutable

object SeedType extends Enumeration {
  val VARIABLE, STATEMENT, EXPRESSION = Value
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
