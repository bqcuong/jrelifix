package net.bqc.jrelifix.context.mutation

import java.util

import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.context.parser.JavaParser
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.ASTUtils
import org.eclipse.text.edits.TextEdit

class DeleteMutation(sourceFileContents: util.HashMap[String, DocumentASTRewrite],
                     faultStatement: Identifier, astParser: JavaParser)
  extends Mutation(sourceFileContents, faultStatement, astParser) {

  override def mutate(): Unit = {
    // Modify source code on ASTRewrite
    ASTUtils.removeNode(this.astRewrite, faultStatement.getJavaNode())

    // Apply changes to the document object
    val edits = this.astRewrite.rewriteAST(this.document.modifiedDocument, null)
    edits.apply(this.document.modifiedDocument, TextEdit.NONE)
  }

  override def unmutate(): Unit = ???

  override def applicable(): Boolean = ???
}
