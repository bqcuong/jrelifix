package net.bqc.lyfix.context.validation.executor;

import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.util.ArrayList;
import java.util.List;

public class TestNGExecutor extends TestExecutor {

    public static void main(String... args) {
        XmlSuite suite = new XmlSuite();
        suite.setName("TmpSuite");

        XmlTest test = new XmlTest(suite);
        test.setName("TmpTest");
        List<XmlClass> classes = new ArrayList<>();
        String[] classMethodNames = args[0].split("#");

        XmlClass x = new XmlClass(classMethodNames[0]);
        if(classMethodNames.length == 2) {
            List<XmlInclude> methods = new ArrayList<>();
            XmlInclude m = new XmlInclude(classMethodNames[1]);
            methods.add(m);
            x.setIncludedMethods(methods);
        }
        classes.add(x);
        test.setXmlClasses(classes) ;

        List<XmlSuite> suites = new ArrayList<>();
        suites.add(suite);
        TestNG testng = new TestNG();
        testng.setXmlSuites(suites);

        TestListener listener = new TestListener();
        testng.addListener(listener);
        testng.setVerbose(10);
        testng.run();
        System.out.println(createOutput(listener));

        if (listener.failures.isEmpty() && !listener.skips.isEmpty()) {
            System.exit(SKIP);
        }
        else {
            System.exit(listener.failures.isEmpty() ? SUCCESS : FAILED);
        }
    }

    public static String createOutput(TestListener listener) {
        StringBuilder res = new StringBuilder("");
        res.append(String.format("Failed/Total: %d/%d", listener.failures.size(), listener.totalRun)).append("\n");
        for (String f : listener.failures) {
            res.append(f).append("\n");
        }
        for (String f : listener.skips) {
            res.append(f).append("\n");
        }
        return res.toString();
    }

    public static class TestListener extends TestListenerAdapter {
        public List<String> failures = new ArrayList<>();
        public List<String> skips = new ArrayList<>();
        public int totalRun = 0;

        @Override
        public void onTestFailure(ITestResult result) {
            Throwable cause = result.getThrowable();
            String info = String.format("[Failure] %s: ", result.getName());
            if (cause != null) {
                info += cause.getMessage();
            }
            else info += "Unknown cause";
            failures.add(info);
            totalRun += 1;
        }

        @Override
        public void onTestSuccess(ITestResult result) {
            totalRun += 1;
        }

        @Override
        public void onTestSkipped(ITestResult result) {
            skips.add(String.format("[Skipped] %s", result.getName()));
            totalRun += 1;
        }
    }
}
