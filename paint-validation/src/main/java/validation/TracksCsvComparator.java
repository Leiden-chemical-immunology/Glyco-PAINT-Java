package validation;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * ============================================================================
 *  TracksCsvComparator.java
 *  Version: 3.1
 *  Author: Herr Doctor
 *
 *  PURPOSE
 *  ---------------------------------------------------------------------------
 *  Compare two "All Tracks" CSV files (Python vs Java) with:
 *    • Field-name mapping (old → new)
 *    • Physical-coordinate-based matching (robust to Track Id reorderings)
 *    • Per-field percentage tolerances and rounding precision
 *
 *  COMPARISON LOGIC
 *  ---------------------------------------------------------------------------
 *  1) Match by composite key:
 *         key = Recording Name + rounded(X,1) + rounded(Y,1)
 *  2) Round numeric fields to defined precision
 *  3) Compute relative difference (%)
 *  4) Fields equal if |Δ| ≤ tolerance %
 *  5) Report Missing / Extra / Different rows
 * ============================================================================
 */
public class TracksCsvComparator {

    // ----------------------------------------------------------------------
    private static final Map<String, String> FIELD_MAP = new LinkedHashMap<>();
    static {
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

    // ----------------------------------------------------------------------
    private static final Map<String, Integer> ROUNDING_MAP = new HashMap<>();
    static {
        ROUNDING_MAP.put("Track Duration", 1);
        ROUNDING_MAP.put("Track X Location", 1);
        ROUNDING_MAP.put("Track Y Location", 1);
        ROUNDING_MAP.put("Track Displacement", 1);
        ROUNDING_MAP.put("Track Max Speed", 1);
        ROUNDING_MAP.put("Track Median Speed", 1);
        ROUNDING_MAP.put("Diffusion Coefficient", 1);
        ROUNDING_MAP.put("Diffusion Coefficient Ext", 1);
        ROUNDING_MAP.put("Total Distance", 2);
        ROUNDING_MAP.put("Confinement Ratio", 1);
    }

    private static final Map<String, Double> TOLERANCE_MAP = new HashMap<>();
    static {
        TOLERANCE_MAP.put("Track Id", 0.0);
        TOLERANCE_MAP.put("Nr Spots", 1.0);
        TOLERANCE_MAP.put("Nr Gaps", 5.0);
        TOLERANCE_MAP.put("Longest Gap", 5.0);
        TOLERANCE_MAP.put("Track Duration", 1.0);
        TOLERANCE_MAP.put("Track X Location", 1.0);
        TOLERANCE_MAP.put("Track Y Location", 1.0);
        TOLERANCE_MAP.put("Track Displacement", 1.0);
        TOLERANCE_MAP.put("Track Max Speed", 2.0);
        TOLERANCE_MAP.put("Track Median Speed", 2.0);
        TOLERANCE_MAP.put("Diffusion Coefficient", 5.0);
        TOLERANCE_MAP.put("Diffusion Coefficient Ext", 5.0);
        TOLERANCE_MAP.put("Total Distance", 1.0);
        TOLERANCE_MAP.put("Confinement Ratio", 5.0);
    }

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
    /** Composite key for physical alignment: Recording Name + rounded(X,Y). */
    private static String trackKey(Map<String, String> row) {
        String rec = row.getOrDefault("Recording Name", "");
        Double x = parseDoubleIfNumeric(row.get("Track X Location"));
        Double y = parseDoubleIfNumeric(row.get("Track Y Location"));
        double rx = round(x, 1);
        double ry = round(y, 1);
        return String.format(Locale.US, "%s_%.1f_%.1f", rec, rx, ry);
    }

    // ----------------------------------------------------------------------
    private static void compare(List<Map<String, String>> oldRows, List<Map<String, String>> newRows) throws IOException {
        Path reportFile = Paths.get("/Users/Hans/Desktop/Tracks Comparison Report.csv");

        // Build maps by physical key
        Map<String, Map<String, String>> oldMap = new HashMap<>();
        for (Map<String, String> o : oldRows) oldMap.put(trackKey(o), o);
        Map<String, Map<String, String>> newMap = new HashMap<>();
        for (Map<String, String> n : newRows) newMap.put(trackKey(n), n);

        // Collect all keys
        Set<String> allKeys = new TreeSet<>(oldMap.keySet());
        allKeys.addAll(newMap.keySet());

        int matched = 0, mismatched = 0, missing = 0, extra = 0;

        try (PrintWriter report = new PrintWriter(reportFile.toFile())) {
            report.println("Recording Name,Track Key,Field,Old Value,New Value,Precision Used,Relative Difference (%),Tolerance (%),Status");

            for (String key : allKeys) {
                Map<String, String> oldRow = oldMap.get(key);
                Map<String, String> newRow = newMap.get(key);

                String rec = key.contains("_") ? key.substring(0, key.indexOf("_")) : key;

                if (oldRow == null) {
                    report.printf("%s,%s,,,,,,,%s%n", quote(rec), quote(key), "Extra in NEW");
                    extra++;
                    continue;
                }
                if (newRow == null) {
                    report.printf("%s,%s,,,,,,,%s%n", quote(rec), quote(key), "Missing in NEW");
                    missing++;
                    continue;
                }

                boolean diff = false;

                for (Map.Entry<String, String> entry : FIELD_MAP.entrySet()) {
                    String oldCol = entry.getKey();
                    String newCol = entry.getValue();

                    String oldVal = oldRow.getOrDefault(oldCol, "");
                    String newVal = newRow.getOrDefault(newCol, "");

                    int digits = ROUNDING_MAP.getOrDefault(oldCol, 3);
                    double relDiff = computeRelativeDifference(oldCol, oldVal, newVal, digits);
                    double tol = TOLERANCE_MAP.getOrDefault(oldCol, 0.0);

                    boolean equal = relDiff <= tol;

                    if (!equal) {
                        diff = true;
                        report.printf(Locale.US,
                                      "%s,%s,%s,%s,%s,%d,%.3f,%.1f,%s%n",
                                      quote(rec), quote(key), quote(newCol),
                                      quote(oldVal), quote(newVal),
                                      digits, relDiff, tol, "DIFFERENT");
                    }
                }

                if (diff) mismatched++; else matched++;
            }
        }

        System.out.printf("✅ Comparison complete. Report: %s%n", reportFile);
        System.out.printf("Matched: %d, Mismatched: %d, Missing: %d, Extra: %d%n",
                          matched, mismatched, missing, extra);
    }

    // ----------------------------------------------------------------------
    private static double computeRelativeDifference(String field, String a, String b, int digits) {
        Double dx = parseDoubleIfNumeric(a);
        Double dy = parseDoubleIfNumeric(b);
        if (dx == null && dy == null) return 0.0;
        if (dx == null || dy == null) return 100.0;
        if (Math.abs(dx) < 1e-12) return (Math.abs(dy) < 1e-12) ? 0.0 : 100.0;
        dx = round(dx, digits);
        dy = round(dy, digits);
        return Math.abs((dy - dx) / dx) * 100.0;
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

    private static double round(Double v, int digits) {
        if (v == null) return 0.0;
        double f = Math.pow(10, digits);
        return Math.round(v * f) / f;
    }

    private static String quote(String s) {
        if (s == null) return "";
        String t = s.replace("\"", "\"\"");
        if (t.contains(",") || t.contains("\"") || t.contains(" ")) return "\"" + t + "\"";
        return t;
    }

    // ----------------------------------------------------------------------
    public static void main(String[] args) throws IOException {
        Path oldCsv = Paths.get("/Users/hans/Paint Test Project/221012 - Python/All Tracks.csv");
        Path newCsv = Paths.get("/Users/hans/Paint Test Project/221012/All Tracks Java.csv");

        List<Map<String, String>> oldRows = readCsv(oldCsv);
        List<Map<String, String>> newRows = readCsv(newCsv);

        compare(oldRows, newRows);
    }
}