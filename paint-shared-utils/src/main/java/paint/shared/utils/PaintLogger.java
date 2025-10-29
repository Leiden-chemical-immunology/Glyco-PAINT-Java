/******************************************************************************
 *  Class:        PaintLogger.java
 *  Package:      paint.shared.utils
 *
 *  PURPOSE:
 *    Provides structured, color-coded logging functionality for the PAINT framework.
 *
 *  DESCRIPTION:
 *    The {@code PaintLogger} class centralizes logging for both console and file
 *    outputs. It supports multiple log levels (DEBUG, INFO, WARN, ERROR), color
 *    display through {@link PaintConsoleWindow}, and persistent logging to disk.
 *
 *    Messages below the current log level threshold are automatically filtered.
 *    Log files are created within a "Logs" directory under a specified project
 *    path. This class also supports aligned documentation block output and
 *    raw printing for unformatted data.
 *
 *  KEY FEATURES:
 *    • Multi-level logging with severity filtering.
 *    • Color-coded console output integrated with {@link PaintConsoleWindow}.
 *    • Automatic log file rotation and initialization under a "Logs" folder.
 *    • Thread-safe static API with Java 8 compatibility.
 *    • Documentation-style formatted block printing for structured logs.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.shared.utils;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Static logger class for color-coded console and file logging.
 * <p>
 * Supports multiple severity levels, formatted messages, and persistent
 * log storage under a project’s "Logs" directory.
 */
public final class  PaintLogger {

    // ───────────────────────────────────────────────────────────────────────────────
    // ENUM: LOG LEVEL
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Defines log levels with severity rank and display color.
     */
    public enum Level {
        
        DEBUG(0, Color.GRAY),
        INFO( 1, Color.BLACK),
        WARN( 2, Color.ORANGE.darker()),
        ERROR(3, Color.RED);
        

        private final int   rank;
        private final Color color;

        Level(int rank, Color color) {
            this.rank  = rank;
            this.color = color;
        }

        /**
         * @return numeric severity rank (lower = less severe).
         */
        public int rank() {
            return rank;
        }

        /**
         * @return color associated with this log level for GUI display.
         */
        public Color color() {
            return color;
        }
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // INTERNAL STATE
    // ───────────────────────────────────────────────────────────────────────────────

    
    private static          BufferedWriter    writer;
    private static          boolean           initialised    = false;
    private static final    DateTimeFormatter TIME_FMT       = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static          boolean           justPrintedRaw = false;
    private static volatile Level             currentLevel   = Level.INFO;
    

    // ───────────────────────────────────────────────────────────────────────────────
    // CONFIGURATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Sets the current global log level. Messages below this level are suppressed.
     *
     * @param level desired {@link Level}
     */
    public static void setLevel(Level level) {
        currentLevel = level;
        log(Level.INFO, "Log level set to: " + level);
    }

    /**
     * Sets the log level using a string value. Accepts:
     * {@code "DEBUG"}, {@code "INFO"}, {@code "WARN"}, {@code "WARNING"}, {@code "ERROR"}.
     * Defaults to INFO for unknown or null input.
     *
     * @param level textual level name
     */
    public static void setLevel(String level) {
        if (level == null) {
            setLevel(Level.INFO);
            return;
        }

        switch (level) {
            case "DEBUG":
                setLevel(Level.DEBUG);
                break;
            case "INFO":
                setLevel(Level.INFO);
                break;
            case "WARNING":
            case "WARN":
                setLevel(Level.WARN);
                break;
            case "ERROR":
                setLevel(Level.ERROR);
                break;
            default:
                setLevel(Level.INFO);
                break;
        }
    }

    /**
     * Initializes the logger by creating a "Logs" directory and a new numbered log file.
     * Logs are written to both file and GUI console.
     *
     * @param projectPath base directory under which the "Logs" folder will be created
     * @param logBaseName base name used for the log file
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
     * Closes the current log writer if open.
     */
    public static void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
                // Deliberately empty
            }
        }
        initialised = false;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // CORE LOGGING
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Performs actual log output to both console and file.
     *
     * @param level   log severity level
     * @param message formatted message text
     */
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

    // ───────────────────────────────────────────────────────────────────────────────
    // PUBLIC LOGGING API
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Logs an INFO message with printf-style formatting.
     */
    public static void infof(String fmt, Object... args) {
        log(Level.INFO, String.format(fmt, args));
    }

    /**
     * Logs an empty INFO line.
     */
    public static void infof() {
        log(Level.INFO, "");
    }

    /**
     * Logs a DEBUG message with printf-style formatting.
     */
    public static void debugf(String fmt, Object... args) {
        log(Level.DEBUG, String.format(fmt, args));
    }

    /**
     * Logs an empty DEBUG line.
     */
    public static void debugf() {
        log(Level.DEBUG, "");
    }

    /**
     * Logs a WARN message with printf-style formatting.
     */
    public static void warnf(String fmt, Object... args) {
        log(Level.WARN, String.format(fmt, args));
    }

    /**
     * Logs an empty WARN line.
     */
    public static void warnf() {
        log(Level.WARN, "");
    }

    /**
     * Logs an ERROR message with printf-style formatting.
     */
    public static void errorf(String fmt, Object... args) {
        log(Level.ERROR, String.format(fmt, args));
    }

    /**
     * Logs an ERROR stack trace for a {@link Throwable}.
     */
    public static void errorf(Throwable t) {
        log(Level.ERROR, getStackTrace(t));
    }

    /**
     * Logs an empty ERROR line.
     */
    public static void errorf() {
        log(Level.ERROR, "");
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // SPECIALIZED OUTPUT
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Prints text directly to console without timestamps or level formatting.
     * Useful for inline output such as progress bars.
     *
     * @param text raw text to print
     */
    public static void raw(String text) {
        PaintConsoleWindow.print(text);
        justPrintedRaw = true;
    }

    /**
     * Inserts a blank line in the console and log file.
     */
    public static void blankline() {
        if (justPrintedRaw) {
            PaintConsoleWindow.print("\n");
            justPrintedRaw = false;
        }
        PaintConsoleWindow.log("", Color.BLACK);

        if (initialised && writer != null) {
            try {
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                System.err.println("PaintLogger failed to write blank line: " + e.getMessage());
            }
        }
    }

    /**
     * Prints a structured documentation-style block to console and file.
     *
     * @param header header text of the block
     * @param lines  iterable list of lines to display below the header
     */
    public static void doc(String header, Iterable<String> lines) {
        blankline();
        log(Level.INFO, header);

        // Compute indentation (exactly up to one space after [INFO ])
        String prefix = String.format("%s [%-5s]", LocalDateTime.now().format(TIME_FMT), Level.INFO);
        String indent = repeat(" ", prefix.length() + 1);

        // Print continuation lines without the timestamp, perfectly aligned
        for (String line : lines) {
            String formatted = indent + line;
            PaintConsoleWindow.log(formatted, Level.INFO.color());

            if (initialised && writer != null) {
                try {
                    writer.write(formatted);
                    writer.newLine();
                    writer.flush();
                } catch (IOException e) {
                    System.err.println("PaintLogger failed to write doc line: " + e.getMessage());
                }
            }
        }
        blankline();
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // INTERNAL UTILITIES
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns formatted stack trace text for a given throwable.
     */
    private static String getStackTrace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.toString()).append("\n");
        for (StackTraceElement el : t.getStackTrace()) {
            sb.append("    at ").append(el.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Java 8-compatible implementation of String.repeat(int).
     */
    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}