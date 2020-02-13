package net.bqc.jrelifix.context.mutation

import java.util

import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.context.parser.JavaParser
import net.bqc.jrelifix.identifier.{Identifier, ModifiedExpression}

import scala.collection.mutable.ArrayBuffer

object MutationType extends Enumeration {
  val REVERT = Value
}

class MutationGenerator(sourceFileContents: util.HashMap[String, DocumentASTRewrite], astParser: JavaParser) {

  def getRandomMutation(faultStatement: Identifier, modifiedExpressions: ArrayBuffer[ModifiedExpression]): Mutation = {
    NegateMutation(sourceFileContents, faultStatement, astParser)
  }
}
