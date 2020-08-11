package net.bqc.jrelifix.engine

import java.io.File

import net.bqc.jrelifix.context.compiler.ICompiler
import net.bqc.jrelifix.context.mutation.MutationType
import net.bqc.jrelifix.context.validation.TestCase
import net.bqc.jrelifix.context.{EngineContext, ProjectData}
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.identifier.fault.Faulty
import net.bqc.jrelifix.identifier.seed.{AssignmentDecoratorSeedIdentifier, AssignmentSeedIdentifier, Seedy}
import net.bqc.jrelifix.search.Searcher
import net.bqc.jrelifix.search.seed.{ConSeedForEngineCondition, ISeedCondition, StmtSeedForEngineCondition}
import net.bqc.jrelifix.utils.DiffUtils
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
  private var currentChosenCon: Identifier = null
  private var currentChosenStmt: Identifier = null
  private var currentFault: Identifier = null

  private def collectConditionExpressions(): mutable.HashSet[Identifier] = {
    val result = mutable.HashSet[Identifier]()
    val seedySet = Searcher.searchSeeds(projectData.seedsMap, null, ConSeedForEngineCondition())
    for (seed <- seedySet) {
      if (seed.getChangeTypes().nonEmpty) {
        seed match {
          case s: AssignmentSeedIdentifier => // convert assignment to conditional expression here
            val newJavaNode = s.generateEqualityExpression() // convert a = 0 -> a != 0
            val decorator = new AssignmentDecoratorSeedIdentifier(s)
            decorator.setJavaNode(newJavaNode) // update new java node for the decorator
            result.addOne(decorator)
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

  def chooseRandomlyExpr(condition: ISeedCondition = null): Identifier = {
    val faultFile = currentFault.getFileName()
    val conExprSet = this.conExprSet.filter(_.getFileName().equals(faultFile))
    chooseRandomlySeed(conExprSet, condition)
  }

  def chooseRandomlyStmt(): Identifier = {
    val faultFile = currentFault.getFileName()
    val stmtSet = this.stmtSet.filter(_.getFileName().equals(faultFile))
    chooseRandomlySeed(stmtSet)
  }

  def chooseRandomlySeed(seedSet: mutable.HashSet[Identifier], condition: ISeedCondition = null): Identifier = {
    val filteredSet = mutable.HashSet[Identifier]()
    if (condition != null) {
      filteredSet.addAll(seedSet.filter(s => condition.satisfied(s.asInstanceOf[Seedy])))
    }
    else {
      filteredSet.addAll(seedSet)
    }

    var chosenSeed: Identifier = null
    if (filteredSet.isEmpty) {
      logger.debug("Seed Set is empty!")
      chosenSeed = null
    }
    else {
      var exceed = 0
      do {
        val randDrop = projectData.randomizer.nextInt(filteredSet.size)
        chosenSeed = filteredSet.drop(randDrop).head
        exceed += 1
      }
      while (exceed < 1000 && tabu.contains(chosenSeed))
      logger.debug("[OPERATOR PARAM] Chosen Parameter Seed: " + chosenSeed)
    }
    chosenSeed
  }

  private def filterFault(faults: ArrayBuffer[Identifier]): ArrayBuffer[Identifier] = {
    val filteredList = ArrayBuffer[Identifier]()
    for (f <- faults) {
      val isChanged = DiffUtils.isChanged(projectData.changedSourcesMap, f, 2)
      val isStmt = f.getJavaNode().isInstanceOf[Statement]
      if (isChanged && isStmt) {
        filteredList.addOne(f)
      }
    }
    filteredList
  }

  private def filterFaultFileScope(faults: ArrayBuffer[Identifier]): ArrayBuffer[Identifier] = {
    val filteredList = ArrayBuffer[Identifier]()
    for (f <- faults) {
      val isChanged = projectData.changedSourcesMap.contains(f.getFileName())
      val isStmt = f.getJavaNode().isInstanceOf[Statement]
      if (f.asInstanceOf[Faulty].getSuspiciousness() > 0.5f && isChanged && isStmt) {
        filteredList.addOne(f)
      }
    }
    filteredList
  }

  override def repair(): Unit = {
    conExprSet.addAll(collectConditionExpressions())
    stmtSet.addAll(collectStatements())
    logger.debug("Condition Expression Seed Set for Engine: " + conExprSet)
    logger.debug("Statement Seed Set for Engine: " + stmtSet)

    val PRIMARY_OPERATORS = mutable.Queue[MutationType.Value](
      MutationType.DELETE,
      MutationType.NEGATE,
      MutationType.SWAP,
      MutationType.REVERT,
      MutationType.CONVERT,
      MutationType.ADDIF,
      MutationType.ADDCON,
      MutationType.ADDSTMT,
      MutationType.ADDTRYCATCH,
//      MutationType.MI,
      )

    val SECONDARY_OPERATORS = mutable.Queue[MutationType.Value](
      MutationType.NEGATE,
      MutationType.CONVERT,
      MutationType.ADDIF,
      MutationType.ADDCON)

    logger.debug("Primary Operators: " + PRIMARY_OPERATORS)
    logger.debug("Secondary Operators: " + SECONDARY_OPERATORS)

    val P = 20
    var iter = 0

    val reducedTSNames: Set[String] = this.context.testValidator.predefinedTests.map(_.getFullName).toSet

    // only support fix on changed-faulty statements
    val stmtFaults = filterFault(faults)
    logger.debug("Filtered Faults:")
    stmtFaults.foreach(logger.info(_))

    for(f <- stmtFaults) {
      currentFault = f
      logger.debug("[FAULT] Try: " + currentFault)
      val faultFile = currentFault.getFileName()
      var changedCount = 0
      this.tabu.clear()

      val operators = projectData.randomizer.shuffle(PRIMARY_OPERATORS)
      logger.debug("[OPERATOR] Candidates: " + operators)

      while(iter <= P && operators.nonEmpty) {
        val nextOperator = operators.dequeue()
        logger.debug("[OPERATOR] Try: " + nextOperator)

        val mutation = this.context.mutationGenerator.getMutation(currentFault, nextOperator)
        var applied: Boolean = false
        if (mutation.isParameterizable) {
          logger.debug("[OPERATOR PARAM] Picking a parameter seed for parameterizable operator %s...".format(nextOperator))
          if (nextOperator == MutationType.ADDSTMT) currentChosenStmt = chooseRandomlyStmt()
          else currentChosenCon = chooseRandomlyExpr()

          if (nextOperator == MutationType.ADDSTMT && currentChosenStmt != null) {
            applied = mutation.mutate(currentChosenStmt)
          }
          else if (nextOperator != MutationType.ADDSTMT && currentChosenCon != null) {
            applied = mutation.mutate(currentChosenCon)
          }
          else {
            applied = false
          }
        }
        else {
          applied = mutation.mutate(null)
        }

        logger.debug("[OPERATOR] Applied: " + (if (applied) "\u2713" else "\u00D7"))
        var reducedTSValidation: (Boolean, ArrayBuffer[TestCase]) = null
        var compileStatus: ICompiler.Status = null
        if (applied) {
          logger.debug("==========> AFTER MUTATING")
          projectData.updateChangedSourceFiles()
          compileStatus = this.context.compiler.compile()
          logger.debug("[COMPILE] Status: " + compileStatus)

          if (compileStatus == ICompiler.Status.COMPILED) {
            reducedTSValidation = this.context.testValidator.validateReducedTestCases()
            logger.debug(" ==> [VALIDATION] REDUCED TS: " + (if (reducedTSValidation._1) "\u2713" else "\u00D7"))
            if (reducedTSValidation._1) {
//              val wholeTSValidation = this.context.testValidator.validateAllTestCases()
              val wholeTSValidation = (true, ArrayBuffer[TestCase]())
              logger.debug("==> [VALIDATION] WHOLE TS: " + (if (wholeTSValidation._1) "\u2713" else "\u00D7"))
              if (wholeTSValidation._1) {
                logger.debug("==========================================")
                logger.debug("FOUND A REPAIR (See below patch):")
                for (faultFile <- projectData.originalFaultFiles) {
                  val changedDocument = projectData.sourceFileContents.get(faultFile)
                  val originalSourceContent = changedDocument.document.get()
                  val patchedSourceContent = changedDocument.modifiedDocument.get()
                  val diff = DiffUtils.getDiff(
                    originalSourceContent,
                    patchedSourceContent,
                    faultFile.replace(projectData.config().projFolder + File.separator, ""))
                  if (!diff.trim.isEmpty) {
                    logger.debug("------------------------------------------\n" + diff)
                  }
                }
                logger.debug("==========================================")
                return
              }
            }
          }

          mutation.unmutate()
          projectData.updateChangedSourceFiles()
        }

        if (applied && compileStatus == ICompiler.Status.COMPILED && !reducedTSValidation._1) {
          changedCount += 1
          iter += 1
          if (mutation.isParameterizable) {
            val newFailedTSNames: Set[String] = reducedTSValidation._2.map(_.getFullName).toSet
            if (diffResults(reducedTSNames, newFailedTSNames)) {
              logger.debug("[OPERATOR] Reuse: " + nextOperator)
              operators.enqueue(nextOperator)
            }
          }
        }
        else if (!applied || (applied && compileStatus != ICompiler.Status.COMPILED)) {
          if (currentChosenCon != null) {
            tabu.addOne(currentChosenCon)
          }
        }
      }
    }

    logger.debug("==========================================")
    logger.debug("NOT FOUND ANY REPAIR")
    logger.debug("==========================================")
  }

  private def diffResults(initialTS: Set[String], newTS: Set[String]): Boolean = {
    !initialTS.equals(newTS)
  }
}