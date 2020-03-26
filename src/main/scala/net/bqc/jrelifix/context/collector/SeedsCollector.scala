package net.bqc.jrelifix.context.collector

import net.bqc.jrelifix.context.ProjectData
import net.bqc.jrelifix.identifier.Identifier
import net.bqc.jrelifix.utils.ASTUtils
import org.apache.log4j.Logger
import org.eclipse.jdt.core.dom.{ASTNode, ASTVisitor, ConditionalExpression, ForStatement, IfStatement, SingleVariableDeclaration, VariableDeclarationFragment, VariableDeclarationStatement, WhileStatement}

import scala.collection.mutable

case class SeedsCollector(projectData: ProjectData) extends Collector(projectData){
  private val logger: Logger = Logger.getLogger(this.getClass)

  override def collect(): ProjectData = {
    val seedFiles = new mutable.HashSet[String]()
    seedFiles.addAll(projectData.changedSourcesMap.keys)
    seedFiles.addAll(projectData.originalFaultFiles)

    for(f <- seedFiles) {
      projectData.conditionsMap.put(f, new mutable.HashSet[Identifier]())
      projectData.variablesMap.put(f, new mutable.HashSet[Identifier]())

      val cu = projectData.filePath2CU(f)
      val seedsVisitor = new SeedsVisitor()
      cu.accept(seedsVisitor)

      for(c <- seedsVisitor.clist) {
        val atomicBools = ASTUtils.getBoolNodes(c)
        for (b <- atomicBools) {
          val atomicBoolCode = ASTUtils.createSeedIdentifierForASTNode(b)
          projectData.conditionsMap(f).addOne(atomicBoolCode)
        }
      }

      for(v <- seedsVisitor.vlist) {
        val variableCode = ASTUtils.createSeedIdentifierForASTNode(v)
        projectData.variablesMap(f).addOne(variableCode)
      }

      logger.debug("Collected conditions: " + projectData.conditionsMap(f))
      logger.debug("Collected variables: " + projectData.variablesMap(f))
    }

    projectData
  }

  class SeedsVisitor extends ASTVisitor {
    val clist: mutable.HashSet[ASTNode] = mutable.HashSet[ASTNode]()
    val vlist: mutable.HashSet[ASTNode] = mutable.HashSet[ASTNode]()

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