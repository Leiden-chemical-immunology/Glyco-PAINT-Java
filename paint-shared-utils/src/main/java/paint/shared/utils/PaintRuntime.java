package paint.shared.utils;

import paint.shared.prefs.PaintPrefs;

/**
 * The PaintRuntime class is a utility class that manages runtime settings for controlling
 * verbosity of logs and the logging level within an application. It interacts with the
 * PaintPrefs class to load and persist these settings in a preference storage system.
 *
 * This class is non-instantiable and consists entirely of static methods and fields.
 * It provides methods to initialize settings from preferences, as well as to retrieve
 * and modify these settings during runtime.
 *
 * Core responsibilities:
 * - Manage the `verbose` setting to enable or disable verbose logging.
 * - Manage the `logLevel` setting to configure the desired logging level.
 * - Initialize these settings using default values or preferences stored via PaintPrefs.
 *
 * Key methods:
 * - {@link #initialiseFromPrefs()} to load settings at the start of the application.
 * - {@link #isVerbose()} and {@link #getLogLevel()} to retrieve current settings.
 * - {@link #setVerbose(boolean)} and {@link #setLogLevel(String)} to update and store settings.
 */
public final class PaintRuntime {

    private static boolean verbose;
    private static String logLevel;

    private PaintRuntime() {}

    /**
     * Initializes the application runtime settings by loading values from the preferences.
     * Specifically, it configures two settings:
     * - `verbose`: Determines whether verbose logging is enabled, defaulting to {@code false}
     *   if the preference is not set.
     * - `logLevel`: Specifies the logging level, defaulting to {@code "INFO"} if the
     *   preference is not set.
     *
     * These preferences are retrieved using the {@link PaintPrefs#getBoolean(String, boolean)}
     * and {@link PaintPrefs#getString(String, String)} methods.
     *
     * This method ensures that the runtime settings reflect user-defined preferences or defaults
     * if the preferences are unavailable.
     */
    public static void initialiseFromPrefs() {
        verbose  = PaintPrefs.getBoolean("Verbose", false);
        logLevel = PaintPrefs.getString("Log Level", "INFO");
    }

    public static boolean isVerbose() {
        return verbose;
    }

    public static String getLogLevel() {
        return logLevel;
    }

    public static void setVerbose(boolean v) {
        verbose = v;
        PaintPrefs.putBoolean("Verbose", v);
    }

    public static void setLogLevel(String level) {
        logLevel = level;
        PaintPrefs.putString("Log Level", level);
    }
}