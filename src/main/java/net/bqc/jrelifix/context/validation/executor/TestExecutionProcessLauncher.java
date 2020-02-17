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
        logger.info("Test Case: " + testToExecute);
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

            long t_start = System.currentTimeMillis();
            p = pb.start();
            Worker worker = new Worker(p);
            worker.start();
            worker.join(waitTime);
            long t_end = System.currentTimeMillis();

            if (!p.waitFor(waitTime, TimeUnit.SECONDS)) { // java 8 feature
                logger.info("Test timed out!");
                p.destroy();
                return null;
            }

            TestResult tr = getTestResult(p, testToExecute);
            p.destroy();
//            logger.debug("Execution time " + ((t_end - t_start) / 1000) + " seconds");
//            logger.debug("-------- End of Test --------");
            return tr;
        }
        catch (Exception ex) {
            logger.error("The validation thread continues working " + ex.getMessage());
            if (p != null)
                p.destroy();
            throw new RuntimeException("Validation return null");
        }

    }

    private TestResult getTestResult(Process p, String testName) {
        TestResult tr = new TestResult();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            ArrayList<String> executedTest = new ArrayList<>();
            executedTest.add(testName);

            while ((line = in.readLine()) != null) {
                logger.debug("Output: " + line);

                if (line.contains("Exception in thread \"main\"")) {
                    throw new RuntimeException("Exception when running test: " + testName + " => " + line);
                }

                if (line.contains(JUnitTestExecutor.SEPARATOR) && line.contains(JUnitTestExecutor.OUTSET)) {
                    String[] sp = line.split(JUnitTestExecutor.SEPARATOR);
                    if (sp[1].equals(JUnitTestExecutor.SUCCESS)) {
                        if (sp[0].contains(JUnitTestExecutor.OUTSET)) {
                            tr.setSuccessTest(executedTest);
                            break;
                        }
                    }
                    else if (sp[1].equals(JUnitTestExecutor.FAILURE)) {
                        if (sp[0].contains(JUnitTestExecutor.OUTSET)) {
                            tr.setFailTest(executedTest);
                            break;
                        }
                    }
                    else {
                        throw new RuntimeException("Invalid Test Execution Output");
                    }
                }
            }
            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return tr;
    }

    protected String urlArrayToString(URL[] urls) {
        StringBuilder s = new StringBuilder();
        for (URL url : urls) {
            s.append(url.getPath()).append(File.pathSeparator);
        }
        return s.toString();
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
