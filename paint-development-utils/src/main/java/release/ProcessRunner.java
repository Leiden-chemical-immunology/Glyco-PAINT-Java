package release;

import java.io.*;
import java.util.*;

final class ProcessRunner {
    private ProcessRunner() {}

    static void enforceJava8(ProcessBuilder pb) {
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

    static Process startAndFilterOutput(ProcessBuilder pb, String moduleName) throws IOException {
        pb.redirectErrorStream(true);
        enforceJava8(pb);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("sun.misc.Unsafe") || line.contains("HiddenClassDefiner")) continue;
                System.out.println("[" + moduleName + "] " + line);
            }
        }
        return process;
    }
}