//package net.bqc.jrelifix.context.mutation
//
//import net.bqc.jrelifix.context.ProjectData
//import net.bqc.jrelifix.context.diff.{ChangedSnippet, ChangedType}
//import net.bqc.jrelifix.identifier.{Identifier, ModifiedType}
//import net.bqc.jrelifix.utils.ASTUtils
//import org.apache.log4j.Logger
//import org.eclipse.text.edits.TextEdit
//
//import scala.collection.mutable.ArrayBuffer
//
//case class RevertMutation(faultStatement: Identifier,
//                          modifiedExpressions: ArrayBuffer[ChangedSnippet],
//                          projectData: ProjectData)
//
//  extends Mutation(faultStatement, projectData) {
//
//  private val logger: Logger = Logger.getLogger(this.getClass)
//
//  override def mutate(): Unit = {
//    // try to revert modified expressions which are faulty lines
//    val faultFile = faultStatement.getFileName()
//    val faultLineNumber = faultStatement.getLine()
//
//    for (modifiedExpr <- modifiedExpressions) {
//      val filePathFromDiff = modifiedExpr.filePath
//      if (filePathFromDiff.endsWith(faultFile)) { // tricky line warning!!!
//
//        // choose modified expressions which has LOCs contains the faults
//        if (modifiedExpr.beginLine <= faultLineNumber && faultLineNumber <= modifiedExpr.endLine) {
//          if (modifiedExpr.modifiedType == ChangedType.MODIFIED) {
//            // Modify source code on ASTRewrite
//            ASTUtils.replaceNode(this.astRewrite, faultStatement.getJavaNode(), modifiedExpr.getJavaNode())
//          }
//          else if (modifiedExpr.modifiedType == ChangedType.ADDED) {
//            val toRemovedNode = ASTUtils.findModifiedNode(document.cu, modifiedExpr)
//
//            // Modify source code on ASTRewrite
//            ASTUtils.removeNode(this.astRewrite, toRemovedNode)
//          }
//          // TODO: support REMOVED, MOVED
//
//          // Apply changes to the document object
//          val edits = this.astRewrite.rewriteAST(this.document.modifiedDocument, null)
//          edits.apply(this.document.modifiedDocument, TextEdit.NONE)
//          return
//        }
//      }
//    }
//  }
//
//  override def unmutate(): Unit = ???
//
//  override def applicable(): Boolean = ???
//}
