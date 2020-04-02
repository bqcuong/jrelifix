package net.bqc.jrelifix.search

import net.bqc.jrelifix.identifier.{SeedIdentifier, SeedType}

import scala.collection.mutable

case class NotEqualingConSeedCondition(notEqualing: mutable.HashSet[String]) extends ISeedCondition {

  override def satisfied(seed: SeedIdentifier): Boolean = {
    seed.seedType == SeedType.CONDITION && !notEqualing.contains(seed.getJavaNode().toString)
  }
}
