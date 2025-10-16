package validation;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * ============================================================================
 *  SquaresCsvComparator.java
 *  Version: 3.2
 *  Author: Herr Doctor
 *
 *  PURPOSE
 *  ---------------------------------------------------------------------------
 *  Compare two "All Squares" CSV files (Python vs Java) by:
 *    1. Normalizing both files to a common schema
 *    2. Sorting by Recording Name + Square Nr
 *    3. Writing normalized CSVs for inspection
 *    4. Generating a detailed, precision-aware difference report
 *
 *  NORMALIZATION RULES
 *  ---------------------------------------------------------------------------
 *  - Remove trailing "-threshold-<number>" from Recording Name in OLD file
 *  - Convert Row Nr and Col Nr in OLD file to 0-based indexing
 *  - Tau < 0 in OLD file → set to empty
 *  - Zero values converted to empty ("") in fields:
 *        R Squared, Median Long/Short Track Duration, Total Track Duration,
 *        Density, Density Ratio, Total Displacement
 *  - Columns aligned via FIELD_MAP so OLD and NEW use identical schema
 *
 *  PRECISION HANDLING
 *  ---------------------------------------------------------------------------
 *  - Detects decimal precision separately in OLD and NEW
 *  - Uses the lowest shared precision for comparison:
 *        effective_precision = min(old_precision, new_precision)
 *  - Rounds all numeric values to effective precision in both:
 *        • Normalized CSV output
 *        • Equality checks during comparison
 *
 *  COMPARISON LOGIC
 *  ---------------------------------------------------------------------------
 *  - Two values are considered equal if:
 *        • Both are empty / NaN / 0.0
 *        • (Tau rule) OLD Tau < 0 and NEW Tau is empty or NaN
 *  - Otherwise, both numeric → compared after rounding to effective precision
 *  - If mismatch:
 *        Relative Difference (%) = 100 * |new - old| / |old|
 *        (shown to 2 decimals, blank if old = 0 or non-numeric)
 *
 *  REPORT OUTPUT
 *  ---------------------------------------------------------------------------
 *  - File: "/Users/Hans/Desktop/Squares Validation - Comparison.csv"
 *  - Columns:
 *        Recording Name, Square Nr, Field,
 *        Old Value, New Value, Precision Used,
 *        Relative Difference (%), Status
 *  - Status values:
 *        DIFFERENT        → Field mismatch detected
 *        Missing in NEW   → Row exists only in OLD file
 *        Extra in NEW     → Row exists only in NEW file
 *  - Rows sorted by:
 *        1) Recording Name (alphabetical)
 *        2) Square Nr (numeric)
 *
 *  NORMALIZED OUTPUTS
 *  ---------------------------------------------------------------------------
 *  For reference and manual inspection:
 *        • Squares Validation - All Squares Python Normalized.csv
 *        • Squares Validation - All Squares Java Normalized.csv
 *
 *  CONSOLE SUMMARY
 *  ---------------------------------------------------------------------------
 *  Prints:
 *        - Detected effective precision per field
 *        - Summary counts of Matched, Mismatched, Missing, Extra
 *
 *  FUTURE IMPROVEMENT (planned)
 *  ---------------------------------------------------------------------------
 *  • Append summary totals (Matched / Mismatched / Missing / Extra)
 *    as a final row in the CSV report for Excel-friendly overviews.
 *
 *  ---------------------------------------------------------------------------
 *  RESULT
 *  ---------------------------------------------------------------------------
 *  A robust, fully automated validator that:
 *    ✓ Harmonizes two heterogeneous “All Squares” datasets
 *    ✓ Aligns indices, schema, and formatting
 *    ✓ Handles Tau and zero-value exceptions gracefully
 *    ✓ Compares numerics with adaptive precision rules
 *    ✓ Quantifies and reports deviations in percentage
 *    ✓ Produces consistent, sortable, and regression-safe reports
 * ============================================================================
 */

public class SquaresCsvComparator {

    private static final Map<String, String> FIELD_MAP = new LinkedHashMap<>();

