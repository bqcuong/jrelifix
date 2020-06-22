package net.bqc.jrelifix.search.seed

import net.bqc.jrelifix.context.diff.{ChangeType, SourceRange}
import net.bqc.jrelifix.identifier.seed.{ExpressionSeedIdentifier, Seedy}
import net.bqc.jrelifix.utils.ASTUtils

case class AddedConSeedCondition(boundary: SourceRange) extends ISeedCondition {

  override def satisfied(seed: Seedy): Boolean = {
    if (!seed.containsChangeType(ChangeType.ADDED)) return false
    seed match {
      case i: ExpressionSeedIdentifier =>
        i.isBool() && ASTUtils.isInRangeForId(i, boundary)
      case _ => false
    }
  }
}
