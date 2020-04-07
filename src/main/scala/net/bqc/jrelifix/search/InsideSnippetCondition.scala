package net.bqc.jrelifix.search

import net.bqc.jrelifix.context.diff.{ChangeSnippet, ChangeType, SourceRange}
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.ASTUtils

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
    ASTUtils.isInRange(source, insideSourceRange)
  }
}
