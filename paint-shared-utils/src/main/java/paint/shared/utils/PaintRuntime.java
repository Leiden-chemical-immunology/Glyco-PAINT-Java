/******************************************************************************
 *  Class:        PaintRuntime.java
 *  Package:      paint.shared.utils
 *
 *  PURPOSE:
 *    Utility class for managing runtime settings of the PAINT application,
 *    such as verbosity and log level. It interacts with preference storage
 *    (via {@link PaintPrefs}) to load and persist settings that affect how
 *    the application logs and reports information.
 *
 *  DESCRIPTION:
 *    • Provides initialization from stored preferences.
 *    • Allows querying whether verbose mode is enabled.
 *    • Allows retrieving and updating the current logging level.
 *    • Ensures that these configurations persist across sessions.
 *
 *  RESPONSIBILITIES:
 *    • Maintain and provide access to global runtime flags for logging behavior.
 *    • Persist changes to these flags into the preference store.
 *
 *  USAGE EXAMPLE:
 *    PaintRuntime.initialiseFromPrefs();
 *    if (PaintRuntime.isVerbose()) { … }
 *    PaintRuntime.setLogLevel("DEBUG");
 *
 *  DEPENDENCIES:
 *    – paint.shared.utils.PaintPrefs
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

/**
 * Provides centralized access to runtime configuration parameters
 * such as verbosity and log level.
 * <p>
 * This class loads and persists its configuration using {@link PaintPrefs}
 * and maintains thread-safe static access to global runtime flags.
 * </p>
 */
public final class PaintRuntime {

    // ───────────────────────────────────────────────────────────────────────────────
    // FIELDS
    // ───────────────────────────────────────────────────────────────────────────────

    private static boolean verbose;
    private static String logLevel;

    /**
     * Private constructor to prevent instantiation.
     */
    private PaintRuntime() {
        // Deliberately left blank
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // INITIALIZATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Initializes runtime settings by loading stored preferences.
     * <ul>
     *   <li>{@code verbose}: whether verbose logging is enabled
     *       (default {@code false}).</li>
     *   <li>{@code logLevel}: current log level string
     *       (default {@code "INFO"}).</li>
     * </ul>
     * <p>
     * Should be called once during application startup.
     * </p>
     */
    public static void initialiseFromPrefs() {
        verbose  = PaintPrefs.getBoolean("Runtime", "Verbose",   false);
        logLevel = PaintPrefs.getString ("Runtime", "Log Level", "INFO");
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // ACCESSORS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Checks whether verbose mode is currently active.
     *
     * @return {@code true} if verbose logging is enabled; otherwise {@code false}
     */
    public static boolean isVerbose() {
        return verbose;
    }

    /**
     * Returns the current log level as a string (e.g. "INFO", "DEBUG", "WARN").
     *
     * @return current log level string
     */
    public static String getLogLevel() {
        return logLevel;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // MUTATORS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Enables or disables verbose logging and persists the new value to preferences.
     *
     * @param v {@code true} to enable verbose logging; {@code false} to disable
     */
    public static void setVerbose(boolean v) {
        verbose = v;
        PaintPrefs.putBoolean("Runtime", "Verbose", v);
    }

    /**
     * Updates the global log level and persists the change to preferences.
     *
     * @param level new log level string (e.g. "DEBUG", "INFO", "WARN", "ERROR")
     */
    public static void setLogLevel(String level) {
        logLevel = level;
        PaintPrefs.putString("Runtime", "Log Level", level);
    }
}