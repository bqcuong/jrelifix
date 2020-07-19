package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.Identifier
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{AST, ASTNode, ASTVisitor, MethodInvocation}

import scala.collection.mutable.ArrayBuffer

case class MethodInvocationMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  override def isParameterizable: Boolean = false

  /**
   * Handle the mutating actions
   *
   * @param paramSeed if not null, this operator is parameterizable
   */
  override def mutate(paramSeed: Identifier): Boolean = {
    if (isParameterizable) assert(paramSeed != null)
    // collect all MI on fault stmt
    val mis = collectMIFromStmt(faultStatement.getJavaNode())
    for (mi <- mis) {
      logger.debug(mi.getName)
    }
    false
  }

  private def collectMIFromStmt(stmt: ASTNode): ArrayBuffer[MethodInvocation] = {
    val results = ArrayBuffer[MethodInvocation]()
    stmt.accept(new ASTVisitor() {
      override def visit(node: MethodInvocation): Boolean = {
        results.append(node)
        true
      }
    })
    results
  }

  override def applicable(): Boolean = ???
}
