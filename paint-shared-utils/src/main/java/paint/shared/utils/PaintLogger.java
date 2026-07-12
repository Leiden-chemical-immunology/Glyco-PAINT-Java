/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.shared.utils;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Static logger class for color-coded console and file logging.
 * <p>
 * Supports multiple severity levels, formatted messages, and persistent
 * log storage under a project’s "Logs" directory.
 */
public final class PaintLogger {

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
    /** Guards all access to the shared {@link #writer} (logging happens from multiple threads). */
    private static final    Object            WRITER_LOCK    = new Object();
    private static final    DateTimeFormatter TIME_FMT       = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static          boolean           justPrintedRaw = false;
    private static volatile Level             currentLevel   = Level.INFO;


    // ───────────────────────────────────────────────────────────────────────────────
    // OUTPUT SINK  (dependency inversion — this module must stay free of any UI)
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * An optional extra destination for log output — in practice the Swing console window,
     * which lives in the UI module.
     * <p>
     * {@code PaintLogger} deliberately knows nothing about Swing. A GUI application registers
     * its console through {@link #setSink}; a headless run (the pipeline, CI, a server) simply
     * never registers one, so no UI class is even loaded on that path. This is a structural
     * guarantee rather than a runtime {@code isHeadless()} check.
     */
    public interface Sink {

        /** A fully formatted line, to be displayed in the given colour. */
        void log(String line, Color color);

        /** Raw text: no timestamp, no level, no trailing newline. */
        void print(String text);
    }

    /** {@code null} whenever there is no UI attached. */
    private static volatile Sink sink;

    /** Registers the output sink. Called by the UI layer when its console is created. */
    public static void setSink(Sink newSink) {
        sink = newSink;
    }

    /** Detaches the output sink (e.g. when the console window is closed). */
    public static void clearSink() {
        sink = null;
    }

    /**
     * Forwards a line to the sink, if one is attached. A failing sink must never break
     * logging or the calling pipeline, so exceptions from it are contained here.
     */
    private static void sinkLog(String line, Color color) {
        Sink s = sink;
        if (s == null) {
            return;
        }
        try {
            s.log(line, color);
        } catch (RuntimeException ignored) {
            // Deliberately swallowed: the file log has already been written.
        }
    }

    /** Forwards raw text to the sink, if one is attached. See {@link #sinkLog}. */
    private static void sinkPrint(String text) {
        Sink s = sink;
        if (s == null) {
            return;
        }
        try {
            s.print(text);
        } catch (RuntimeException ignored) {
            // Deliberately swallowed: the file log has already been written.
        }
    }


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
            case "WARNING":
            case "WARN":
                setLevel(Level.WARN);
                break;
            case "ERROR":
                setLevel(Level.ERROR);
                break;
            case "INFO":
            default:
                setLevel(Level.INFO);
                break;
        }
    }

    /**
     * Returns the current log level as an uppercase string.
     *
     * @return the name of the current log level (e.g., "DEBUG", "INFO").
     */
    public static String getLevelName() {
        return currentLevel.name();
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

            synchronized (WRITER_LOCK) {
                writer = new BufferedWriter(new FileWriter(logFile.toFile(), true));
                initialised = true;
            }
            infof("Logger initialised: %s", logFile);
        } catch (IOException e) {
            System.err.println("PaintLogger could not initialise: " + e.getMessage());
        }
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
            sinkPrint("\n");
            justPrintedRaw = false;
        }

        // File first: the persistent log must never be lost because a UI sink misbehaved.
        writeLineToFile(formatted);

        sinkLog(formatted, level.color());
    }

    /**
     * Writes a single line to the log file, if a writer is active. Synchronized
     * on {@link #WRITER_LOCK} so concurrent log calls from worker/watchdog
     * threads cannot interleave or corrupt the shared buffered writer.
     *
     * @param text the line text (may be empty for a blank line)
     */
    private static void writeLineToFile(String text) {
        synchronized (WRITER_LOCK) {
            if (initialised && writer != null) {
                try {
                    writer.write(text);
                    writer.newLine();
                    writer.flush();
                } catch (IOException e) {
                    System.err.println("PaintLogger failed to write log: " + e.getMessage());
                }
            }
        }
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // PUBLIC LOGGING API
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Logs an INFO message with printf-style formatting.
     *
     * @param fmt  printf-style format string
     * @param args arguments for the format string
     */
    public static void infof(String fmt, Object... args) {
        log(Level.INFO, String.format(fmt, args));
    }

    /**
     * Logs a DEBUG message with printf-style formatting.
     *
     * @param fmt  printf-style format string
     * @param args arguments for the format string
     */
    public static void debugf(String fmt, Object... args) {
        log(Level.DEBUG, String.format(fmt, args));
    }

    /**
     * Logs a WARN message with printf-style formatting.
     *
     * @param fmt  printf-style format string
     * @param args arguments for the format string
     */
    public static void warnf(String fmt, Object... args) {
        log(Level.WARN, String.format(fmt, args));
    }

    /**
     * Logs an ERROR message with printf-style formatting.
     *
     * @param fmt  printf-style format string
     * @param args arguments for the format string
     */
    public static void errorf(String fmt, Object... args) {
        log(Level.ERROR, String.format(fmt, args));
    }

    /**
     * Logs an ERROR message together with the full stack trace of a throwable.
     * Prefer this over {@code printStackTrace()} / {@code System.err} so the
     * diagnostic reaches the log file and console like any other message.
     *
     * @param message descriptive context for the error
     * @param t       the throwable whose stack trace should be logged (may be null)
     */
    public static void error(String message, Throwable t) {
        log(Level.ERROR, message);
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            log(Level.ERROR, sw.toString());
        }
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
        sinkPrint(text);
        justPrintedRaw = true;
    }

    /**
     * Inserts a blank line in the console and log file.
     */
    public static void blankline() {
        if (justPrintedRaw) {
            sinkPrint("\n");
            justPrintedRaw = false;
        }

        writeLineToFile("");

        sinkLog("", Color.BLACK);
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

            writeLineToFile(formatted);

            sinkLog(formatted, Level.INFO.color());
        }
        blankline();
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // INTERNAL UTILITIES
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Java 8-compatible implementation of String.repeat(int).
     */
    @SuppressWarnings("SameParameterValue")
    private static String repeat(String string, int count) {
        StringBuilder sb = new StringBuilder(string.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(string);
        }
        return sb.toString();
    }
}