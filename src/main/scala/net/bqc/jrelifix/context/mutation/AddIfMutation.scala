package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.search.seed.NotEqualSeedCondition
import net.bqc.jrelifix.utils.ASTUtils
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{IfStatement, Statement, VariableDeclarationStatement}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


class AddIfMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  override def mutate(paramSeed: Identifier = null): Boolean = {
    if (isParameterizable) assert(paramSeed != null)
    var applied = false
    val astNode = faultStatement.getJavaNode()
    if (faultStatement.isIfStatement()) {
      applied = replaceConditionForIfStatement(astNode.asInstanceOf[IfStatement], paramSeed)
    }
    else if (faultStatement.isVariableDeclarationStatement()) {
      applied = addConditionForVariableDeclaration(astNode.asInstanceOf[VariableDeclarationStatement], paramSeed)
    }
    else {
      applied = addConditionForOtherStatement(astNode.asInstanceOf[Statement], paramSeed)
    }

    if (applied) {
      doMutating()
      true
    }
    else false
  }

  /**
   * In case the faulty statement is a if-statement
   */
  private def replaceConditionForIfStatement(faultNode: IfStatement, chosenCon: Identifier): Boolean = {
    val replacedCon = ASTUtils.getConditionalNode(faultNode)
    var chosenInsertlyCon = chosenCon
    val replacedCode = replacedCon.toString
    if (replacedCode.equals(chosenInsertlyCon.getJavaNode().toString)) {
      chosenInsertlyCon = projectData.getEngine.chooseRandomlyExpr(NotEqualSeedCondition(replacedCode))
    }
    if (chosenInsertlyCon == null) {
      logger.debug("Could not find any satisfied condition from expr seed set to replace for current condition...")
      return false
    }

    logger.debug("The current condition will be replaced: " + replacedCon)
    ASTUtils.replaceNode(this.astRewrite, replacedCon, chosenInsertlyCon.getJavaNode())
    true
  }

  /**
   * In case the faulty statement is a statement that is not neither if-statement or variable declaration
   */
  private def addConditionForOtherStatement(faultNode: Statement, chosenCon: Identifier): Boolean = {
    val wrappedNode = faultNode
    val newIfCode = "if (%s) {%s}".format(chosenCon.getJavaNode().toString, wrappedNode.toString.trim)
    val newIfNode = ASTUtils.createStmtNodeFromString(newIfCode)

    ASTUtils.replaceNode(this.astRewrite, wrappedNode, newIfNode)
    true
  }

  /**
   * In case the faulty statement is a variable declaration statement
   */
  private def addConditionForVariableDeclaration(faultNode: VariableDeclarationStatement, chosenCon: Identifier): Boolean = {
    logger.debug("Add if condition for: " + faultNode.toString.trim)

    val variableCodes = ASTUtils.getVariableCodes(faultNode)
    var chosenCon2 = chosenCon
    assert(variableCodes.nonEmpty)
    logger.debug("Affected Variables: " + variableCodes)
    for (variable <- variableCodes) {
      val varName = variable.getJavaNode().toString
      if (chosenCon.getJavaNode().toString.contains(varName)) {
        chosenCon2 = ASTUtils.createIdentifierForASTNode(ASTUtils.createExprNodeFromString("false"))
      }
    }
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

    // create and insert the if-statement after the original declaration
    val assignListStr = assignList.mkString("")
    val ifStr = "if (%s) {%s}".format(chosenCon2.getJavaNode().toString, assignListStr)
    val ifNode = ASTUtils.createStmtNodeFromString(ifStr)
    ASTUtils.insertNode(this.astRewrite, faultNode, ifNode)

    // replace original declaration with new declaration
    var declStr =  "%s ".format(variableCodes(0).declType.toString)
    declStr += declList.mkString(", ")
    declStr += ";"
    val declNode = ASTUtils.createStmtNodeFromString(declStr)
    ASTUtils.replaceNode(this.astRewrite, faultNode, declNode)
    true
  }

  override def applicable(): Boolean = ???

  override def isParameterizable: Boolean = true
}
