package net.bqc.lyfix.search.cs

import net.bqc.lyfix.context.diff.{ChangeSnippet, ChangeType, SourceRange}
import net.bqc.lyfix.utils.ASTUtils

/**
 * Search only in current version
 * @param insideRange
 */
case class RemovedOutsideStmtSnippetCondition(insideRange: SourceRange,
                                              mappingParentId: String,
                                              lineDistance: Int,
                                              overlapped: Boolean)
  extends IChangeSnippetCondition {
  override def satisfied(cs: ChangeSnippet): Boolean = {
    cs.changeType match {
      case ChangeType.REMOVED =>
        val srcRange = cs.srcRange
        val srcNode = cs.srcSource
        assert(srcRange != null && srcNode != null)
        srcNode.isStatement() &&
        (mappingParentId == null || cs.mappingParentId.equals(mappingParentId)) &&
        ASTUtils.isInRange(insideRange, srcRange, lineDistance, overlapped)
      case _ => false
    }
  }
}
