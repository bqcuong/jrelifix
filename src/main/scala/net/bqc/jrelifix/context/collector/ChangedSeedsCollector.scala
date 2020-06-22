package net.bqc.jrelifix.context.collector

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.diff.ChangeType
import net.bqc.jrelifix.identifier.{Identifier, PositionBasedIdentifier}
import net.bqc.jrelifix.identifier.seed.{ExpressionSeedIdentifier, Seedy, StatementSeedIdentifier}
import net.bqc.jrelifix.search.cs.{ChildSnippetCondition, SameCodeSnippetCondition}
import net.bqc.jrelifix.search.Searcher
import net.bqc.jrelifix.utils.ASTUtils
import net.bqc.jrelifix.utils.ASTUtils.{getNodePosition, searchNodeByIdentifier}
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{CompilationUnit, InfixExpression, Statement}

import scala.collection.mutable

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
        var oldSeed: Seedy = null
        var newSeed: Seedy = null
        val seedSet = projectData.seedsMap(f)
        cs.changeType match {
          case ChangeType.REMOVED =>
            oldSeed = generateSeed(cs.srcSource, changedFile.oldCUnit)
            addOrUpdateSeedToSeedSet(oldSeed, seedSet, ChangeType.REMOVED)
          case ChangeType.ADDED =>
            newSeed = generateSeed(cs.dstSource, changedFile.newCUnit)
            addOrUpdateSeedToSeedSet(newSeed, seedSet, ChangeType.ADDED)
          case ChangeType.MODIFIED =>
            oldSeed = generateSeed(cs.srcSource, changedFile.oldCUnit)
            addOrUpdateSeedToSeedSet(oldSeed, seedSet, ChangeType.REMOVED)
            newSeed = generateSeed(cs.dstSource, changedFile.newCUnit)
            addOrUpdateSeedToSeedSet(newSeed, seedSet, ChangeType.MODIFIED)
          case ChangeType.MOVED =>
            newSeed = generateSeed(cs.dstSource, changedFile.newCUnit)
            addOrUpdateSeedToSeedSet(newSeed, seedSet, ChangeType.MOVED)
          case _ =>
        }

        // add more statement seed from changed expression
        val parentStmtSeed1 = generateStmtSeedFromExpr(cs.srcSource, changedFile.oldCUnit)
        val parentStmtSeed2 = generateStmtSeedFromExpr(cs.dstSource, changedFile.newCUnit)
        addOrUpdateSeedToSeedSet(parentStmtSeed1, seedSet, ChangeType.MODIFIED)
        addOrUpdateSeedToSeedSet(parentStmtSeed2, seedSet, ChangeType.MODIFIED)

      }
    }
  }

  private def generateStmtSeedFromExpr(id: Identifier, cu: CompilationUnit): Seedy = {
    if (id != null ) {
      val seedJavaNode = id.getJavaNode()
      if (!ASTUtils.containsStmt(seedJavaNode)) {
        val stmtNode = ASTUtils.getParentStmt(seedJavaNode)
        if (stmtNode != null) {
          val (bl, el, bc, ec) = getNodePosition(stmtNode, cu)
          val stmtCode = new StatementSeedIdentifier(bl, el, bc, ec, id.getFileName())
          stmtCode.setJavaNode(stmtNode)
          return stmtCode
        }
      }
    }
    null
  }

  private def addOrUpdateSeedToSeedSet(seed: Seedy, seedSet: mutable.HashSet[Identifier], changeType: ChangeType.Value): Unit = {
    if (seed != null) {
      val seedAsIdentifier = seed.asInstanceOf[Identifier]
      val seedJavaNode = seedAsIdentifier.getJavaNode()
      val seedCode = seedJavaNode.toString.trim
      var duplicated = false
      for (exSeed <- seedSet) {
        if (seedCode.equals(exSeed.getJavaNode().toString.trim)) {
          duplicated = true
          exSeed.asInstanceOf[Seedy].addChangeType(changeType)
          logger.debug("Update seed change status: [%s] %s".format(exSeed.asInstanceOf[Seedy].getChangeTypes(), exSeed.getJavaNode().toString.trim))
        }
      }
      if (!duplicated) {
        seed.addChangeType(changeType)
        seedSet.addOne(seedAsIdentifier)
        logger.debug("Additional seed from change history: [%s] %s".format(seed.getChangeTypes(), seedAsIdentifier.getJavaNode().toString.trim))
      }
    }
  }

  private def generateSeed(code: Identifier, cu: CompilationUnit): Seedy = {
    val javaNode = code.getJavaNode()
    javaNode match {
      case node: InfixExpression =>
        val op = node.getOperator
        // expression in if-statement, for-statement, while-statement
        if ((ASTUtils.isConditionalOperator(op) || ASTUtils.isEqualityOperator(op)) && ASTUtils.belongsToConditionStatement(node)) {
          val (bl, el, bc, ec) = getNodePosition(node, cu)
          val atomicBoolCode = new ExpressionSeedIdentifier(bl, el, bc, ec, code.getFileName())
          atomicBoolCode.setBool(true)
          atomicBoolCode.setJavaNode(node)
          return atomicBoolCode
        }

      case node: Statement =>
        val (bl, el, bc, ec) = getNodePosition(node, cu)
        val stmtCode = new StatementSeedIdentifier(bl, el, bc, ec, code.getFileName())
        stmtCode.setJavaNode(node)
        return stmtCode
      case _ => return null
    }
    null
  }
}