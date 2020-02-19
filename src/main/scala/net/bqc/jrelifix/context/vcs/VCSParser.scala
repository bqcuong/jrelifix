package net.bqc.jrelifix.context.vcs

import net.bqc.jrelifix.context.diff.ChangedFile

import scala.collection.mutable.ArrayBuffer

abstract class VCSParser {
  def getModifiedFiles(commit: String): ArrayBuffer[ChangedFile]
}
