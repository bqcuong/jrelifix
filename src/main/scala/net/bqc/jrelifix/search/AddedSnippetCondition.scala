package net.bqc.jrelifix.search
import net.bqc.jrelifix.context.diff.{ChangedSnippet, ChangedType, SourceRange}
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.DiffUtils

/**
 * Search for an added ASTNode which contains or equals a given node
 */
case class AddedSnippetCondition(containOrEqualCode: Identifier)
  extends IChangedSnippetCondition {

  override def satisfied(changedSnippet: ChangedSnippet): Boolean = {
    if (changedSnippet.changedType != ChangedType.ADDED) return false
    val dstSource = changedSnippet.dstSource
    assert(dstSource != null)
    DiffUtils.isInRange(containOrEqualCode, dstSource.toSourceRange())
  }
}
