package net.bqc.lyfix.search.seed

import net.bqc.lyfix.identifier.seed.{ExpressionSeedIdentifier, Seedy}

import scala.collection.mutable

case class NotEqualingConSeedCondition(notEqualing: mutable.HashSet[String]) extends ISeedCondition {

  override def satisfied(seed: Seedy): Boolean = {
    seed match {
      case i: ExpressionSeedIdentifier =>
        i.isBool() && !notEqualing.contains(i.getJavaNode().toString)
      case _ => false
    }
  }
}
