package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.search.{AddedConSeedCondition, Searcher}
import net.bqc.jrelifix.utils.{ASTUtils, DiffUtils}
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.ASTNode

import scala.util.Random

case class NegateMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)
  private var replacedCon: ASTNode = _
  private var negatedCon: ASTNode = _

  override def mutate(conditionExpr: Identifier = null): Boolean = {
    if (isParameterizable) assert(conditionExpr != null)
    // only applicable to conditional statement
    if (!faultStatement.isConditionalStatement()) return false
    // check if faulty statement is changed
    if (!DiffUtils.isChanged(projectData.changedSourcesMap, faultStatement)) return false

    val faultFile = faultStatement.getFileName()
    val addedAtomicCodes = Searcher.searchSeeds(projectData.seedsMap, faultFile,
      AddedConSeedCondition(faultStatement.toSourceRange()))

    if (addedAtomicCodes.isEmpty) {
      logger.error("Not found any added atomic conditions in the fault statement! Give up...")
      return false
    }
    else {
      logger.debug("List of added conditions: " + addedAtomicCodes)
    }
    // if there are many added condition, try to randomly choose one
    val randomTaken = Random.nextInt(addedAtomicCodes.size) + 1
    val chosenCode = addedAtomicCodes.takeRight(randomTaken).head.asInstanceOf[Identifier]
    this.replacedCon = ASTUtils.searchNodeByIdentifier(document.cu, chosenCode)
    logger.debug("Chosen code to negate: " + chosenCode.getJavaNode())
    this.negatedCon = getNegatedNode(chosenCode)
    logger.debug("Negated code: " + negatedCon.toString)

    ASTUtils.replaceNode(this.document.rewriter, this.replacedCon, negatedCon)
    doMutating()
    true
  }

  private def getNegatedNode(boolCode: Identifier): ASTNode = {
    val source = boolCode.getJavaNode().toString
    val negatedStr: String = "!(%s)".format(source)
    ASTUtils.createExprNodeFromString(negatedStr)
  }

  override def applicable(): Boolean = ???

  override def isParameterizable: Boolean = false
}
