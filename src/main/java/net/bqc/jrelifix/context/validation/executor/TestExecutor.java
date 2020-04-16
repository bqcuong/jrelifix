package net.bqc.jrelifix.context.validation.executor;

public abstract class TestExecutor {

    public static final int SUCCESS = 0;
    public static final int FAILED = 1;
    public static final int SKIP = 2;

    protected static int exitCode = -1;
}
