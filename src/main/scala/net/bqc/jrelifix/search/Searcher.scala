package net.bqc.jrelifix.search

import net.bqc.jrelifix.context.diff.{ChangedFile, ChangedSnippet}

import scala.collection.mutable.ArrayBuffer

object Searcher {

  def searchChangedSnippets(changedFile: ChangedFile, condition: IChangedSnippetCondition)
    : ArrayBuffer[ChangedSnippet] = {
    val result = ArrayBuffer[ChangedSnippet]()
    val changedSnippets = changedFile.allCS
    for (cs <- changedSnippets) {
      if (condition.satisfied(cs)) result.addOne(cs)
    }
    result
  }
}
