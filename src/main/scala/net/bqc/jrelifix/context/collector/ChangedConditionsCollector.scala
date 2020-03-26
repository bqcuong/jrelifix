package net.bqc.jrelifix.context.collector
import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.diff.{ChangedFile, ChangedSnippet, ChangedType}
import net.bqc.jrelifix.identifier.{HistoricalIdentifier, Identifier}
import net.bqc.jrelifix.utils.ASTUtils
import org.eclipse.jdt.core.dom._

import scala.collection.mutable

case class ChangedConditionsCollector(projectData: ProjectData) extends Collector(projectData) {

  override def collect(): ProjectData = {
    val result = mutable.HashMap[String, mutable.HashSet[Identifier]]()
    for (f <- projectData.changedSourcesMap.keys) {
      val fResult = mutable.HashSet[Identifier]()
      val changedFile = projectData.changedSourcesMap.get(f).orNull
      for (s <- changedFile.rootCS) {
        fResult.addAll(collectConditions(changedFile, s))
      }
      result.put(f, fResult)
    }
    projectData
  }

  private def collectConditions(changedFile: ChangedFile, changedSnippet: ChangedSnippet): mutable.HashSet[Identifier] = {
    val result = mutable.HashSet[Identifier]()
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
      val conditionCode = HistoricalIdentifier(bl, el, bc, ec,
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
  private def collectConditionASTNodes(code: Identifier): mutable.HashSet[ASTNode] = {
    val result = mutable.HashSet[ASTNode]()
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
  val list: mutable.HashSet[ASTNode] = mutable.HashSet[ASTNode]()

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