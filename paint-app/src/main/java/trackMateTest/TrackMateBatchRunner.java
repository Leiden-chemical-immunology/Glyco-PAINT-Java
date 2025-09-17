package trackMateTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TrackMateBatchRunner {

    public static void main(String[] args) throws IOException, InterruptedException {

        // Directory with .nd2 files to process
        File recordingsDir = new File("/Users/hans/Downloads/221012 Images");

        if (!recordingsDir.exists() || !recordingsDir.isDirectory()) {
            System.err.println("❌ Directory not found: " + recordingsDir.getAbsolutePath());
            return;
        }

        File[] nd2Files = recordingsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".nd2"));

        if (nd2Files == null || nd2Files.length == 0) {
            System.err.println("❌ No .nd2 files found in: " + recordingsDir.getAbsolutePath());
            return;
        }

        List<Process> processes = new ArrayList<>();

        for (File nd2File : nd2Files) {

            ProcessBuilder pb = new ProcessBuilder(
                    "/Users/hans/Library/Java/JavaVirtualMachines/ms-11.0.28/Contents/Home/bin/java",
                    "-javaagent:/Applications/Fiji.app/jars/ij1-patcher-1.2.9-SNAPSHOT.jar=init",
                    "-cp", "/Applications/Fiji.app/jars/*:/Users/hans/IdeaProjects/utilities/target/utilities-1.0-SNAPSHOT-jar-with-dependencies.jar",
                    "trackMateTest.TrackMateRunnerSingle",
                    nd2File.getAbsolutePath()
            );

            pb.inheritIO(); // For debugging: inherit output
            System.out.printf("✅ %s starting%n ", nd2File.getName());
            Process p = pb.start();
            processes.add(p);

            // Optional: wait for each to complete before starting next
            int exitCode = p.waitFor();
            System.out.printf("✅ %s exited with code %d%n", nd2File.getName(), exitCode);
        }
    }
}
