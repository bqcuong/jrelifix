package net.bqc.jrelifix.context.mutation

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.context.compiler.DocumentASTRewrite
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.search.Searcher
import net.bqc.jrelifix.search.seed.BoolMethodInvocationCondition
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.ExpressionStatement

import scala.collection.mutable

case class ConvertStmt2ConMutation(faultStatement: Identifier, projectData: ProjectData, doc: DocumentASTRewrite)
  extends AddIfMutation(faultStatement, projectData, doc) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  override def mutate(paramSeed: Identifier = null): Boolean = {
    val faultFile = faultStatement.getFileName()
    val seedList = Searcher
      .searchSeeds(projectData.seedsMap, faultFile, BoolMethodInvocationCondition(faultStatement))
      .map(_.asInstanceOf[Identifier])

    // only retain the method invocations which stand as alone statements
    val seedStmtList = seedList.foldLeft(mutable.HashSet[Identifier]()) {
      (res, seed) => {
        val javaNode = seed.getJavaNode()
        val parentNode = javaNode.getParent
        if (parentNode.isInstanceOf[ExpressionStatement]) res.addOne(seed)
        res
      }
    }

    val closetMI = faultStatement.findCloset(seedStmtList)
    if (closetMI == null) return false

    // convert the method invocation (closetMI) as a condition???
    // Not necessary, a bool method invocation has been already a condition

    super.mutate(closetMI)
    true
  }

  override def applicable(): Boolean = ???

  override def isParameterizable: Boolean = false
}
