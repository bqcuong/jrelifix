package net.bqc.jrelifix.context.validation.executor;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestExecutionProcessLauncher {
    private Logger logger = Logger.getLogger(this.getClass());

    public static void main(String[] args) {
        String cp = "/Users/cuong/APR/samples/test/target/classes:/Users/cuong/APR/samples/test/target/test-classes:";

        new TestExecutionProcessLauncher().execute(
                cp,
                "net.bqc.sampleapr.MainTest",
                JUnitTestExecutor.class,
                null,
                100,
                new String[]{}
        );
    }

    public TestResult execute(String classpath, String testToExecute, Class testExecutor, String javaHome, int waitTime, String[] props) {
        Process p = null;

        if (javaHome == null) javaHome = System.getProperty("java.home");
//        logger.info("Java Home: " + javaHome);
        String systemcp = System.getProperty("java.class.path");

        // Be careful when rewrite path: the ones that come first would get picked first,
        // and the picked ones would not get overwritten!
        classpath = classpath + File.pathSeparator + systemcp;

//        logger.debug("Classpath: " + classpath.replace(systemcp, ""));

        try {
            List<String> command = new ArrayList<>();
            command.add(javaHome + File.separator + "bin/java");
            for (String prop : props) {
                command.add("-D" + prop);
            }
            command.add("-cp");
            command.add(classpath);
            command.add(testExecutor.getCanonicalName());
            command.add(testToExecute);

            ProcessBuilder pb = new ProcessBuilder(command.toArray(new String[command.size()]));
            pb.redirectOutput();
            pb.redirectErrorStream(true);

            p = pb.start();
            Worker worker = new Worker(p);
            worker.start();
            worker.join(waitTime);

            if (!p.waitFor(waitTime, TimeUnit.SECONDS)) { // java 8 feature
                logger.info("Test timed out!");
                p.destroy();
                return null;
            }

            TestResult tr = getTestResult(p, testToExecute);
            logger.info(String.format("Test Case: %s %s", testToExecute, tr.wasSuccessful() ? "\u2713" : "\u00D7"));
            p.destroy();
            return tr;
        }
        catch (Exception ex) {
            logger.error("The validation thread continues working " + ex.getMessage());
            if (p != null)
                p.destroy();
            throw new RuntimeException("Validation return null");
        }
    }

    private TestResult getTestResult(Process p, String testToExecute) {
        TestResult tr = new TestResult();
        if (p.exitValue() != 0) tr.failTest.add(testToExecute);
        return tr;
    }

    private static class Worker extends Thread {
        private final Process process;

        private Worker(Process process) {
            this.process = process;
        }

        public void run() {
            try {
                process.waitFor();
            }
            catch (InterruptedException ignore) {}
        }
    }

}
