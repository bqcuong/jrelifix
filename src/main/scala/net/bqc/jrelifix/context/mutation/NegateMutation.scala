package net.bqc.jrelifix.context.mutation

import java.util

import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.context.parser.JavaParser
import net.bqc.jrelifix.identifier.{Identifier, ModifiedExpression}

import scala.collection.mutable.ArrayBuffer

case class NegateMutation(sourceFileContents: util.HashMap[String, DocumentASTRewrite],
                          faultStatement: Identifier,
                          modifiedExpressions: ArrayBuffer[ModifiedExpression],
                          astParser: JavaParser)

  extends Mutation(sourceFileContents, faultStatement, astParser) {

  override def mutate(): Unit = {

  }

  override def unmutate(): Unit = ???

  override def applicable(): Boolean = ???
}
