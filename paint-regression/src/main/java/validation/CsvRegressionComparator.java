package validation;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * ============================================================================
 *  CsvRegressionComparator.java
 *  Version: 2.4 (documented)
 * <p>
 *  PURPOSE
 *  ---------------------------------------------------------------------------
 *  Compare two already-normalized CSV files (e.g. "Squares" or "Tracks")
 *  to detect regressions between a baseline (reference) and a test file.
 * <p>
 *  This tool performs a deterministic, row-by-row comparison and reports
 *  any textual or numeric differences.  It is especially useful for validating
 *  output stability across code changes or environments.
 * <p>
 *  KEY FEATURES
 *  ---------------------------------------------------------------------------
 *   • No short-circuit — both files are fully read and compared even if identical.
 *   • Deterministic 1 : 1 row comparison (no reordering or fuzzy matching).
 *   • Console progress reporting for large datasets.
 *   • Summary preview of the first few detected differences.
 *   • Full difference report written to:
 *       ~/Downloads/Validate/Squares/Squares Regression Differences.csv
 * ============================================================================
 */
public class CsvRegressionComparator {

    /**
     * Entry point for command-line execution.
     * Expects exactly two arguments: baseline CSV and test CSV paths.
     *
     * @param args [0] = baseline.csv, [1] = test.csv
     */
    public static void main(String[] args) {
        try {
            if (args.length != 2) {
                System.err.println("Usage: java validation.CsvRegressionComparator <baseline.csv> <test.csv>");
                System.exit(2);
            }

            // Resolve input and output paths
            Path baseline = Paths.get(args[0]).toAbsolutePath().normalize();
            Path testfile = Paths.get(args[1]).toAbsolutePath().normalize();
            Path report   = defaultReportPath();

            // Console header
            System.out.println("🔍 CSV Regression Comparator");
            System.out.println("------------------------------------");
            System.out.println("Baseline file : " + baseline);
            System.out.println("Test file     : " + testfile);
            System.out.println("Report output : " + report);
            System.out.println();

            Files.createDirectories(report.getParent());

            // Perform comparison
            int diffs = compareFiles(baseline, testfile, report);

            // Completion summary
            System.out.println("\n✅ Regression comparison complete.");
            System.out.println("📄 Report written to: " + report.toAbsolutePath());
            System.out.println("🔢 Total differences detected: " + diffs);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /** @return default report path under the user’s Downloads/Validate/Squares directory. */
    private static Path defaultReportPath() {
        return Paths.get(System.getProperty("user.home"), "Downloads", "Validate", "Squares",
                         "Squares Regression Differences.csv");
    }

    // ----------------------------------------------------------------------
    /**
     * Compare two CSV files line by line and generate a difference report.
     *
     * @param oldCsv  baseline (reference) CSV file
     * @param newCsv  test CSV file
     * @param outCsv  destination path for difference report
     * @return total number of differences detected
     * @throws IOException if file I/O fails
     */
    public static int compareFiles(Path oldCsv, Path newCsv, Path outCsv) throws IOException {

        // === Load both files ===
        System.out.println("📥 Reading baseline file...");
        List<Map<String, String>> oldRows = readCsv(oldCsv);
        System.out.println("   → " + oldRows.size() + " rows loaded.");

        System.out.println("📥 Reading test file...");
        List<Map<String, String>> newRows = readCsv(newCsv);
        System.out.println("   → " + newRows.size() + " rows loaded.");

        if (oldRows.size() == newRows.size()) {
            System.out.printf("✅ Same number of rows (%d). Ready for strict 1:1 comparison.%n", oldRows.size());
        } else {
            System.out.printf("⚠️  Different row counts: baseline=%d, test=%d%n", oldRows.size(), newRows.size());
        }

        // Build (Recording Name + Square Nr) → list of rows
        Map<String, List<Map<String, String>>> oldMulti = toMultiMap(oldRows);
        Map<String, List<Map<String, String>>> newMulti = toMultiMap(newRows);

        Set<String> allKeys = new TreeSet<>(oldMulti.keySet());
        allKeys.addAll(newMulti.keySet());

        System.out.println("\n🔎 Starting detailed comparison...");
        System.out.println("   → Total unique (Recording Name + Square Nr) keys: " + allKeys.size());
        System.out.println();

        List<String[]> diffs = new ArrayList<>();
        int diffCount = 0;

        int processed = 0;
        int totalKeys = allKeys.size();

        // === Core comparison loop ===
        for (String key : allKeys) {
            List<Map<String, String>> ol = oldMulti.getOrDefault(key, Collections.emptyList());
            List<Map<String, String>> nl = newMulti.getOrDefault(key, Collections.emptyList());

            int max = Math.max(ol.size(), nl.size());

            for (int i = 0; i < max; i++) {
                Map<String, String> o = (i < ol.size()) ? ol.get(i) : null;
                Map<String, String> n = (i < nl.size()) ? nl.get(i) : null;

                // Handle added or missing rows
                if (o == null && n != null) {
                    String rec = safe(n.get("Recording Name"));
                    String sq  = safe(n.get("Square Nr"));
                    diffs.add(new String[]{ rec, sq, String.valueOf(i + 1), "", "", "", "Extra in NEW" });
                    diffCount++;
                    continue;
                } else if (o != null && n == null) {
                    String rec = safe(o.get("Recording Name"));
                    String sq  = safe(o.get("Square Nr"));
                    diffs.add(new String[]{ rec, sq, String.valueOf(i + 1), "", "", "", "Missing in NEW" });
                    diffCount++;
                    continue;
                }

                // Compare common fields
                Set<String> fields = new LinkedHashSet<>();
                fields.addAll(o.keySet());
                fields.addAll(n.keySet());

                String rec = safe(o.get("Recording Name"));
                String sq  = safe(o.get("Square Nr"));

                for (String f : fields) {
                    if (f == null || f.trim().isEmpty()) continue;

                    String ov = clean(o.get(f));
                    String nv = clean(n.get(f));

                    if (valuesEqual(ov, nv)) continue;

                    Double od = parseDouble(ov);
                    Double nd = parseDouble(nv);
                    String status = (od != null && nd != null)
                            ? "NUMERIC DIFFERENCE" : "TEXT DIFFERENCE";

                    diffs.add(new String[]{ rec, sq, String.valueOf(i + 1), f, ov, nv, status });
                    diffCount++;
                }
            }

            processed++;
            if (totalKeys <= 20) {
                System.out.println("   ✓ Compared: " + key);
            } else if (processed % 100 == 0) {
                System.out.printf("   ...processed %d/%d keys%n", processed, totalKeys);
            }
        }

        // === Difference summary preview ===
        if (!diffs.isEmpty()) {
            System.out.println("\n🔎 Summary of first few differences:");
            int shown = 0;
            for (String[] row : diffs) {
                if (shown >= 10) {
                    System.out.println("   ... (" + (diffs.size() - shown) + " more)");
                    break;
                }
                String rec   = row[0];
                String sq    = row[1];
                String field = row[3];
                String oldV  = row[4];
                String newV  = row[5];
                String stat  = row[6];
                System.out.printf("   → [%s | Square %s] %s: '%s' vs '%s' (%s)%n",
                                  rec, sq, field, oldV, newV, stat);
                shown++;
            }
        } else {
            System.out.println("\n✅ No differences detected.");
        }

        // === Write report ===
        System.out.println("\n📝 Writing comparison report...");
        try (PrintWriter pw = new PrintWriter(outCsv.toFile())) {
            pw.println("Recording Name,Square Nr,Occurrence,Field,Old Value,New Value,Status");
            for (String[] row : diffs) {
                for (int j = 0; j < row.length; j++) row[j] = csv(row[j]);
                pw.println(String.join(",", row));
            }
            pw.println();
            pw.println("SUMMARY,,,,,,");
            pw.println("Differences," + diffCount);
        }

        System.out.println("   → Report written successfully.");
        return diffCount;
    }

    // ----------------------------------------------------------------------
    /**
     * Convert list of rows to a map keyed by "Recording Name - Square Nr".
     * Multiple rows for the same key are stored in a list (preserving order).
     */
    private static Map<String, List<Map<String, String>>> toMultiMap(List<Map<String, String>> rows) {
        Map<String, List<Map<String, String>>> mm = new TreeMap<>();
        for (Map<String, String> r : rows) {
            String rec = safe(r.get("Recording Name"));
            String sq  = safe(r.get("Square Nr"));
            String key = rec + " - " + sq;
            mm.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }
        return mm;
    }

    // ----------------------------------------------------------------------
    /**
     * Read a CSV file into a list of maps (header → value).
     * Commas are used as separators, no quoting support needed for normalized files.
     */
    private static List<Map<String, String>> readCsv(Path path) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String headerLine = br.readLine();
            if (headerLine == null) return rows;

            String[] headers = headerLine.split(",", -1);
            for (int i = 0; i < headers.length; i++) headers[i] = headers[i].trim();

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] vals = line.split(",", -1);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    String h = headers[i];
                    String v = (i < vals.length ? vals[i] : "");
                    row.put(h, v == null ? "" : v.trim());
                }
                rows.add(row);
            }
        }
        return rows;
    }

    // ----------------------------------------------------------------------
    /** Clean field value: trim and normalize "NaN"/"null" to empty string. */
    private static String clean(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.equalsIgnoreCase("nan") || t.equalsIgnoreCase("null")) return "";
        return t;
    }

    /**
     * Determine whether two string values are equivalent,
     * performing numeric equality check if both are valid numbers.
     */
    private static boolean valuesEqual(String a, String b) {
        if (Objects.equals(a, b)) return true;
        Double da = parseDouble(a);
        Double db = parseDouble(b);
        if (da != null && db != null) return Double.compare(da, db) == 0;
        return false;
    }

    /** Parse a string as a Double, returning null for invalid or NaN input. */
    private static Double parseDouble(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            double v = Double.parseDouble(s);
            return (Double.isNaN(v) ? null : v);
        } catch (Exception e) {
            return null;
        }
    }

    /** Safe null-to-empty conversion. */
    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    /** Escape field content for CSV output if it contains commas or quotes. */
    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}