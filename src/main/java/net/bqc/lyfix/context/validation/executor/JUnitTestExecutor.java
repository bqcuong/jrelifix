package net.bqc.lyfix.context.validation.executor;

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class JUnitTestExecutor extends TestExecutor {

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

        String output = createOutput(result);
        System.out.println(output);
        if (exitCode != SKIP) exitCode = result.wasSuccessful() ? SUCCESS : FAILED;
        System.exit(exitCode);
    }
    
    public static String createOutput(Result result) {
        StringBuilder res = new StringBuilder("");
        res.append(String.format("Failed/Total: %d/%d", result.getFailureCount(), result.getRunCount())).append("\n");
        for (Failure f : result.getFailures()) {
            String message = f.getMessage();
            res.append("Failure: ").append(message).append("\n");
            if (message != null && message.startsWith("No tests found matching")) {
                exitCode = SKIP;
            }
        }
        return res.toString();
    }
}
