package net.bqc.jrelifix.context.validation.executor;

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.io.OutputStream;
import java.io.PrintStream;

public class JUnitTestExecutor {

    public static final int SUCCESS = 0;
    public static final int FAILED = 1;
    public static final int NOT_FOUND = 2;

    private static int exitCode = -1;

    public static void main(String... args) throws ClassNotFoundException {
        String[] classAndMethod = args[0].split("#");
        Request request;

        if (classAndMethod.length == 2) {
            request = Request.method(Class.forName(classAndMethod[0]), classAndMethod[1]);
        }
        else {
            request = Request.aClass(Class.forName(classAndMethod[0]));
        }

//        PrintStream original = System.out;
//        ignoreOutput();
        Result result = new JUnitCore().run(request);
//        System.setOut(original);

        String output = createOutput(result);
        System.out.println(output);
        if (exitCode != NOT_FOUND) exitCode = result.wasSuccessful() ? SUCCESS : FAILED;
        System.exit(exitCode);
    }

    private static void ignoreSystemOut() {
        System.setOut(new PrintStream(new OutputStream() {
            public void write(int b) {
            }
        }));
        System.setErr(new PrintStream(new OutputStream() {
            public void write(int b) {
            }
        }));
    }
    
    public static String createOutput(Result result) {
        StringBuilder res = new StringBuilder("");
        res.append(String.format("Failed/Total: %d/%d", result.getFailureCount(), result.getRunCount())).append("\n");
        for (Failure f : result.getFailures()) {
            String message = f.getMessage();
            res.append("Failure: ").append(message).append("\n");
            if (message != null && message.startsWith("No tests found matching")) {
                exitCode = NOT_FOUND;
            }
        }
        return res.toString();
    }
}
