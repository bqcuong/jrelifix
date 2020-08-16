package net.bqc.lyfix.search.seed

import net.bqc.lyfix.identifier.node.StatementIdentifier
import net.bqc.lyfix.identifier.seed.Seedy

case class StmtSeedForEngineCondition() extends ISeedCondition {
  override def satisfied(seed: Seedy): Boolean = {
    seed match {
      case i: StatementIdentifier => true
      case _ => false
    }
  }
}
