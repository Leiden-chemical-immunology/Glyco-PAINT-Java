/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.compare;

import java.util.*;

import static java.util.Map.entry;

/**
 * Defines the configuration and tolerance rules for regression tests.
 * <p>
 * The {@code RegressionRules} class specifies which CSV columns should be compared, which
 * should be ignored, and the allowable numerical variance (epsilon) for specific metrics.
 * </p>
 */
public final class RegressionRules {

    // ===========================
    //  Column groups
    // ===========================
    static final Set<String> IGNORE_COLUMNS_RELAXED = Set.of(
            "Run Time",
            "Time Stamp",
            "Row Number",
            "Column Number",
            "Label Number",
            "Median Median Speed",
            "Median Mean Speed",
            "Max Median Speed",
            "Max Mean Speed",
            "Density Ratio"
    );

    static final Set<String> IGNORE_COLUMNS_STRICT = Set.of(
            "Run Time",
            "Time Stamp",
            "Median Mean Speed",
            "Max Mean Speed",
            "Median Median Speed",
            "Max Median Speed"
    );

    static final Set<String> IGNORE_CASE_FIELDS = Set.of(
            "Visible",
            "Square Manually Excluded",
            "Image Excluded"
    );

    // ===========================
    //  Numeric tolerances
    // ===========================
    static final Map<String, Double> RELATIVE_TOLERANCE_RELAXED;
    static final Map<String, Double> RELATIVE_TOLERANCE_STRICT;

    static final Map<String, Integer> ROUNDING_RELAXED;
    static final Map<String, Integer> ROUNDING_STRICT;

    static {
        RELATIVE_TOLERANCE_RELAXED = Map.ofEntries(
                entry("Tau",                   0.01),
                entry("Density",               0.02),
                entry("Density Ratio",         0.02),
                entry("R Squared",             0.01),
                entry("Variability",           0.02),
                entry("Median Displacement",   0.02),
                entry("Max Displacement",      0.02),
                entry("Total Displacement",    0.02),
                entry("Median Max Speed",      0.02),
                entry("Max Max Speed",         0.02),
                entry("Median Median Speed",   0.02),
                entry("Max Mean Speed",        0.02),
                entry("Max Track Duration",    0.02),
                entry("Total Track Duration",  0.02),
                entry("Median Track Duration", 0.02),
                entry("Density Ratio Ori",     0.02)
        );

        RELATIVE_TOLERANCE_STRICT = Map.ofEntries(
                entry("Tau",                   0.0001),
                entry("Density",               0.0001),
                entry("Density Ratio",         0.0001),
                entry("R Squared",             0.0001),
                entry("Variability",           0.0001),
                entry("Median Displacement",   0.0001),
                entry("Max Displacement",      0.0001),
                entry("Total Displacement",    0.0001),
                entry("Median Max Speed",      0.0001),
                entry("Max Max Speed",         0.0001),
                entry("Median Median Speed",   0.0001),
                entry("Max Mean Speed",        0.0001),
                entry("Max Track Duration",    0.0001),
                entry("Total Track Duration",  0.0001),
                entry("Median Track Duration", 0.0001),
                entry("Density Ratio Ori",     0.0001)
        );

        ROUNDING_RELAXED = Map.ofEntries(
                entry("R Squared",                         2),
                entry("Tau",                               0),
                entry("Density",                           1),
                entry("Density Ratio",                     1),
                entry("Variability",                       2),
                entry("Median Displacement",               1),
                entry("Max Displacement",                  1),
                entry("Total Displacement",                1),
                entry("Median Max Speed",                  1),
                entry("Max Max Speed",                     1),
                entry("Median Median Speed",               1),
                entry("Max Mean Speed",                    1),
                entry("Max Track Duration",                1),
                entry("Total Track Duration",              1),
                entry("Median Track Duration",             1),
                entry("Density Ratio Ori",                 1),
                entry("Median Diffusion Coefficient",      2),
                entry("Median Diffusion Coefficient Ext",  2)
        );

        ROUNDING_STRICT = Map.ofEntries(
                entry("R Squared",                         3),
                entry("Tau",                               3),
                entry("Density",                           3),
                entry("Density Ratio",                     3),
                entry("Variability",                       3),
                entry("Median Displacement",               1),
                entry("Max Displacement",                  3),
                entry("Total Displacement",                3),
                entry("Median Max Speed",                  3),
                entry("Max Max Speed",                     3),
                entry("Median Median Speed",               3),
                entry("Max Mean Speed",                    3),
                entry("Max Track Duration",                3),
                entry("Total Track Duration",              3),
                entry("Median Track Duration",             3),
                entry("Density Ratio Ori",                 3),
                entry("Median Diffusion Coefficient",      3),
                entry("Median Diffusion Coefficient Ext",  3)
        );
    }

