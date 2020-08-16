package net.bqc.lyfix.context.vcs

import net.bqc.lyfix.context.diff.ChangedFile

import scala.collection.mutable.ArrayBuffer

abstract class VCSParser {
  def getModifiedFiles(projectPath: String, currentCommit: String, previousCommit: String): ArrayBuffer[ChangedFile]
}
