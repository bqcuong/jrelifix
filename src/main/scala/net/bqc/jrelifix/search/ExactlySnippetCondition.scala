package net.bqc.jrelifix.search
import net.bqc.jrelifix.context.diff.{ChangedSnippet, SourceRange}

class ExactlySnippetCondition(exactlyCode: String) extends IChangedSnippetCondition {
  override def satisfied(changedSnippet: ChangedSnippet): Boolean = {
    val srcCode = changedSnippet.srcSource
    val dstCode = changedSnippet.dstSource
    if (dstCode != null) {
      return dstCode.getJavaNode().toString.equals(exactlyCode)
    }
    else return srcCode.getJavaNode().toString.equals(exactlyCode)
  }
}
