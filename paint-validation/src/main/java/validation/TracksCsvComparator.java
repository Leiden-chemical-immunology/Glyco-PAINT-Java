package validation;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * ============================================================================
 *  TracksCsvComparator.java
 *  Version: 4.3
 *  Author: Herr Doctor
 *
 *  PURPOSE
 *  ---------------------------------------------------------------------------
 *  Compare two "All Tracks" CSV files (Python vs Java) by:
 *    1. Normalizing both to a shared schema
 *    2. Rounding doubles to the lowest shared precision
 *    3. Comparing all numeric fields (int & double) with % tolerance
 *    4. Writing a detailed difference report
 *
 *  NUMERIC COMPARISON RULES
 *  ---------------------------------------------------------------------------
 *  - Both "", "NaN", or 0 → equal
 *  - Integers: numeric compare, else equal if relative difference ≤ tolerance%
 *  - Doubles: round to min precision; equal if rounded match OR relative diff ≤ tolerance%
 *  - Relative diff = 100 * |new - old| / |old| (empty if old = 0)
 *
 *  REPORT
 *  ---------------------------------------------------------------------------
 *  Columns:
 *    Recording Name,Track Id,Field,Old Value,New Value,
 *    Precision Used,Relative Difference (%),Tolerance (%),Status
 *
 *  OUTPUT FILE:
 *    /Users/Hans/Desktop/Tracks Validation - Comparison.csv
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

    // ----------------------------------------------------------------------
    private static final Map<String, Double> TOLERANCE_MAP = new HashMap<>();
    static {
        // Integers (relative % tolerance)
        TOLERANCE_MAP.put("Nr Spots", 2.0);
        TOLERANCE_MAP.put("Nr Gaps", 5.0);
        TOLERANCE_MAP.put("Longest Gap", 5.0);
        TOLERANCE_MAP.put("Track Id", 0.0);

        // Doubles
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
            String rec = row.getOrDefault("Ext Recording Name", "");
            if (rec != null) rec = rec.replaceAll("-threshold-\\d+$", "").trim();
            n.put("Recording Name", rec);
            for (String oldCol : FIELD_MAP.keySet()) {
                n.put(oldCol, row.getOrDefault(oldCol, "").trim());
            }
            norm.add(n);
        }
        return norm;
    }

    private static List<Map<String, String>> normalizeNew(List<Map<String, String>> newRows) {
        List<Map<String, String>> norm = new ArrayList<>();
        for (Map<String, String> row : newRows) {
            Map<String, String> n = new LinkedHashMap<>();
            n.put("Recording Name", row.getOrDefault("Recording Name", "").trim());
            for (Map.Entry<String, String> e : FIELD_MAP.entrySet()) {
                n.put(e.getKey(), row.getOrDefault(e.getValue(), "").trim());
            }
            norm.add(n);
        }
        return norm;
    }

    // ----------------------------------------------------------------------
    private static void computeEffectivePrecisions(List<Map<String, String>> oldRows,
                                                   List<Map<String, String>> newRows) {
        for (String field : DOUBLE_FIELDS) {
            int oldP = detectPrecision(oldRows, field);
            int newP = detectPrecision(newRows, field);
            EFFECTIVE_PRECISION_MAP.put(field, Math.min(oldP, newP));
        }
    }

    private static int detectPrecision(List<Map<String, String>> rows, String field) {
        int best = 0;
        for (Map<String, String> row : rows) {
            String s = row.get(field);
            if (s != null && s.matches("^-?\\d+\\.\\d+$")) {
                int p = s.length() - s.indexOf('.') - 1;
                if (p > best) best = p;
            }
        }
        return best;
    }

    // ----------------------------------------------------------------------
    private static class ComparisonResult {
        boolean equal;
        double relDiff;
        ComparisonResult(boolean equal, double relDiff) {
            this.equal = equal;
            this.relDiff = relDiff;
        }
    }

    // ----------------------------------------------------------------------
    /** Integers: compare numerically, allow relative tolerance. */
    private static ComparisonResult compareIntegersWithTolerance(String field, String a, String b) {
        Double dx = parseDouble(a);
        Double dy = parseDouble(b);
        if (dx == null && dy == null) return new ComparisonResult(true, Double.NaN);
        if (dx == null || dy == null) return new ComparisonResult(false, Double.NaN);

        if (Math.abs(dx - dy) < 1e-12) return new ComparisonResult(true, 0.0);

        if (Math.abs(dx) < 1e-12) return new ComparisonResult(Math.abs(dy) < 1e-12, Double.NaN);

        double relDiff = Math.abs((dy - dx) / dx) * 100.0;
        double tol = TOLERANCE_MAP.getOrDefault(field, 0.0);
        return new ComparisonResult(relDiff <= tol, relDiff);
    }

    /** Doubles: round to shared precision, then apply tolerance. */
    private static ComparisonResult compareDoublesWithTolerance(String field, String a, String b) {
        String ta = (a == null) ? "" : a.trim();
        String tb = (b == null) ? "" : b.trim();

        if (isZeroNaNOrEmpty(ta) && isZeroNaNOrEmpty(tb))
            return new ComparisonResult(true, Double.NaN);

        Double dx = parseDouble(ta);
        Double dy = parseDouble(tb);
        if (dx == null || dy == null)
            return new ComparisonResult(false, Double.NaN);

        int prec = EFFECTIVE_PRECISION_MAP.getOrDefault(field, ROUNDING_MAP.getOrDefault(field, 3));
        double f = Math.pow(10, prec);
        double rx = Math.round(dx * f) / f;
        double ry = Math.round(dy * f) / f;

        if (Double.compare(rx, ry) == 0)
            return new ComparisonResult(true, 0.0);

        if (Math.abs(dx) < 1e-12)
            return new ComparisonResult(Math.abs(dy) < 1e-12, Double.NaN);

        double relDiff = Math.abs((dy - dx) / dx) * 100.0;
        double tol = TOLERANCE_MAP.getOrDefault(field, 0.0);
        return new ComparisonResult(relDiff <= tol, relDiff);
    }

    // ----------------------------------------------------------------------
    private static boolean isZeroNaNOrEmpty(String s) {
        if (s == null) return true;
        String t = s.trim();
        if (t.isEmpty() || t.equalsIgnoreCase("NaN")) return true;
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

    // ----------------------------------------------------------------------
    private static void compare(List<Map<String, String>> oldNorm, List<Map<String, String>> newNorm) throws IOException {
        Path reportFile = Paths.get("/Users/Hans/Desktop/Tracks Validation - Comparison.csv");

        Comparator<Map<String, String>> sorter = Comparator
                .comparing((Map<String, String> r) -> r.getOrDefault("Recording Name", ""))
                .thenComparing(r -> {
                    String s = r.getOrDefault("Track Id", "");
                    try { return Integer.parseInt(s); } catch (Exception e) { return Integer.MAX_VALUE; }
                });
        oldNorm.sort(sorter);
        newNorm.sort(sorter);

        Map<String, Map<String, String>> newMap = new HashMap<>();
        for (Map<String, String> n : newNorm)
            newMap.put(n.get("Recording Name") + "||" + n.get("Track Id"), n);

        List<String[]> reportRows = new ArrayList<>();
        int matched = 0, mismatched = 0, missing = 0, extra = 0;

        for (Map<String, String> oldRow : oldNorm) {
            String key = oldRow.get("Recording Name") + "||" + oldRow.get("Track Id");
            Map<String, String> newRow = newMap.get(key);

            if (newRow == null) {
                reportRows.add(new String[]{oldRow.get("Recording Name"), oldRow.get("Track Id"),
                        "", "", "", "", "", "", "Missing in NEW"});
                missing++;
                continue;
            }

            boolean diff = false;

            for (String field : FIELD_MAP.keySet()) {
                String oldVal = oldRow.getOrDefault(field, "");
                String newVal = newRow.getOrDefault(field, "");

                ComparisonResult result;
                int prec = -1;
                if (INTEGER_FIELDS.contains(field)) {
                    result = compareIntegersWithTolerance(field, oldVal, newVal);
                    prec = 0;
                } else if (DOUBLE_FIELDS.contains(field)) {
                    result = compareDoublesWithTolerance(field, oldVal, newVal);
                    prec = EFFECTIVE_PRECISION_MAP.getOrDefault(field, ROUNDING_MAP.getOrDefault(field, 3));
                } else {
                    boolean equal = Objects.equals(oldVal, newVal);
                    result = new ComparisonResult(equal, Double.NaN);
                }

                if (!result.equal) {
                    diff = true;
                    double tol = TOLERANCE_MAP.getOrDefault(field, 0.0);
                    String relStr = Double.isNaN(result.relDiff) ? "" : String.format(Locale.US, "%.2f", result.relDiff);
                    reportRows.add(new String[]{
                            oldRow.get("Recording Name"),
                            oldRow.get("Track Id"),
                            field,
                            oldVal,
                            newVal,
                            String.valueOf(prec),
                            relStr,
                            String.format(Locale.US, "%.2f", tol),
                            "DIFFERENT"
                    });
                }
            }

            if (diff) mismatched++; else matched++;
        }

        // Extras
        Set<String> oldKeys = new HashSet<>();
        for (Map<String, String> o : oldNorm)
            oldKeys.add(o.get("Recording Name") + "||" + o.get("Track Id"));
        for (Map<String, String> n : newNorm) {
            String k = n.get("Recording Name") + "||" + n.get("Track Id");
            if (!oldKeys.contains(k)) {
                reportRows.add(new String[]{
                        n.get("Recording Name"), n.get("Track Id"),
                        "", "", "", "", "", "", "Extra in NEW"});
                extra++;
            }
        }

        reportRows.sort(Comparator
                                .comparing((String[] r) -> r[0])
                                .thenComparing(r -> {
                                    try { return Integer.parseInt(r[1]); } catch (Exception e) { return Integer.MAX_VALUE; }
                                }));

        try (PrintWriter report = new PrintWriter(reportFile.toFile())) {
            report.println("Recording Name,Track Id,Field,Old Value,New Value,Precision Used,Relative Difference (%),Tolerance (%),Status");
            for (String[] row : reportRows) {
                List<String> esc = new ArrayList<>();
                for (String v : row) esc.add(quote(v));
                report.println(String.join(",", esc));
            }
            report.println();
            report.println("SUMMARY,,,,,,,,");
            report.printf("Matched,%d,,,,,,,,%n", matched);
            report.printf("Mismatched,%d,,,,,,,,%n", mismatched);
            report.printf("Missing,%d,,,,,,,,%n", missing);
            report.printf("Extra,%d,,,,,,,,%n", extra);
        }

        System.out.printf("✅ Comparison complete. Report: %s%n", reportFile);
        System.out.printf("Matched: %d, Mismatched: %d, Missing: %d, Extra: %d%n",
                          matched, mismatched, missing, extra);
    }

    // ----------------------------------------------------------------------
    public static void main(String[] args) throws IOException {
        Path oldCsv = Paths.get("/Users/hans/Paint Test Project/221012 - Python/All Tracks.csv");
        Path newCsv = Paths.get("/Users/hans/Paint Test Project/221012/All Tracks Java.csv");

        List<Map<String, String>> oldRows = readCsv(oldCsv);
        List<Map<String, String>> newRows = readCsv(newCsv);

        List<Map<String, String>> normOld = normalizeOld(oldRows);
        List<Map<String, String>> normNew = normalizeNew(newRows);

        computeEffectivePrecisions(normOld, normNew);

        System.out.println("🔧 Effective precisions per field:");
        for (Map.Entry<String, Integer> e : EFFECTIVE_PRECISION_MAP.entrySet())
            System.out.printf("   %-30s → %d%n", e.getKey(), e.getValue());

        System.out.println("\n🎯 Field tolerances (%):");
        for (Map.Entry<String, Double> e : TOLERANCE_MAP.entrySet())
            System.out.printf("   %-30s → %.2f%%%n", e.getKey(), e.getValue());

        compare(normOld, normNew);
    }
}