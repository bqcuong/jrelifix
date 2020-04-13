package net.bqc.jrelifix.context.collector

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.diff.ChangeType
import net.bqc.jrelifix.identifier.{Identifier, PositionBasedIdentifier}
import net.bqc.jrelifix.identifier.seed.{ExpressionSeedIdentifier, Seedy}
import net.bqc.jrelifix.search.{ChildSnippetCondition, SameCodeSnippetCondition, Searcher}
import net.bqc.jrelifix.utils.ASTUtils
import net.bqc.jrelifix.utils.ASTUtils.{getNodePosition, searchNodeByIdentifier}
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{CompilationUnit, InfixExpression}

case class ChangedSeedsCollector(projectData: ProjectData) extends Collector(projectData){
  private val logger: Logger = Logger.getLogger(this.getClass)

  override def collect(): ProjectData = {
    updateChangeStatus4ExistingSeeds()
    collectAdditionalSeedsFromChangeHistory()
    projectData
  }

  private def updateChangeStatus4ExistingSeeds(): Unit = {
    val seedFiles = projectData.seedsMap.keys
    for(f <- seedFiles) {
      val seedCodes = projectData.seedsMap(f)
      val changedFile = projectData.changedSourcesMap.get(f).orNull
      if (changedFile != null) {
        for (s <- seedCodes) {
          val seed = s.asInstanceOf[Seedy]
          val seedAsIdentifier = s.asInstanceOf[PositionBasedIdentifier]
          if (seedAsIdentifier.getJavaNode().toString.trim == "channel != null") {
            println("z")
          }

          // find the changed snippet that exactly equals to seed
          var changedRes = Searcher.searchChangeSnippets(changedFile, SameCodeSnippetCondition(seedAsIdentifier.getJavaNode().toString))
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
            changedRes = Searcher.searchChangeSnippets(changedFile, ChildSnippetCondition(seedAsIdentifier.getJavaNode().toString))
            if (changedRes.nonEmpty) {
              seed.addChangeType(ChangeType.MODIFIED)
              alreadySet = true
            }
          }
          if (alreadySet) logger.debug("Update seed change status: [%s] %s".format(seed.getChangeTypes(), seedAsIdentifier.getJavaNode().toString))
        }
      }
    }
  }

  private def collectAdditionalSeedsFromChangeHistory(): Unit = {
    for (f <- projectData.changedSourcesMap.keys) {
      val changedFile = projectData.changedSourcesMap(f)
      for (cs <- changedFile.allCS) {
        var seed: Seedy = null
        cs.changeType match {
          case ChangeType.REMOVED | ChangeType.MODIFIED =>
            seed = generateSeed(cs.srcSource, changedFile.oldCUnit)
          case _ =>
        }

        if (seed != null) {
          val seedAsIdentifier = seed.asInstanceOf[Identifier]
          val seedCode = seedAsIdentifier.getJavaNode().toString.trim
          var duplicated = false
          for (exSeed <- projectData.seedsMap(f)) {
            if (seedCode.equals(exSeed.getJavaNode().toString.trim)) {
              duplicated = true
              exSeed.asInstanceOf[Seedy].addChangeType(ChangeType.REMOVED)
              logger.debug("Update seed change status: [%s] %s".format(exSeed.asInstanceOf[Seedy].getChangeTypes(), exSeed.getJavaNode().toString))
            }
          }
          if (!duplicated) {
            seed.addChangeType(ChangeType.REMOVED)
            projectData.seedsMap(f).addOne(seedAsIdentifier)
            logger.debug("Additional seed from change history: [%s] %s".format(seed.getChangeTypes(), seedAsIdentifier.getJavaNode().toString))
          }
        }
      }
    }
  }

  private def generateSeed(prevCode: Identifier, prevCu: CompilationUnit): Seedy = {
    val javaNode = prevCode.getJavaNode()
    javaNode match {
      case node: InfixExpression =>
        val op = node.getOperator
        // expression in if-statement, for-statement, while-statement
        if ((ASTUtils.isConditionalOperator(op) || ASTUtils.isEqualityOperator(op)) && ASTUtils.belongsToConditionStatement(node)) {
          val (bl, el, bc, ec) = getNodePosition(node, prevCu)
          val atomicBoolCode = new ExpressionSeedIdentifier(bl, el, bc, ec, prevCode.getFileName())
          atomicBoolCode.setBool(true)
          atomicBoolCode.setJavaNode(searchNodeByIdentifier(prevCu, atomicBoolCode))
          return atomicBoolCode
        }

      case _ => return null
    }
    null
  }
}