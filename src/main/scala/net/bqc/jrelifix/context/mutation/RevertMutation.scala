package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.context.diff.{ChangeSnippet, ChangeType}
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.search.{ChildSnippetCondition, ExactlySnippetCondition, SameCodeSnippetCondition, Searcher}
import net.bqc.jrelifix.utils.{ASTUtils, DiffUtils}
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{ASTNode, Block, Statement}

/**
 * To revert the modified statement/expression to old ones in previous version:
 * Change actions covered:
 * - remove ADDED (already supported by Delete Mutation)
 * - add REMOVED
 * - revert MODIFIED
 * - re-swap MOVED
 * @param faultStatement
 * @param projectData
 */
case class RevertMutation(faultStatement: Identifier, projectData: ProjectData, doc: DocumentASTRewrite)
  extends Mutation(faultStatement, projectData, doc) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  // the whole faulty statement is added, moved
  private def revertStatementAsWhole(faultNode: Identifier, cs: ChangeSnippet): Boolean = {
    cs.changeType match {
      case ChangeType.MOVED =>
        ASTUtils.replaceNode(this.astRewrite, faultNode.getJavaNode(), this.astRewrite.getAST.createInstance(classOf[Block]))

        // identify the related node to the faulty node in the previous version
        val faultNodeInPrev = cs.srcSource
        var sibNode: ASTNode = ASTUtils.getSiblingNode(faultNodeInPrev.getJavaNode(), after = true)
        if (sibNode != null) { // try to insert before the pos-sibling node
          if (ASTUtils.isExistedNode(this.document.cu, sibNode)) {
            ASTUtils.insertNode(this.astRewrite, sibNode, faultNode.getJavaNode(), insertAfter = false)
            return true
          }
        }

        sibNode = ASTUtils.getSiblingNode(faultNodeInPrev.getJavaNode(), after = false)
        if (sibNode != null) { // try to insert after the pre-sibling node
          if (ASTUtils.isExistedNode(this.document.cu, sibNode)) {
            ASTUtils.insertNode(this.astRewrite, sibNode, faultNode.getJavaNode(), insertAfter = true)
            return true
          }
        }

        val parentNode = faultNodeInPrev.getJavaNode().getParent
        if (parentNode != null) { // try to insert inside the parent node
          if (ASTUtils.isExistedNode(this.document.cu, parentNode)) {
            ASTUtils.appendNode(this.astRewrite, parentNode, faultNode.getJavaNode())
            return true
          }
        }
        true

      case ChangeType.ADDED =>
        ASTUtils.replaceNode(this.astRewrite, faultNode.getJavaNode(), this.astRewrite.getAST.createInstance(classOf[Block]))
        true

      case _ =>
        logger.debug("Not support statement level revert action for change type: " + cs.changeType)
        false
    }
  }

  override def mutate(paramSeed: Identifier = null): Boolean = {
    if (isParameterizable) assert(paramSeed != null)
    var applied = false
    val faultLineNumber = faultStatement.getLine()
    val faultFile = faultStatement.getFileName()
    val faultCode = faultStatement.getJavaNode().toString

    var changedSnippet = DiffUtils.searchChangeSnippetOutside(projectData.changedSourcesMap, faultStatement)
    if (changedSnippet != null) {
      if (changedSnippet.changeType == ChangeType.MODIFIED) {
        applied = revertModifiedCode(changedSnippet)
      }
      else if (changedSnippet.changeType == ChangeType.MOVED) {
        applied = revertMovedStatement(changedSnippet)
      }
      else if (changedSnippet.changeType == ChangeType.ADDED) {
        ASTUtils.replaceNode(this.astRewrite, faultStatement.getJavaNode(), this.astRewrite.getAST.createInstance(classOf[Block]))
        applied = true
      }
    }

    if (!applied) {
      val insideCSs = Searcher.searchChangeSnippets(projectData.changedSourcesMap(faultFile), ChildSnippetCondition(faultCode))
      // revert all or partial the inside changes?? -> Try to revert whole stmt at first
      if (insideCSs.nonEmpty) {
        val armCss = insideCSs.filter(
            cs => cs.changeType == ChangeType.MODIFIED ||
            cs.changeType == ChangeType.MOVED ||
            cs.changeType == ChangeType.REMOVED)

        if (armCss.nonEmpty) { // try to revert the whole stmt thanks to modification/movement/removal operations
          var prevStmt: Statement = null
          var idx = 0
          while (prevStmt == null && idx < armCss.size) {
            val cs = armCss(idx)
            prevStmt = ASTUtils.getParentStmt(cs.srcSource.getJavaNode())
            idx += 1
          }

          if (prevStmt != null) {
            ASTUtils.replaceNode(this.astRewrite, faultStatement.getJavaNode(), prevStmt)
            applied = true
          }
        }

        // TODO: support for remove addedly expression here
      }
    }

    if (!applied) {
      changedSnippet = DiffUtils.searchChangeSnippetOutside(projectData.changedSourcesMap, faultStatement, MAX_LINE_DISTANCE)
      if (changedSnippet != null && changedSnippet.changeType == ChangeType.REMOVED) {
        val prevCode = changedSnippet.srcSource
        assert(prevCode != null)

        if (changedSnippet.srcRange.beginLine > faultStatement.getBeginLine()) {
          // insert after fault statement
          ASTUtils.insertNode(this.astRewrite, faultStatement.getJavaNode(), prevCode.getJavaNode())
        }
        else {
          // insert before fault statement
          ASTUtils.insertNode(this.astRewrite, faultStatement.getJavaNode(), prevCode.getJavaNode(), insertAfter = false)
        }
        logger.debug("REVERT: Add removedly statement: " + prevCode.getJavaNode().toString.trim)
        applied = true
      }
    }

    if (applied) {
      doMutating()
      true
    }
    else false
  }

  private def revertMovedStatement(cs: ChangeSnippet): Boolean = {
    val prevCode = cs.srcSource
    val currCode = cs.dstSource
    assert(prevCode != null)
    assert(currCode != null)
    if (currCode.getBeginLine() != faultStatement.getLine()) return false
    val prevLine = prevCode.getBeginLine()
    val currentNodeAtPrevLine = ASTUtils.searchNodeByLineNumber(document.cu, prevLine)
    if (currentNodeAtPrevLine == null) return false
    val currentNode = ASTUtils.searchNodeByIdentifier(document.cu, currCode)
    assert(currentNode != null)

    // Step 1: Remove the current block of code
    ASTUtils.removeNode(astRewrite, currentNode)

    // Step 2: Put it again at the previous line number
    if (prevLine < currCode.getBeginLine()) { // move down
      ASTUtils.insertNode(astRewrite, currentNodeAtPrevLine, currCode.getJavaNode(), insertAfter = false)
    }
    else { // move up
      ASTUtils.insertNode(astRewrite, currentNodeAtPrevLine, currCode.getJavaNode())
    }

    true
  }

  private def revertModifiedCode(cs: ChangeSnippet): Boolean = {
    val prevCode = cs.srcSource
    val currCode = cs.dstSource
    assert(prevCode != null)
    assert(currCode != null)
    val currASTNodeOnDocument = ASTUtils.searchNodeByIdentifier(document.cu, currCode)
    ASTUtils.replaceNode(this.astRewrite, currASTNodeOnDocument, prevCode.getJavaNode())
    true
  }

  override def applicable(): Boolean = ???

  override def isParameterizable: Boolean = false
}
