package net.bqc.lyfix.utils;

import net.bqc.lyfix.context.validation.executor.TestExecutionProcessLauncher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ShellUtils {

    private static final int DEFAULT_TIMEOUT = 30;

    public static boolean execute(String executionDir, String fullCommand) {
        String[] parts = fullCommand.split(" ");
        String command = parts[0];
        List<String> options = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            options.add(parts[i]);
        }
        return execute(executionDir, command, options.toArray(new String[0]), DEFAULT_TIMEOUT, true);
    }

    public static boolean execute(String executionDir, String command, String[] options, int timeout, boolean redirectOutput) {
        Process p = null;
        List<String> commands = new ArrayList<>();
        try {
            commands.add(command);
            commands.addAll(Arrays.asList(options));
            ProcessBuilder pb = new ProcessBuilder(commands.toArray(new String[0]));
            pb.directory(new File(executionDir));
            if (redirectOutput) {
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectErrorStream(true);
            }

            p = pb.start();
            TestExecutionProcessLauncher.Worker worker = new TestExecutionProcessLauncher.Worker(p);
            worker.start();
            worker.join(timeout);

            if (!p.waitFor(timeout, TimeUnit.MINUTES)) {
                p.destroy();
                return false;
            }

            p.destroy();
            return p.exitValue() == 0;
        }
        catch (InterruptedException | IOException ex) {
            if (p != null)
                p.destroy();
            throw new RuntimeException("Shell launcher encounters error");
        }
    }
}
