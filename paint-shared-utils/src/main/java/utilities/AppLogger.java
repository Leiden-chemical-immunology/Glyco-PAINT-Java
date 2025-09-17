package utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.*;
import java.util.prefs.Preferences;

public class AppLogger {

    private static Logger logger;

    public static void init(String logFileName) {
        setupLogger(logFileName);
        setDefaultLoggingLevel();
    }

    public static void init(String logFileName, String debugLevelString) {
        setupLogger(logFileName);
        setLevel(debugLevelString);
    }

    private static void setupLogger(String baseName) {
        logger = Logger.getLogger("Paint");
        logger.setUseParentHandlers(false);

        // Console handler
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new MessageOnlyFormatter());
        consoleHandler.setLevel(Level.ALL);
        logger.addHandler(consoleHandler);

        try {
            // base directory where logs should live (adjust as needed)
            Path logDirPath = Paths.get(System.getProperty("user.home"));
            logDirPath = logDirPath.resolve("Paint").resolve("Logger");
            Files.createDirectories(logDirPath);

            // full path to the log file
            String logFileName = nextLogFileName(baseName);
            Path logFilePath = logDirPath.resolve(logFileName);

            FileHandler fileHandler = new FileHandler(logFilePath.toString(), false); // always fresh file
            fileHandler.setFormatter(new MessageOnlyFormatter());
            fileHandler.setLevel(Level.ALL);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            consoleHandler.publish(new LogRecord(Level.WARNING,
                    "File logging disabled: " + e.getMessage()));
        }

        logger.setLevel(Level.ALL);
    }

    private static String nextLogFileName(String baseName) {
        int counter = 0;
        while (true) {
            String name = baseName + "-" + counter + ".log";
            if (!Files.exists(Paths.get(name))) {
                return name; // first unused filename
            }
            counter++;
        }
    }

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
            return level + " " + record.getMessage() + System.lineSeparator();
        }
    }

    public static void setLevel(Level level) {
        if (logger != null) {
            logger.setLevel(level);
            for (Handler handler : logger.getHandlers()) {
                handler.setLevel(level);
            }
        }
    }

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
                // fallback if user passes something unrecognized
                level = Level.INFO;
                warningf("Unknown log level '%s', defaulting to INFO", levelName);
        }
        setLevel(level);
    }


    // Convenience methods

    public static void infof(String format, Object... args) {
        logger.info(String.format(format, args));
    }

    public static void warningf(String format, Object... args) {
        logger.warning(String.format(format, args));
    }

    public static void errorf(String format, Object... args) {
        logger.severe(String.format(format, args));
    }

    public static void debugf(String format, Object... args) {
        logger.fine(String.format(format, args));
    }

    public static void setDefaultLoggingLevel() {
        // Set up logging
        // LoggingLevel is retrieved from ~/Library/Preferences/com.apple.java.util.prefs.plist
        // The default is Info. If the key does not exist, it is created and set to Info.
        // Value can be edited with XCode (double-click on the plist)

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