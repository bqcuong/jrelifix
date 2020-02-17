package net.bqc.jrelifix.context.diff

import java.util

import com.github.gumtreediff.actions.ActionGenerator
import com.github.gumtreediff.actions.model.Action
import com.github.gumtreediff.client.Run
import com.github.gumtreediff.matchers.Matchers
import com.github.gumtreediff.tree.ITree
import net.bqc.jrelifix.config.OptParser
import net.bqc.jrelifix.context.diff.gumtree.MyJdtTreeGenerator
import net.bqc.jrelifix.context.vcs.GitParser
import net.bqc.jrelifix.identifier
import net.bqc.jrelifix.identifier.{ModifiedExpression, ModifiedType}
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{ASTNode, ASTVisitor, CompilationUnit}

import scala.collection.mutable.ArrayBuffer

case class DiffCollector() {

  private val gitParser = new GitParser
  private val logger: Logger = Logger.getLogger(this.getClass)

  /**
   * Default supporting VCS is Git
   * @return
   */
  def collectModifiedExpressions(): ArrayBuffer[ModifiedExpression] = {
    val modifiedExpressions = ArrayBuffer[ModifiedExpression]()
    val bugInducingCommits = OptParser.params().bugInducingCommits
    this.initializeGumTree()

    for (commit <- bugInducingCommits) {
      val modifiedFiles = gitParser.getModifiedFiles(commit)
      for (modifiedFile <- modifiedFiles) {
//        logger.debug("File: " + modifiedFile.filePath)
        val (actions, srcTG, dstTG) = collectDiffActions(modifiedFile)
        import scala.jdk.CollectionConverters._
        for (action <- actions.asScala) {
          val srcAstNode = getAstNode(action.getNode, srcTG.getCompilationUnit)
          val dstAstNode = getAstNode(action.getNode, dstTG.getCompilationUnit)
//          logger.debug("Action: " + action)
//          logger.debug("SrcASTNode: " + (if (srcAstNode != null) srcAstNode.getClass.getName else null) + "---" + srcAstNode)
//          logger.debug("DstASTNode: " + (if (dstAstNode != null) dstAstNode.getClass.getName else null) + "---" + dstAstNode)

          var expAst = if (srcAstNode != null) srcAstNode else dstAstNode
          if (action.getName.equals("INS")) expAst = dstAstNode
          if (action.getName.equals("DEL")) expAst = srcAstNode

          val cu = if (srcAstNode != null) srcTG.getCompilationUnit else dstTG.getCompilationUnit
          // TODO: @bqcuong filter which modified node can be collect? an expression? a method invocation? etc
          if (expAst != null) {
            val mExpression = ModifiedExpression(
              modifiedFile.filePath,
              action.toString,
              getModifiedType(action.getName),
              cu.getLineNumber(expAst.getStartPosition),
              cu.getLineNumber(expAst.getStartPosition + expAst.getLength),
              cu.getColumnNumber(expAst.getStartPosition) + 1,
              cu.getColumnNumber(expAst.getStartPosition + expAst.getLength) + 1
            )
            mExpression.setJavaNode(expAst)
            modifiedExpressions.append(mExpression)
          }
        }
      }
    }

    modifiedExpressions
  }

  private def getModifiedType(actionName: String): ModifiedType.Value = {
    actionName match {
      case "UPD" => ModifiedType.CHANGED
      case "INS" => ModifiedType.ADDED
      case "DEL" => ModifiedType.REMOVED
      case "MOV" => ModifiedType.MOVED
      case _ => ModifiedType.CHANGED
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

  private def collectDiffActions(modifiedFile: ModifiedFile)
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
