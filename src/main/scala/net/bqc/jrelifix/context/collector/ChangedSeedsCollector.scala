package net.bqc.jrelifix.context.collector

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.diff.ChangedType
import net.bqc.jrelifix.identifier.SeedIdentifier
import net.bqc.jrelifix.search.{ExactlySnippetCondition, InsideSnippetCondition, Searcher}
import org.apache.log4j.Logger

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

          // find the changed snippet that exactly equals to seed
          var changedRes = Searcher.searchChangedSnippets(changedFile, ExactlySnippetCondition(seed.getJavaNode().toString))
          var alreadySet = false
          if (changedRes.nonEmpty) {
            // prioritize on update as ADDED over other change operations (if many occur on the same code)
            for (s <- changedRes) {
              if (s.changedType == ChangedType.ADDED) {
                seed.addChangedType(s.changedType)
                alreadySet = true
              }
            }
            if (!alreadySet)
            {
              val snippet = changedRes(0)
              seed.addChangedType(snippet.changedType)
            }
            alreadySet = true
          }
          else {
            // try to check if there are any changed snippets inside this seed
            changedRes = Searcher.searchChangedSnippets(changedFile, InsideSnippetCondition(seed.getJavaNode().toString))
            if (changedRes.nonEmpty) {
              seed.addChangedType(ChangedType.MODIFIED)
              alreadySet = true
            }
          }
          if (alreadySet) logger.debug("Update seeds change status: [%s] %s".format(seed.getChangedTypes(), seed.getJavaNode().toString))
        }
      }
    }
    projectData
  }


}