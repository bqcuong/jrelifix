package net.bqc.lyfix.search.seed

import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.identifier.node.MethodInvocationIdentifier
import net.bqc.lyfix.identifier.seed.Seedy

case class BoolMethodInvocationCondition(to_before: Identifier) extends ISeedCondition {

  override def satisfied(seed: Seedy): Boolean = {
    seed match {
      case s: MethodInvocationIdentifier =>
        to_before.after(s) && s.isBool()
      case _ => false
    }
  }
}
