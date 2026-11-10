/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package release.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Utility to execute external system processes and capture their output.
 */
public final class ProcessRunner {
    private ProcessRunner() {
    }

    /**
     * Points the process at an installed JDK 21 or newer (via /usr/libexec/java_home), so the
     * build does not depend on whichever Java happens to be the default.
     */
    public static void enforceJava21(ProcessBuilder pb) {
        try {
            Process proc = new ProcessBuilder("/usr/libexec/java_home", "-v", "21+").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String javaHome = reader.readLine();
                proc.waitFor();
                if (javaHome != null && !javaHome.isEmpty()) {
                    Map<String, String> env = pb.environment();
                    env.put("JAVA_HOME", javaHome);
                    env.put("PATH", javaHome + "/bin:" + env.get("PATH"));
                } else {
                    System.err.println("⚠️  Java 21 or newer not found; Maven will use the default JDK.");
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️  Could not select a Java 21 environment: " + e.getMessage());
        }
    }

    public static Process startAndFilterOutput(ProcessBuilder pb, String moduleName) throws IOException {
        pb.redirectErrorStream(true);
        enforceJava21(pb);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("sun.misc.Unsafe") || line.contains("HiddenClassDefiner")) {
                    continue;
                }
                System.out.println("[" + moduleName + "] " + line);
            }
        }
        return process;
    }
}