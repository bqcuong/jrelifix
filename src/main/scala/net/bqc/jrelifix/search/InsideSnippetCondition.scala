package net.bqc.jrelifix.search
import net.bqc.jrelifix.context.diff.ChangedSnippet

/**
 * Check if a changed snippet is a child of the given parent code
 * @param parentCode
 */
case class InsideSnippetCondition(parentCode: String) extends IChangedSnippetCondition {
  override def satisfied(changedSnippet: ChangedSnippet): Boolean = {
    var childCode = changedSnippet.dstSource
    if (childCode == null) {
      childCode = changedSnippet.srcSource
    }
    var childNode = childCode.getJavaNode()
    while (childNode.getParent != null) {
      childNode = childNode.getParent
      if (childNode.toString.equals(parentCode))
        return true
    }
    false
  }
}
