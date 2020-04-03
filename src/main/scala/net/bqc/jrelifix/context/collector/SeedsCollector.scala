package net.bqc.jrelifix.context.collector

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.seed.{ExpressionSeedIdentifier, SeedType, VariableSeedIdentifier}
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.ASTUtils
import net.bqc.jrelifix.utils.ASTUtils.{getNodePosition, searchNodeByIdentifier}
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{ASTNode, ASTVisitor, CompilationUnit, ConditionalExpression, ForStatement, IMethodBinding, IfStatement, MethodInvocation, SingleVariableDeclaration, Type, VariableDeclarationFragment, VariableDeclarationStatement, WhileStatement}

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
      val seedsVisitor = new SeedsVisitor()
      cu.accept(seedsVisitor)

      for(c <- seedsVisitor.clist) {
        val atomicBools = ASTUtils.getBoolNodes(c)
        for (b <- atomicBools) {
          val (bl, el, bc, ec) = getNodePosition(b, cu)
          val atomicBoolCode = new ExpressionSeedIdentifier(bl, el, bc, ec)
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
        val variableCode = new VariableSeedIdentifier(bl, el, bc, ec, declType, initializer)
        variableCode.setJavaNode(searchNodeByIdentifier(cu, variableCode))
        projectData.seedsMap(f).addOne(variableCode)
      }

      logger.debug("Collected seeds: " + projectData.seedsMap(f))
    }

    projectData
  }

  class SeedsVisitor extends ASTVisitor {
    val clist: mutable.HashSet[ASTNode] = mutable.HashSet[ASTNode]()
    val vlist: mutable.HashSet[ASTNode] = mutable.HashSet[ASTNode]()
    val mlist: mutable.HashSet[ASTNode] = mutable.HashSet[ASTNode]()

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
              clist.addOne(f.getInitializer)
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
      assert(binding != null)

      var defaultValue: String = null
      val returnType = binding.getReturnType
      val returnTypeStr = returnType.toString
      if (returnType.isPrimitive) { // return type is a primitive type
        defaultValue = returnTypeStr match {
          case "byte" | "short" | "char" | "int" | "long" => "0"
          case "float" | "double" => "0.0"
          case "boolean" => "false"
          case _ => "null"
        }
      }
      else { // other type
        defaultValue = "null"
      }
      true
    }

    // parameter declaration
    override def visit(node: SingleVariableDeclaration): Boolean = {
      vlist.addOne(node.getName)
      true
    }

    override def visit(node: IfStatement): Boolean = {
      clist.addOne(ASTUtils.getConditionalNode(node))
      true
    }

    override def visit(node: WhileStatement): Boolean = {
      clist.addOne(ASTUtils.getConditionalNode(node))
      true
    }

    override def visit(node: ForStatement): Boolean = {
      clist.addOne(ASTUtils.getConditionalNode(node))
      true
    }

    override def visit(node: ConditionalExpression): Boolean = {
      clist.addOne(ASTUtils.getConditionalNode(node))
      true
    }
  }
}