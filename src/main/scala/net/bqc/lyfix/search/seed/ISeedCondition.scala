package net.bqc.lyfix.search.seed

import net.bqc.lyfix.identifier.seed.Seedy

trait ISeedCondition {
  def satisfied(seed: Seedy): Boolean
}
