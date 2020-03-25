package net.bqc.jrelifix.context.collector
import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.diff.{ChangedFile, ChangedSnippet, ChangedType}
import net.bqc.jrelifix.identifier.{HistoricalIdentifier, Identifier}
import net.bqc.jrelifix.utils.ASTUtils
import org.eclipse.jdt.core.dom.{ASTNode, ASTVisitor, Block, CompilationUnit, ForStatement, IfStatement, WhileStatement}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class ChangedConditionsCollector(projectData: ProjectData) extends Collector(projectData) {

  override def collect(): mutable.HashMap[String, ArrayBuffer[Identifier]] = {
    val result = mutable.HashMap[String, ArrayBuffer[Identifier]]()
    for (f <- projectData.changedSourcesMap.keys) {
      val fResult = ArrayBuffer[Identifier]()
      val changedFile = projectData.changedSourcesMap.get(f).orNull
      for (s <- changedFile.changedSnippets) {
        fResult.addAll(collectConditions(changedFile, s))
      }
      result.put(f, fResult)
    }
    result
  }

  private def collectConditions(changedFile: ChangedFile, changedSnippet: ChangedSnippet): ArrayBuffer[Identifier] = {
    val result = ArrayBuffer[Identifier]()
    var node: Identifier = null

    changedSnippet.changedType match {
      case ChangedType.REMOVED =>
        node = changedSnippet.srcSource
      case ChangedType.ADDED =>
        node = changedSnippet.dstSource
      case ChangedType.MODIFIED =>
        node = changedSnippet.dstSource
      case ChangedType.MOVED =>
        node = changedSnippet.dstSource
    }

    val conditions = collectConditionASTNodes(node)
    for (c <- conditions) {
      val cu: CompilationUnit = c.getRoot.asInstanceOf[CompilationUnit]
      val nodeLength: Int = c.getLength
      val bl: Int = cu.getLineNumber(c.getStartPosition)
      val el: Int = cu.getLineNumber(c.getStartPosition + nodeLength)
      val bc: Int = cu.getColumnNumber(c.getStartPosition) + 1
      val ec: Int = cu.getColumnNumber(c.getStartPosition + nodeLength) + 1
      val conditionCode = new HistoricalIdentifier(bl, el, bc, ec,
        changedFile.filePath, changedSnippet.changedType)
      conditionCode.setJavaNode(ASTUtils.searchNodeByIdentifier(cu, conditionCode))
      result.addOne(conditionCode)
    }

    result
  }

  /**
   * Atomic condition
   * @param code
   * @return
   */
  private def collectConditionASTNodes(code: Identifier): ArrayBuffer[ASTNode] = {
    val result = ArrayBuffer[ASTNode]()
    val astNode = code.getJavaNode()
    // this changed node is inside a conditional statement
    if (ASTUtils.belongsToConditionStatement(astNode)) {
      result.addAll(ASTUtils.getBoolNodes(astNode))
    }
    else { // this changed node contains conditional statement(s)
      val visitor = new CollectConditionStatements(astNode)
      result.addAll(visitor.list)
    }
    result
  }
}

class CollectConditionStatements(root: ASTNode) extends ASTVisitor {
  val list: ArrayBuffer[ASTNode] = ArrayBuffer[ASTNode]()

  override def visit(node: IfStatement): Boolean = {
    list.addOne(ASTUtils.getConditionalNode(node))
    true
  }

  override def visit(node: WhileStatement): Boolean = {
    list.addOne(ASTUtils.getConditionalNode(node))
    true
  }

  override def visit(node: ForStatement): Boolean = {
    list.addOne(ASTUtils.getConditionalNode(node))
    true
  }
}