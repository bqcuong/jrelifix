package net.bqc.jrelifix.search.seed

import net.bqc.jrelifix.identifier.node.StatementIdentifier
import net.bqc.jrelifix.identifier.seed.Seedy

case class StmtSeedForEngineCondition() extends ISeedCondition {
  override def satisfied(seed: Seedy): Boolean = {
    seed match {
      case i: StatementIdentifier => true
      case _ => false
    }
  }
}
