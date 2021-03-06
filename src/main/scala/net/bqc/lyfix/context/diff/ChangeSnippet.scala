package net.bqc.lyfix.context.diff

import net.bqc.lyfix.identifier.Identifier

object ChangeType extends Enumeration {
  val NONE, ADDED, REMOVED, MODIFIED, MOVED = Value
}

class SourceRange(val beginLine: Int, val endLine: Int, val beginColumn: Int, val endColumn: Int) {
  override def equals(obj: Any): Boolean =
    obj match {
      case that: SourceRange =>
        that.beginLine == this.beginLine && that.endLine == this.endLine &&
        that.beginColumn == this.beginColumn && that.endColumn == this.endColumn
  }
}

case class ChangeSnippet(srcRange: SourceRange,
                         dstRange: SourceRange,
                         srcSource: Identifier,
                         dstSource: Identifier,
                         changeType: ChangeType.Value,
                         mappingParentId: String) {

}