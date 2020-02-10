package net.bqc.jrelifix.context.parser

import java.io.File

import net.bqc.jrelifix.model.Identifier
import net.bqc.jrelifix.utils.{ASTUtils, ClassPathUtils, FileFolderUtils}
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.dom.{ASTNode, ASTParser, ASTVisitor, CompilationUnit, FileASTRequestor, IBinding, TypeDeclaration}

case class JavaParser(projectPath: String, sourcePath: String, classPath: String) {
  private val API_LEVEL: Int = 8 // target Java 8
  private val COMPLIANCE_LEVEL: String = JavaCore.VERSION_1_8

  /**
   * relative file path -> CompilationUnit
   */
  private val compilationUnitMap: scala.collection.mutable.HashMap[String, CompilationUnit] = new scala.collection.mutable.HashMap[String, CompilationUnit]

  /**
   * fully qualified class name -> relative file path
   */
  private val class2FilePathMap: scala.collection.mutable.HashMap[String, String] = new scala.collection.mutable.HashMap[String, String]

  def class2CU(className: String): CompilationUnit = compilationUnitMap(class2FilePathMap(className))
  def filePath2CU(relativeFilePath: String): CompilationUnit = compilationUnitMap(relativeFilePath)

  def identifier2ASTNode(identifier: Identifier): ASTNode = {
    val cu = class2CU(identifier.getClassName())
    ASTUtils.findNode(cu, identifier)
  }

  def batchParse(): Unit = {
    val files: java.util.List[File] = FileFolderUtils.walk(sourcePath, ".java", new java.util.ArrayList[File])

    import scala.jdk.CollectionConverters._
    val sourceFilePaths = files.asScala.collect {
      case f: File if f.isFile => f.getAbsolutePath
    }

    val astParser = ASTParser.newParser(API_LEVEL)

    // set up libraries (.jar, .class or .java)
    // Array(new File(Options.sourceFolder).getParent)
    // new File(Main.options.sourceFolder).toString
    astParser.setEnvironment(classPath.split(ClassPathUtils.CP_DELIMITER), Array(sourcePath), null, true)

    astParser.setResolveBindings(true)

    // with Binding Recovery on, the compiler can detect
    // binding among the set of compilation units
    astParser.setBindingsRecovery(true)

    // set default options, especially for Java 1.8
    val options = JavaCore.getOptions()
    JavaCore.setComplianceOptions(COMPLIANCE_LEVEL, options)
    astParser.setCompilerOptions(options)

    val requester = new FileASTRequestor() {
      override def acceptAST(sourceFilePath: String, cu: CompilationUnit): Unit = {
        val relativeFilePath = FileFolderUtils.relativePath(projectPath, sourceFilePath)
        compilationUnitMap.put(relativeFilePath, cu)

        val packageName = if (cu.getPackage != null) cu.getPackage.getName.getFullyQualifiedName else ""

        cu.accept(new ASTVisitor() {
          override def visit(node: TypeDeclaration): Boolean = {
            val className = node.getName.toString
            class2FilePathMap.put("%s.%s".format(packageName, className), relativeFilePath)
            super.visit(node)
          }
        })
      }

      override def acceptBinding(bindingKey: String, binding: IBinding): Unit = {
        // do nothing
      }
    }

    astParser.createASTs(sourceFilePaths.toArray, null, Array[String](), requester, null)
  }
}
