package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.search.AddedSnippetCondition
import net.bqc.jrelifix.utils.{ASTUtils, DiffUtils}
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.ASTNode

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

case class NegateMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  override def mutate(): Unit = {
    // only applicable to conditional statement
    if (!faultStatement.isConditionalStatement()) return
    // check if faulty statement is changed
    if (!DiffUtils.isChanged(projectData.changedSourcesMap, faultStatement)) return

    val condition = ASTUtils.getConditionalNode(faultStatement.getJavaNode())
    if (condition == null) return

    // TODO: Gum tree output the wrong node for insert action, propose fix to gum tree lib
    val atomicBools = ASTUtils.getBoolNodes(condition)
    val addedAtomicCodes = ArrayBuffer[Identifier]()
    for (atomicBool <- atomicBools) {
      val atomicBoolCode = ASTUtils.createIdentifierForASTNode(atomicBool)
      val addedAtomicBool = DiffUtils.searchChangedSnippets(
        projectData.changedSourcesMap,
        AddedSnippetCondition(atomicBoolCode),
        faultStatement.getFileName())

      if (addedAtomicBool.nonEmpty) {
        addedAtomicCodes.addOne(atomicBoolCode)
      }
    }

    if (addedAtomicCodes.isEmpty) {
      logger.error("Not found any added atomic conditions in the fault statement! Give up...")
      return
    }
    else {
      logger.debug("List of added conditions: " + addedAtomicCodes)
    }
    // if there are many added condition, try to randomly choose one
    val randomIndex = Random.nextInt(addedAtomicCodes.length)
    val chosenCode = addedAtomicCodes(randomIndex)
    val chosenNodeOnDocument = ASTUtils.searchNodeByIdentifier(document.cu, chosenCode)
    logger.debug("Chosen code to negate: " + chosenCode.getJavaNode())
    val negatedCode = getNegatedNode(chosenCode)
    logger.debug("Negated code: " + negatedCode.toString)

    ASTUtils.replaceNode(this.document.rewriter, chosenNodeOnDocument, negatedCode)
    doMutating()
  }

  private def getNegatedNode(boolCode: Identifier): ASTNode = {
    val source = boolCode.getJavaNode().toString
    val negatedStr: String = "!(%s)".format(source)
    ASTUtils.createNodeFromString(negatedStr)
  }

  override def unmutate(): Unit = ???

  override def applicable(): Boolean = ???
}
