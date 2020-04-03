package net.bqc.jrelifix.search

import net.bqc.jrelifix.context.diff.{ChangedSnippet, ChangedType, SourceRange}
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.ASTUtils

/**
 * Condition for a valid added snippet
 */
case class ChangedSnippetCondition(insideSourceRange: SourceRange)
  extends IChangedSnippetCondition {

  override def satisfied(changedSnippet: ChangedSnippet): Boolean = {
    var source: Identifier = null
    if (changedSnippet.changedType != ChangedType.REMOVED)
      source = changedSnippet.dstSource
    else
      source = changedSnippet.srcSource

    assert(source != null)
    ASTUtils.isInRange(source, insideSourceRange)
  }
}
