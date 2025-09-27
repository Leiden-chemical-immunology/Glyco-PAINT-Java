package paint.shared.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.awt.Color;


public class PaintLogger {

    public enum Level {
        DEBUG(0, Color.GRAY),
        INFO(1, Color.BLACK),
        WARN(2, Color.ORANGE.darker()),
        ERROR(3, Color.RED);

        private final int rank;
        private final Color color;

        Level(int rank, Color color) {
            this.rank = rank;
            this.color = color;
        }
        public int rank() {
            return rank;
        }
        public Color color() {
            return color;
        }
    }

    private static BufferedWriter writer;
    private static boolean initialised = false;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static boolean justPrintedRaw = false;

    private static volatile Level currentLevel = Level.INFO;

    // --- Configuration ---

    public static void setLevel(Level level) {
        currentLevel = level;
        log(Level.INFO, "Log level set to: " + level);
    }

    public static Level getLevel() {
        return currentLevel;
    }

    /**
     * Initialise the logger: creates Logs directory and rotates file names.
     * The log file will be created in the project directory/Logs/paint-log-XX.log
     */
    public static void initialise(Path projectPath, String logBaseName) {
        try {
            Path logsDir = projectPath.resolve("Logs");
            if (!logsDir.toFile().exists()) {
                logsDir.toFile().mkdirs();
            }

            // Find next available numbered log file
            int index = 1;
            Path logFile;
            do {
                logFile = logsDir.resolve(String.format("%s-%d.log", logBaseName, index++));
            } while (logFile.toFile().exists());

            writer = new BufferedWriter(new FileWriter(logFile.toFile(), true));
            initialised = true;
            infof("Logger initialised: %s", logFile);
        } catch (IOException e) {
            System.err.println("PaintLogger could not initialise: " + e.getMessage());
        }
    }

    /**
     * Find the next available numbered log file in the directory.
     */
    private static Path nextLogFile(Path logPath, String baseName) throws IOException {
        final String prefix = baseName + "-";
        final String suffix = ".log";
        int maxIndex = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logPath, baseName + "-*.log")) {
            for (Path path : stream) {
                String name = path.getFileName().toString();
                if (name.startsWith(prefix) && name.endsWith(suffix)) {
                    String numberPart = name.substring(prefix.length(), name.length() - suffix.length());
                    try {
                        int index = Integer.parseInt(numberPart);
                        if (index > maxIndex) {
                            maxIndex = index;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        int nextIndex = maxIndex + 1;
        String nextFileName = String.format("%s-%02d%s", baseName, nextIndex, suffix);
        return logPath.resolve(nextFileName);
    }

    public static void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {}
        }
        initialised = false;
    }

    // --- Core logging ---
    private static void log(Level level, String message) {
        if (level.rank() < currentLevel.rank()) {
            return;
        }

        String timestamp = LocalDateTime.now().format(TIME_FMT);
        String formatted = String.format("%s [%-5s] %s", timestamp, level, message);

        if (justPrintedRaw) {
            PaintConsoleWindow.print("\n");
            justPrintedRaw = false;
        }

        PaintConsoleWindow.log(formatted, level.color());

        if (initialised && writer != null) {
            try {
                writer.write(formatted);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                System.err.println("PaintLogger failed to write log: " + e.getMessage());
            }
        }
    }

    // --- API methods ---

    public static void infof(String fmt, Object... args) {
        log(Level.INFO, String.format(fmt, args));
    }

    public static void infof() {
        log(Level.INFO, "");
    }

    public static void debugf(String fmt, Object... args) {
        log(Level.DEBUG, String.format(fmt, args));
    }

    public static void debugf() {
        log(Level.DEBUG, "");
    }

    public static void warningf(String fmt, Object... args) {
        log(Level.WARN, String.format(fmt, args));
    }

    public static void warningf() {
        log(Level.WARN, "");
    }

    public static void errorf(String fmt, Object... args) {
        log(Level.ERROR, String.format(fmt, args));
    }

    public static void errorf(Throwable t) {
        log(Level.ERROR, getStackTrace(t));
    }

    public static void errorf() {
        log(Level.ERROR, "");
    }

    private static String getStackTrace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.toString()).append("\n");
        for (StackTraceElement el : t.getStackTrace()) {
            sb.append("    at ").append(el.toString()).append("\n");
        }
        return sb.toString();
    }

    public static void raw(String text) {
        PaintConsoleWindow.print(text);
        justPrintedRaw = true;
    }

    public static void raw(char c) {
        PaintConsoleWindow.printChar(c);
        justPrintedRaw = true;
    }

    public static void blankline() {
        if (justPrintedRaw) {
            PaintConsoleWindow.print("\n");
            justPrintedRaw = false;
        }
        PaintConsoleWindow.log("", Color.BLACK); // empty line in GUI console

        if (initialised && writer != null) {
            try {
                writer.newLine(); // or: writer.write("[INFO ]"); writer.newLine();
                writer.flush();
            } catch (IOException e) {
                System.err.println("PaintLogger failed to write blank line: " + e.getMessage());
            }
        }
    }
}