package net.bqc.lyfix.search.cs

import net.bqc.lyfix.context.diff.{ChangeSnippet, ChangeType}
import net.bqc.lyfix.identifier.Identifier

case class AddedSnippetCondition(id: Identifier) extends IChangeSnippetCondition {

  override def satisfied(cs: ChangeSnippet): Boolean = {
    cs.changeType == ChangeType.ADDED && cs.dstSource.equals(id)
  }
}
