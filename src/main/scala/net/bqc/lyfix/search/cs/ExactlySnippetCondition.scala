package net.bqc.lyfix.search.cs

import net.bqc.lyfix.context.diff.{ChangeSnippet, ChangeType}
import net.bqc.lyfix.identifier.Identifier

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
