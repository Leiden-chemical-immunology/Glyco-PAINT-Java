package paint.shared.utils;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Central logging utility for the Paint application.
 * <p>
 * Provides both console and file logging with configurable log levels,
 * timestamp formatting, and color-coded output in the GUI console.
 * </p>
 * <p>
 * Features:
 * <ul>
 *   <li>Four log levels: DEBUG, INFO, WARN, and ERROR</li>
 *   <li>Automatic timestamp formatting</li>
 *   <li>Optional file-based logging with log rotation</li>
 *   <li>Colored output in {@link PaintConsoleWindow}</li>
 *   <li>Java 8 compatibility (custom string repeat method)</li>
 * </ul>
 */
public class PaintLogger {

    /**
     * Represents a log level with associated severity rank and display color.
     */
    public enum Level {

        // @formatter: off
        DEBUG(0, Color.GRAY),
        INFO( 1, Color.BLACK),
        WARN( 2, Color.ORANGE.darker()),
        ERROR(3, Color.RED);
        // @formatter: on

        private final int rank;
        private final Color color;

        Level(int rank, Color color) {
            this.rank = rank;
            this.color = color;
        }

        /** @return numeric severity rank (lower = less severe). */
        public int rank() {
            return rank;
        }

        /** @return color associated with this log level for GUI display. */
        public Color color() {
            return color;
        }
    }

    // @formatter:off
    private static       BufferedWriter    writer;
    private static       boolean           initialised    = false;
    private static final DateTimeFormatter TIME_FMT       = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static       boolean           justPrintedRaw = false;
    // @formatter:on

    private static volatile Level currentLevel = Level.INFO;

    // ---------------------------------------------------------------------
    // Configuration
    // ---------------------------------------------------------------------

    /**
     * Sets the current log level.
     * Messages below this level will be suppressed.
     *
     * @param level the desired {@link Level}
     */
    public static void setLevel(Level level) {
        currentLevel = level;
        log(Level.INFO, "Log level set to: " + level);
    }

    /**
     * Sets the log level from a string value.
     * Accepts {@code "DEBUG"}, {@code "INFO"}, {@code "WARN"}, {@code "WARNING"}, or {@code "ERROR"}.
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
     * Initializes the logger by creating a {@code Logs} directory under the project path
     * and opening a new log file (e.g., {@code paint-log-1.log}, {@code paint-log-2.log}, etc.).
     *
     * @param projectPath  base project directory
     * @param logBaseName  base name for the log file
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
     * Closes the current log writer, if open.
     */
    public static void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
        }
        initialised = false;
    }

    // ---------------------------------------------------------------------
    // Core logging
    // ---------------------------------------------------------------------

    /**
     * Core method performing the actual log output.
     *
     * @param level   log severity
     * @param message message text
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

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /**
     * Logs an INFO message using {@link String#format(String, Object...)}.
     *
     * @param fmt  format string
     * @param args arguments referenced by the format specifiers
     */
    public static void infof(String fmt, Object... args) {
        log(Level.INFO, String.format(fmt, args));
    }

    /** Logs an empty INFO line. */
    public static void infof() {
        log(Level.INFO, "");
    }

    /**
     * Logs a DEBUG message using {@link String#format(String, Object...)}.
     *
     * @param fmt  format string
     * @param args arguments referenced by the format specifiers
     */
    public static void debugf(String fmt, Object... args) {
        log(Level.DEBUG, String.format(fmt, args));
    }

    /** Logs an empty DEBUG line. */
    public static void debugf() {
        log(Level.DEBUG, "");
    }

    /**
     * Logs a WARN message using {@link String#format(String, Object...)}.
     *
     * @param fmt  format string
     * @param args arguments referenced by the format specifiers
     */
    public static void warnf(String fmt, Object... args) {
        log(Level.WARN, String.format(fmt, args));
    }

    /** Logs an empty WARN line. */
    public static void warnf() {
        log(Level.WARN, "");
    }

    /**
     * Logs an ERROR message using {@link String#format(String, Object...)}.
     *
     * @param fmt  format string
     * @param args arguments referenced by the format specifiers
     */
    public static void errorf(String fmt, Object... args) {
        log(Level.ERROR, String.format(fmt, args));
    }

    /**
     * Logs a full stack trace for a throwable at {@link Level#ERROR}.
     *
     * @param t the thrown exception or error
     */
    public static void errorf(Throwable t) {
        log(Level.ERROR, getStackTrace(t));
    }

    /** Logs an empty ERROR line. */
    public static void errorf() {
        log(Level.ERROR, "");
    }

    /**
     * Builds and returns a formatted stack trace string for the given throwable.
     *
     * @param t the exception or error
     * @return formatted stack trace text
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
     * Prints text directly to the console window without timestamp or level formatting.
     * Useful for raw data output (e.g., progress bars).
     *
     * @param text the text to print
     */
    public static void raw(String text) {
        PaintConsoleWindow.print(text);
        justPrintedRaw = true;
    }

    /**
     * Inserts a blank line in both console and log file.
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
     * Prints a formatted documentation block to the console and log file.
     * <p>
     * The header is logged with standard INFO formatting, followed by indented continuation lines.
     * </p>
     *
     * @param header descriptive header line
     * @param lines  iterable sequence of lines to print below the header
     */
    public static void doc(String header, Iterable<String> lines) {
        // Blank line before block
        blankline();

        // Print the main header line using standard INFO formatting
        log(Level.INFO, header);

        // Compute indentation (exactly up to one space after [INFO ])
        String prefix = String.format("%s [%-5s]",
                                      LocalDateTime.now().format(TIME_FMT), Level.INFO);
        String indent = repeat(" ", prefix.length() + 1);

        // Print continuation lines without timestamp, perfectly aligned
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

        // Blank line after block
        blankline();
    }

    /**
     * Replacement for the Java 11 {@code String.repeat(int)} method
     * to maintain Java 8 compatibility.
     *
     * @param s     string to repeat
     * @param count number of repetitions
     * @return concatenated string repeated {@code count} times
     */
    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) sb.append(s);
        return sb.toString();
    }
}