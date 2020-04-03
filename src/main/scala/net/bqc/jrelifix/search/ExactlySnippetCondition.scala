package net.bqc.jrelifix.search
import net.bqc.jrelifix.context.diff.ChangedSnippet

case class ExactlySnippetCondition(exactlyCode: String) extends IChangedSnippetCondition {
  override def satisfied(changedSnippet: ChangedSnippet): Boolean = {
    val srcCode = changedSnippet.srcSource
    val dstCode = changedSnippet.dstSource
    var code: String = null
    if (dstCode != null) {
      code = dstCode.getJavaNode().toString.trim
    }
    else {
      code = srcCode.getJavaNode().toString.trim
    }

    if (code.equals(exactlyCode)) return true
    code.endsWith(";") && code.dropRight(1).equals(exactlyCode)
  }
}
