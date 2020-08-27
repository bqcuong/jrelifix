package net.bqc.lyfix.context.mutation

import java.util.Objects

import net.bqc.lyfix.context.ProjectData
import net.bqc.lyfix.context.diff.ChangeType
import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.identifier.seed.VariableSeedIdentifier
import net.bqc.lyfix.utils.ASTUtils
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{AST, ASTNode, ASTVisitor, IBinding, MethodInvocation, ParameterizedType, SimpleName, Type}
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class MethodInvocationMutation(faultStatement: Identifier, projectData: ProjectData)
  extends Mutation(faultStatement, projectData) {

  private val logger: Logger = Logger.getLogger(this.getClass)

  override def isParameterizable: Boolean = false

  /**
   * Handle the mutating actions
   *
   * @param paramSeeds if not null, this operator is parameterizable
   */
  override def mutate(paramSeeds: ArrayBuffer[Identifier]): Boolean = {
    if (isParameterizable) assert(paramSeeds != null)

    val faultFile = faultStatement.getFileName()
    val seedSet = projectData.seedsMap(faultFile)
    val variableSet = new mutable.HashSet[Identifier]()

    // collect all the changed variables in the faulty file
    if (seedSet != null) {
      for (originalSeed <- seedSet) {
        originalSeed match {
          case s: VariableSeedIdentifier =>
            if (s.getChangeTypes().nonEmpty) {
              variableSet.addOne(s)
            }
          case _ =>
        }
      }
    }

    // collect all MI on fault stmt
    val mis = collectMIFromStmt(faultStatement.getJavaNode())
    for (mi <- mis) {
      replaceArguments(mi, variableSet)
    }
    false
  }

  private def replaceArguments(mi: MethodInvocation, variableSet: mutable.HashSet[Identifier]): Unit = {
    for (i <-0 until mi.arguments().size()) {
      val argNode: ASTNode = mi.arguments().get(i).asInstanceOf[ASTNode]
      argNode match {
        case variableArg: SimpleName =>
          val argBinding = variableArg.resolveBinding()
          if (argBinding != null) {
            replaceArgument(variableArg, argBinding.getKey, variableSet)
          }
        case _ =>
      }
    }
  }

  private def replaceArgument(argNode: ASTNode, bindingKey: String, variableSet: mutable.HashSet[Identifier]): Unit = {
    val declarationId = projectData.bindingMap.getOrElse(bindingKey, null)
    if (!declarationId.isInstanceOf[VariableSeedIdentifier]) return
    if (declarationId == null) return
    val lhsDeclaredType = declarationId.asInstanceOf[VariableSeedIdentifier].declType

    for (seedVariable <- variableSet) {
      seedVariable match {
        case v: VariableSeedIdentifier =>
          val rhsDeclaredType = v.asInstanceOf[VariableSeedIdentifier].declType
          if (argNode.toString.trim != seedVariable.getJavaNode().toString.trim) {
            if (checkCompatibleType(lhsDeclaredType, rhsDeclaredType)) {
              // do replacing
              val patch = new Patch(this.document)
              val replaceAction = ASTActionFactory.generateReplaceAction(argNode, seedVariable.getJavaNode())
              patch.addAction(replaceAction)
              patch.addUsingSeed(seedVariable)
              addPatch(patch)
            }
          }
      }
    }
  }

  private def checkCompatibleType(lhs: ASTNode, rhs: ASTNode): Boolean = {
    if (!lhs.isInstanceOf[Type] || !rhs.isInstanceOf[Type]) return false
    val lhsType = lhs.asInstanceOf[Type]
    val rhsType = rhs.asInstanceOf[Type]
    if (lhsType.isParameterizedType && rhsType.isParameterizedType) {
      val lhsTypeArgs = lhsType.asInstanceOf[ParameterizedType].typeArguments()
      val rhsTypeArgs = rhsType.asInstanceOf[ParameterizedType].typeArguments()
      if (lhsTypeArgs.size() == rhsTypeArgs.size()) {
        for (i <- 0 until lhsTypeArgs.size()) {
          if (lhsTypeArgs.get(i).toString.trim != rhsTypeArgs.get(i).toString.trim)
            return false
        }
        return true
      }
    }
    lhsType.toString.trim == rhsType.toString.trim
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
