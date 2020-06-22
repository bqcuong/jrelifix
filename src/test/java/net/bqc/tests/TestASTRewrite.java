package net.bqc.tests;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TestASTRewrite {

    @Test
    public void test() throws BadLocationException {
        Document document = new Document("class X {\npublic static int f(int a, int b){\nint c=a;\nint d=b;\nreturn c+d;\n}\n}\n");
        CompilationUnit cu = getCuFromDocument(document);
        AST ast = cu.getAST();
        ASTRewrite rewriter = ASTRewrite.create(ast);

        TypeDeclaration td = (TypeDeclaration) cu.types().get(0);
        MethodDeclaration md = td.getMethods()[0];
        List stmts = md.getBody().statements();
        assert stmts.size() == 3;

        Statement faultyStmt_1 = (Statement) stmts.get(0); // int c=a;
        Statement faultyStmt_2 = (Statement) stmts.get(2); // return c+d;

        // Mutate 1, remove int c=a;
        rewriter.remove(faultyStmt_1, null);
        TextEdit edit_1 = rewriter.rewriteAST(document, null);
        UndoEdit undo_1 = edit_1.apply(document, TextEdit.CREATE_UNDO);

        Assert.assertEquals("class X {\npublic static int f(int a, int b){\nint d=b;\nreturn c+d;\n}\n}\n", document.get());
        undo_1.apply(document);

        // Mutate 2, update return c+d; -> return a+d;
        ASTNode newNode_2 = createStmtNodeFromString("return a + d;");
        rewriter.replace(faultyStmt_2, newNode_2, null);
        TextEdit edit_2 = rewriter.rewriteAST(document, null);

        undo_1 = edit_1.apply(document, TextEdit.CREATE_UNDO);
        UndoEdit undo_2 = edit_2.apply(document, TextEdit.CREATE_UNDO);

        Assert.assertEquals("class X {\npublic static int f(int a, int b){\nint d=b;\nreturn a + d;\n}\n}\n", document.get());

        // TODO: Undo Mutating 1

        // TODO: Undo Mutating 2
    }


    public static CompilationUnit getCuFromDocument(IDocument document) {
        ASTParser parser = ASTParser.newParser(AST.JLS11);
        parser.setSource(document.get().toCharArray());
        return (CompilationUnit) parser.createAST(null);
    }

    public static ASTNode createStmtNodeFromString(String toTransform) {
        IDocument document = new Document("public class X{public void replace(){" + toTransform + "}}");
        CompilationUnit cu = getCuFromDocument(document);
        MyASTVisitor visitor = new MyASTVisitor();
        cu.accept(visitor);
        return visitor.toTransNode;
    }

    public static class MyASTVisitor extends ASTVisitor {
        public ASTNode toTransNode = null;

        @Override
        public boolean visit(MethodDeclaration node) {
            Block methodBody = node.getBody();
            toTransNode = (ASTNode) methodBody.statements().get(0);
            return false;
        }
    }
}
