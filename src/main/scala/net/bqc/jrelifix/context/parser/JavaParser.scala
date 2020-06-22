package net.bqc.jrelifix.context.parser

import java.io.File

import net.bqc.jrelifix.utils.{ClassPathUtils, FileFolderUtils}
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.dom._

import scala.collection.mutable

case class JavaParser(projectPath: String, sourcePath: String, classPath: String) {
  private val API_LEVEL: Int = 8 // target Java 8
  private val COMPLIANCE_LEVEL: String = JavaCore.VERSION_1_8

  def batchParse(): (scala.collection.mutable.HashMap[String, CompilationUnit], scala.collection.mutable.HashMap[String, String]) = {
    val files: java.util.List[File] = FileFolderUtils.walk(sourcePath, ".java", new java.util.ArrayList[File])

    import scala.jdk.CollectionConverters._
    val sourceFilePaths = files.asScala.collect {
      case f: File if f.isFile => f.getCanonicalPath
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

    /**
     * file path -> CompilationUnit
     */
    val compilationUnitMap: mutable.HashMap[String, CompilationUnit] = new mutable.HashMap[String, CompilationUnit]

    /**
     * fully qualified class name -> file path
     */
    val class2FilePathMap: mutable.HashMap[String, String] = new mutable.HashMap[String, String]

    val requester = new FileASTRequestor() {
      override def acceptAST(sourceFilePath: String, cu: CompilationUnit): Unit = {
        compilationUnitMap.put(sourceFilePath, cu)

        val packageName = if (cu.getPackage != null) cu.getPackage.getName.getFullyQualifiedName else null

        cu.accept(new ASTVisitor() {
          override def visit(node: TypeDeclaration): Boolean = {
            val className = node.getName.toString
            if (packageName != null) class2FilePathMap.put("%s.%s".format(packageName, className), sourceFilePath)
            else class2FilePathMap.put(className, sourceFilePath)
            super.visit(node)
          }
        })
      }

      override def acceptBinding(bindingKey: String, binding: IBinding): Unit = {
        // do nothing
      }
    }

    astParser.createASTs(sourceFilePaths.toArray, null, Array[String](), requester, null)
    (compilationUnitMap, class2FilePathMap)
  }

  def genASTFromSource(source: String): CompilationUnit = {
    val astParser = ASTParser.newParser(API_LEVEL)
    val options = JavaCore.getOptions
    JavaCore.setComplianceOptions(COMPLIANCE_LEVEL, options)
    astParser.setCompilerOptions(options)
    astParser.setSource(source.toCharArray)
    astParser.setResolveBindings(true)
    astParser.setBindingsRecovery(true)
    astParser.createAST(null).asInstanceOf[CompilationUnit]
  }
}

object JavaParser {
  def parseAST(source: String): CompilationUnit = {
    JavaParser.apply(null, null, null).genASTFromSource(source)
  }
}
