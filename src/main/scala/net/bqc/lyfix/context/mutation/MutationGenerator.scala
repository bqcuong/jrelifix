package net.bqc.lyfix.context.mutation

import net.bqc.lyfix.context.ProjectData
import net.bqc.lyfix.context.compiler.DocumentASTRewrite
import net.bqc.lyfix.identifier.Identifier

object MutationType extends Enumeration {
  val DELETE, NEGATE, SWAP, REVERT, ADDIF, CONVERT, ADDCON, ADDSTMT, ADDTRYCATCH, MI, NULLCHECKER = Value
}

class MutationGenerator(projectData: ProjectData) {

  def getMutation(faultStatement: Identifier, mutationType: MutationType.Value): Mutation = {
    mutationType match {
      case MutationType.DELETE => DeleteMutation(faultStatement, projectData)
      case MutationType.NEGATE => NegateMutation(faultStatement, projectData)
      case MutationType.SWAP => SwapMutation(faultStatement, projectData)
      case MutationType.REVERT => RevertMutation(faultStatement, projectData)
      case MutationType.ADDIF => new AddIfMutation(faultStatement, projectData)
      case MutationType.CONVERT => ConvertStmt2ConMutation(faultStatement, projectData)
      case MutationType.ADDCON => AddCon2ConStmtMutation(faultStatement, projectData)
      case MutationType.ADDSTMT => AddStmtMutation(faultStatement, projectData)
      case MutationType.ADDTRYCATCH => AddTryCatchMutation(faultStatement, projectData)
      case MutationType.MI => MethodInvocationMutation(faultStatement, projectData)
      case MutationType.NULLCHECKER => NullCheckerMutation(faultStatement, projectData)
    }
  }
}
