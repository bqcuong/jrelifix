package net.bqc.lyfix.engine

import java.io.File
import java.util.Objects

import net.bqc.lyfix.context.compiler.ICompiler
import net.bqc.lyfix.context.mutation.MutationType
import net.bqc.lyfix.context.validation.TestCase
import net.bqc.lyfix.context.{EngineContext, ProjectData}
import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.identifier.fault.Faulty
import net.bqc.lyfix.identifier.seed.{AssignmentDecoratorSeedIdentifier, AssignmentSeedIdentifier, Seedy}
import net.bqc.lyfix.search.Searcher
import net.bqc.lyfix.search.seed.{ConSeedForEngineCondition, ISeedCondition, StmtSeedForEngineCondition}
import net.bqc.lyfix.utils.{DiffUtils, FileFolderUtils, ShellUtils}
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.Statement

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class LyFixEngine(override val faults: ArrayBuffer[Identifier],
                       override val projectData: ProjectData,
                       override val context: EngineContext)
  extends APREngine(faults, projectData, context) {

  private val logger: Logger = Logger.getLogger(this.getClass)
  private val tabu = mutable.HashSet[Identifier]()
  private val conExprSet: mutable.HashSet[Identifier] = mutable.HashSet[Identifier]()
  private val stmtSet: mutable.HashSet[Identifier] = mutable.HashSet[Identifier]()
  private var currentFault: Identifier = null

  private def collectConditionExpressions(): mutable.HashSet[Identifier] = {
    val result = mutable.HashSet[Identifier]()
    val seedySet = Searcher.searchSeeds(projectData.seedsMap, null, ConSeedForEngineCondition())
    for (seed <- seedySet) {
      if (seed.getChangeTypes().nonEmpty) {
        seed match {
          case s: AssignmentSeedIdentifier => // convert assignment to conditional expression here
            val newJavaNode = s.generateEqualityExpression() // convert a = 0 -> a != 0
            if (newJavaNode != null) {
              val decorator = new AssignmentDecoratorSeedIdentifier(s)
              decorator.setJavaNode(newJavaNode) // update new java node for the decorator
              result.addOne(decorator)
            }
          case _ => result.addOne(seed.asInstanceOf[Identifier])
        }
      }
    }
    result
  }

  private def collectStatements(): mutable.HashSet[Identifier] = {
    val result = mutable.HashSet[Identifier]()
    val seedySet = Searcher.searchSeeds(projectData.seedsMap, null, StmtSeedForEngineCondition())
    for (seed <- seedySet) {
      if (seed.getChangeTypes().nonEmpty) {
        result.addOne(seed.asInstanceOf[Identifier])
      }
    }
    result
  }

  private def reRankFaults(faultyLocations: ArrayBuffer[Identifier]): ArrayBuffer[Identifier] = {
    val stmtFaults = faultyLocations.filter(_.getJavaNode() != null)
    for (f <- stmtFaults) {
      val isChanged = DiffUtils.isChanged(projectData.changedSourcesMap, f, 2)
      val isStmt = f.getJavaNode().isInstanceOf[Statement]
      if (isChanged && isStmt) {
        val faulty = f.asInstanceOf[Faulty]
        faulty.setSuspiciousness(faulty.getSuspiciousness() + 1.0)
      }
    }
    stmtFaults.sortWith((i1, i2) => i1.asInstanceOf[Faulty].getSuspiciousness() > i2.asInstanceOf[Faulty].getSuspiciousness())
  }

  private def rankSeeds(seeds: mutable.HashSet[Identifier]): ArrayBuffer[Identifier] = {
    val results = ArrayBuffer[Identifier]()
    for (seed <- seeds) {
      if (!tabu.contains(seed)) {
        results.addOne(seed)
      }
    }
    results
  }

  override def repair(): Unit = {
    conExprSet.addAll(collectConditionExpressions())
    stmtSet.addAll(collectStatements())
    logger.debug("Condition Expression Seed Set for Engine: " + conExprSet)
    logger.debug("Statement Seed Set for Engine: " + stmtSet)

    val PRIMARY_OPERATORS = mutable.Queue[MutationType.Value](
      MutationType.NULLCHECKER,
      MutationType.DELETE,
      MutationType.NEGATE,
      MutationType.SWAP,
      MutationType.REVERT,
      MutationType.CONVERT,
      MutationType.ADDIF,
      MutationType.ADDCON,
      MutationType.ADDSTMT,
      MutationType.ADDTRYCATCH,
      MutationType.MI,
    )

    val SECONDARY_OPERATORS = mutable.Queue[MutationType.Value](
      MutationType.NEGATE,
      MutationType.CONVERT,
      MutationType.ADDIF,
      MutationType.ADDCON)

    logger.info("Primary Operators: " + PRIMARY_OPERATORS)
    logger.info("Secondary Operators: " + SECONDARY_OPERATORS)

    // only support fix on changed-faulty statements
    val stmtFaults = reRankFaults(faults)
    logger.info("Filtered Faults:")
    stmtFaults.foreach(logger.info(_))

    val patchDiffs: ArrayBuffer[String] = ArrayBuffer[String]()

    for(fidx <- stmtFaults.indices) {
      val f = stmtFaults(fidx)
      val fLine = f.getLine()
      currentFault = f
      logger.info("[FAULT] Try: " + currentFault)
      this.tabu.clear()

      val operators = projectData.randomizer.shuffle(PRIMARY_OPERATORS)
//      val operators = PRIMARY_OPERATORS.clone()
      logger.info("[OPERATOR] Candidates: " + operators)

      while(operators.nonEmpty) {
        val nextOperator = operators.dequeue()
        logger.info("[OPERATOR] Try: " + nextOperator)

        val mutation = this.context.mutationGenerator.getMutation(currentFault, nextOperator)
        if (mutation.isParameterizable) {
          logger.info("[OPERATOR PARAM] Picking a parameter seed for parameterizable operator %s...".format(nextOperator))
          var seeds: ArrayBuffer[Identifier] = null
          if (nextOperator == MutationType.ADDSTMT) {
            seeds = rankSeeds(stmtSet)
          }
          else if (nextOperator != MutationType.ADDSTMT) {
            seeds = rankSeeds(conExprSet)
          }

          if (seeds.nonEmpty) {
            mutation.mutate(seeds)
          }
        }
        else {
          mutation.mutate(null)
        }
        var applied = mutation.getPatches().nonEmpty
        logger.info("[OPERATOR] " + nextOperator + " Able to generate patches? " + (if (applied) "\u2713" else "\u00D7"))
        var compileStatus: ICompiler.Status = null
        if (applied) {
          val patches = mutation.getPatches()
          for (i <- patches.indices) {
            val patch = patches(i)
            patch.applyEdits()
            projectData.updateChangedSourceFiles() // update document to hard disk
            logger.info("[PATCH][" + nextOperator + "] Applied patch: " + i)

            compileStatus = this.context.compiler.compile()
            logger.info("[COMPILE] Status: " + compileStatus)

            if (compileStatus == ICompiler.Status.COMPILED) {
              var reducedTSValidation = false
              if (Objects.nonNull(projectData.config().externalReducedTestCommand)) {
                reducedTSValidation = ShellUtils.execute(
                  if (Objects.nonNull(projectData.config().rootProjFolder)) projectData.config().rootProjFolder else projectData.config().projFolder,
                  projectData.config().externalReducedTestCommand)
              } else {
                reducedTSValidation = this.context.testValidator.validateReducedTestCases()._1
              }
              logger.info("[VALIDATION] REDUCED TS: " + (if (reducedTSValidation) "\u2713" else "\u00D7"))
              if (reducedTSValidation) {
                var wholeTSValidation = false
                if (Objects.nonNull(projectData.config().externalTestCommand)) {
                  wholeTSValidation = ShellUtils.execute(
                    if (Objects.nonNull(projectData.config().rootProjFolder)) projectData.config().rootProjFolder else projectData.config().projFolder,
                    projectData.config().externalTestCommand)
                }
                else {
                  wholeTSValidation = this.context.testValidator.validateAllTestCases()._1
                }

                logger.info("[VALIDATION] WHOLE TS: " + (if (wholeTSValidation) "\u2713" else "\u00D7"))
                if (wholeTSValidation) {
                  logger.info("==========================================")
                  logger.info("FOUND A REPAIR (See below patch):")
                  for (faultFile <- projectData.originalFaultFiles) {
                    val changedDocument = projectData.sourceFileContents.get(faultFile)
                    val originalSourceContent = changedDocument.document.get()
                    val patchedSourceContent = changedDocument.modifiedDocument.get()
                    val diff = DiffUtils.getDiff(originalSourceContent, patchedSourceContent,
                      faultFile.replace(projectData.config().projFolder + File.separator, ""))
                    if (!diff.trim.isEmpty) {
                      logger.info("------------------------------------------\n" + diff)
                      patchDiffs.addOne(diff)
                    }

                    val patchFolder = new File("patches" + File.separator + projectData.bugId)
                    if (!patchFolder.exists()) patchFolder.mkdirs()
                    FileFolderUtils.writeFile(patchFolder + File.separator + fLine + "_" + nextOperator + "_" + i + ".patch", diff)
                  }
                }
              }
            }
            else { // not compiled
              tabu.addAll(patch.getUsingSeeds())
            }

            patch.undoEdits()
            projectData.updateChangedSourceFiles()
          }
        }
      }
    }

    logger.info("==========================================")
    logger.info("THE REPAIR PROCESS ENDED")
    logger.info("==========================================")
    logger.info("GENERATED PATCHES:")
    for (patchDiff <- patchDiffs) {
      logger.info("------------------------------------------\n" + patchDiff)
    }
  }
}
