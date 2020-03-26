package net.bqc.jrelifix.search

import net.bqc.jrelifix.context.diff.{ChangedFile, ChangedSnippet}
import net.bqc.jrelifix.identifier.{Identifier, SeedIdentifier}

import scala.collection.mutable
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

  def searchSeeds(seedMap: mutable.Map[String, mutable.HashSet[Identifier]], filePath: String, condition: ISeedCondition)
  : mutable.HashSet[SeedIdentifier] = {
    var seedSet: mutable.HashSet[Identifier] = seedMap.get(filePath).orNull
    // requested file not found in seed map, try to find seeds in all possible files of the map
    if (seedSet == null) {
      seedSet = seedMap.values.foldLeft(new mutable.HashSet[Identifier]()) {
        (res, seed) => {
          res.addAll(seed)
          res
        }
      }
    }

    val result = mutable.HashSet[SeedIdentifier]()
    for (s <- seedSet) {
      s match {
        case seed: SeedIdentifier =>
          if (condition.satisfied(seed)) result.addOne(seed)
      }
    }
    result
  }
}
