package validation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * ============================================================================
 *  SquaresCsvComparator.java
 *
 *  Purpose:
 *    Utility for comparing two "All Squares" CSV files where field names differ.
 *    Uses a provided mapping from old → new column names.
 *
 *  Notes:
 *    - Ignores column order
 *    - Compares integer and double fields numerically
 *    - Rule for doubles:
 *        1) If BOTH values are in { "", NaN, 0, 0.0 } → equal
 *        2) Else BOTH must be numeric → round per field precision and compare
 *        3) Else → not equal
 *    - Writes a detailed CSV report of differences
 *
 *  Author: Herr Doctor
 *  Version: 2.2
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

    private static final Set<String> INTEGER_FIELDS = new HashSet<>(Arrays.asList(
            "Recording Sequence Nr",
            "Row Nr",
            "Col Nr",
            "Nr Tracks",
            "Square Nr"
    ));

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

    /** Per-field rounding precision (digits after decimal). */
    private static final Map<String, Integer> ROUNDING_MAP = new HashMap<>();

    static {
        // Common sensible rounding
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

    private static List<Map<String, String>> readCsv(Path path) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String[] headers = reader.readLine().split(",");
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

    private static void compare(List<Map<String, String>> oldRows, List<Map<String, String>> newRows) {
        if (oldRows.isEmpty() || newRows.isEmpty()) {
            System.err.println("One or both files are empty.");
            return;
        }

        Path reportFile = Paths.get("/Users/Hans/Desktop/Squares Comparison Report.csv");
        int matched = 0, mismatched = 0, missing = 0, extra = 0;

        try (java.io.PrintWriter report = new java.io.PrintWriter(reportFile.toFile())) {
            report.println("Square Nr,Field,Old Value,New Value,Status");

            for (Map<String, String> oldRow : oldRows) {
                String oldSquareNr = oldRow.getOrDefault("Square Nr", "");

                Optional<Map<String, String>> maybeMatch = newRows.stream()
                        .filter(n -> Objects.equals(n.get("Square Number"), oldSquareNr))
                        .findFirst();

                if (!maybeMatch.isPresent()) {
                    report.printf("%s,%s,%s,%s,%s%n",
                                  quote(oldSquareNr), "", "", "", "Missing in NEW");
                    missing++;
                    continue;
                }

                Map<String, String> newRow = maybeMatch.get();
                boolean rowHasDifferences = false;

                for (Map.Entry<String, String> entry : FIELD_MAP.entrySet()) {
                    String oldCol = entry.getKey();
                    String newCol = entry.getValue();

                    String oldVal = oldRow.getOrDefault(oldCol, "");
                    String newVal = newRow.getOrDefault(newCol, "");

                    boolean equal;
                    if (INTEGER_FIELDS.contains(oldCol)) {
                        equal = compareIntegersStrict(oldVal, newVal, oldCol, newCol);
                    } else if (DOUBLE_FIELDS.contains(oldCol)) {
                        equal = compareDoublesWithRoundingRule(oldCol, oldVal, newVal);
                    } else {
                        equal = Objects.equals(oldVal, newVal);
                    }

                    if (!equal) {
                        report.printf("%s,%s,%s,%s,%s%n",
                                      quote(oldSquareNr), quote(newCol),
                                      quote(oldVal), quote(newVal), "DIFFERENT");
                        rowHasDifferences = true;
                    }
                }

                if (rowHasDifferences) mismatched++;
                else matched++;
            }

            // Extra rows check
            Set<String> oldIds = new HashSet<>();
            for (Map<String, String> r : oldRows) oldIds.add(r.getOrDefault("Square Nr", ""));
            for (Map<String, String> r : newRows) {
                String id = r.getOrDefault("Square Number", "");
                if (!oldIds.contains(id)) {
                    report.printf("%s,%s,%s,%s,%s%n", quote(id), "", "", "", "Extra in NEW");
                    extra++;
                }
            }

            report.flush();
            System.out.println("\n✅ Squares comparison complete. Report written to: " + reportFile.toAbsolutePath());
            System.out.printf("Matched: %d, Mismatched: %d, Missing: %d, Extra: %d%n",
                              matched, mismatched, missing, extra);
        } catch (IOException e) {
            System.err.println("Failed to write comparison report: " + e.getMessage());
        }
    }

    // ---- helpers ----

    private static String quote(String s) {
        if (s == null) return "";
        String t = s.replace("\"", "\"\"");
        if (t.contains(",") || t.contains("\"") || t.contains(" ")) return "\"" + t + "\"";
        return t;
    }

    private static boolean compareIntegersStrict(String a, String b, String oldCol, String newCol) {
        try {
            if (a == null || a.trim().isEmpty() || b == null || b.trim().isEmpty()) {
                return Objects.equals(a == null ? "" : a.trim(), b == null ? "" : b.trim());
            }
            int x = (int) Double.parseDouble(a.trim());
            int y = (int) Double.parseDouble(b.trim());

            String oldKey = oldCol.trim().toLowerCase(Locale.ROOT);
            String newKey = newCol.trim().toLowerCase(Locale.ROOT);

            // Adjustments for 0-based vs 1-based numbering
            if (oldKey.equals("row nr") && newKey.equals("row number")) {
                return x - 1 == y;
            }
            if (oldKey.equals("col nr") && newKey.equals("column number")) {
                return x - 1 == y;
            }

            return x == y;
        } catch (Exception e) {
            return Objects.equals(a == null ? "" : a.trim(), b == null ? "" : b.trim());
        }
    }

    private static boolean compareDoublesWithRoundingRule(String field, String a, String b) {
        String ta = (a == null) ? "" : a.trim();
        String tb = (b == null) ? "" : b.trim();

        boolean aSpecial = isZeroNaNOrEmpty(ta);
        boolean bSpecial = isZeroNaNOrEmpty(tb);
        if (aSpecial && bSpecial) return true;

        // --- Tau special rule ---
        if (field.equalsIgnoreCase("Tau")) {
            try {
                if (!ta.isEmpty()) {
                    double oldTau = Double.parseDouble(ta);
                    if (oldTau < 0 && isZeroNaNOrEmpty(tb)) {
                        return true; // negative old Tau is ok if new Tau is empty, NaN or 0
                    }
                }
            } catch (NumberFormatException ignored) {
                // fall through to normal comparison
            }
        }

        // --- Normal numeric comparison ---
        Double dx = parseDoubleIfNumeric(ta);
        Double dy = parseDoubleIfNumeric(tb);
        if (dx != null && dy != null) {
            int digits = ROUNDING_MAP.getOrDefault(field, 3);
            double factor = Math.pow(10, digits);
            double rx = Math.round(dx * factor) / factor;
            double ry = Math.round(dy * factor) / factor;
            return Double.compare(rx, ry) == 0;
        }
        return false;
    }

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

    private static Double parseDoubleIfNumeric(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty() || t.equalsIgnoreCase("NaN")) return null;
        try {
            double v = Double.parseDouble(t);
            return Double.isNaN(v) ? null : v;
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) throws IOException {
        Path oldCsv = Paths.get("/Users/hans/Paint Test Project/221012 - Python/All Squares.csv");
        Path newCsv = Paths.get("/Users/hans/Paint Test Project/221012/All Squares Java.csv");

        List<Map<String, String>> oldRows = readCsv(oldCsv);
        List<Map<String, String>> newRows = readCsv(newCsv);

        compare(oldRows, newRows);
    }
}