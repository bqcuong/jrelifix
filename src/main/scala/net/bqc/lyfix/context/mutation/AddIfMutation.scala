package net.bqc.lyfix.context.mutation

import net.bqc.lyfix.context.ProjectData
import net.bqc.lyfix.context.compiler.DocumentASTRewrite
import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.search.seed.NotEqualSeedCondition
import net.bqc.lyfix.utils.ASTUtils
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{IfStatement, Statement, VariableDeclarationStatement}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


class AddIfMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  override def mutate(paramSeeds: ArrayBuffer[Identifier]): Boolean = {
    if (isParameterizable) assert(paramSeeds != null)
    var applied = false
    val astNode = faultStatement.getJavaNode()
    if (faultStatement.isIfStatement()) {
      applied = replaceConditionForIfStatement(astNode.asInstanceOf[IfStatement], paramSeeds)
    }
    else if (faultStatement.isVariableDeclarationStatement()) {
      applied = addConditionForVariableDeclaration(astNode.asInstanceOf[VariableDeclarationStatement], paramSeeds)
    }
    else {
      applied = addConditionForOtherStatement(astNode.asInstanceOf[Statement], paramSeeds)
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
  private def replaceConditionForIfStatement(faultNode: IfStatement, chosenCons: ArrayBuffer[Identifier]): Boolean = {
    val replacedCon = ASTUtils.getConditionalNode(faultNode)
    val replacedCode = replacedCon.toString

    for (chosenInsertlyCon <- chosenCons) {
      if (!replacedCode.equals(chosenInsertlyCon.getJavaNode().toString)) {
        logger.debug("The current condition will be replaced: " + replacedCon)
        val replaceAction = ASTActionFactory.generateReplaceAction(replacedCon, chosenInsertlyCon.getJavaNode())
        val patch = new Patch(document)
        patch.addAction(replaceAction)
        patch.addUsingSeed(chosenInsertlyCon)
        addPatch(patch)
      }
    }
    true
  }

  /**
   * In case the faulty statement is a statement that is not neither if-statement or variable declaration
   */
  private def addConditionForOtherStatement(faultNode: Statement, chosenCons: ArrayBuffer[Identifier]): Boolean = {
    val wrappedNode = faultNode

    for (chosenCon <- chosenCons) {
      val newIfCode = "if (%s) {%s}".format(chosenCon.getJavaNode().toString, wrappedNode.toString.trim)
      val newIfNode = ASTUtils.createStmtNodeFromString(newIfCode)
      val replaceAction = ASTActionFactory.generateReplaceAction(wrappedNode, newIfNode)
      val patch = new Patch(document)
      patch.addAction(replaceAction)
      patch.addUsingSeed(chosenCon)
      addPatch(patch)
    }
    true
  }

  /**
   * In case the faulty statement is a variable declaration statement
   */
  private def addConditionForVariableDeclaration(faultNode: VariableDeclarationStatement, chosenCons: ArrayBuffer[Identifier]): Boolean = {
    logger.debug("Add if condition for: " + faultNode.toString.trim)

    val variableCodes = ASTUtils.getVariableCodes(faultNode)
    assert(variableCodes.nonEmpty)
    logger.debug("Affected Variables: " + variableCodes)

    val filteredChosenCons = ArrayBuffer[Identifier]()
    for (variable <- variableCodes) {
      val varName = variable.getJavaNode().toString
      for (chosenCon <- chosenCons) {
        if (!chosenCon.getJavaNode().toString.contains(varName)) {
          filteredChosenCons.addOne(chosenCon)
        }
      }
    }

    // add negative condition
    val cc = ASTUtils.createIdentifierForASTNode(ASTUtils.createExprNodeFromString("false"))
    filteredChosenCons.addOne(cc)

    for (chosenCon <- chosenCons) {
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

      if (hasInitializer) {
        val patch = new Patch(document)
        // create and insert the if-statement after the original declaration
        val assignListStr = assignList.mkString("")
        val ifStr = "if (%s) {%s}".format(chosenCon.getJavaNode().toString, assignListStr)
        val ifNode = ASTUtils.createStmtNodeFromString(ifStr)
        ASTUtils.insertNode(this.astRewrite, faultNode, ifNode)
        val insertAction = ASTActionFactory.generateInsertAction(faultNode, ifNode)
        patch.addAction(insertAction)
        patch.addUsingSeed(chosenCon)

        // replace original declaration with new declaration
        var declStr = "%s ".format(variableCodes(0).declType.toString)
        declStr += declList.mkString(", ")
        declStr += ";"
        val declNode = ASTUtils.createStmtNodeFromString(declStr)
        val replaceAction = ASTActionFactory.generateReplaceAction(faultNode, declNode)
        patch.addAction(replaceAction)

        // add to patch list
        addPatch(patch)
      }
    }

    true
  }

  override def applicable(): Boolean = ???

  override def isParameterizable: Boolean = true
}
