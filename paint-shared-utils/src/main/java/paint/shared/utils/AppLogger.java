package paint.shared.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;
import java.util.prefs.Preferences;

/**
 * Central logging utility for the Paint applications.
 * <p>
 * Provides both console and file-based logging with a simple,
 * consistent format. Supports configurable log levels via
 * preferences and runtime configuration.
 * </p>
 */
public class AppLogger {

    private static Logger logger;

    /**
     * Initialize the logger with a given log file name and set the
     * default logging level (from preferences).
     *
     * @param logFileName base name of the log file (without extension)
     */
    public static void init(String logFileName) {
        setupLogger(logFileName);
        setDefaultLoggingLevel();
    }

    /**
     * Initialize the logger with a given log file name and an explicit log level.
     *
     * @param logFileName       base name of the log file (without extension)
     * @param debugLevelString  log level name (e.g. "INFO", "DEBUG", "WARN")
     */
    public static void init(String logFileName, String debugLevelString) {
        setupLogger(logFileName);
        setLevel(debugLevelString);
    }

    /**
     * Internal helper to configure the logger, its handlers, and the log file.
     *
     * @param baseName base name for the log file
     */
    private static void setupLogger(String baseName) {
        logger = Logger.getLogger("Paint");
        logger.setUseParentHandlers(false);

        // Console handler
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new MessageOnlyFormatter());
        consoleHandler.setLevel(Level.ALL);
        logger.addHandler(consoleHandler);

        try {
            // Create log directory under ~/Paint/Logger if it does not exist
            Path logDirPath = Paths.get(System.getProperty("user.home"))
                    .resolve("Paint").resolve("Logger");
            Files.createDirectories(logDirPath);

            String logFileName = nextLogFileName(logDirPath, baseName);
            Path logFilePath = logDirPath.resolve(logFileName);

            FileHandler fileHandler = new FileHandler(logFilePath.toString(), false); // overwrite new file
            fileHandler.setFormatter(new MessageOnlyFormatter());
            fileHandler.setLevel(Level.ALL);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            consoleHandler.publish(new LogRecord(Level.WARNING,
                    "File logging disabled: " + e.getMessage()));
        }

        logger.setLevel(Level.ALL);
    }

    /**
     * Generate the next available log file name by incrementing a counter
     * until an unused name is found.
     *
     * @param baseName base name (prefix) for the log file
     * @return unique log file name with ".log" extension
     */
    private static String nextLogFileName(Path logDir, String baseName) {
        int counter = 0;
        while (true) {
            String name = baseName + "-" + counter + ".log";
            Path candidate = logDir.resolve(name);
            if (!Files.exists(candidate)) {
                return name;
            }
            counter++;
        }
    }

    /**
     * Custom log formatter that only prints the level and message.
     */
    static class MessageOnlyFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String level;
            if (record.getLevel() == Level.SEVERE) {
                level = "[ERROR]";
            } else if (record.getLevel() == Level.WARNING) {
                level = "[WARN ]";
            } else if (record.getLevel() == Level.INFO) {
                level = "[INFO ]";
            } else if (record.getLevel() == Level.FINE
                    || record.getLevel() == Level.FINER
                    || record.getLevel() == Level.FINEST) {
                level = "[DEBUG]";
            } else {
                level = "[" + record.getLevel().getName() + "]";
            }
            String timeLabel = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            return String.format("%s %s %s%s", timeLabel, level, record.getMessage(), System.lineSeparator());
        }
    }

    /**
     * Set the logging level for all handlers.
     *
     * @param level the {@link Level} to apply
     */
    public static void setLevel(Level level) {
        if (logger != null) {
            logger.setLevel(level);
            for (Handler handler : logger.getHandlers()) {
                handler.setLevel(level);
            }
        }
    }

    /**
     * Set the logging level by name, with support for common aliases
     * (e.g. "ERROR" = "SEVERE", "DEBUG" = "FINE").
     * Falls back to INFO if the name is unrecognized.
     *
     * @param levelName name of the logging level
     */
    public static void setLevel(String levelName) {
        Level level;

        switch (levelName.toUpperCase()) {
            case "SEVERE":
            case "ERROR":
                level = Level.SEVERE;
                break;
            case "WARNING":
            case "WARN":
                level = Level.WARNING;
                break;
            case "INFO":
                level = Level.INFO;
                break;
            case "CONFIG":
                level = Level.CONFIG;
                break;
            case "DEBUG":
            case "FINE":
                level = Level.FINE;
                break;
            case "FINER":
                level = Level.FINER;
                break;
            case "FINEST":
            case "TRACE":
                level = Level.FINEST;
                break;
            case "ALL":
                level = Level.ALL;
                break;
            case "OFF":
                level = Level.OFF;
                break;
            default:
                level = Level.INFO;
                warningf("Unknown log level '%s', defaulting to INFO", levelName);
        }
        setLevel(level);
    }

    // Convenience methods

    /**
     * Log an INFO level message with printf-style formatting.
     *
     * @param format format string
     * @param args   arguments
     */
    public static void infof(String format, Object... args) {
        logger.info(String.format(format, args));
    }

    /**
     * Log a WARNING level message with printf-style formatting.
     *
     * @param format format string
     * @param args   arguments
     */
    public static void warningf(String format, Object... args) {
        logger.warning(String.format(format, args));
    }

    /**
     * Log a SEVERE (error) level message with printf-style formatting.
     *
     * @param format format string
     * @param args   arguments
     */
    public static void errorf(String format, Object... args) {
        logger.severe(String.format(format, args));
    }

    /**
     * Log a FINE (debug) level message with printf-style formatting.
     *
     * @param format format string
     * @param args   arguments
     */
    public static void debugf(String format, Object... args) {
        logger.fine(String.format(format, args));
    }

    /**
     * Sets the logging level from user preferences (Mac: {@code ~/Library/Preferences/com.apple.java.util.prefs.plist}).
     * <p>
     * Node: {@code Glyco-PAINT}, Key: {@code loggingLevel}.
     * Defaults to "Info" if missing.
     * </p>
     * This allows persistent configuration of logging verbosity across runs.
     */
    public static void setDefaultLoggingLevel() {
        String PREF_NODE = "Glyco-PAINT";
        String LOGGING_LEVEL = "loggingLevel";

        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        String loggingLevel = prefs.get(LOGGING_LEVEL, "Info");
        if (loggingLevel == null) {
            loggingLevel = "Info";
            prefs.put(LOGGING_LEVEL, loggingLevel);  // store default in plist
        }
        setLevel(loggingLevel);
        System.out.println("Logging level set to " + loggingLevel);
        AppLogger.infof("Logging level set to %s", loggingLevel);
    }
}