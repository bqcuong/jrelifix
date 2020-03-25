package net.bqc.jrelifix.context.diff

import java.util

import com.github.gumtreediff.actions.ActionGenerator
import com.github.gumtreediff.actions.model.Action
import com.github.gumtreediff.client.Run
import com.github.gumtreediff.matchers.Matchers
import com.github.gumtreediff.tree.ITree
import gumtree.spoon.AstComparator
import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.diff.gt.MyJdtTreeGenerator
import net.bqc.jrelifix.context.vcs.GitParser
import net.bqc.jrelifix.identifier.{Identifier, SimpleIdentifier}
import net.bqc.jrelifix.utils.ASTUtils
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{ASTNode, ASTVisitor, CompilationUnit}
import spoon.reflect.cu.position.NoSourcePosition
import spoon.reflect.declaration.CtElement

import scala.collection.mutable.ArrayBuffer

case class DiffCollector(projectData: ProjectData) {

  val PREVIOUS_VERSION_PREFIX = "^"
  private val gitParser = new GitParser
  private val logger: Logger = Logger.getLogger(this.getClass)

  /**
   * Default supporting VCS is Git
   * @return
   */
  def collectChangedSources(): ArrayBuffer[ChangedFile] = {
    val changedSources = ArrayBuffer[ChangedFile]()
    val bugInducingCommits = projectData.config().bugInducingCommits
    this.initializeGumTree()

    for (commit <- bugInducingCommits) {
      val changedFiles = gitParser.getModifiedFiles(projectData.config().projFolder, commit)
      for (changedFile <- changedFiles) {
        changedSources.addOne(diffAST(changedFile))
      }
    }
    changedSources
  }

  private def getModifiedType(actionName: String): ChangedType.Value = {
    actionName match {
      case "UPD" => ChangedType.MODIFIED
      case "INS" => ChangedType.ADDED
      case "DEL" => ChangedType.REMOVED
      case "MOV" => ChangedType.MOVED
      case _ => ChangedType.MODIFIED
    }
  }

  private def getAstNode(tree: ITree, cu: CompilationUnit): ASTNode = {
    var foundNode: ASTNode = null
    cu.accept(new ASTVisitor() {
      override def preVisit2(node: ASTNode): Boolean = {
        if (node.getStartPosition == tree.getPos && node.getLength == tree.getLength && node.getNodeType == tree.getType) {
          foundNode = node
          return false
        }
        true
      }
    })
    foundNode
  }

  private def initializeGumTree(): Unit = {
    Run.initGenerators()
  }

  private def diffAST(changedFile: ChangedFile) : ChangedFile = {
    val comparator: AstComparator = new AstComparator()
    val differ = comparator.compare(changedFile.oldVersion, changedFile.newVersion)
    val diffs = differ.getRootOperations

    import scala.jdk.CollectionConverters._
    for (diff <- diffs.asScala) {
      logger.debug(diff)
      val changeType = getModifiedType(diff.getAction.getName)
      var srcNode: CtElement = null
      var dstNode: CtElement = null
      changeType match {
        case ChangedType.ADDED =>
          dstNode = diff.getSrcNode
        case ChangedType.REMOVED =>
          srcNode = diff.getSrcNode
        case ChangedType.MOVED =>
          srcNode = diff.getSrcNode
          dstNode = diff.getDstNode
        case ChangedType.MODIFIED =>
          srcNode = diff.getSrcNode
          dstNode = diff.getDstNode
        case _ =>
      }

      val (srcRange, srcCodeIdentifier) = getSources(srcNode, changedFile, oldVersion = true)
      val (dstRange, dstCodeIdentifier) = getSources(dstNode, changedFile, oldVersion = false)
      val changedSnippet = ChangedSnippet(srcRange, dstRange, srcCodeIdentifier, dstCodeIdentifier, changeType)
      changedFile.changedSnippets.addOne(changedSnippet)
    }
    changedFile
  }

  private def getSources(node: CtElement, containerFile: ChangedFile, oldVersion: Boolean): (SourceRange, Identifier) = {
    if (node != null) {
      val srcPos = node.getPosition
      if (!srcPos.isInstanceOf[NoSourcePosition]) {
        val srcRange = new SourceRange(srcPos.getLine, srcPos.getEndLine, srcPos.getColumn, srcPos.getEndColumn + 1)
        val codeIdentifier = SimpleIdentifier(
          srcRange.beginLine, srcRange.endLine,
          srcRange.beginColumn, srcRange.endColumn,
          if (oldVersion) PREVIOUS_VERSION_PREFIX + containerFile.filePath else containerFile.filePath)

        val astNode = ASTUtils.searchNodeByIdentifier(if (oldVersion) containerFile.oldCUnit else containerFile.newCUnit, codeIdentifier)
        codeIdentifier.setJavaNode(astNode)

        if (astNode == null) logger.debug("Not found ast node for changed node: " + node)
        return (srcRange, codeIdentifier)
      }
    }
    (null, null)
  }

  private def collectDiffActions(modifiedFile: ChangedFile)
    : (util.List[Action], MyJdtTreeGenerator, MyJdtTreeGenerator) = {
    val srcTG = new MyJdtTreeGenerator()
    val dstTG = new MyJdtTreeGenerator()
    val srcTC = srcTG.generateFromString(modifiedFile.oldVersion)
    val dstTC = dstTG.generateFromString(modifiedFile.newVersion)
    val src = srcTC.getRoot
    val dst = dstTC.getRoot
    val m = Matchers.getInstance.getMatcher(src, dst)
    m.`match`()
    val g = new ActionGenerator(src, dst, m.getMappings)
    g.generate()
    val actions = g.getActions
    (actions, srcTG, dstTG)
  }
}
