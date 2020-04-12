package net.bqc.jrelifix.context.validation.executor;

import org.apache.log4j.Logger;
import org.testng.util.Strings;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestExecutionProcessLauncher {

    private static Logger logger = Logger.getLogger(TestExecutionProcessLauncher.class);
    private List<String> command = new ArrayList<>();

    public static void main(String[] args) {
        String cp = "/Users/cuong/IdeaProjects/apr-repo/yamcs/yamcs-core/target/classes_temp:/Users/cuong/IdeaProjects/apr-repo/yamcs/yamcs-core/target/test-classes:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/artemis-selector-1.5.0.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/netty-all-4.0.32.Final.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/artemis-core-client-1.5.0.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/johnzon-core-0.9.5.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/jgroups-3.6.9.Final.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/jdatepicker-1.3.2.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/commons-logging-1.1.1.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/jackson-core-2.7.1.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/jboss-logging-3.3.0.Final.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/artemis-server-1.5.0.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/junit-4.12.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/artemis-native-1.5.0.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/log4j-1.2.14.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/guava-18.0.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/javacsv-2.0.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/protobuf-java-2.5.0.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/artemis-commons-1.5.0.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/geronimo-json_1.0_spec-1.0-alpha-1.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/slf4j-api-1.7.1.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/janino-2.5.15.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/yamcs-api-0.30.0.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/rocksdbjni-4.13.4.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/hamcrest-core-1.3.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/yamcs-xtce-0.30.0.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/commons-collections-3.2.1.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/protostuff-json-1.4.0.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/snakeyaml-1.9.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/slf4j-jdk14-1.7.1.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/jxl-2.6.10.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/artemis-jdbc-store-1.5.0.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/jcommander-1.48.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/commons-beanutils-1.9.2.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/jython-standalone-2.5.3.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/protostuff-api-1.4.0.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs/artemis-journal-1.5.0.jar:/Users/cuong/IdeaProjects/apr-repo/yamcs-yamcs-186324159-libs";

//        new TestExecutionProcessLauncher().execute(
//                "/Users/cuong/IdeaProjects/apr-repo/yamcs/yamcs-core",
//                cp,
//                "org.yamcs.artemis.ProducerKillerTest#testProducerKiller",
//                JUnitTestExecutor.class,
//                null,
//                15000,
//                new String[]{}
//        );

        logger.debug("[Running] org.yamcs.ParameterArchiveIntegrationTest#testReplayFillup1 |");
        logger.debug("[ ✓ ] <---------------------------------------------------------------/");
    }

    public TestResult execute(String executeDir, String classpath, String testToExecute, Class testExecutor, String javaHome, int waitTime, String[] props) {
        Process p = null;

        if (javaHome == null) javaHome = System.getProperty("java.home");
//        logger.info("Java Home: " + javaHome);
        String systemcp = System.getProperty("java.class.path");

        // Be careful when rewrite path: the ones that come first would get picked first,
        // and the picked ones would not get overwritten!
        classpath = classpath + File.pathSeparator + systemcp;

//        logger.debug("Classpath: " + classpath.replace(systemcp, ""));

        try {
            command.add(javaHome + File.separator + "bin/java");
            for (String prop : props) {
                command.add("-D" + prop);
            }
            command.add("-cp");
            command.add(classpath);
            command.add(testExecutor.getCanonicalName());
            command.add(testToExecute);
            logger.info(String.format("[Running] %s |", testToExecute));
            ProcessBuilder pb = new ProcessBuilder(command.toArray(new String[0]));
            if (executeDir != null) {
                pb.directory(new File(executeDir));
            }
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
            char[] dashes = new char[testToExecute.length() + 2];
            Arrays.fill(dashes, '-');
            logger.info(String.format("[ %s ] <--%s/",
                    tr.ignoredTest.size() > 0 ? "\uD83D\uDEAB" : (tr.wasSuccessful() ? "\u2713" : "\u00D7"),
                    new String(dashes)));
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
        if (p.exitValue() == JUnitTestExecutor.FAILED) {
            tr.failTest.add(testToExecute);
            logger.debug("Execute Command: " + Strings.join(" ", command.toArray(new String[0])));

            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    logger.debug("Test Runner Output: " + line);
                }
                in.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (p.exitValue() == JUnitTestExecutor.NOT_FOUND) tr.ignoredTest.add(testToExecute);
        return tr;
    }

    public static class Worker extends Thread {
        private final Process process;

        public Worker(Process process) {
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
