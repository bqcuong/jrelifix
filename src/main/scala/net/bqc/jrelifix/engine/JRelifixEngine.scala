package net.bqc.jrelifix.engine

import java.io.File

import net.bqc.jrelifix.context.compiler.JavaJDKCompiler
import net.bqc.jrelifix.context.mutation.MutationType
import net.bqc.jrelifix.context.{EngineContext, ProjectData}
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.identifier.seed.{AssignmentDecoratorSeedIdentifier, AssignmentSeedIdentifier}
import net.bqc.jrelifix.search.{ConSeedForEngineCondition, Searcher}
import net.bqc.jrelifix.utils.DiffUtils
import org.apache.log4j.Logger

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

case class JRelifixEngine(override val faults: ArrayBuffer[Identifier],
                          override val projectData: ProjectData,
                          override val context: EngineContext)
  extends APREngine(faults, projectData, context) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  private def collectConditionExpressions(): mutable.HashSet[Identifier] = {
    val result = mutable.HashSet[Identifier]()
    val seedySet = Searcher.searchSeeds(projectData.seedsMap, null, ConSeedForEngineCondition())
    for (seed <- seedySet) {
      seed match {
        case s: AssignmentSeedIdentifier => // convert assignment to conditional expression here
          val newJavaNode = s.generateEqualityExpression() // convert a = 0 -> a != 0
          val decorator = new AssignmentDecoratorSeedIdentifier(s)
          decorator.setJavaNode(newJavaNode) // update new java node for the decorator
          result.addOne(decorator)
        case _ => result.addOne(seed.asInstanceOf[Identifier])
      }
    }
    result
  }

  override def repair(): Unit = {
    val conExprSet = collectConditionExpressions()
    assert(conExprSet.nonEmpty)
    logger.debug("Condition Expression Set for Engine: " + conExprSet)

    val initialOperators = mutable.Queue[MutationType.Value](
      MutationType.CONVERT
//      MutationType.DELETE, MutationType.NEGATE, MutationType.SWAP, MutationType.REVERT, MutationType.ADDIF,
    )
    logger.debug("Initial Operators: " + initialOperators)


    val P = 20
    val tabu = mutable.HashSet[Identifier]()
    val reducedTSNames: Set[String] = this.context.testValidator.predefinedNegTests.map(_.getFullName).toSet

    for(faultLine <- faults) {
      logger.debug("[FAULT] Try: " + faultLine)
      var changedCount = 0
      var iter = 0

      val operators = Random.shuffle(initialOperators)
      logger.debug("[OPERATOR] Candidates: " + operators)

      while(iter <= P && operators.nonEmpty) {
        val nextOperator = operators.dequeue()
        logger.debug("[OPERATOR] Try: " + nextOperator)

        val mutation = this.context.mutationGenerator.getMutation(faultLine, nextOperator)
        var applied: Boolean = false
        var chosenCon: Identifier = null
        if (mutation.isParameterizable) {
          do {
            val randDrop = Random.nextInt(conExprSet.size)
            chosenCon = conExprSet.drop(randDrop).head
          }
          while (tabu.contains(chosenCon))

          logger.debug("[OPERATOR PARAM] Chosen Condition: " + chosenCon)
          applied = mutation.mutate(chosenCon)
        }
        else {
          applied = mutation.mutate()
        }

        if (applied) {
          // Try to compile
          val compileStatus = this.context.compiler.compile()
          logger.debug("[COMPILE] Status: " + compileStatus)
          var unmutated = true

          if (compileStatus == JavaJDKCompiler.Status.COMPILED) {
            val reducedTSValidation = this.context.testValidator.validateTestCases(this.context.testValidator.predefinedNegTests, projectData.config().classpath())
            logger.debug(" ==> [VALIDATION] REDUCED TS: " + (if (reducedTSValidation._1) "\u2713" else "\u00D7"))

            if (reducedTSValidation._1) {
              val wholeTSValidation = this.context.testValidator.validateAllTestCases(projectData.config().classpath())
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
            else {
              changedCount += 1
              iter += 1
              if (mutation.isParameterizable) {
                val newFailedTSNames: Set[String] = reducedTSValidation._2.map(_.getFullName).toSet
                if (diffResults(reducedTSNames, newFailedTSNames)) {
                  operators.enqueue(nextOperator)
                }
              }
            }
          }
          if (unmutated) projectData.resetDocuments()
        }
        else if (chosenCon != null) {
          tabu.addOne(chosenCon)
        }
      }
    }

    logger.debug("==========================================")
    logger.debug("NOT FOUND ANY REPAIR")
    logger.debug("==========================================")
  }

  private def diffResults(initialTS: Set[String], newTS: Set[String]): Boolean = {
    initialTS.equals(newTS)
  }
}