    private RegressionRules() {
        // no instances
    }

    // ===========================
    //  Helpers
    // ===========================
    public static boolean isIgnoredColumn(String field, boolean relaxed) {
        return field != null && (relaxed ? IGNORE_COLUMNS_RELAXED : IGNORE_COLUMNS_STRICT).contains(field);
    }

    public static boolean isIgnoreCaseField(String field) {
        return field != null && IGNORE_CASE_FIELDS.contains(field);
    }

    public static String clean(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.equalsIgnoreCase("nan")) {
            return "";
        }
        if (t.equalsIgnoreCase("null")) {
            return "";
        }
        return t;
    }

    public static boolean emptyAndZeroEquiv(String a, String b) {
        String x = (a == null ? "" : a.trim());
        String y = (b == null ? "" : b.trim());

        if (x.isEmpty() && isZeroOrSentinel(y)) {
            return true;
        }
        return y.isEmpty() && isZeroOrSentinel(x);
    }

    private static boolean isZeroOrSentinel(String v) {
        return "0".equals(v) || "0.0".equals(v) ||
                "-1".equals(v) || "-1.0".equals(v) ||
                "-2".equals(v) || "-2.0".equals(v) ||
                "-3".equals(v) || "-3.0".equals(v);
    }

    public static boolean valuesEqual(String a, String b) {
        if (Objects.equals(a, b)) {
            return true;
        }
        Double da = parseDouble(a);
        Double db = parseDouble(b);
        if (da != null && db != null) {
            double ra = Math.round(da * 1000.0) / 1000.0;
            double rb = Math.round(db * 1000.0) / 1000.0;
            return Double.compare(ra, rb) == 0;
        }
        return false;
    }

    public static Double parseDouble(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            double v = Double.parseDouble(s);
            return Double.isNaN(v) ? null : v;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean numericEqualWithTolerance(String field, double a, double b, boolean relaxed) {

        if (relaxed) {
            if (Double.isNaN(a) || Double.isNaN(b)) {
                return true;
            }
        }

        // The maps (Map.ofEntries) reject a null key, so a null field is handled here
        Map<String, Integer> roundMap = relaxed ? ROUNDING_RELAXED : ROUNDING_STRICT;
        Map<String, Double> tolMap = relaxed ? RELATIVE_TOLERANCE_RELAXED : RELATIVE_TOLERANCE_STRICT;

        if (field == null) {
            return false;
        }

        Integer prec = roundMap.get(field);
        if (prec != null) {
            double ra = round(a, prec);
            double rb = round(b, prec);
            if (Double.compare(ra, rb) == 0) {
                return true;
            }
        }

        Double relTol = tolMap.get(field);
        if (relTol != null) {
            double denom = Math.max(1e-9, Math.max(Math.abs(a), Math.abs(b)));
            double relErr = Math.abs(a - b) / denom;
            return relErr <= relTol;
        }

        return false;
    }

    private static double round(double v, int decimals) {
        double f = Math.pow(10, decimals);
        return Math.round(v * f) / f;
    }

    /**
     * Track-count-based correction for density-like quantities.
     */
    public static Double correctedValueIfTrackDependent(
            String field, Double oldVal, Double newVal,
            Map<String, String> oldRow, Map<String, String> newRow) {

        if (oldVal == null || newVal == null) {
            return newVal;
        }

        if (!"Density".equals(field) && !"Density Ratio Ori".equals(field)) {
            return newVal;
        }

        Double oldTracks = parseDouble(oldRow.get("Number of Tracks"));
        Double newTracks = parseDouble(newRow.get("Number of Tracks"));
        if (oldTracks == null || newTracks == null) {
            return newVal;
        }
        if (oldTracks <= 0 || newTracks <= 0) {
            return newVal;
        }
        if (Objects.equals(oldTracks, newTracks)) {
            return newVal;
        }

        double ratio = oldTracks / newTracks;
        return newVal * ratio;
    }

    /**
     * If a numeric field is missing (empty/NaN/null) on one side,
     * we treat it as *not a difference*.
     * <p>
     * Applies to BOTH relaxed and strict modes.
     */
    public static boolean numericMissingSkipDifference(Double a, Double b) {

        // Both present → evaluate normally
        if (a != null && b != null) {
            return false;
        }

        // One present, one missing → skip reporting a difference
        return a != null || b != null;
    }

}