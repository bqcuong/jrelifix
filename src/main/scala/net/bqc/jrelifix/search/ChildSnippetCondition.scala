package net.bqc.jrelifix.search
import net.bqc.jrelifix.context.diff.ChangeSnippet

/**
 * Check if a changed snippet is a child of the given parent code
 * @param parentCode
 */
case class ChildSnippetCondition(parentCode: String) extends IChangeSnippetCondition {
  override def satisfied(cs: ChangeSnippet): Boolean = {
    var childCode = cs.dstSource
    if (childCode == null) {
      childCode = cs.srcSource
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
