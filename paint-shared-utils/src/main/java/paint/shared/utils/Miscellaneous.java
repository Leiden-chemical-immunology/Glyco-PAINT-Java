/******************************************************************************
 *  Class:        Miscellaneous.java
 *  Package:      paint.shared.utils
 *
 *  PURPOSE:
 *    Provides general-purpose utility methods used throughout the PAINT framework.
 *
 *  DESCRIPTION:
 *    The {@code Miscellaneous} class serves as a static utility collection for
 *    handling common operations not tied to a specific subsystem. This includes
 *    duration formatting, rounding, exception message handling, and flexible
 *    boolean string interpretation.
 *
 *    It is designed for convenience and internal consistency across all modules
 *    of the PAINT system and is fully static and non-instantiable.
 *
 *  KEY FEATURES:
 *    • Format time durations into human-readable strings.
 *    • Round floating-point numbers to a given number of decimals.
 *    • Safely extract concise error messages from exceptions.
 *    • Interpret textual boolean expressions in flexible formats.
 *    • Validate user or CSV-provided boolean strings robustly.
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

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Static utility class containing miscellaneous helper methods used across PAINT modules.
 * <p>
 * Provides methods for time formatting, numeric rounding, and
 * robust boolean string parsing.
 */
public final class Miscellaneous {

    /**
     * Prevents instantiation of this static utility class.
     */
    private Miscellaneous() {
        // Prevent instantiation
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // MAIN (DEMONSTRATION)
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Demonstration entry point for manual testing.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        int[] testValues = {0, 5, 59, 60, 61, 65, 3599, 3600, 3605, 3665, 7322};
        for (int seconds : testValues) {
            System.out.printf("%5d seconds → %s%n", seconds, formatDuration(seconds));
        }
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // DURATION FORMATTING UTILITIES
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Formats a duration given in seconds into a human-readable string such as
     * "1 hour 2 minutes 5 seconds".
     *
     * @param totalSeconds duration in seconds
     * @return formatted human-readable duration string
     */
    public static String formatDuration(int totalSeconds) {
        int hours   = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int secs    = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append(" hour").append(hours == 1 ? "" : "s").append(" ");
        }
        if (minutes > 0) {
            sb.append(minutes).append(" minute").append(minutes == 1 ? "" : "s").append(" ");
        }
        if (secs > 0 || sb.length() == 0) {
            sb.append(secs).append(" second").append(secs == 1 ? "" : "s");
        }

        return sb.toString().trim();
    }

    /**
     * Converts a {@link Duration} object into a human-readable string.
     *
     * @param duration duration instance to format
     * @return formatted string representation of the duration
     */
    public static String formatDuration(Duration duration) {
        int totalSeconds = (int) duration.getSeconds();
        return formatDuration(totalSeconds);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // ERROR HANDLING UTILITIES
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Extracts a concise user-friendly message from a {@link Throwable}.
     * If no colon is present in the exception message, returns the raw text.
     *
     * @param t exception or error object
     * @return trimmed message string or an empty string if {@code t} is null
     */
    public static String friendlyMessage(Throwable t) {
        if (t == null) {
            return "";
        }
        String m     = t.toString();
        int    colon = m.lastIndexOf(':');
        return (colon != -1) ? m.substring(colon + 1).trim() : m;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // NUMERIC UTILITIES
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Rounds a floating-point number to a specified number of decimal places.
     *
     * @param value    numeric value to round
     * @param decimals number of decimal places
     * @return rounded numeric value
     */
    public static double round(double value, int decimals) {
        if (decimals < 0) {
            return value;
        } else {
            double scale = Math.pow(10, decimals);
            return Math.round(value * scale) / scale;
        }
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // BOOLEAN VALUE UTILITIES
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Evaluates a string as a boolean. Supports variations such as
     * {@code "true", "yes", "y", "1"} for {@code true} and
     * {@code "false", "no", "n", "0"} for {@code false}.
     * Invalid values log an error via {@link PaintLogger}.
     *
     * @param value input string
     * @return boolean interpretation of the input
     */
    public static boolean isBooleanValue(String value) {
        if (value.equals("true") || value.equals("1") ||
                value.equals("yes") || value.equals("y") || value.equals("t")) {
            return true;
        } else if (value.equals("false") || value.equals("0") ||
                value.equals("no") || value.equals("n") || value.equals("f")) {
            return false;
        } else {
            PaintLogger.errorf("Invalid boolean value: %s", value);
            return false;
        }
    }

    /**
     * Checks whether the provided string represents a valid boolean expression.
     *
     * @param value input string to check
     * @return {@code true} if recognizable as boolean, {@code false} otherwise
     */
    public static Boolean checkBooleanValue(String value) {
        value                 = value.trim().toLowerCase();
        Set<String> yesValues = new HashSet<>(Arrays.asList("y", "ye", "yes", "ok", "true", "t", "1"));
        Set<String> noValues  = new HashSet<>(Arrays.asList("n", "no", "false", "f", "0"));
        return yesValues.contains(value) || noValues.contains(value);
    }

    /**
     * Parses a flexible string-based boolean expression into a {@code Boolean}.
     * <p>
     * Logs an error if the input cannot be parsed and defaults to {@code false}.
     *
     * @param value input string
     * @return parsed boolean value, defaults to {@code false} for invalid input
     */
    public static Boolean getBooleanValue(String value) {
        value                 = value.trim().toLowerCase();
        Set<String> yesValues = new HashSet<>(Arrays.asList("y", "ye", "yes", "ok", "true", "t", "1"));
        Set<String> noValues  = new HashSet<>(Arrays.asList("n", "no", "false", "f", "0"));

        if (yesValues.contains(value)) {
            return true;
        } else if (noValues.contains(value)) {
            return false;
        } else {
            PaintLogger.errorf("Invalid boolean value: %s, default to 'false'", value);
            return false;
        }
    }

    /**
     * Returns {@code true} if the input string is a recognized affirmative boolean value.
     *
     * @param value input string
     * @return {@code true} if value is affirmative (e.g., "y", "yes", "true", "1")
     */
    public static Boolean isBooleanTrue(String value) {
        value = value.trim().toLowerCase();
        Set<String> yesValues = new HashSet<>(Arrays.asList("y", "ye", "yes", "ok", "true", "t", "1"));
        return yesValues.contains(value);
    }
}