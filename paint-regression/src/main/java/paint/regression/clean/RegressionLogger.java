package paint.regression.clean;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

final class RegressionLogger {

    private final PrintStream console;
    private PrintStream file;
    private boolean fileLoggingEnabled = true;

    RegressionLogger(PrintStream console) {
        this.console = console;
    }

    void enableFileLogging(Path logDir) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        Path logFile = logDir.resolve("paint-regression-" + timestamp + ".log");
        Files.createDirectories(logFile.getParent());
        this.file = new PrintStream(Files.newOutputStream(logFile));
        println("🧾 Logging to: " + logFile.toAbsolutePath());
        println("");
    }

    void disableFileLogging() {
        fileLoggingEnabled = false;
    }

    void enableFileLoggingToCurrentFile() {
        fileLoggingEnabled = true;
    }

    void println(String msg) {
        console.println(msg);
        if (file != null && fileLoggingEnabled) {
            file.println(msg);
        }
    }

    void printf(String fmt, Object... args) {
        console.printf(fmt, args);
        if (file != null && fileLoggingEnabled) {
            file.printf(fmt, args);
        }
    }

    void flush() {
        console.flush();
        if (file != null && fileLoggingEnabled) {
            file.flush();
        }
    }

    /**
     * Compare the two latest log files in the given directory.
     * Uses console-only logging during the comparison.
     */
    void compareLatestLogWithPrevious(Path logDir) throws IOException {

        // console-only while comparing logs
        boolean oldFlag = fileLoggingEnabled;
        fileLoggingEnabled = false;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logDir, "paint-regression-*.log")) {
            List<Path> logs = new ArrayList<Path>();
            for (Path p : stream) {
                logs.add(p);
            }

            if (logs.size() < 2) {
                println("ℹ️ Not enough log files to compare (" + logs.size() + " found).");
                return;
            }

            Collections.sort(logs);

            Path previous = logs.get(logs.size() - 2);
            Path latest   = logs.get(logs.size() - 1);

            println("");
            println("🧪 Comparing log files:");
            println("   Previous: " + previous.getFileName());
            println("   Latest  : " + latest.getFileName());
            println("");

            List<String> oldLines = Files.readAllLines(previous);
            List<String> newLines = Files.readAllLines(latest);

            int max = Math.max(oldLines.size(), newLines.size());
            int differences = 0;

            for (int i = 0; i < max; i++) {
                String oldL = (i < oldLines.size()) ? oldLines.get(i) : "";
                String newL = (i < newLines.size()) ? newLines.get(i) : "";

                if (!Objects.equals(oldL, newL)) {
                    printf("🔸 Line %d:%n", (i + 1));
                    printf("     OLD: %s%n", oldL);
                    printf("     NEW: %s%n", newL);
                    differences++;
                }
            }

            if (differences == 0) {
                println("✅ No differences in log files.");
            } else {
                println("\n📊 Total log differences detected: " + differences);
            }
        } finally {
            fileLoggingEnabled = oldFlag;
        }
    }
}