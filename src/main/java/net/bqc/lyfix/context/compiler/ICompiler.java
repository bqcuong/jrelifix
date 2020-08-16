package net.bqc.lyfix.context.compiler;

public interface ICompiler {
    enum Status {
        NOT_COMPILED, COMPILED
    }

    Status compile() throws Exception;
    String dequeueCompileError();
    void updateSourceFileContents(String keyFile, DocumentASTRewrite newDocument);
}


