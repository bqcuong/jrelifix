package net.bqc.jrelifix.search
import net.bqc.jrelifix.context.diff.{ChangedSnippet, SourceRange}

case class ExactlySnippetCondition(exactlyCode: String) extends IChangedSnippetCondition {
  override def satisfied(changedSnippet: ChangedSnippet): Boolean = {
    val srcCode = changedSnippet.srcSource
    val dstCode = changedSnippet.dstSource
    if (dstCode != null) {
      dstCode.getJavaNode().toString.equals(exactlyCode)
    }
    else srcCode.getJavaNode().toString.equals(exactlyCode)
  }
}
