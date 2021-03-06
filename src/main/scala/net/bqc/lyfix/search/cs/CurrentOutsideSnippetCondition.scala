package net.bqc.lyfix.search.cs

import net.bqc.lyfix.context.diff.{ChangeSnippet, ChangeType, SourceRange}
import net.bqc.lyfix.utils.ASTUtils

/**
 * Search only in current version
 * @param insideRange
 */
case class CurrentOutsideSnippetCondition(insideRange: SourceRange) extends IChangeSnippetCondition {
  override def satisfied(cs: ChangeSnippet): Boolean = {
    cs.changeType match {
      case ChangeType.ADDED | ChangeType.MOVED | ChangeType.MODIFIED =>
        val dstRange = cs.dstRange
        assert(dstRange != null)
        ASTUtils.isInRange(insideRange, dstRange)
      case _ => false
    }
  }
}
