package net.bqc.jrelifix.search.cs

import net.bqc.jrelifix.context.diff.ChangeSnippet
import net.bqc.jrelifix.identifier.Identifier

/**
 * Check if a changed snippet is a child of the given parent code
 *
 * @param parentNode
 */
case class ChildSnippetCondition(parentNode: Identifier) extends IChangeSnippetCondition {
  override def satisfied(cs: ChangeSnippet): Boolean = {
    val parentCode = parentNode.getJavaNode().toString
    var childCode = cs.dstSource
    if (childCode != null) {
      var childNode = childCode.getJavaNode()
      while (childNode.getParent != null) {
        childNode = childNode.getParent
        if (childNode.toString.equals(parentCode))
          return true
      }
      false
    }
    else {
      // in case of removed expression inside a stmt, i.e. childCode doesn't belong to current parentCode
      childCode = cs.srcSource
      // try to match previous child expr with curr stmt by line number?
      childCode.getLine() == parentNode.getLine()
    }

  }
}
