package net.bqc.jrelifix.search

import net.bqc.jrelifix.identifier.seed.{ExpressionSeedIdentifier, Seedy}

case class ConSeedCondition() extends ISeedCondition {

  override def satisfied(seed: Seedy): Boolean = {
    seed match {
      case i: ExpressionSeedIdentifier => i.isBool()
      case _ => false
    }
  }
}
