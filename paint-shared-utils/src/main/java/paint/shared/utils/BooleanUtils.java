/*=============================================================================
 *  Class:        BooleanUtils.java
 *  Package:      paint.shared.utils
 *
 *  PURPOSE:
 *    Provides a fully unified and consistent interpretation of boolean-like
 *    values across all PAINT components (validators, converters, readers).
 *
 *  DESCRIPTION:
 *    • Defines a single authoritative set of accepted TRUE/FALSE tokens.
 *    • Exposes multiple helpers (validation, normalization, parsing).
 *    • Eliminates inconsistent boolean logic across modules.
 *    • Ensures all modules classify values using the exact same rule set.
 *
 *  RESPONSIBILITIES:
 *    • Centralized boolean classification.
 *    • String → boolean parsing with strict/lenient modes.
 *    • Normalization to canonical "true"/"false" strings.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-11-25
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
=============================================================================*/

package paint.shared.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class BooleanUtils {

    // ───────────────────────────────────────────────────────────────────────────────
    // SINGLE SOURCE OF TRUTH
    // ───────────────────────────────────────────────────────────────────────────────

    private static final Set<String> TRUE_VALUES = new HashSet<>(Arrays.asList(
            "y", "ye", "yes", "ok", "true", "t", "1"
    ));

    private static final Set<String> FALSE_VALUES = new HashSet<>(Arrays.asList(
            "n", "no", "false", "f", "0"
    ));

    private BooleanUtils() {
        // Utility class
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // INTERNAL CLASSIFIER
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Classifies a raw string as TRUE, FALSE, or invalid.
     *
     * @param raw input string
     * @return TRUE, FALSE, or null if invalid or empty
     */
    private static Boolean classify(String raw) {
        if (raw == null) {
            return null;
        }

        String v = raw.trim().toLowerCase();
        if (v.isEmpty()) {
            return null;
        }

        if (TRUE_VALUES.contains(v)) {
            return Boolean.TRUE;
        }
        if (FALSE_VALUES.contains(v)) {
            return Boolean.FALSE;
        }

        return null; // invalid
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the input is a recognized boolean token.
     */
    public static boolean checkBooleanValue(String value) {
        return classify(value) != null;
    }

    /**
     * Normalizes to canonical "true" or "false".
     *
     * @return "true", "false", or null if invalid
     */
    public static String normalizeBoolean(String value) {
        Boolean b = classify(value);
        return (b == null ? null : b.toString());
    }

    /**
     * Returns true if the given value is explicitly recognized as a TRUE token.
     * Returns false for FALSE tokens, null/empty, or invalid values.
     * TRUE tokens: y, ye, yes, ok, true, t, 1
     */
    public static boolean isBooleanTrue(String value) {
        Boolean b = classify(value);
        return Boolean.TRUE.equals(b);
    }
}