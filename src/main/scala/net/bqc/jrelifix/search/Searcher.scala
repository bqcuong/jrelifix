package net.bqc.jrelifix.search

import net.bqc.jrelifix.context.diff.{ChangedFile, ChangedSnippet}
import net.bqc.jrelifix.identifier.{Identifier, SeedIdentifier}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object Searcher {

  def search1RandomSeed(allSeeds: mutable.HashSet[Identifier], condition: ISeedCondition)
  : Identifier = {
    val chosenSeeds = mutable.HashSet[Identifier]()
    for (seed <- allSeeds) {
      if (condition.satisfied(seed.asInstanceOf[SeedIdentifier]))
        chosenSeeds.add(seed)
    }
    if (chosenSeeds.nonEmpty) {
      val randIndex = Random.nextInt(chosenSeeds.size)
      chosenSeeds.iterator.drop(randIndex).next()
    }
    else null
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
