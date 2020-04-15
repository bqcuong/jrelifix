package net.bqc.jrelifix.context.diff

import java.util

import com.github.gumtreediff.actions.ActionGenerator
import com.github.gumtreediff.actions.model.Action
import com.github.gumtreediff.client.Run
import com.github.gumtreediff.matchers.Matchers
import com.github.gumtreediff.tree.ITree
import gumtree.spoon.AstComparator
import gumtree.spoon.diff.operations.Operation
import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.diff.gt.MyJdtTreeGenerator
import net.bqc.jrelifix.context.vcs.GitParser
import net.bqc.jrelifix.identifier.{Identifier, SimpleIdentifier}
import net.bqc.jrelifix.utils.ASTUtils
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{ASTNode, ASTVisitor, CompilationUnit}
import spoon.reflect.cu.position.NoSourcePosition
import spoon.reflect.declaration.CtElement
import spoon.support.reflect.cu.position.DeclarationSourcePositionImpl

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
    val bugInducingCommit = projectData.config().bugInducingCommit
    this.initializeGumTree()

    val sourcePath = projectData.config().sourceFolder
    var gitProjectPath = projectData.config().rootProjFolder
    if (gitProjectPath == null) gitProjectPath = projectData.config().projFolder
    val changedFiles = gitParser.getModifiedFiles(gitProjectPath, "HEAD", bugInducingCommit)
    for (changedFile <- changedFiles) {
      // only process java file in the source path
      if (changedFile.filePath.startsWith(sourcePath)) {
        changedSources.addOne(diffAST(changedFile))
      }
    }
    changedSources
  }

  private def getModifiedType(actionName: String): ChangeType.Value = {
    actionName match {
      case "UPD" => ChangeType.MODIFIED
      case "INS" => ChangeType.ADDED
      case "DEL" => ChangeType.REMOVED
      case "MOV" => ChangeType.MOVED
      case _ => ChangeType.MODIFIED
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
    val rootDiffs = differ.getRootOperations
    val allDiffs = differ.getAllOperations
    changedFile.rootCS.addAll(parseDiffOperations(rootDiffs, changedFile))
    changedFile.allCS.addAll(parseDiffOperations(allDiffs, changedFile))
    changedFile
  }

  private def parseDiffOperations(ops: util.List[Operation[_ <: Action]], changedFile: ChangedFile): ArrayBuffer[ChangeSnippet] = {
    val result = ArrayBuffer[ChangeSnippet]()
    import scala.jdk.CollectionConverters._
    for (op <- ops.asScala) {
//      logger.debug(op)
      val changeType = getModifiedType(op.getAction.getName)
      var srcNode: CtElement = null
      var dstNode: CtElement = null
      changeType match {
        case ChangeType.ADDED =>
          dstNode = op.getSrcNode
        case ChangeType.REMOVED =>
          srcNode = op.getSrcNode
        case ChangeType.MOVED =>
          srcNode = op.getSrcNode
          dstNode = op.getDstNode
        case ChangeType.MODIFIED =>
          srcNode = op.getSrcNode
          dstNode = op.getDstNode
        case _ =>
      }

      val (srcRange, srcCodeIdentifier) = getSources(srcNode, changedFile, oldVersion = true)
      val (dstRange, dstCodeIdentifier) = getSources(dstNode, changedFile, oldVersion = false)

      val validChangeSnippet =
        changeType == ChangeType.ADDED && dstRange != null ||
        changeType == ChangeType.REMOVED && srcRange != null ||
        changeType == ChangeType.MODIFIED && srcRange != null && dstRange != null ||
        changeType == ChangeType.MOVED && srcRange != null && dstRange != null && !srcRange.equals(dstRange) // this is to prevent a wrong move action detect of GumTree

      if (validChangeSnippet) {
        val cs = ChangeSnippet(srcRange, dstRange, srcCodeIdentifier, dstCodeIdentifier, changeType)
        result.addOne(cs)
      }
    }
    result
  }

  private def getSources(node: CtElement, containerFile: ChangedFile, oldVersion: Boolean): (SourceRange, Identifier) = {
    if (node != null) {
      val cu = if (oldVersion) containerFile.oldCUnit else containerFile.newCUnit
      val srcPos = node.getPosition
      if (!srcPos.isInstanceOf[NoSourcePosition]) {
        var srcRange: SourceRange = null
        srcRange = srcPos match {
          case sp: DeclarationSourcePositionImpl =>
            val startPos = sp.getModifierSourceStart
            val endPos = sp.getSourceEnd
            val bl: Int = cu.getLineNumber(startPos)
            val el: Int = cu.getLineNumber(endPos)
            val bc: Int = cu.getColumnNumber(startPos) + 1
            val ec: Int = cu.getColumnNumber(endPos) + 1
            new SourceRange(bl, el, bc, ec + 1)
          case _ =>
            new SourceRange(srcPos.getLine, srcPos.getEndLine, srcPos.getColumn, srcPos.getEndColumn + 1)
        }

        val codeIdentifier = SimpleIdentifier(
          srcRange.beginLine, srcRange.endLine,
          srcRange.beginColumn, srcRange.endColumn,
          if (oldVersion) PREVIOUS_VERSION_PREFIX + containerFile.filePath else containerFile.filePath)

        val astNode = ASTUtils.searchNodeByIdentifier(cu, codeIdentifier)
        codeIdentifier.setJavaNode(astNode)

        if (astNode != null) {
          return (srcRange, codeIdentifier)
        }
        else {
          logger.debug("Not found ast node for changed node: " + node)
          return (null, null)
        }
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
