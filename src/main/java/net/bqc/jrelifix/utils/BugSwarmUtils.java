package net.bqc.jrelifix.utils;

import net.bqc.jrelifix.context.validation.executor.TestExecutionProcessLauncher;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BugSwarmUtils {

    public static final int WAIT_TIME = 10;
    public static final String BUG_CLI = "./scripts/bugcli.py";
    public static final String VALIDATE_SUB_COMMAND = "validate";
    private static Logger logger = Logger.getLogger(BugSwarmUtils.class);

    public static void main(String[] args) {
        validate("puniverse-capsule-78565048");
    }

    public static boolean validate(String imageTag) {
        logger.info("[BugSwarm] Validating for " + imageTag + "...");
        boolean result = execute(VALIDATE_SUB_COMMAND, imageTag, true);
        logger.info("[BugSwarm] Validation result: " + (result ? "\u2713" : "\u00D7"));
        return result;
    }

    public static boolean execute(String subCommand, String argument, boolean redirectOutput) {
        Process p = null;
        List<String> command = new ArrayList<>();
        try {
            command.add(BUG_CLI);
            command.add(subCommand);
            command.add(argument);
            ProcessBuilder pb = new ProcessBuilder(command.toArray(new String[0]));

            if (redirectOutput) {
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectErrorStream(true);
            }

            p = pb.start();
            TestExecutionProcessLauncher.Worker worker = new TestExecutionProcessLauncher.Worker(p);
            worker.start();
            worker.join(WAIT_TIME);

            if (!p.waitFor(WAIT_TIME, TimeUnit.MINUTES)) { // java 8 feature
                logger.info("BugSwarm validation timed out!");
                p.destroy();
                return false;
            }

            p.destroy();
            return p.exitValue() == 0;
        }
        catch (InterruptedException | IOException ex) {
            if (p != null)
                p.destroy();
            throw new RuntimeException("BugSwarm validation error");
        }
    }
}
