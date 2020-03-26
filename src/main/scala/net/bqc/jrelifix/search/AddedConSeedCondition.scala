package net.bqc.jrelifix.search
import net.bqc.jrelifix.context.diff.{ChangedType, SourceRange}
import net.bqc.jrelifix.identifier.{SeedIdentifier, SeedType}
import net.bqc.jrelifix.utils.{ASTUtils, DiffUtils}

case class AddedConSeedCondition(boundary: SourceRange) extends ISeedCondition {

  override def satisfied(seed: SeedIdentifier): Boolean = {
    if (seed.changedType != ChangedType.ADDED) return false
    if (seed.seedType != SeedType.CONDITION) return false
    ASTUtils.isInRange(seed, boundary)
  }
}
