package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.search.{ConSeedCondition, Searcher}
import net.bqc.jrelifix.utils.ASTUtils
import org.apache.log4j.Logger
import sun.tools.tree.IfStatement

case class AddIfMutation (faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData){

  private val logger: Logger = Logger.getLogger(this.getClass)

  override def mutate(): Unit = {
    var applied = false
    if (faultStatement.isIfStatement()) {
      applied = replaceConditionForIfStatement()
    }
    else if (faultStatement.isVariableDeclarationStatement()) {
      addConditionForVariableDeclaration()
    }
    else {
      applied = addConditionForOtherStatement()
    }

    if (applied) {
      doMutating()
    }
  }

  /**
   * In case the faulty statement is a if-statement
   */
  private def replaceConditionForIfStatement(): Boolean = {
    val chosenCon = Searcher.search1RandomSeed(projectData.allSeeds, ConSeedCondition())
    logger.debug("Chosen condition expression: " + chosenCon)

    val replacedCon = ASTUtils.getConditionalNode(faultStatement.getJavaNode())
    logger.debug("The current condition will be replaced: " + replacedCon)

    ASTUtils.replaceNode(this.document.rewriter, replacedCon, chosenCon.getJavaNode())
    true
  }

  /**
   * In case the faulty statement is a statement that is not neither if-statement or variable declaration
   */
  private def addConditionForOtherStatement(): Boolean = {
    logger.debug("Add if condition for: " + faultStatement.getJavaNode().toString.trim)
    val chosenCon = Searcher.search1RandomSeed(projectData.allSeeds, ConSeedCondition())
    logger.debug("Chosen condition expression: " + chosenCon)

    val wrappedNode = faultStatement.getJavaNode()
    val newIfCode = "if (%s) {%s}".format(chosenCon.getJavaNode().toString, wrappedNode.toString.trim)
    val newIfNode = ASTUtils.createStmtNodeFromString(newIfCode)

    ASTUtils.replaceNode(this.document.rewriter, wrappedNode, newIfNode)
    true
  }

  /**
   * In case the faulty statement is a variable declaration statement
   */
  private def addConditionForVariableDeclaration(): Unit = ???

  override def unmutate(): Unit = ???

  override def applicable(): Boolean = ???
}
