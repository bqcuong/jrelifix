package net.bqc.jrelifix.search

import net.bqc.jrelifix.identifier.SeedIdentifier

trait ISeedCondition {
  def satisfied(seed: SeedIdentifier): Boolean
}
