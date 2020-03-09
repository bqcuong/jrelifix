package net.bqc.jrelifix.utils

import net.bqc.jrelifix.context.diff.{ChangedFile, ChangedSnippet, ChangedType, SourceRange}
import net.bqc.jrelifix.identifier.Identifier
import org.apache.log4j.Logger

import scala.collection.mutable

object DiffUtils {

  val logger: Logger = Logger.getLogger(this.getClass)

  def getChangedSnippet(changedSourcesMap: mutable.HashMap[String, ChangedFile],
                toCheck: Identifier): ChangedSnippet = {

    val fileName = toCheck.getFileName()
    val changedFile: ChangedFile = changedSourcesMap.get(fileName).orNull
    if (changedFile == null) {
      logger.debug("Not found modified file for " + fileName)
      return null
    }

    var changed = false
    val changedSnippets = changedFile.changedSnippets
    for (cs <- changedSnippets) {
      cs.changedType match {
        case ChangedType.ADDED =>
          changed = isInRange(toCheck, cs.dstRange)
        case ChangedType.REMOVED =>
          changed = isInRange(toCheck, cs.srcRange)
        case ChangedType.MODIFIED =>
          changed = isInRange(toCheck, cs.srcRange) || isInRange(toCheck, cs.dstRange)
        case ChangedType.MOVED =>
          changed = isInRange(toCheck, cs.srcRange) || isInRange(toCheck, cs.dstRange)
      }

      if (changed) return cs
    }
    null
  }

  def isChanged(changedSourcesMap: mutable.HashMap[String, ChangedFile],
                toCheck: Identifier): Boolean = {

    val fileName = toCheck.getFileName()
    val changedFile: ChangedFile = changedSourcesMap.get(fileName).orNull
    if (changedFile == null) {
      logger.debug("Not found modified file for " + fileName)
      return false
    }

    var changed = false
    val changedSnippets = changedFile.changedSnippets
    for (cs <- changedSnippets) {
      cs.changedType match {
        case ChangedType.ADDED =>
          changed = isInRange(toCheck, cs.dstRange)
        case ChangedType.REMOVED =>
          changed = isInRange(toCheck, cs.srcRange)
        case ChangedType.MODIFIED =>
          changed = isInRange(toCheck, cs.srcRange) || isInRange(toCheck, cs.dstRange)
        case ChangedType.MOVED =>
          changed = isInRange(toCheck, cs.srcRange) || isInRange(toCheck, cs.dstRange)
      }

      if (changed) return changed
    }
    changed
  }

  def isInRange(toCheck: Identifier, range: SourceRange) : Boolean = {
    val c1 = toCheck.getBeginLine() >= range.beginLine && toCheck.getEndLine() <= range.endLine
    val c2 = toCheck.getBeginColumn() == -1 ||
      (toCheck.getBeginColumn() >= range.beginColumn && toCheck.getEndColumn() <= range.endColumn)
    c1 && c2
  }
}
