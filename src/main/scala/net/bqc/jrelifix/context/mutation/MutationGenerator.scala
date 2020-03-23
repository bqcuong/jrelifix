package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.Identifier

object MutationType extends Enumeration {
  val REVERT, NEGATE, DELETE = Value
}

class MutationGenerator(projectData: ProjectData) {

  def getRandomMutation(faultStatement: Identifier): Mutation = {
    SwapMutation(faultStatement, projectData, SwapMutation.SWAP_UP)
  }
}
