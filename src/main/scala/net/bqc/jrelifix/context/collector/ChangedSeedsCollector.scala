package net.bqc.jrelifix.context.collector

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.diff.ChangeType
import net.bqc.jrelifix.identifier.PositionBasedIdentifier
import net.bqc.jrelifix.identifier.seed.Seedy
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
          val seed = s.asInstanceOf[Seedy]
          val seedAsIdentifier = s.asInstanceOf[PositionBasedIdentifier]

          // find the changed snippet that exactly equals to seed
          var changedRes = Searcher.searchChangedSnippets(changedFile, ExactlySnippetCondition(seedAsIdentifier.getJavaNode().toString))
          var alreadySet = false
          if (changedRes.nonEmpty) {
            // prioritize on update as ADDED over other change operations (if many occur on the same code)
            for (s <- changedRes) {
              if (s.changeType == ChangeType.ADDED) {
                seed.addChangeType(s.changeType)
                alreadySet = true
              }
            }
            if (!alreadySet)
            {
              val snippet = changedRes(0)
              seed.addChangeType(snippet.changeType)
            }
            alreadySet = true
          }
          else {
            // try to check if there are any changed snippets inside this seed
            changedRes = Searcher.searchChangedSnippets(changedFile, InsideSnippetCondition(seedAsIdentifier.getJavaNode().toString))
            if (changedRes.nonEmpty) {
              seed.addChangeType(ChangeType.MODIFIED)
              alreadySet = true
            }
          }
          if (alreadySet) logger.debug("Update seeds change status: [%s] %s".format(seed.getChangeTypes(), seedAsIdentifier.getJavaNode().toString))
        }
      }
    }
    projectData
  }


}