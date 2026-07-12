/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

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
        //logLevel = PaintPrefs.getString ("Runtime", "Log Level", "INFO");
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
}