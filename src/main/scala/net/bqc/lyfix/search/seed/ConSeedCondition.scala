package net.bqc.lyfix.search.seed

import net.bqc.lyfix.identifier.seed.{ExpressionSeedIdentifier, Seedy}

case class ConSeedCondition() extends ISeedCondition {

  override def satisfied(seed: Seedy): Boolean = {
    seed match {
      case i: ExpressionSeedIdentifier => i.isBool()
      case _ => false
    }
  }
}
