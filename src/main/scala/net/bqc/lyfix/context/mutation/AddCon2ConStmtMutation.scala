package net.bqc.lyfix.context.mutation

import net.bqc.lyfix.context.ProjectData
import net.bqc.lyfix.context.compiler.DocumentASTRewrite
import net.bqc.lyfix.context.diff.ChangedFile
import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.search.seed.NotBelongSeedCondition
import net.bqc.lyfix.search.Searcher
import net.bqc.lyfix.search.cs.InsideSnippetCondition
import net.bqc.lyfix.utils.{ASTUtils, DiffUtils}
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{ASTNode, IfStatement, Statement, VariableDeclarationStatement}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

case class AddCon2ConStmtMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  override def isParameterizable: Boolean = true

  override def mutate(paramSeeds: ArrayBuffer[Identifier]): Boolean = {
    if (isParameterizable) assert(paramSeeds != null)
//    if (!DiffUtils.isChanged(projectData.changedSourcesMap, faultStatement)) return false
    if (!faultStatement.isConditionalStatement()) return false

    var applied = false
    val astNode = faultStatement.getJavaNode()

    applied = addConditionForConditionalStatement(astNode.asInstanceOf[IfStatement], paramSeeds)

    if (applied) {
      doMutating()
      true
    }
    else false
  }

  def addConditionForConditionalStatement(faultNode: IfStatement, insertedCons: ArrayBuffer[Identifier]): Boolean = {
    val faultFile = faultStatement.getFileName()
    val parentCon = ASTUtils.getConditionalNode(faultNode)
    val boolNodes = ASTUtils.getBoolNodes(parentCon)
    assert(boolNodes.nonEmpty)

    // assure the insertedCon does not exist in the parenCond
    var filteredInsertedCon = ArrayBuffer[Identifier]()
    val parenCode = parentCon.toString

    for (insertedCon <- insertedCons) {
      if (!parenCode.contains(insertedCon.getJavaNode().toString)) {
        filteredInsertedCon.addOne(insertedCon)
      }
    }

    if (filteredInsertedCon.isEmpty) {
      logger.debug("Could not find any satisfied condition from expr seed set to insert...")
      return false
    }

    // choose a atomic bool to be combined with new condition, changed bool expressions in history are prioritized
    var changedBoolNodes: ArrayBuffer[ASTNode] = null
    if (boolNodes.size == 1) changedBoolNodes = boolNodes

    // prioritize on changed expression first
    changedBoolNodes = boolNodes.foldLeft(ArrayBuffer[ASTNode]()) {
      (res, node) => {
        val code = ASTUtils.createIdentifierForASTNode(node, faultFile)
        val changedFile: ChangedFile = projectData.changedSourcesMap.getOrElse(faultFile, null)
        if (changedFile != null) {
          val css = Searcher.searchChangeSnippets(changedFile, InsideSnippetCondition(code.toSourceRange()))
          if (css.nonEmpty) res.addOne(node)
        }
        res
      }
    }
    if (changedBoolNodes.isEmpty) changedBoolNodes = boolNodes

    for (chosenCon <- filteredInsertedCon) {
      logger.debug("The chosen newly condition to be added: " + chosenCon.getJavaNode().toString)
      for (changedBoolNode <- changedBoolNodes) {
        logger.debug("The chosen condition to be combined with: " + changedBoolNode)

        // create new condition node with &&
        val newCode1 = "(%s %s %s)".format(changedBoolNode, "&&", chosenCon.getJavaNode().toString)
        val newNode1 = ASTUtils.createExprNodeFromString(newCode1)
        logger.debug("Combining condition: " + newCode1)

        // create new condition node with ||
        val newCode2 = "(%s %s %s)".format(changedBoolNode, "||", chosenCon.getJavaNode().toString)
        val newNode2 = ASTUtils.createExprNodeFromString(newCode2)
        logger.debug("Combining condition: " + newCode2)

        val patch1 = new Patch(this.document)
        val replaceAction1 = ASTActionFactory.generateReplaceAction(changedBoolNode, newNode1)
        patch1.addAction(replaceAction1)
        patch1.addUsingSeed(chosenCon)
        addPatch(patch1)

        val patch2 = new Patch(this.document)
        val replaceAction2 = ASTActionFactory.generateReplaceAction(changedBoolNode, newNode2)
        patch2.addAction(replaceAction2)
        patch2.addUsingSeed(chosenCon)
        addPatch(patch2)
      }
    }
    true
  }

  override def applicable(): Boolean = ???
}
