package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.{Identifier, ModifiedExpression}

import scala.collection.mutable.ArrayBuffer

object MutationType extends Enumeration {
  val REVERT, NEGATE, DELETE = Value
}

class MutationGenerator(projectData: ProjectData) {

  def getRandomMutation(faultStatement: Identifier, modifiedExpressions: ArrayBuffer[ModifiedExpression]): Mutation = {
    NegateMutation(faultStatement, modifiedExpressions, projectData)
  }
}
