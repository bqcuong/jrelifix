package net.bqc.lyfix.context.compiler;

import net.bqc.lyfix.context.validation.executor.BugSwarmLauncher;

public class BugSwarmCompiler implements ICompiler {
    public static final int COMPILATION_TIMEOUT = 30;
    private String imageTag = null;

    public BugSwarmCompiler(String imageTag) {
        this.imageTag = imageTag;
    }

    @Override
    public Status compile() throws Exception {
        boolean compileResult = BugSwarmLauncher.compile(this.imageTag, COMPILATION_TIMEOUT);
        return compileResult ? Status.COMPILED : Status.NOT_COMPILED;
    }

    @Override
    public String dequeueCompileError() {
        return null;
    }

    @Override
    public void updateSourceFileContents(String keyFile, DocumentASTRewrite newDocument) {

    }
}
