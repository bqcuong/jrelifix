package net.bqc.jrelifix.search

import net.bqc.jrelifix.identifier.{SeedIdentifier, SeedType}

case class ConSeedCondition() extends ISeedCondition {

  override def satisfied(seed: SeedIdentifier): Boolean = {
    seed.seedType == SeedType.CONDITION
  }
}
