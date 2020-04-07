package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.identifier.Identifier

object MutationType extends Enumeration {
  val DELETE, NEGATE, SWAP, REVERT, ADDIF, CONVERT, ADDCON = Value
}

class MutationGenerator(projectData: ProjectData) {

  def getMutation(faultStatement: Identifier, mutationType: MutationType.Value, doc: DocumentASTRewrite = null, coinUp: Boolean = false): Mutation = {
    mutationType match {
      case MutationType.DELETE => DeleteMutation(faultStatement, projectData, doc)
      case MutationType.NEGATE => NegateMutation(faultStatement, projectData, doc)
      case MutationType.SWAP => SwapMutation(faultStatement, projectData, doc, if (coinUp) SwapMutation.SWAP_UP else SwapMutation.SWAP_DOWN)
      case MutationType.REVERT => RevertMutation(faultStatement, projectData, doc)
      case MutationType.ADDIF => new AddIfMutation(faultStatement, projectData, doc)
      case MutationType.CONVERT => ConvertStmt2ConMutation(faultStatement, projectData, doc)
      case MutationType.ADDCON => AddCon2ConStmtMutation(faultStatement, projectData, doc)
    }
  }
}
