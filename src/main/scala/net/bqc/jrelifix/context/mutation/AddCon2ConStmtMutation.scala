package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.search.seed.NotBelongSeedCondition
import net.bqc.jrelifix.search.Searcher
import net.bqc.jrelifix.search.cs.InsideSnippetCondition
import net.bqc.jrelifix.utils.{ASTUtils, DiffUtils}
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{ASTNode, IfStatement, Statement, VariableDeclarationStatement}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

case class AddCon2ConStmtMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  override def isParameterizable: Boolean = true

  override def mutate(paramSeed: Identifier): Boolean = {
    if (isParameterizable) assert(paramSeed != null)
    if (!DiffUtils.isChanged(projectData.changedSourcesMap, faultStatement)) return false
    if (!faultStatement.isConditionalStatement()) return false

    var applied = false
    val astNode = faultStatement.getJavaNode()

    applied = addConditionForConditionalStatement(astNode.asInstanceOf[IfStatement], paramSeed)

    if (applied) {
      doMutating()
      true
    }
    else false
  }

  def addConditionForConditionalStatement(faultNode: IfStatement, insertedCon: Identifier): Boolean = {
    val faultFile = faultStatement.getFileName()
    val parentCon = ASTUtils.getConditionalNode(faultNode)
    val boolNodes = ASTUtils.getBoolNodes(parentCon)
    assert(boolNodes.nonEmpty)

    // assure the insertedCon does not exist in the parenCond
    var chosenInsertlyCon = insertedCon
    val parenCode = parentCon.toString
    if (parenCode.contains(chosenInsertlyCon.getJavaNode().toString)) {
      chosenInsertlyCon = projectData.getEngine.chooseRandomlyExpr(NotBelongSeedCondition(parenCode))
    }
    if (chosenInsertlyCon == null) {
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
        val css = Searcher.searchChangeSnippets(projectData.changedSourcesMap(faultFile), InsideSnippetCondition(code.toSourceRange()))
        if (css.nonEmpty) res.addOne(node)
        res
      }
    }
    if (changedBoolNodes.isEmpty) changedBoolNodes = boolNodes

    // randomly choose on the candidate list, very fair
    val ranIndex = projectData.randomizer.nextInt(changedBoolNodes.size)
    val chosenCondition = changedBoolNodes(ranIndex)
    logger.debug("The chosen condition to be combined with: " + chosenCondition)

    // choose logic operator
    val randOp = projectData.randomizer.between(0, 2)
    val op = if (randOp > 0) "||" else "&&"

    // create new condition node
    val newCode = "(%s %s %s)".format(chosenCondition, op, chosenInsertlyCon.getJavaNode().toString)
    val newNode = ASTUtils.createExprNodeFromString(newCode)
    logger.debug("Combining condition: " + newCode)

    // replace the chosenCondition by the new combining condition node
    ASTUtils.replaceNode(this.astRewrite, chosenCondition, newNode)

    true
  }

  override def applicable(): Boolean = ???
}
