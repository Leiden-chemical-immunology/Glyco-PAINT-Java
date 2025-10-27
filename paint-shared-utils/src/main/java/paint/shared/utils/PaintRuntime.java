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
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-27
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.shared.utils;

/**
 * The {@code PaintRuntime} class is a utility class that manages runtime settings
 * for controlling verbosity of logs and the logging level within the application.
 * It interacts with {@link PaintPrefs} to load and persist these settings in a
 * preference storage system.
 *
 * <p>This class is non-instantiable and consists entirely of static methods and fields.
 *
 * Core responsibilities:
 * <ul>
 *   <li>Manage the {@code verbose} setting to enable or disable verbose logging.</li>
 *   <li>Manage the {@code logLevel} setting to configure the desired logging level.</li>
 *   <li>Initialize these settings using default values or preferences stored via {@link PaintPrefs}.</li>
 * </ul>
 */
public final class PaintRuntime {

    private static boolean verbose;
    private static String  logLevel;

    private PaintRuntime() {
        // Prevent instantiation
    }

    /**
     * Initializes the application runtime settings by loading values from preferences.
     * Specifically:
     * <ul>
     *   <li>{@code verbose}: whether verbose logging is enabled, defaulted to {@code false}
     *       if the preference is not yet set.</li>
     *   <li>{@code logLevel}: the logging level as a string, defaulting to {@code "INFO"} if
     *       the preference is not yet set.</li>
     * </ul>
     * <p>This method must be called during application startup to ensure correct
     * initial behavior of logging and verbosity flags.
     */
    public static void initialiseFromPrefs() {
        verbose  = PaintPrefs.getBoolean("Runtime", "Verbose",   false);
        logLevel = PaintPrefs.getString ("Runtime", "Log Level", "INFO");
    }

    /**
     * Queries whether the application is currently in verbose mode.
     *
     * @return {@code true} if verbose logging is enabled; {@code false} otherwise.
     */
    public static boolean isVerbose() {
        return verbose;
    }

    /**
     * Retrieves the current logging level setting.
     *
     * @return the log level string, e.g. {@code "INFO"}, {@code "DEBUG"}, etc.
     */
    public static String getLogLevel() {
        return logLevel;
    }

    /**
     * Sets the verbose logging flag and persists the new value to preferences.
     *
     * @param v The new verbose flag value; {@code true} to enable verbose logging, {@code false} to disable.
     */
    public static void setVerbose(boolean v) {
        verbose = v;
        PaintPrefs.putBoolean("Runtime", "Verbose", v);
    }

    /**
     * Sets the log level and persists the new setting to preferences.
     *
     * @param level The new log level string to set, e.g. {@code "DEBUG"}, {@code "ERROR"}, etc.
     */
    public static void setLogLevel(String level) {
        logLevel = level;
        PaintPrefs.putString("Runtime", "Log Level", level);
    }
}