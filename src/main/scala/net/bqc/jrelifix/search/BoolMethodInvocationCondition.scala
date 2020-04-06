package net.bqc.jrelifix.search

import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.identifier.node.MethodInvocationIdentifier
import net.bqc.jrelifix.identifier.seed.Seedy

case class BoolMethodInvocationCondition(to_before: Identifier) extends ISeedCondition {

  override def satisfied(seed: Seedy): Boolean = {
    seed match {
      case s: MethodInvocationIdentifier =>
        to_before.after(s) && s.isBool()
      case _ => false
    }
  }
}
