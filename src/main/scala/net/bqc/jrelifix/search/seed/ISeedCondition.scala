package net.bqc.jrelifix.search.seed

import net.bqc.jrelifix.identifier.seed.Seedy

trait ISeedCondition {
  def satisfied(seed: Seedy): Boolean
}
