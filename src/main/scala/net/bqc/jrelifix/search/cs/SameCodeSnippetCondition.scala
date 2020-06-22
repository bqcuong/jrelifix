package net.bqc.jrelifix.search.cs

import net.bqc.jrelifix.context.diff.ChangeSnippet

case class SameCodeSnippetCondition(exactlyCode: String) extends IChangeSnippetCondition {
  override def satisfied(cs: ChangeSnippet): Boolean = {
    val srcCode = cs.srcSource
    val dstCode = cs.dstSource
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
