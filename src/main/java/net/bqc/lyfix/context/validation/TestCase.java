package net.bqc.lyfix.context.validation;

import java.util.Objects;

public class TestCase {

    public final String SEPARATOR = "#";
    private String className;
    private String methodName;

    public TestCase(String fullName) {
        if (fullName.contains(SEPARATOR)) {
            String[] parts = fullName.split(SEPARATOR);
            this.className = parts[0];
            this.methodName = parts[1];
        }
        else {
            this.className = fullName;
        }
    }

    public TestCase(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    public String getFullName() {
        StringBuilder builder = new StringBuilder(className);
        if (methodName != null)
            builder.append(SEPARATOR).append(methodName);
        return builder.toString();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public String toString() {
        return getFullName();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getFullName());
    }
}
