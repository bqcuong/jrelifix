/**
 * Source: https://github.com/SpoonLabs/astor/raw/master/src/main/java/fr/inria/astor/core/validation/results/TestResult.java
 */
package net.bqc.lyfix.context.validation.executor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Matias Martinez,  matias.martinez@inria.fr
 */
public class TestResult {

    public List<String> successTest = new ArrayList<>();
    public List<String> failTest = new ArrayList<>();
    public List<String> ignoredTest = new ArrayList<>();

    public void setSuccessTest(List<String> successTest) {
        this.successTest = successTest;
    }

    public void setFailTest(List<String> failTest) {
        this.failTest = failTest;
    }

    public boolean wasSuccessful() {
        return failTest.size() == 0;
    }

    @Override
    public String toString() {
        return "TR: Success: " + (failTest.size() == 0) + ", failTest= "
                + failTest.size() + ", was successful: " + this.wasSuccessful() + ", cases executed: " + (failTest.size() + successTest.size()) + "] ," + this.failTest;
    }
}