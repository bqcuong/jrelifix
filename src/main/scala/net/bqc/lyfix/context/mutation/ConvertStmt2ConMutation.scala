package net.bqc.lyfix.context.mutation

import net.bqc.lyfix.context.ProjectData
import net.bqc.lyfix.context.compiler.DocumentASTRewrite
import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.search.Searcher
import net.bqc.lyfix.search.seed.BoolMethodInvocationCondition
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.ExpressionStatement

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class ConvertStmt2ConMutation(faultStatement: Identifier, projectData: ProjectData)
  extends AddIfMutation(faultStatement, projectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  override def mutate(paramSeeds: ArrayBuffer[Identifier]): Boolean = {
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
    super.mutate(ArrayBuffer[Identifier] { closetMI })
    true
  }

  override def applicable(): Boolean = ???

  override def isParameterizable: Boolean = false
}
