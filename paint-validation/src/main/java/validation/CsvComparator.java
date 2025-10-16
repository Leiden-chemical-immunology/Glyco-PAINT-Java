package validation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * ============================================================================
 *  CsvComparator.java
 *
 *  Purpose:
 *    Utility for comparing two CSV files where field names differ.
 *    Uses a provided mapping from old → new column names.
 *
 *  Notes:
 *    - Ignores column order
 *    - Compares integer and double fields numerically
 *    - Rule for doubles:
 *        1) If BOTH values are in { "", NaN, 0, 0.0 } → equal
 *        2) Else BOTH must be numeric → round per field precision and compare
 *        3) Else → not equal
 *    - Reports missing and differing rows with detailed per-field context
 *
 *  Author: Herr Doctor
 *  Version: 2.1
 * ============================================================================
 */
public class CsvComparator {

    private static final Map<String, String> FIELD_MAP = new LinkedHashMap<>();

    static {
        //FIELD_MAP.put("Ext Recording Name", "Recording Name");
        FIELD_MAP.put("Track Id", "Track ID");
        FIELD_MAP.put("Nr Spots", "Number of Spots");
        FIELD_MAP.put("Nr Gaps", "Number of Gaps");
        FIELD_MAP.put("Longest Gap", "Longest Gap");
        FIELD_MAP.put("Track Duration", "Track Duration");
        FIELD_MAP.put("Track X Location", "Track X Location");
        FIELD_MAP.put("Track Y Location", "Track Y Location");
        FIELD_MAP.put("Track Displacement", "Track Displacement");
        FIELD_MAP.put("Track Max Speed", "Track Max Speed");
        FIELD_MAP.put("Track Median Speed", "Track Median Speed");
        FIELD_MAP.put("Diffusion Coefficient", "Diffusion Coefficient");
        FIELD_MAP.put("Diffusion Coefficient Ext", "Diffusion Coefficient Ext");
        FIELD_MAP.put("Total Distance", "Total Distance");
        FIELD_MAP.put("Confinement Ratio", "Confinement Ratio");
    }

    private static final Set<String> INTEGER_FIELDS = new HashSet<>(
            Arrays.asList("Track Id", "Nr Spots", "Nr Gaps", "Longest Gap")
    );

    private static final Set<String> DOUBLE_FIELDS = new HashSet<>(
            Arrays.asList(
                    "Track Duration",
                    "Track X Location",
                    "Track Y Location",
                    "Track Displacement",
                    "Track Max Speed",
                    "Track Median Speed",
                    "Diffusion Coefficient",
                    "Diffusion Coefficient Ext",
                    "Total Distance",
                    "Confinement Ratio"
            )
    );

    /** Per-field rounding precision (digits after decimal). */
    private static final Map<String, Integer> ROUNDING_MAP = new HashMap<>();

