package net.bqc.jrelifix.context.validation.executor;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BugSwarmLauncher {

    public static final String BUG_CLI = "./scripts/bugcli.py";
    public static final String COMPILE_SUB_COMMAND = "compile";
    public static final String VALIDATE_SUB_COMMAND = "validate";

    private static Logger logger = Logger.getLogger(BugSwarmLauncher.class);

    public static void main(String[] args) {
        validateReducedTS("nutzam-nutz-333553716", 30);
    }

    public static boolean compile(String imageTag, int timeout) {
        logger.info("[BugSwarm] Compiling for " + imageTag + "...");
        boolean result = execute(COMPILE_SUB_COMMAND, new String[]{}, imageTag, timeout, true);
        logger.info("[BugSwarm] Compilation result: " + (result ? "\u2713" : "\u00D7"));
        return result;
    }

    public static boolean validateReducedTS(String imageTag, int timeout) {
        logger.info("[BugSwarm] Validating REDUCED TEST SUITE for " + imageTag + "...");
        String[] options = new String[]{ "--reduced-ts" };
        boolean result = execute(VALIDATE_SUB_COMMAND, options, imageTag, timeout, true);
        logger.info("[BugSwarm] Validation REDUCED TEST SUITE result: " + (result ? "\u2713" : "\u00D7"));
        return result;
    }

    public static boolean validateAllTS(String imageTag, int timeout) {
        logger.info("[BugSwarm] Validating ALL TEST SUITE for " + imageTag + "...");
        boolean result = execute(VALIDATE_SUB_COMMAND, new String[]{}, imageTag, timeout, true);
        logger.info("[BugSwarm] Validation ALL TEST SUITE result: " + (result ? "\u2713" : "\u00D7"));
        return result;
    }

    public static boolean execute(String subCommand, String[] options, String argument, int timeout, boolean redirectOutput) {
        Process p = null;
        List<String> command = new ArrayList<>();
        try {
            command.add(BUG_CLI);
            command.add(subCommand);
            command.addAll(Arrays.asList(options));
            command.add(argument);
            ProcessBuilder pb = new ProcessBuilder(command.toArray(new String[0]));

            if (redirectOutput) {
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectErrorStream(true);
            }

            p = pb.start();
            TestExecutionProcessLauncher.Worker worker = new TestExecutionProcessLauncher.Worker(p);
            worker.start();
            worker.join(timeout);

            if (!p.waitFor(timeout, TimeUnit.MINUTES)) {
                logger.info("BugSwarm launcher timed out!");
                p.destroy();
                return false;
            }

            p.destroy();
            return p.exitValue() == 0;
        }
        catch (InterruptedException | IOException ex) {
            if (p != null)
                p.destroy();
            throw new RuntimeException("BugSwarm launcher validation error");
        }
    }
}
