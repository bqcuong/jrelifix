package net.bqc.jrelifix.search

import net.bqc.jrelifix.context.diff.{ChangeType, ChangeSnippet, SourceRange}
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.ASTUtils

/**
 * Condition for a valid added snippet
 */
case class ExactlySnippetCondition(to_check: Identifier)
  extends IChangeSnippetCondition {

  override def satisfied(cs: ChangeSnippet): Boolean = {
    var source: Identifier = null
    if (cs.changeType != ChangeType.REMOVED)
      source = cs.dstSource
    else
      source = cs.srcSource

    assert(source != null)
    source.equals(to_check)
  }
}
