package net.bqc.jrelifix.engine

import java.io.File

import net.bqc.jrelifix.context.compiler.{DocumentASTRewrite, JavaJDKCompiler}
import net.bqc.jrelifix.context.mutation.MutationType
import net.bqc.jrelifix.context.{EngineContext, ProjectData}
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.identifier.fault.PredefinedFaultIdentifier
import net.bqc.jrelifix.identifier.seed.{AssignmentDecoratorSeedIdentifier, AssignmentSeedIdentifier}
import net.bqc.jrelifix.search.{ConSeedForEngineCondition, Searcher}
import net.bqc.jrelifix.utils.{ASTUtils, DiffUtils}
import org.apache.log4j.Logger

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

case class JRelifixEngine(override val faults: ArrayBuffer[Identifier],
                          override val projectData: ProjectData,
                          override val context: EngineContext)
  extends APREngine(faults, projectData, context) {

  private val logger: Logger = Logger.getLogger(this.getClass)
  private val tabu = mutable.HashSet[Identifier]()
  private val conExprSet: mutable.HashSet[Identifier] = mutable.HashSet[Identifier]()
  private var currentChosenCon: Identifier = null

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

  def chooseRandomlyExpr(): Identifier = {
    var exceed = 0
    do {
      val randDrop = Random.nextInt(conExprSet.size)
      currentChosenCon = conExprSet.drop(randDrop).head
      exceed += 1
    }
    while (exceed < 1000 && tabu.contains(currentChosenCon))
    logger.debug("[OPERATOR PARAM] Chosen Condition: " + currentChosenCon)
    currentChosenCon
  }

  override def repair(): Unit = {
    conExprSet.addAll(collectConditionExpressions())
    assert(conExprSet.nonEmpty)
    logger.debug("Condition Expression Set for Engine: " + conExprSet)

    val initialOperators = mutable.Queue[MutationType.Value](
      MutationType.REVERT
//      MutationType.DELETE, MutationType.NEGATE, MutationType.SWAP, MutationType.REVERT, MutationType.ADDIF, MutationType.ADDCON, MutationType.CONVERT
    )
    val secondaryOperators = mutable.Queue[MutationType.Value](
//      MutationType.ADDCON, MutationType.ADDIF
      MutationType.NEGATE, MutationType.ADDIF, MutationType.ADDCON, MutationType.CONVERT
    )
    logger.debug("Initial Operators: " + initialOperators)
    logger.debug("Secondary Operators: " + secondaryOperators)

    val P = 20

    val reducedTSNames: Set[String] = this.context.testValidator.predefinedNegTests.map(_.getFullName).toSet

    for(f <- faults) {
      var faultLine = f
      logger.debug("[FAULT] Try: " + faultLine)
      val faultFile = faultLine.getFileName()
      var changedCount = 0
      var iter = 0
      var secondaryDoc: DocumentASTRewrite = null
      this.tabu.clear()

      var operators = Random.shuffle(initialOperators)
      logger.debug("[OPERATOR] Candidates: " + operators)

      while(iter <= P && operators.nonEmpty) {
        val nextOperator = operators.dequeue()
        logger.debug("[OPERATOR] Try: " + nextOperator)

        val mutation = this.context.mutationGenerator.getMutation(faultLine, nextOperator, secondaryDoc)
        var applied: Boolean = false
        if (mutation.isParameterizable) {
          currentChosenCon = chooseRandomlyExpr()
        }

        applied = mutation.mutate(currentChosenCon)

        if (applied) {
          // Try to compile
          if (secondaryDoc != null) { // update new doc for compiler
            this.context.compiler.updateSourceFileContents(faultFile, secondaryDoc)
          }
          else { // set the original doc for compiler
            this.context.compiler.updateSourceFileContents(faultFile, projectData.sourceFileContents.get(faultFile))
          }

          logger.debug("==========> AFTER MUTATING")
          val compileStatus = this.context.compiler.compile()
          logger.debug("[COMPILE] Status: " + compileStatus)

          if (compileStatus == JavaJDKCompiler.Status.COMPILED) {
            val reducedTSValidation = this.context.testValidator.validateTestCases(
              this.context.testValidator.predefinedNegTests,
              projectData.config().projFolder,
              projectData.config().classpath())
            logger.debug(" ==> [VALIDATION] REDUCED TS: " + (if (reducedTSValidation._1) "\u2713" else "\u00D7"))
            if (reducedTSValidation._1) {
              val wholeTSValidation = this.context.testValidator.validateAllTestCases(projectData.config().projFolder, projectData.config().classpath())
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
              else if (secondaryDoc == null) { // introduce new regression, allow maximum 2 operators to be applied
                // start using current variant as a faulty program to be repaired
                // this secondary document will be used to be applied for secondary operators
                secondaryDoc = projectData.sourceFileContents.get(faultFile).generateModifiedDocument()
                // update new location, new java node for fault statement
                val astNode = ASTUtils.searchNodeByLineNumber(secondaryDoc.cu, faultLine.getLine())
                if (astNode != null) { // new faulty node is able to be identified in new variant
                  logger.debug("[FAULT] Update to: " + faultLine)
                  val (bl, el, bc, ec) = ASTUtils.getNodePosition(astNode, secondaryDoc.cu)
                  faultLine = PredefinedFaultIdentifier(bl, el, bc, ec, null)
                  faultLine.asInstanceOf[PredefinedFaultIdentifier].setFileName(faultFile)
                  faultLine.setJavaNode(astNode)
                  // construct secondary operator set
                  operators = Random.shuffle(secondaryOperators)
                }
                else secondaryDoc = null // could not identify new fault statement, ignore
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

          mutation.unmutate()
        }
        else if (currentChosenCon != null) {
          tabu.addOne(currentChosenCon)
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
