package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.search.{ConSeedCondition, NotEqualingConSeedCondition, Searcher}
import net.bqc.jrelifix.utils.ASTUtils
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{IfStatement, Statement, VariableDeclarationStatement}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class ConvertStmt2ConMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  override def mutate(): Unit = {
    var applied = false
    val astNode = faultStatement.getJavaNode()
    if (faultStatement.isIfStatement()) {
      applied = replaceConditionForIfStatement(astNode.asInstanceOf[IfStatement])
    }
    else if (faultStatement.isVariableDeclarationStatement()) {
      applied = addConditionForVariableDeclaration(astNode.asInstanceOf[VariableDeclarationStatement])
    }
    else {
      applied = addConditionForOtherStatement(astNode.asInstanceOf[Statement])
    }

    if (applied) {
      doMutating()
    }
  }

  /**
   * In case the faulty statement is a if-statement
   */
  private def replaceConditionForIfStatement(faultNode: IfStatement): Boolean = {
    val chosenCon = Searcher.search1RandomSeed(projectData.allSeeds, ConSeedCondition())
    if (chosenCon == null) return false
    logger.debug("Chosen condition: " + chosenCon)

    val replacedCon = ASTUtils.getConditionalNode(faultNode)
    logger.debug("The current condition will be replaced: " + replacedCon)

    ASTUtils.replaceNode(this.document.rewriter, replacedCon, chosenCon.getJavaNode())
    true
  }

  /**
   * In case the faulty statement is a statement that is not neither if-statement or variable declaration
   */
  private def addConditionForOtherStatement(faultNode: Statement): Boolean = {
    logger.debug("Add if condition for: " + faultNode.toString.trim)
    val chosenCon = Searcher.search1RandomSeed(projectData.allSeeds, ConSeedCondition())
    if (chosenCon == null) return false
    logger.debug("Chosen condition: " + chosenCon)

    val wrappedNode = faultNode
    val newIfCode = "if (%s) {%s}".format(chosenCon.getJavaNode().toString, wrappedNode.toString.trim)
    val newIfNode = ASTUtils.createStmtNodeFromString(newIfCode)

    ASTUtils.replaceNode(this.document.rewriter, wrappedNode, newIfNode)
    true
  }

  /**
   * In case the faulty statement is a variable declaration statement
   */
  private def addConditionForVariableDeclaration(faultNode: VariableDeclarationStatement): Boolean = {
    logger.debug("Add if condition for: " + faultNode.toString.trim)

    val variableCodes = ASTUtils.getVariableCodes(faultNode)
    assert(variableCodes.nonEmpty)
    logger.debug("Affected Variables: " + variableCodes)
    var hasInitializer = false

    val varSet = mutable.HashSet[String]()
    val declList = ArrayBuffer[String]()
    val assignList = ArrayBuffer[String]()

    for (varCode <- variableCodes) {
      val decl = varCode.getDefaultInitializer()
      val assign = varCode.getInitializerAssignment()
      varSet.add(varCode.getJavaNode().toString)
      declList.addOne(decl)
      if (assign !=null) assignList.addOne(assign)

      if (varCode.initializer != null) hasInitializer = true
    }

    if (!hasInitializer) return false

    val chosenCon = Searcher.search1RandomSeed(projectData.allSeeds, NotEqualingConSeedCondition(varSet))
    if (chosenCon == null) return false
    logger.debug("Chosen condition: " + chosenCon)

    // create and insert the if-statement after the original declaration
    val assignListStr = assignList.mkString("")
    val ifStr = "if (%s) {%s}".format(chosenCon.getJavaNode().toString, assignListStr)
    val ifNode = ASTUtils.createStmtNodeFromString(ifStr)
    ASTUtils.insertNode(this.document.rewriter, faultNode, ifNode)

    // replace original declaration with new declaration
    var declStr =  "%s ".format(variableCodes(0).declType.toString)
    declStr += declList.mkString(", ")
    declStr += ";"
    val declNode = ASTUtils.createStmtNodeFromString(declStr)
    ASTUtils.replaceNode(this.document.rewriter, faultNode, declNode)
    true
  }

  override def unmutate(): Unit = ???

  override def applicable(): Boolean = ???
}
