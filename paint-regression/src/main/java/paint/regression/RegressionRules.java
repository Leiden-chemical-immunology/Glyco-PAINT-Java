/*=============================================================================
 *  Class:        RegressionRules.java
 *  Package:      paint.regression.clean
 *
 *  PURPOSE:
 *    Defines the configuration and tolerance rules for regression tests.
 *
 *  DESCRIPTION:
 *    The {@code RegressionRules} class specifies which CSV columns should
 *    be compared, which should be ignored, and the allowable numerical
 *    variance (epsilon) for specific metrics.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-regression
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *=============================================================================*/

package paint.regression;

import java.util.*;

final class RegressionRules {

    // ===========================
    //  Column groups
    // ===========================
    static final Set<String> IGNORE_COLUMNS_RELAXED = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
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
            ))
    );

    static final Set<String> IGNORE_COLUMNS_STRICT = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "Run Time",
                    "Time Stamp",
                    "Median Mean Speed",
                    "Max Mean Speed",
                    "Median Median Speed",
                    "Max Median Speed"
            ))
    );

    static final Set<String> IGNORE_CASE_FIELDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "Visible",
                    "Square Manually Excluded",
                    "Image Excluded"
            ))
    );

    // ===========================
    //  Numeric tolerances
    // ===========================
    static final Map<String, Double> RELATIVE_TOLERANCE_RELAXED;
    static final Map<String, Double> RELATIVE_TOLERANCE_STRICT;

    static final Map<String, Integer> ROUNDING_RELAXED;
    static final Map<String, Integer> ROUNDING_STRICT;

    static {
        RELATIVE_TOLERANCE_RELAXED = Collections.unmodifiableMap(
                newDoubleMap(new Object[][]{
                        {"Tau",                   0.01},
                        {"Density",               0.02},
                        {"Density Ratio",         0.02},
                        {"R Squared",             0.01},
                        {"Variability",           0.02},
                        {"Median Displacement",   0.02},
                        {"Max Displacement",      0.02},
                        {"Total Displacement",    0.02},
                        {"Median Max Speed",      0.02},
                        {"Max Max Speed",         0.02},
                        {"Median Median Speed",   0.02},
                        {"Max Mean Speed",        0.02},
                        {"Max Track Duration",    0.02},
                        {"Total Track Duration",  0.02},
                        {"Median Track Duration", 0.02},
                        {"Density Ratio Ori",     0.02}
                })
        );

        RELATIVE_TOLERANCE_STRICT = Collections.unmodifiableMap(
                newDoubleMap(new Object[][]{
                        {"Tau",                   0.0001},
                        {"Density",               0.0001},
                        {"Density Ratio",         0.0001},
                        {"R Squared",             0.0001},
                        {"Variability",           0.0001},
                        {"Median Displacement",   0.0001},
                        {"Max Displacement",      0.0001},
                        {"Total Displacement",    0.0001},
                        {"Median Max Speed",      0.0001},
                        {"Max Max Speed",         0.0001},
                        {"Median Median Speed",   0.0001},
                        {"Max Mean Speed",        0.0001},
                        {"Max Track Duration",    0.0001},
                        {"Total Track Duration",  0.0001},
                        {"Median Track Duration", 0.0001},
                        {"Density Ratio Ori",     0.0001}
                })
        );

        ROUNDING_RELAXED = Collections.unmodifiableMap(
                newIntMap(new Object[][]{
                        {"R Squared",                         2},
                        {"Tau",                               0},
                        {"Density",                           1},
                        {"Density Ratio",                     1},
                        {"Variability",                       2},
                        {"Median Displacement",               1},
                        {"Max Displacement",                  1},
                        {"Total Displacement",                1},
                        {"Median Max Speed",                  1},
                        {"Max Max Speed",                     1},
                        {"Median Median Speed",               1},
                        {"Max Mean Speed",                    1},
                        {"Max Track Duration",                1},
                        {"Total Track Duration",              1},
                        {"Median Track Duration",             1},
                        {"Density Ratio Ori",                 1},
                        {"Median Diffusion Coefficient",      2},
                        {"Median Diffusion Coefficient Ext",  2}
                })
        );

        ROUNDING_STRICT = Collections.unmodifiableMap(
                newIntMap(new Object[][]{
                        {"R Squared",                         3},
                        {"Tau",                               3},
                        {"Density",                           3},
                        {"Density Ratio",                     3},
                        {"Variability",                       3},
                        {"Median Displacement",               1},
                        {"Max Displacement",                  3},
                        {"Total Displacement",                3},
                        {"Median Max Speed",                  3},
                        {"Max Max Speed",                     3},
                        {"Median Median Speed",               3},
                        {"Max Mean Speed",                    3},
                        {"Max Track Duration",                3},
                        {"Total Track Duration",              3},
                        {"Median Track Duration",             3},
                        {"Density Ratio Ori",                 3},
                        {"Median Diffusion Coefficient",      3},
                        {"Median Diffusion Coefficient Ext",  3}
                })
        );
    }

    private RegressionRules() {
        // no instances
    }

    private static Map<String, Double> newDoubleMap(Object[][] data) {
        Map<String, Double> m = new HashMap<>();
        for (Object[] e : data) {
            m.put((String) e[0], (Double) e[1]);
        }
        return m;
    }

    private static Map<String, Integer> newIntMap(Object[][] data) {
        Map<String, Integer> m = new HashMap<>();
        for (Object[] e : data) {
            m.put((String) e[0], (Integer) e[1]);
        }
        return m;
    }

    // ===========================
    //  Helpers
    // ===========================
    static boolean isIgnoredColumn(String field, boolean relaxed) {
        return (relaxed ? IGNORE_COLUMNS_RELAXED : IGNORE_COLUMNS_STRICT).contains(field);
    }

    static boolean isIgnoreCaseField(String field) {
        return IGNORE_CASE_FIELDS.contains(field);
    }

    static String clean(String s) {
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

    static boolean emptyAndZeroEquiv(String a, String b) {
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

    static boolean valuesEqual(String a, String b) {
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

    static Double parseDouble(String s) {
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

    static boolean numericEqualWithTolerance(String field, double a, double b, boolean relaxed) {

        if (relaxed) {
            if (Double.isNaN(a) || Double.isNaN(b)) {
                return true;
            }
        }

        Map<String, Integer> roundMap = relaxed ? ROUNDING_RELAXED : ROUNDING_STRICT;
        Map<String, Double> tolMap = relaxed ? RELATIVE_TOLERANCE_RELAXED : RELATIVE_TOLERANCE_STRICT;

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
    static Double correctedValueIfTrackDependent(
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
    static boolean numericMissingSkipDifference(Double a, Double b) {

        // Both present → evaluate normally
        if (a != null && b != null) {
            return false;
        }

        // One present, one missing → skip reporting a difference
        return a != null || b != null;
    }

}