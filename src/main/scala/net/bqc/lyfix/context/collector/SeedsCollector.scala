package net.bqc.lyfix.context.collector

import net.bqc.lyfix.context.ProjectData
import net.bqc.lyfix.identifier.Identifier
import net.bqc.lyfix.identifier.seed.{AssignmentSeedIdentifier, ExpressionSeedIdentifier, MethodInvocationSeedIdentifier, VariableSeedIdentifier}
import net.bqc.lyfix.utils.ASTUtils
import net.bqc.lyfix.utils.ASTUtils.{getNodePosition, searchNodeByIdentifier}
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.Assignment.Operator
import org.eclipse.jdt.core.dom._

import scala.collection.mutable

case class SeedsCollector(projectData: ProjectData) extends Collector(projectData) {
  private val logger: Logger = Logger.getLogger(this.getClass)

  override def collect(): ProjectData = {
    val seedFiles = new mutable.HashSet[String]()
    seedFiles.addAll(projectData.changedSourcesMap.keys)
    seedFiles.addAll(projectData.originalFaultFiles)

    for(f <- seedFiles) {
      projectData.seedsMap.put(f, new mutable.HashSet[Identifier]())

      val cu = projectData.filePath2CU(f)
      val keysCollector = new BindingKeysCollector()
      cu.accept(keysCollector)
      val seedsVisitor = new SeedsVisitor(keysCollector.keys)
      cu.accept(seedsVisitor)

      for(c <- seedsVisitor.clist) {
        val atomicBools = ASTUtils.getBoolNodes(c)
        for (b <- atomicBools) {
          val (bl, el, bc, ec) = getNodePosition(b, cu)
          val atomicBoolCode = new ExpressionSeedIdentifier(bl, el, bc, ec, f)
          atomicBoolCode.setBool(true)
          atomicBoolCode.setJavaNode(searchNodeByIdentifier(cu, atomicBoolCode))
          projectData.seedsMap(f).addOne(atomicBoolCode)
        }
      }

      for(v <- seedsVisitor.vlist) {
        var declType: ASTNode = null
        var initializer: ASTNode = null
        v.getParent match {
          case p: SingleVariableDeclaration => // parameter
            declType = p.getType
          case p: VariableDeclarationFragment => // local variable
            initializer = p.getInitializer
            declType = p.getParent
        }

        val (bl, el, bc, ec) = getNodePosition(v, cu)
        val variableCode = new VariableSeedIdentifier(bl, el, bc, ec, f, declType, initializer)
        variableCode.setJavaNode(searchNodeByIdentifier(cu, variableCode))
        projectData.seedsMap(f).addOne(variableCode)
      }

      for(m <- seedsVisitor.mlist) {
        val mi = m.asInstanceOf[MethodInvocation]
        val binding: IMethodBinding = mi.resolveMethodBinding()
        assert(binding != null)
        val returnType = binding.getReturnType
        val (bl, el, bc, ec) = getNodePosition(m, cu)
        val miCode = new MethodInvocationSeedIdentifier(bl, el, bc, ec, f, returnType)
        miCode.setJavaNode(searchNodeByIdentifier(cu, miCode))
        projectData.seedsMap(f).addOne(miCode)
      }

      for(a <- seedsVisitor.alist) {
        val assignment = a.asInstanceOf[Assignment]
        val (bl, el, bc, ec) = getNodePosition(a, cu)
        val assignmentCode = new AssignmentSeedIdentifier(bl, el, bc, ec, f, assignment.getLeftHandSide, assignment.getRightHandSide)
        assignmentCode.setJavaNode(searchNodeByIdentifier(cu, assignmentCode))
        projectData.seedsMap(f).addOne(assignmentCode)
      }

      logger.debug("Collected seeds: " + projectData.seedsMap(f))
    }

    projectData
  }

  class BindingKeysCollector extends ASTVisitor {
    val keys: mutable.HashSet[String] = mutable.HashSet[String]()

    override def visit(node: TypeDeclaration): Boolean = {
      keys.add(node.resolveBinding().getKey)
      true
    }

    override def visit(node: MethodDeclaration): Boolean = {
      keys.add(node.resolveBinding().getKey)
      true
    }

    override def visit(node: FieldDeclaration): Boolean = {
      val frags = node.fragments()
      for (i <- 0 until frags.size()) {
        val frag = frags.get(i)
        frag match {
          case f: VariableDeclaration =>
            keys.add(f.resolveBinding().getKey)
        }
      }
      true
    }
  }

  class SeedsVisitor(keys: mutable.HashSet[String]) extends ASTVisitor {
    val clist: mutable.HashSet[ASTNode] = mutable.HashSet[ASTNode]()
    val vlist: mutable.HashSet[ASTNode] = mutable.HashSet[ASTNode]()
    val mlist: mutable.HashSet[ASTNode] = mutable.HashSet[ASTNode]()
    val alist: mutable.HashSet[ASTNode] = mutable.HashSet[ASTNode]()

    /**
     * Generate condition expression from assignment
     * E.g: a = 0 -> a != 0
     * @param node
     * @return
     */
    override def visit(node: Assignment): Boolean = {
      if (node.getOperator == Operator.ASSIGN) {
        alist.addOne(node)
      }
      true
    }

    // local variable declaration
    override def visit(node: VariableDeclarationStatement): Boolean = {
      val declType = node.getType.toString
      val isBoolDecl = declType == "boolean" || declType == "Boolean"
      val frags = node.fragments()
      for(i <- 0 until frags.size()) {
        val frag = frags.get(i)
        frag match {
          case f: VariableDeclarationFragment =>
            vlist.addOne(f.getName)
            if (isBoolDecl && f.getInitializer != null) {
              val initStr = f.getInitializer.toString.trim
              if (!initStr.equals("true") && !initStr.equals("false")) {
                clist.addOne(f.getInitializer)
              }
            }
        }
      }
      true
    }

    /**
     * Generate condition expression from method call
     * E.g: f(a) -> f(a) != false, f(a) != 0, f(a) != null
     * @param node
     * @return
     */
    override def visit(node: MethodInvocation): Boolean = {
      val binding: IMethodBinding = node.resolveMethodBinding()
      if (binding != null && keys.contains(binding.getKey))
        mlist.addOne(node)
      true
    }

    // parameter declaration
    override def visit(node: SingleVariableDeclaration): Boolean = {
      vlist.addOne(node.getName)
      true
    }

    override def visit(node: IfStatement): Boolean = {
      val c = ASTUtils.getConditionalNode(node)
      if (c != null) clist.addOne(c)
      true
    }

    override def visit(node: WhileStatement): Boolean = {
      val c = ASTUtils.getConditionalNode(node)
      if (c != null) clist.addOne(c)
      true
    }

    override def visit(node: ForStatement): Boolean = {
      val c = ASTUtils.getConditionalNode(node)
      if (c != null) clist.addOne(c)
      true
    }

    override def visit(node: ConditionalExpression): Boolean = {
      val c = ASTUtils.getConditionalNode(node)
      if (c != null) clist.addOne(c)
      true
    }
  }
}