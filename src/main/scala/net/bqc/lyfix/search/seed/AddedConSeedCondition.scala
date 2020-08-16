package net.bqc.lyfix.search.seed

import net.bqc.lyfix.context.diff.{ChangeType, SourceRange}
import net.bqc.lyfix.identifier.seed.{ExpressionSeedIdentifier, Seedy}
import net.bqc.lyfix.utils.ASTUtils

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
