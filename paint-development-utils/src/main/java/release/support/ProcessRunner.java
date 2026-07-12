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

    public static void enforceJava8(ProcessBuilder pb) {
        try {
            Process proc = new ProcessBuilder("/usr/libexec/java_home", "-v", "1.8").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String java8Home = reader.readLine();
                proc.waitFor();
                if (java8Home != null && !java8Home.isEmpty()) {
                    Map<String, String> env = pb.environment();
                    env.put("JAVA_HOME", java8Home);
                    env.put("PATH", java8Home + "/bin:" + env.get("PATH"));
                } else {
                    System.err.println("⚠️  Java 8 not found; Maven may build with a newer JDK.");
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️  Could not enforce Java 8 environment: " + e.getMessage());
        }
    }

    public static Process startAndFilterOutput(ProcessBuilder pb, String moduleName) throws IOException {
        pb.redirectErrorStream(true);
        enforceJava8(pb);
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