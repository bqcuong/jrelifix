package net.bqc.jrelifix.search
import net.bqc.jrelifix.context.diff.{ChangeType, ChangedSnippet}
import net.bqc.jrelifix.identifier.Identifier

case class AddedSnippetCondition(id: Identifier) extends IChangedSnippetCondition {

  override def satisfied(cs: ChangedSnippet): Boolean = {
    cs.changeType == ChangeType.ADDED && cs.dstSource.equals(id)
  }
}
