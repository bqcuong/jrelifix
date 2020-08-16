package net.bqc.lyfix.search.seed

import net.bqc.lyfix.identifier.seed.{AssignmentSeedIdentifier, ExpressionSeedIdentifier, MethodInvocationSeedIdentifier, Seedy}

case class ConSeedForEngineCondition() extends ISeedCondition {
  override def satisfied(seed: Seedy): Boolean = {
    seed match {
      case i: ExpressionSeedIdentifier => i.isBool()
      case i: MethodInvocationSeedIdentifier => i.isBool()
      case i: AssignmentSeedIdentifier => true
      case _ => false
    }
  }
}
