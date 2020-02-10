package net.bqc.jrelifix.validation.executor;

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
        System.out.println(System.getProperty("java.home"));
    }

    public TestResult execute(URL[] path, String classToExecute, String testExecutorName, String javaHome, int waitTime, String[] props) {
        return execute(urlArrayToString(path), classToExecute, testExecutorName, javaHome, waitTime, props);
    }

    public TestResult execute(String path, String classToExecute, String testExecutorName, String javaHome, int waitTime, String[] props) {
        Process p = null;

        if (javaHome == null) javaHome = System.getProperty("java.home");
        logger.info("Java Home: " + javaHome);
        javaHome += File.separator + "bin/java";
        String systemcp = System.getProperty("java.class.path");

        // Be careful when rewrite path: the ones that come first would get picked first,
        // and the picked ones would not get overwritten!
        String fullPath = path + File.pathSeparator + systemcp;

        logger.debug("Classpath: " + fullPath);

        try {
            List<String> command = new ArrayList<String>();
            command.add(javaHome);
            for (String prop : props) {
                command.add("-D" + prop);
            }
            command.add("-cp");
            command.add(fullPath);
            command.add(testExecutorName);
            command.add(classToExecute);

            ProcessBuilder pb = new ProcessBuilder(command.toArray(new String[command.size()]));
            pb.redirectOutput();
            pb.redirectErrorStream(true);

            long t_start = System.currentTimeMillis();
            p = pb.start();
            Worker worker = new Worker(p);
            worker.start();
            worker.join(waitTime);
            long t_end = System.currentTimeMillis();
            logger.debug("Execution time " + ((t_end - t_start) / 1000) + "seconds");

            if (!p.waitFor(waitTime, TimeUnit.SECONDS)) { // java 8 feature
                logger.info("Test timed out!");
                p.destroy();
                return null;
            }

            TestResult tr = getTestResult(p, classToExecute);
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

    private TestResult getTestResult(Process p, String testName) {
        TestResult tr = new TestResult();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            ArrayList<String> executedTest = new ArrayList<>();
            executedTest.add(testName);

            while ((line = in.readLine()) != null) {
                logger.debug("Result of running test: " + line);

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

            logger.debug("-------- End of Test --------");
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
