package paint.localUtilities;

import java.io.IOException;
import java.util.logging.*;

public class AppLoggerOld {
    private static Logger LOGGER;

    /**
     * Initialize logger with default settings.
     * - Level: INFO
     * - Rotating file handler: 5 MB per file, 3 files kept
     * - Append: as provided
     */
    public static void init(String logFileName, boolean append) {
        init(logFileName, append, Level.INFO, 5 * 1024 * 1024, 3);
    }

    /**
     * Initialize logger with configurable level and rotating file handler.
     *
     * @param logFileName Log file name or pattern. If a plain name is provided, rotation indices will be appended.
     *                    You can also use java.util.logging.FileHandler pattern tokens like %u and %g if desired.
     * @param append      Whether to append to existing files.
     * @param level       Log level to use for logger and handlers.
     * @param limitBytes  Approximate max size in bytes for each log file before rotation. Use >0 to enable rotation.
     * @param fileCount   Number of files to rotate through. Use >=1. When 1, rotation still applies by overwriting the same file.
     */
    public static void init(String logFileName, boolean append, Level level, int limitBytes, int fileCount) {
        LOGGER = Logger.getLogger("PaintLogger");
        LOGGER.setUseParentHandlers(false); // prevents double logging
        LOGGER.setLevel(level);

        // Remove any existing handlers to allow re-init with new config
        for (Handler h : LOGGER.getHandlers()) {
            try { h.close(); } catch (Exception ignored) {}
            LOGGER.removeHandler(h);
        }

        try {
            Formatter formatter = new SimpleFormatter() {
                @Override
                public synchronized String format(LogRecord record) {
                    return String.format("[%1$tF %1$tT] [%2$-7s] %3$s %n",
                            new java.util.Date(record.getMillis()),
                            record.getLevel().getName(),
                            record.getMessage()
                    );
                }
            };

            // 📁 File output with rotation
            Handler fileHandler;
            if (limitBytes > 0 && fileCount > 0) {
                // Ensure rotation keeps original extension by inserting %g before the extension
                String pattern = ensureRotationPatternKeepsExtension(logFileName);
                fileHandler = new FileHandler(pattern, limitBytes, fileCount, append);
            } else {
                // Fallback to simple file handler if rotation not desired
                fileHandler = new FileHandler(logFileName, append);
            }
            fileHandler.setFormatter(formatter);
            fileHandler.setLevel(level);
            LOGGER.addHandler(fileHandler);

            // 🖥️ Console output
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(formatter);
            consoleHandler.setLevel(level);
            // ConsoleHandler by default might filter below INFO; ensure handler and logger level are aligned
            LOGGER.addHandler(consoleHandler);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize logger", e);
        }
    }

    public static Logger getLogger() {
        if (LOGGER == null) {
            throw new IllegalStateException("Logger not initialized. Call AppLogger.init() first.");
        }
        return LOGGER;
    }

    /**
     * If a plain filename is provided for rotation, Java's FileHandler will append generation numbers
     * after the entire name (e.g., "file.log.0"), effectively changing the extension. This method
     * ensures the generation index is placed before the final extension to keep the extension intact
     * (e.g., "file.0.log"). If the pattern already includes %g or %u, it is returned unchanged.
     */
    private static String ensureRotationPatternKeepsExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) return fileName;
        // If the user already supplied a pattern, respect it.
        if (fileName.contains("%g") || fileName.contains("%u")) {
            return fileName;
        }
        int lastSlash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > lastSlash) {
            // Insert .%g before the extension
            return fileName.substring(0, lastDot) + ".%g" + fileName.substring(lastDot);
        } else {
            // No extension found; just append .%g
            return fileName + ".%g";
        }
    }
}