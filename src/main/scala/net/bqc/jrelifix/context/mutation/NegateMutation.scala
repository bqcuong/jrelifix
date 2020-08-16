package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.search.Searcher
import net.bqc.jrelifix.search.seed.AddedConSeedCondition
import net.bqc.jrelifix.utils.{ASTUtils, DiffUtils}
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.ASTNode

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

case class NegateMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  override def mutate(paramSeeds: ArrayBuffer[Identifier]): Boolean = {
    if (isParameterizable) assert(paramSeeds != null)
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

    for (chosenCon <- addedAtomicCodes) {
      val chosenCode = chosenCon.asInstanceOf[Identifier]
      val replacedCon = ASTUtils.searchNodeByIdentifier(document.cu, chosenCode)
      logger.debug("Chosen code to negate: " + chosenCode.getJavaNode())
      val negatedCon = getNegatedNode(chosenCode)
      logger.debug("Negated code: " + negatedCon.toString)

      val negateAction = ASTActionFactory.generateReplaceAction(replacedCon, negatedCon)
      val patch = new Patch(document)
      patch.addAction(negateAction)
      addPatch(patch)
    }
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
