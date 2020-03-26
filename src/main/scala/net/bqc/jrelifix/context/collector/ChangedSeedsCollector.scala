package net.bqc.jrelifix.context.collector

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.diff.ChangedType
import net.bqc.jrelifix.identifier.SeedIdentifier
import net.bqc.jrelifix.search.{ExactlySnippetCondition, Searcher}
import org.apache.log4j.Logger

import scala.collection.mutable

case class ChangedSeedsCollector(projectData: ProjectData) extends Collector(projectData){
  private val logger: Logger = Logger.getLogger(this.getClass)

  override def collect(): ProjectData = {
    val seedFiles = projectData.seedsMap.keys
    for(f <- seedFiles) {
      val seedCodes = projectData.seedsMap(f)
      val changedFile = projectData.changedSourcesMap.get(f).orNull
      if (changedFile != null) {
        for (s <- seedCodes) {
          val seed = s.asInstanceOf[SeedIdentifier]
          val changedRes = Searcher.searchChangedSnippets(changedFile,
            new ExactlySnippetCondition(seed.getJavaNode().toString))
          if (changedRes.nonEmpty) {
            // prioritize on update as ADDED over other change operations (if many occur on the same code)
            var alreadySet = false
            for (s <- changedRes) {
              if (s.changedType == ChangedType.ADDED) {
                seed.setChangedType(s.changedType)
                alreadySet = true
              }
            }

            if (!alreadySet)
            {
              val snippet = changedRes(0)
              seed.setChangedType(snippet.changedType)
            }
            logger.debug("Update seeds change status: [%s] %s".format(seed.changedType, seed.getJavaNode().toString))
          }
        }
      }
    }
    projectData
  }


}