package paint.shared.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.awt.Color;

/**
 * Drop-in replacement PaintLogger.
 * - Full feature parity with original.
 * - Routes all messages to PaintConsoleWindow (with colors).
 * - Supports file logging.
 * - Supports log levels.
 */
public class PaintLogger {

    public enum Level {
        DEBUG(0, Color.GRAY),
        INFO(1, Color.BLACK),
        WARNING(2, Color.ORANGE.darker()),
        ERROR(3, Color.RED);

        private final int rank;
        private final Color color;
        Level(int rank, Color color) {
            this.rank = rank;
            this.color = color;
        }
        public int rank() { return rank; }
        public Color color() { return color; }
    }

    private static BufferedWriter writer;
    private static boolean initialised = false;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static volatile Level currentLevel = Level.INFO;

    // --- Configuration ---

    public static void setLevel(Level level) {
        currentLevel = level;
        log(Level.INFO, "Log level set to: " + level);
    }

    public static Level getLevel() {
        return currentLevel;
    }

    public static void initialise(Path logDir, String logName) {
        try {
            if (!logDir.toFile().exists()) {
                logDir.toFile().mkdirs();
            }
            writer = new BufferedWriter(new FileWriter(
                    logDir.resolve(logName + ".log").toFile(), true));
            initialised = true;
            infof("Logger initialised: %s", logDir.resolve(logName + ".log"));
        } catch (IOException e) {
            System.err.println("PaintLogger could not initialise: " + e.getMessage());
        }
    }

    public static void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {}
        }
        initialised = false;

        // Close the console window too
        // PaintConsoleWindow.close();
    }

    // --- Core logging ---
    private static void log(Level level, String message) {
        if (level.rank() < currentLevel.rank()) {
            return; // skip messages below current log level
        }

        String timestamp = LocalDateTime.now().format(TIME_FMT);
        String formatted = String.format("%s [%s] %s", timestamp, level, message);

        // Paint console (colored by level)
        PaintConsoleWindow.log(formatted, level.color());

        // File logging
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

    // --- API methods (same as before) ---

    // Info
    public static void infof(String fmt, Object... args) {
        log(Level.INFO, String.format(fmt, args));
    }
    public static void infof() { log(Level.INFO, ""); }

    // Debug
    public static void debugf(String fmt, Object... args) {
        log(Level.DEBUG, String.format(fmt, args));
    }
    public static void debugf() { log(Level.DEBUG, ""); }

    // Warning
    public static void warningf(String fmt, Object... args) {
        log(Level.WARNING, String.format(fmt, args));
    }
    public static void warningf() { log(Level.WARNING, ""); }

    // Error
    public static void errorf(String fmt, Object... args) {
        log(Level.ERROR, String.format(fmt, args));
    }
    public static void errorf(Throwable t) {
        log(Level.ERROR, getStackTrace(t));
    }
    public static void errorf() { log(Level.ERROR, ""); }

    // --- Helpers ---
    private static String getStackTrace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.toString()).append("\n");
        for (StackTraceElement el : t.getStackTrace()) {
            sb.append("    at ").append(el.toString()).append("\n");
        }
        return sb.toString();
    }
}