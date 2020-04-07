package net.bqc.jrelifix.search
import net.bqc.jrelifix.context.diff.{ChangeType, ChangeSnippet}
import net.bqc.jrelifix.identifier.Identifier

case class AddedSnippetCondition(id: Identifier) extends IChangeSnippetCondition {

  override def satisfied(cs: ChangeSnippet): Boolean = {
    cs.changeType == ChangeType.ADDED && cs.dstSource.equals(id)
  }
}