    static {
        FIELD_MAP.put("Row Nr", "Row Number");
        FIELD_MAP.put("Col Nr", "Column Number");
        FIELD_MAP.put("Nr Tracks", "Number of Tracks");
        FIELD_MAP.put("X0", "X0");
        FIELD_MAP.put("Y0", "Y0");
        FIELD_MAP.put("X1", "X1");
        FIELD_MAP.put("Y1", "Y1");
        FIELD_MAP.put("Variability", "Variability");
        FIELD_MAP.put("Density", "Density");
        FIELD_MAP.put("Density Ratio", "Density Ratio Ori");
        FIELD_MAP.put("Tau", "Tau");
        FIELD_MAP.put("R Squared", "R Squared");
        FIELD_MAP.put("Median Diffusion Coefficient", "Median Diffusion Coefficient");
        FIELD_MAP.put("Median Diffusion Coefficient Ext", "Median Diffusion Coefficient Ext");
        FIELD_MAP.put("Median Long Track Duration", "Median Long Track Duration");
        FIELD_MAP.put("Median Short Track Duration", "Median Short Track Duration");
        FIELD_MAP.put("Median Displacement", "Median Displacement");
        FIELD_MAP.put("Max Displacement", "Max Displacement");
        FIELD_MAP.put("Total Displacement", "Total Displacement");
        FIELD_MAP.put("Median Max Speed", "Median Max Speed");
        FIELD_MAP.put("Max Max Speed", "Max Max Speed");
        FIELD_MAP.put("Max Track Duration", "Max Track Duration");
        FIELD_MAP.put("Total Track Duration", "Total Track Duration");
        FIELD_MAP.put("Median Track Duration", "Median Track Duration");
        FIELD_MAP.put("Square Nr", "Square Number");
    }

    private static final Set<String> DOUBLE_FIELDS = new HashSet<>(Arrays.asList(
            "X0", "Y0", "X1", "Y1",
            "Variability", "Density", "Density Ratio",
            "Tau", "R Squared",
            "Median Diffusion Coefficient", "Median Diffusion Coefficient Ext",
            "Median Long Track Duration", "Median Short Track Duration",
            "Median Displacement", "Max Displacement", "Total Displacement",
            "Median Max Speed", "Max Max Speed",
            "Max Track Duration", "Total Track Duration", "Median Track Duration"
    ));

    private static final Map<String, Integer> ROUNDING_MAP = new HashMap<>();

    static {
        ROUNDING_MAP.put("X0", 4);
        ROUNDING_MAP.put("Y0", 4);
        ROUNDING_MAP.put("X1", 4);
        ROUNDING_MAP.put("Y1", 4);
        ROUNDING_MAP.put("Variability", 1);
        ROUNDING_MAP.put("Density", 3);
        ROUNDING_MAP.put("Density Ratio", 2);
        ROUNDING_MAP.put("Tau", 2);
        ROUNDING_MAP.put("R Squared", 2);
        ROUNDING_MAP.put("Median Diffusion Coefficient", 1);
        ROUNDING_MAP.put("Median Diffusion Coefficient Ext", 1);
        ROUNDING_MAP.put("Median Long Track Duration", 0);
        ROUNDING_MAP.put("Median Short Track Duration", 0);
        ROUNDING_MAP.put("Median Displacement", 0);
        ROUNDING_MAP.put("Max Displacement", 0);
        ROUNDING_MAP.put("Total Displacement", 0);
        ROUNDING_MAP.put("Median Max Speed", 0);
        ROUNDING_MAP.put("Max Max Speed", 0);
        ROUNDING_MAP.put("Max Track Duration", 0);
        ROUNDING_MAP.put("Total Track Duration", 0);
        ROUNDING_MAP.put("Median Track Duration", 0);
    }

    private static final Map<String, Integer> EFFECTIVE_PRECISION_MAP = new HashMap<>();

    // ----------------------------------------------------------------------

