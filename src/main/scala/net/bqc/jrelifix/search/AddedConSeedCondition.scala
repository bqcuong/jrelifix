package net.bqc.jrelifix.search
import net.bqc.jrelifix.context.diff.{ChangedType, SourceRange}
import net.bqc.jrelifix.identifier.seed.{ExpressionSeedIdentifier, Seedy}
import net.bqc.jrelifix.utils.ASTUtils

case class AddedConSeedCondition(boundary: SourceRange) extends ISeedCondition {

  override def satisfied(seed: Seedy): Boolean = {
    if (!seed.containsChangedType(ChangedType.ADDED)) return false
    seed match {
      case i: ExpressionSeedIdentifier =>
        i.isBool() && ASTUtils.isInRange(i, boundary)
      case _ => false
    }
  }
}