    static {
        ROUNDING_MAP.put("Track Duration", 1);
        ROUNDING_MAP.put("Track X Location", 2);
        ROUNDING_MAP.put("Track Y Location", 2);
        ROUNDING_MAP.put("Track Displacement", 1);
        ROUNDING_MAP.put("Track Max Speed", 1);
        ROUNDING_MAP.put("Track Median Speed", 1);
        ROUNDING_MAP.put("Diffusion Coefficient", 21);
        ROUNDING_MAP.put("Diffusion Coefficient Ext", 1);
        ROUNDING_MAP.put("Total Distance", 2);
        ROUNDING_MAP.put("Confinement Ratio", 1);
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

        int matched = 0;
        int mismatched = 0;
        int missing = 0;
        int extra = 0;

        Path reportFile = Paths.get("/Users/Hans/Desktop/comparison_report.csv");
        try (java.io.PrintWriter report = new java.io.PrintWriter(reportFile.toFile())) {
            // Write CSV header
            report.println("Track ID,Field,Old Value,New Value,Status");

            for (Map<String, String> oldRow : oldRows) {

                String oldTrackId = oldRow.getOrDefault("Track Id", "");

                Optional<Map<String, String>> maybeMatch = newRows.stream()
                        .filter(n -> Objects.equals(n.get("Track ID"), oldTrackId))
                        .findFirst();

                if (!maybeMatch.isPresent()) {
                    report.printf("%s,%s,%s,%s,%s%n",
                                  quote(oldTrackId), "", "", "", "Missing in NEW");
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
                        equal = compareIntegersStrict(oldVal, newVal);
                    } else if (DOUBLE_FIELDS.contains(oldCol)) {
                        equal = compareDoublesWithRoundingRule(oldCol, oldVal, newVal);
                    } else {
                        equal = Objects.equals(oldVal, newVal);
                    }

                    if (!equal) {
                        report.printf("%s,%s,%s,%s,%s%n",
                                      quote(oldTrackId),
                                      quote(newCol),
                                      quote(oldVal),
                                      quote(newVal),
                                      "DIFFERENT");
                        rowHasDifferences = true;
                    }
                }

                if (rowHasDifferences) {
                    mismatched++;
                } else {
                    matched++;
                }
            }

            // Check for extra rows
            Set<String> oldIds = new HashSet<>();
            for (Map<String, String> r : oldRows) {
                oldIds.add(r.getOrDefault("Track Id", ""));
            }
            for (Map<String, String> r : newRows) {
                String id = r.getOrDefault("Track ID", "");
                if (!oldIds.contains(id)) {
                    report.printf("%s,%s,%s,%s,%s%n",
                                  quote(id), "", "", "", "Extra in NEW");
                    extra++;
                }
            }

            report.flush();
            System.out.println("\n✅ Comparison complete. Report written to: " + reportFile.toAbsolutePath());
            System.out.println("Matched: " + matched + ", Mismatched: " + mismatched +
                                       ", Missing: " + missing + ", Extra: " + extra);
        } catch (IOException e) {
            System.err.println("Failed to write comparison report: " + e.getMessage());
        }
    }

    /** Escapes CSV fields with quotes when needed. */
    private static String quote(String s) {
        if (s == null) return "";
        String t = s.replace("\"", "\"\"");
        if (t.contains(",") || t.contains("\"") || t.contains(" ")) {
            return "\"" + t + "\"";
        }
        return t;
    }
    private static boolean compareIntegersStrict(String a, String b) {
        try {
            if (a == null || a.trim().isEmpty() || b == null || b.trim().isEmpty()) {
                return Objects.equals(a == null ? "" : a.trim(), b == null ? "" : b.trim());
            }
            int x = (int) Double.parseDouble(a.trim());
            int y = (int) Double.parseDouble(b.trim());
            return x == y;
        } catch (Exception e) {
            return Objects.equals(a == null ? "" : a.trim(), b == null ? "" : b.trim());
        }
    }

    /**
     * Comparison rule for doubles:
     *   1) If BOTH values are in the special category { "", NaN, 0, 0.0 } → equal
     *   2) Else BOTH must be numeric → round per field precision and compare
     *   3) Else → not equal
     */
    /**
     * Comparison rule for doubles:
     *   1) If BOTH values are in the special category { "", NaN, 0, 0.0 } → equal
     *   2) Else BOTH must be numeric → round per field precision and compare
     *   3) Else → not equal
     */
    private static boolean compareDoublesWithRoundingRule(String field, String a, String b) {
        String ta = (a == null) ? "" : a.trim();
        String tb = (b == null) ? "" : b.trim();

        boolean aSpecial = isZeroNaNOrEmpty(ta);
        boolean bSpecial = isZeroNaNOrEmpty(tb);

        // Rule (1): both special → equal
        if (aSpecial && bSpecial) {
            return true;
        }

        // Rule (2): both numeric → round and compare
        Double dx = parseDoubleIfNumeric(ta);
        Double dy = parseDoubleIfNumeric(tb);
        if (dx != null && dy != null) {
            int digits = ROUNDING_MAP.getOrDefault(field, 3);
            double factor = Math.pow(10, digits);
            double rx = Math.round(dx * factor) / factor;
            double ry = Math.round(dy * factor) / factor;

            // Use string formatting to enforce consistent decimal precision before compare
            String sx = String.format(Locale.US, "%." + digits + "f", rx);
            String sy = String.format(Locale.US, "%." + digits + "f", ry);

            return sx.equals(sy);
        }

        // Rule (3): one special, one non-numeric → not equal
        return false;
    }
    /** Special category membership: "", "NaN", numeric zero (±tiny). */
    private static boolean isZeroNaNOrEmpty(String s) {
        if (s == null) return true;
        String t = s.trim();
        if (t.isEmpty()) return true;
        if (t.equalsIgnoreCase("NaN")) return true;
        try {
            double v = Double.parseDouble(t);
            return Math.abs(v) < 1e-12 || Double.isNaN(v);
        } catch (Exception e) {
            return false; // non-numeric string is NOT special unless empty/NaN literal
        }
    }

    /** Returns Double if numeric (including 0), otherwise null. NaN returns null (not numeric). */
    private static Double parseDoubleIfNumeric(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty() || t.equalsIgnoreCase("NaN")) return null;
        try {
            double v = Double.parseDouble(t);
            if (Double.isNaN(v)) return null;
            return v;
        } catch (Exception e) {
            return null;
        }
    }

    private static double round(double value, int digits) {
        double factor = Math.pow(10, digits);
        return Math.round(value * factor) / factor;
    }

    public static void main(String[] args) throws IOException {

        Path oldCsv = Paths.get("/Users/hans/Paint Test Project/221012 - Python/All Tracks.csv");
        Path newCsv = Paths.get("/Users/hans/Paint Test Project/221012/All Tracks Java.csv");

        List<Map<String, String>> oldRows = readCsv(oldCsv);
        List<Map<String, String>> newRows = readCsv(newCsv);

        compare(oldRows, newRows);
    }
}