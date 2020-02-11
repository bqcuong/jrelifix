package net.bqc.jrelifix.context.validation.executor;

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class JUnitTestExecutor {
    public static String SUCCESS = "true";
    public static String FAILURE = "false";
    public static String OUTSET = "BQCJRELEFIX";
    public static String SEPARATOR = "=";

    public static void main(String... args) throws ClassNotFoundException {
        String[] classAndMethod = args[0].split("#");
        Request request;

        if (classAndMethod.length == 2) {
            request = Request.method(Class.forName(classAndMethod[0]), classAndMethod[1]);
        }
        else {
            request = Request.aClass(Class.forName(classAndMethod[0]));
        }

        Result result = new JUnitCore().run(request);
        System.out.println(createOutput(result));
        System.exit(result.wasSuccessful() ? 0 : 1);
    }
    
    public static String createOutput(Result result) {
        StringBuilder res = new StringBuilder();
        res.append("Run count ").append(result.getRunCount()).append("\n");
        res.append("Run fail count ").append(result.getFailureCount()).append("\n");
        for (Failure f : result.getFailures()) {
            res.append("Run failure: ").append(f.getMessage()).append("\n");
        }
        res.append(OUTSET).append(SEPARATOR).append(result.getFailureCount() == 0).append("\n");
        return res.toString();
    }
}
