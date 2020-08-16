/**
 * Source: https://raw.githubusercontent.com/qhanam/Java-RSRepair/master/src/ca/uwaterloo/ece/qhanam/jrsrepair/DocumentASTRewrite.java
 */
package net.bqc.lyfix.context.compiler;

import net.bqc.lyfix.context.parser.JavaParser;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import java.io.File;

/**
 * Stores a Document/ASTRewrite pair so that we can synchronize changes to Documents
 * and ASTs.
 * @author qhanam
 *
 */
public class DocumentASTRewrite {
    public File backingFile;
    public IDocument document;
    public IDocument modifiedDocument;
    public CompilationUnit cu;
    public AST ast;
    private boolean modified;
    private boolean tainted;

    public DocumentASTRewrite(IDocument document, File backingFile, CompilationUnit cu) {
        this.backingFile = backingFile;
        this.document = document;
        this.tainted = false;
        this.modified = false;
        this.cu = cu;

        this.resetModifiedDocument();
    }

    public void taintDocument(){
        this.modified = true;
        this.tainted = true;
    }

    public DocumentASTRewrite generateModifiedDocument() {
        IDocument newDoc = new Document(this.modifiedDocument.get());
        CompilationUnit newCu = JavaParser.parseAST(newDoc.get());
        return new DocumentASTRewrite(newDoc, this.backingFile, newCu);
    }

    public ASTRewrite generateASTRewrite() {
        ast = cu.getAST();
        return ASTRewrite.create(ast);
    }

    public void untaintDocument(){
        this.tainted = false;
    }

    public boolean isDocumentTainted(){
        return this.tainted;
    }

    public void resetModifiedDocument(){
        this.modifiedDocument = new Document(document.get());
    }

    public boolean isDocumentModified(){
        return this.modified;
    }
}