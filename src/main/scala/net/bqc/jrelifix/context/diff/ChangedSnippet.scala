package net.bqc.jrelifix.context.diff

import net.bqc.jrelifix.identifier.Identifier

import scala.collection.mutable.ArrayBuffer

object ChangedType extends Enumeration {
  val ADDED, REMOVED, MODIFIED, MOVED = Value
}

class SourceRange(val beginLine: Int, val endLine: Int, val beginColumn: Int, val endColumn: Int)

case class ChangedSnippet(srcRange: SourceRange,
                          dstRange: SourceRange,
                          sources: ArrayBuffer[Identifier],
                          modifiedType: ChangedType.Value) {

}