    private static List<Map<String, String>> readCsv(Path path) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return rows;
            String[] headers = headerLine.split(",", -1);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",", -1);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                    row.put(headers[i].trim(), values[i].trim());
                }
                rows.add(row);
            }
        }
        return rows;
    }

    // ----------------------------------------------------------------------

    private static List<Map<String, String>> normalizeOld(List<Map<String, String>> oldRows) {
        List<Map<String, String>> norm = new ArrayList<>();
        for (Map<String, String> row : oldRows) {
            Map<String, String> n = new LinkedHashMap<>();
            n.put("Recording Name", row.getOrDefault("Ext Recording Name", ""));

            // --- Clean up the old Recording Name: remove trailing "-threshold-<number>" ---
            String recName = n.get("Recording Name");
            if (recName != null) {
                recName = recName.replaceAll("-threshold-\\d+$", "").trim();
                n.put("Recording Name", recName);
            }

            for (String oldCol : FIELD_MAP.keySet()) {
                String val = row.getOrDefault(oldCol, "").trim();

                // Tau normalization
                if (oldCol.equalsIgnoreCase("Tau")) {
                    try {
                        if (!val.isEmpty() && Double.parseDouble(val) < 0)
                            val = "";
                    } catch (NumberFormatException ignored) {}
                }

                // 0-to-empty normalization
                if (oldCol.equalsIgnoreCase("R Squared")
                        || oldCol.equalsIgnoreCase("Median Long Track Duration")
                        || oldCol.equalsIgnoreCase("Median Short Track Duration")
                        || oldCol.equalsIgnoreCase("Total Track Duration")
                        || oldCol.equalsIgnoreCase("Density")
                        || oldCol.equalsIgnoreCase("Density Ratio")
                        || oldCol.equalsIgnoreCase("Total Displacement")) {
                    try {
                        if (!val.isEmpty() && Double.parseDouble(val) == 0.0)
                            val = "";
                    } catch (NumberFormatException ignored) {}
                }

                n.put(oldCol, val);
            }

            // --- Normalize Row Nr and Col Nr to 0-based indexing ---
            try {
                String rowVal = n.get("Row Nr");
                if (rowVal != null && !rowVal.isEmpty()) {
                    int rowNum = (int) Double.parseDouble(rowVal);
                    n.put("Row Nr", String.valueOf(rowNum - 1));
                }
            } catch (NumberFormatException ignored) {}

            try {
                String colVal = n.get("Col Nr");
                if (colVal != null && !colVal.isEmpty()) {
                    int colNum = (int) Double.parseDouble(colVal);
                    n.put("Col Nr", String.valueOf(colNum - 1));
                }
            } catch (NumberFormatException ignored) {}

            norm.add(n);
        }
        return norm;
    }

    private static List<Map<String, String>> normalizeNew(List<Map<String, String>> newRows) {
        List<Map<String, String>> norm = new ArrayList<>();
        for (Map<String, String> row : newRows) {
            Map<String, String> n = new LinkedHashMap<>();
            n.put("Recording Name", row.getOrDefault("Recording Name", ""));
            for (Map.Entry<String, String> entry : FIELD_MAP.entrySet()) {
                String oldCol = entry.getKey();
                String newCol = entry.getValue();
                n.put(oldCol, row.getOrDefault(newCol, ""));
            }
            norm.add(n);
        }
        return norm;
    }

    // ----------------------------------------------------------------------

    private static Map<String, Integer> computeEffectivePrecisions(
            List<Map<String, String>> oldRows,
            List<Map<String, String>> newRows,
            Set<String> numericFields) {

        Map<String, Integer> precisionMap = new HashMap<>();
        for (String field : numericFields) {
            int oldPrec = detectPrecision(oldRows, field);
            int newPrec = detectPrecision(newRows, field);
            precisionMap.put(field, Math.min(oldPrec, newPrec));
        }
        return precisionMap;
    }

    private static int detectPrecision(List<Map<String, String>> rows, String field) {
        int best = 0;
        for (Map<String, String> row : rows) {
            String s = row.get(field);
            if (s != null) {
                s = s.trim();
                if (s.matches("^-?\\d+\\.\\d+$")) {
                    int p = s.length() - s.indexOf('.') - 1;
                    if (p > best) best = p;
                }
            }
        }
        return best;
    }

    // ----------------------------------------------------------------------

    private static String compositeKey(Map<String, String> row) {
        return row.getOrDefault("Recording Name", "").trim() + " - " +
                row.getOrDefault("Square Nr", "").trim();
    }

    private static void writeCsv(List<Map<String, String>> rows, Path file) throws IOException {
        if (rows.isEmpty()) return;
        Files.createDirectories(file.getParent());
        try (PrintWriter pw = new PrintWriter(file.toFile())) {
            pw.println(String.join(",", rows.get(0).keySet()));
            for (Map<String, String> r : rows) {
                List<String> vals = new ArrayList<>();
                for (String c : r.keySet()) {
                    String val = r.get(c);
                    if (val == null) val = "";
                    String out = val;

                    if (DOUBLE_FIELDS.contains(c)) {
                        Double num = parseDouble(val);
                        if (num != null) {
                            int digits = EFFECTIVE_PRECISION_MAP.getOrDefault(
                                    c, ROUNDING_MAP.getOrDefault(c, 3));
                            double factor = Math.pow(10, digits);
                            double rounded = Math.round(num * factor) / factor;
                            out = String.format(Locale.US, "%." + digits + "f", rounded);
                        }
                    }
                    vals.add(escapeCsv(out));
                }
                pw.println(String.join(",", vals));
            }
        }
    }

    // ----------------------------------------------------------------------

    private static boolean compareDoubles(String field, String a, String b) {
        String ta = (a == null) ? "" : a.trim();
        String tb = (b == null) ? "" : b.trim();

        boolean aSpecial = isZeroNaNOrEmpty(ta);
        boolean bSpecial = isZeroNaNOrEmpty(tb);
        if (aSpecial && bSpecial) return true;

        if (field.equalsIgnoreCase("Tau")) {
            try {
                if (!ta.isEmpty() && Double.parseDouble(ta) < 0 && isZeroNaNOrEmpty(tb))
                    return true;
            } catch (NumberFormatException ignored) {}
        }

        Double dx = parseDouble(ta);
        Double dy = parseDouble(tb);
        if (dx != null && dy != null) {
            int d = EFFECTIVE_PRECISION_MAP.getOrDefault(field, ROUNDING_MAP.getOrDefault(field, 3));
            double f = Math.pow(10, d);
            double rx = Math.round(dx * f) / f;
            double ry = Math.round(dy * f) / f;
            return Double.compare(rx, ry) == 0;
        }
        return false;
    }

    // ----------------------------------------------------------------------

    private static boolean isZeroNaNOrEmpty(String s) {
        if (s == null) return true;
        String t = s.trim();
        if (t.isEmpty()) return true;
        if (t.equalsIgnoreCase("NaN")) return true;
        try {
            double v = Double.parseDouble(t);
            return Math.abs(v) < 1e-12 || Double.isNaN(v);
        } catch (Exception e) {
            return false;
        }
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isEmpty() || s.equalsIgnoreCase("NaN")) return null;
        try {
            double v = Double.parseDouble(s);
            return Double.isNaN(v) ? null : v;
        } catch (Exception e) {
            return null;
        }
    }

    private static String quote(String s) {
        if (s == null) return "";
        String t = s.replace("\"", "\"\"");
        if (t.contains(",") || t.contains("\"") || t.contains(" ")) return "\"" + t + "\"";
        return t;
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    // ----------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        Path oldCsv = Paths.get("/Users/hans/Paint Test Project/221012 - Python/All Squares.csv");
        Path newCsv = Paths.get("/Users/hans/Paint Test Project/221012/All Squares Java.csv");

        List<Map<String, String>> oldRows = readCsv(oldCsv);
        List<Map<String, String>> newRows = readCsv(newCsv);

        List<Map<String, String>> normOld = normalizeOld(oldRows);
        List<Map<String, String>> normNew = normalizeNew(newRows);

        Path normOldPath = Paths.get("/Users/Hans/Desktop/Squares Validation - All Squares Python Normalized.csv");
        Path normNewPath = Paths.get("/Users/Hans/Desktop/Squares Validation - All Squares Java Normalized.csv");

        writeCsv(normOld, normOldPath);
        writeCsv(normNew, normNewPath);

        System.out.println("🧩 Normalized files written:");
        System.out.println("   - " + normOldPath);
        System.out.println("   - " + normNewPath);

        EFFECTIVE_PRECISION_MAP.putAll(computeEffectivePrecisions(normOld, normNew, DOUBLE_FIELDS));

        System.out.println("🔍 Effective precisions per field:");
        for (Map.Entry<String, Integer> e : EFFECTIVE_PRECISION_MAP.entrySet()) {
            System.out.printf("   %-35s → %d%n", e.getKey(), e.getValue());
        }

        compare(normOld, normNew);
    }

    // ----------------------------------------------------------------------

    private static void compare(List<Map<String, String>> oldNorm, List<Map<String, String>> newNorm) throws IOException {
        Path reportFile = Paths.get("/Users/Hans/Desktop/Squares Validation - Comparison.csv");

        // Sort input lists by key for consistent processing
        Comparator<Map<String, String>> sorter = Comparator
                .comparing((Map<String, String> r) -> r.get("Recording Name"))
                .thenComparing(r -> {
                    String s = r.get("Square Nr");
                    try {
                        return Integer.parseInt(s);
                    } catch (Exception e) {
                        return 0;
                    }
                });
        oldNorm.sort(sorter);
        newNorm.sort(sorter);

        Map<String, Map<String, String>> newMap = new HashMap<>();
        for (Map<String, String> n : newNorm) newMap.put(compositeKey(n), n);

        // Collect report rows first → so we can sort the report itself
        List<String[]> reportRows = new ArrayList<>();

        int matched = 0, mismatched = 0, missing = 0, extra = 0;

        for (Map<String, String> oldRow : oldNorm) {
            String key = compositeKey(oldRow);
            Map<String, String> newRow = newMap.get(key);

            if (newRow == null) {
                reportRows.add(new String[]{
                        oldRow.get("Recording Name"),
                        oldRow.get("Square Nr"),
                        "", "", "", "", "", "Missing in NEW"
                });
                missing++;
                continue;
            }

            boolean diff = false;
            for (String field : FIELD_MAP.keySet()) {
                String oldVal = oldRow.getOrDefault(field, "");
                String newVal = newRow.getOrDefault(field, "");

                boolean equal;
                if (DOUBLE_FIELDS.contains(field))
                    equal = compareDoubles(field, oldVal, newVal);
                else
                    equal = Objects.equals(oldVal, newVal);

                if (!equal) {
                    diff = true;
                    int prec = EFFECTIVE_PRECISION_MAP.getOrDefault(field,
                                                                    ROUNDING_MAP.getOrDefault(field, -1));

                    // Compute relative difference (%)
                    String relDiffStr = "";
                    Double dx = parseDouble(oldVal);
                    Double dy = parseDouble(newVal);
                    if (dx != null && dy != null && Math.abs(dx) > 1e-12) {
                        double relDiff = Math.abs((dy - dx) / dx) * 100.0;
                        relDiffStr = String.format(Locale.US, "%.2f", relDiff);
                    }

                    reportRows.add(new String[]{
                            oldRow.get("Recording Name"),
                            oldRow.get("Square Nr"),
                            field,
                            oldVal,
                            newVal,
                            String.valueOf(prec),
                            relDiffStr,
                            "DIFFERENT"
                    });
                }
            }
            if (diff) mismatched++; else matched++;
        }

        // Extra rows in NEW
        Set<String> oldKeys = new HashSet<>();
        for (Map<String, String> o : oldNorm) oldKeys.add(compositeKey(o));
        for (Map<String, String> n : newNorm) {
            if (!oldKeys.contains(compositeKey(n))) {
                reportRows.add(new String[]{
                        n.get("Recording Name"),
                        n.get("Square Nr"),
                        "", "", "", "", "", "Extra in NEW"
                });
                extra++;
            }
        }

        // Sort report rows by Recording Name + Square Nr (numeric sort)
        reportRows.sort(Comparator
                                .comparing((String[] r) -> r[0] == null ? "" : r[0])
                                .thenComparing(r -> {
                                    try {
                                        return Integer.parseInt(r[1]);
                                    } catch (Exception e) {
                                        return 0;
                                    }
                                }));

        // Write the sorted report
        try (PrintWriter report = new PrintWriter(reportFile.toFile())) {
            report.println("Recording Name,Square Nr,Field,Old Value,New Value,Precision Used,Relative Difference (%),Status");
            for (String[] row : reportRows) {
                List<String> escaped = new ArrayList<>();
                for (String v : row) escaped.add(quote(v));
                report.println(String.join(",", escaped));
            }
        }

        System.out.printf("✅ Comparison complete. Report: %s%n", reportFile);
        System.out.printf("Matched: %d, Mismatched: %d, Missing: %d, Extra: %d%n",
                          matched, mismatched, missing, extra);
    }
}