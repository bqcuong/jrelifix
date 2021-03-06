package net.bqc.lyfix.search

import net.bqc.lyfix.context.diff.{ChangeSnippet, ChangeType, ChangedFile}
import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.identifier.seed.Seedy
import net.bqc.lyfix.search.cs.IChangeSnippetCondition
import net.bqc.lyfix.search.seed.ISeedCondition
import org.apache.log4j.Logger

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object Searcher {

  private val logger: Logger = Logger.getLogger(this.getClass)

/*  def search1RandomSeed(allSeeds: mutable.HashSet[Identifier], condition: ISeedCondition)
  : Identifier = {
    val chosenSeeds = mutable.HashSet[Identifier]()
    for (seed <- allSeeds) {
      if (condition.satisfied(seed.asInstanceOf[Seedy]))
        chosenSeeds.add(seed)
    }
    if (chosenSeeds.nonEmpty) {
      val randIndex = Random.nextInt(chosenSeeds.size)
      chosenSeeds.iterator.drop(randIndex).next()
    }
    else null
  }*/

  def searchSeeds(seedMap: mutable.Map[String, mutable.HashSet[Identifier]], filePath: String, condition: ISeedCondition)
  : mutable.HashSet[Seedy] = {
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

    val result = mutable.HashSet[Seedy]()
    for (s <- seedSet) {
      s match {
        case seed: Seedy =>
          if (condition.satisfied(seed)) result.addOne(seed)
      }
    }
    result
  }

  /**
   * Find more removed change snippets which has the same mapping parent as found added change snippets
   * @param changedFile
   * @param condition
   * @return
   */
  def searchChangeSnippets2(changedFile: ChangedFile, condition: IChangeSnippetCondition)
  : ArrayBuffer[ChangeSnippet] = {
    val result = ArrayBuffer[ChangeSnippet]()
    val css = changedFile.allCS
    val parentMappingIds = mutable.HashSet[String]() // very complex meaning

    val remainingCss = ArrayBuffer[ChangeSnippet]()
    for (cs <- css) {
      if (condition.satisfied(cs)) {
        result.addOne(cs)
        if (cs.changeType == ChangeType.ADDED && cs.mappingParentId != null) {
          parentMappingIds.addOne(cs.mappingParentId)
        }
      }
      else remainingCss.addOne(cs)
    }

    // add more snippets in previous version that have the same parent mapping with chosen css
    for (cs <- remainingCss) {
      val parentId = cs.mappingParentId
      if (cs.changeType == ChangeType.REMOVED && parentId != null && parentMappingIds.contains(parentId)) {
        result.addOne(cs)
        logger.debug("Got more change snippet: " + cs)
      }
    }

    result
  }

  def searchChangeSnippets(changedFile: ChangedFile, condition: IChangeSnippetCondition, onlyRoot: Boolean = false)
    : ArrayBuffer[ChangeSnippet] = {
    val result = ArrayBuffer[ChangeSnippet]()
    val css = if (onlyRoot) changedFile.rootCS else changedFile.allCS
    for (cs <- css) {
      if (condition.satisfied(cs)) {
        result.addOne(cs)
      }
    }
    result
  }
}
