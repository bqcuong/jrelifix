package net.bqc.jrelifix.context.diff

import net.bqc.jrelifix.identifier.Identifier

object ChangedType extends Enumeration {
  val ADDED, REMOVED, MODIFIED, MOVED = Value
}

class SourceRange(val beginLine: Int, val endLine: Int, val beginColumn: Int, val endColumn: Int)

case class ChangedSnippet(srcRange: SourceRange,
                          dstRange: SourceRange,
                          srcSource: Identifier,
                          dstSource: Identifier,
                          changedType: ChangedType.Value) {

}