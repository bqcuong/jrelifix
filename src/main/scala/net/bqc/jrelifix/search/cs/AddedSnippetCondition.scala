package net.bqc.jrelifix.search.cs

import net.bqc.jrelifix.context.diff.{ChangeSnippet, ChangeType}
import net.bqc.jrelifix.identifier.Identifier

case class AddedSnippetCondition(id: Identifier) extends IChangeSnippetCondition {

  override def satisfied(cs: ChangeSnippet): Boolean = {
    cs.changeType == ChangeType.ADDED && cs.dstSource.equals(id)
  }
}
