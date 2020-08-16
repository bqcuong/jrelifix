package net.bqc.lyfix.search.cs

import net.bqc.lyfix.context.diff.{ChangeSnippet, ChangeType, SourceRange}
import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.utils.ASTUtils

/**
 * Condition for a valid added snippet
 */
case class InsideSnippetCondition(insideSourceRange: SourceRange)
  extends IChangeSnippetCondition {

  override def satisfied(cs: ChangeSnippet): Boolean = {
    var source: Identifier = null
    if (cs.changeType != ChangeType.REMOVED)
      source = cs.dstSource
    else
      source = cs.srcSource

    assert(source != null)
    ASTUtils.isInRangeForId(source, insideSourceRange)
  }
}
