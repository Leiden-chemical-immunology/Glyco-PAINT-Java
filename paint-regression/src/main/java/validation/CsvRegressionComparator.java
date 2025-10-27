/******************************************************************************
 *  Class:        CsvRegressionComparator.java
 *  Package:      validation
 *
 *  PURPOSE:
 *    Compares two CSV files line by line and reports all detected differences.
 *    Designed for regression validation of Paint-generated data files.
 *
 *  DESCRIPTION:
 *    Performs row-wise and field-wise comparison between a baseline and
 *    a test CSV file, identifying added, missing, or differing values.
 *    Adapts automatically to the structure of Paint CSV files such as
 *    Tracks, Squares, and Recordings by detecting the presence of
 *    “Recording Name” and “Square Nr” columns.
 *
 *  RESPONSIBILITIES:
 *    • Read and parse CSV files into structured maps
 *    • Group rows by “Recording Name” and optionally “Square Nr”
 *    • Detect numeric and textual differences between files
 *    • Generate a structured in-memory difference report
 *    • Provide detailed console summaries of mismatches
 *
 *  USAGE EXAMPLE:
 *    CsvRegressionComparator.compareFiles(
 *        Paths.get("baseline/Squares.csv"),
 *        Paths.get("test/Squares.csv")
 *    );
 *
 *  DEPENDENCIES:
 *    - java.io
 *    - java.nio.file
 *    - java.util
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-27
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package validation;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Provides functionality to compare two CSV files line by line and report
 * all differences between them. The comparison is performed row-wise and
 * field-wise, detecting added, missing, or differing values.
 *
 * <p>This class is designed for Paint-generated CSVs (Tracks, Squares,
 * Recordings) and adapts automatically based on which columns are present.
 * It can also be executed from the command line or used programmatically.
 */
public class CsvRegressionComparator {


    // ----------------------------------------------------------------------
    /**
     * Compares two CSV files for differences based on their content, generates
     * a detailed comparison report in CSV format, and returns the total number
     * of detected differences.
     *
     * @param oldCsv the path to the baseline CSV file
     * @param newCsv the path to the test CSV file to compare against the baseline
     * @return the total number of differences detected between the two CSV files
     * @throws IOException if an I/O error occurs while reading or writing files
     */
    public static int compareFiles(Path oldCsv, Path newCsv) throws IOException {

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

        // Build (Recording Name [+ Square Nr]) → list of rows
        Map<String, List<Map<String, String>>> oldMulti = toMultiMap(oldRows);
        Map<String, List<Map<String, String>>> newMulti = toMultiMap(newRows);

        Set<String> allKeys = new TreeSet<>(oldMulti.keySet());
        allKeys.addAll(newMulti.keySet());

        System.out.println("\n🔎 Starting detailed comparison...");
        System.out.println("   → Total unique (Recording Name [+ Square Nr]) keys: " + allKeys.size());
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
                    String sq  = n.containsKey("Square Nr") ? safe(n.get("Square Nr")) : "";
                    diffs.add(new String[]{ rec, sq, String.valueOf(i + 1), "", "", "", "Extra in NEW" });
                    diffCount++;
                    continue;
                } else if (o != null && n == null) {
                    String rec = safe(o.get("Recording Name"));
                    String sq  = o.containsKey("Square Nr") ? safe(o.get("Square Nr")) : "";
                    diffs.add(new String[]{ rec, sq, String.valueOf(i + 1), "", "", "", "Missing in NEW" });
                    diffCount++;
                    continue;
                }

                // Compare common fields
                Set<String> fields = new LinkedHashSet<>();
                fields.addAll(o.keySet());
                fields.addAll(n.keySet());

                String rec = safe(o.get("Recording Name"));
                String sq  = o.containsKey("Square Nr") ? safe(o.get("Square Nr")) : "";

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
                System.out.printf("   → [%s | %s] %s: '%s' vs '%s' (%s)%n",
                                  rec, sq.isEmpty() ? "—" : "Square " + sq,
                                  field, oldV, newV, stat);
                shown++;
            }
        } else {
            System.out.println("\n✅ No differences detected.");
        }

        return diffCount;
    }

    // ----------------------------------------------------------------------
    /**
     * Builds a grouping key based on the available columns in the CSV row.
     * If both "Recording Name" and "Square Nr" exist, both are used;
     * otherwise, only "Recording Name" is used.
     *
     * @param r a CSV row represented as a map
     * @return a combined key string identifying the group
     */
    private static String buildKey(Map<String, String> r) {
        String rec = safe(r.get("Recording Name"));
        String sq  = r.containsKey("Square Nr") ? safe(r.get("Square Nr")) : "";
        return sq.isEmpty() ? rec : rec + " - " + sq;
    }

    // ----------------------------------------------------------------------
    /**
     * Converts a list of CSV rows into a grouped map keyed by
     * "Recording Name" or "Recording Name - Square Nr", depending
     * on column availability.
     *
     * @param rows list of CSV rows as maps
     * @return a multi-map keyed appropriately for Paint CSVs
     */
    private static Map<String, List<Map<String, String>>> toMultiMap(List<Map<String, String>> rows) {
        Map<String, List<Map<String, String>>> mm = new TreeMap<>();
        for (Map<String, String> r : rows) {
            String key = buildKey(r);
            mm.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }
        return mm;
    }

    // ----------------------------------------------------------------------
    /**
     * Reads a CSV file from the given path and parses its content into a list of maps.
     * Each map represents a row in the CSV file, where the keys are the column headers
     * and the values are the corresponding cell values. Empty rows are skipped and
     * missing or empty values are represented as empty strings.
     *
     * @param path the path to the input CSV file
     * @return a list of maps, where each map represents a row with column headers as keys
     *         and cell values as values
     * @throws IOException if an I/O error occurs while reading the file
     */
    private static List<Map<String, String>> readCsv(Path path) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                return rows;
            }

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
    /**
     * Cleans the given string input by trimming whitespace and replacing specific
     * invalid values ("nan", "null") with an empty string.
     *
     * @param s the input string to clean; may be {@code null}
     * @return a cleaned string with whitespace removed; returns an empty string if
     *         the input is {@code null} or contains invalid values ("nan", "null")
     */
    private static String clean(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.equalsIgnoreCase("nan") || t.equalsIgnoreCase("null")) {
            return "";
        }
        return t;
    }

    /**
     * Compares two string values for equality. If the strings are equal using {@link Objects#equals},
     * the method returns true. If the strings represent valid numeric values, they are parsed and
     * compared as doubles. The method returns true if the numeric representations are equal, and
     * false otherwise.
     *
     * @param a the first string value to compare, may be {@code null}
     * @param b the second string value to compare, may be {@code null}
     * @return {@code true} if the strings are equal, either as exact string matches or as equivalent
     *         numeric values; {@code false} otherwise
     */
    private static boolean valuesEqual(String a, String b) {
        if (Objects.equals(a, b)) {
            return true;
        }
        Double da = parseDouble(a);
        Double db = parseDouble(b);
        return da != null && db != null && Double.compare(da, db) == 0;
    }

    /**
     * Attempts to parse a {@code String} into a {@code Double}. If the input string is
     * {@code null}, empty, or cannot be parsed as a double, the method returns {@code null}.
     * Also returns {@code null} if the parsed value is {@code NaN}.
     *
     * @param s the input string to parse into a {@code Double}; may be {@code null} or empty
     * @return the {@code Double} value represented by the input string, or {@code null}
     *         if the input is invalid, unparseable, or represents {@code NaN}
     */
    private static Double parseDouble(String s) {
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

    /**
     * Ensures that the given string is non-null by returning an empty string if the input is {@code null}.
     * If the input is not {@code null}, the original string is returned unchanged.
     *
     * @param s the input string to check; may be {@code null}
     * @return the original string if non-null, or an empty string if the input is {@code null}
     */
    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    /**
     * Processes a string to make it safe for use in a CSV file by properly escaping
     * special characters and enclosing the string in quotes if necessary. Special
     * characters include commas, double quotes, newlines, and carriage returns.
     *
     * @param s the input string to be processed; may be {@code null}
     * @return a CSV-safe string, enclosed in quotes if special characters are present,
     *         or an empty string if the input is {@code null}
     */
    private static String csv(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    static int compare_stub(Path baseline, Path testfile) throws IOException {
        System.out.println("🔍 CSV Regression Comparator");
        System.out.println("------------------------------------");
        System.out.println("Baseline file : " + baseline);
        System.out.println("Test file     : " + testfile);
        System.out.println();

        int diffs = compareFiles(baseline, testfile);

        System.out.println("\n✅ Regression comparison complete.");
        System.out.println("🔢 Differences detected: " + diffs);
        return diffs;
    }

    public static void main(String[] args) {
        Path baseline;
        Path testfile;
        int diffs = 0;

        try {
            baseline = Paths.get("/Users/hans/Paint Test Project/221012/Tracks.csv");
            testfile = Paths.get("/Users/hans/JavaPaintProjects/paint-regression/src/main/resources/221012 reference/Tracks.csv");
            diffs += compare_stub(baseline, testfile);

            baseline = Paths.get("/Users/hans/Paint Test Project/221012/Squares.csv");
            testfile = Paths.get("/Users/hans/JavaPaintProjects/paint-regression/src/main/resources/221012 reference/Squares.csv");
            diffs += compare_stub(baseline, testfile);

            baseline = Paths.get("/Users/hans/Paint Test Project/221012/Recordings.csv");
            testfile = Paths.get("/Users/hans/JavaPaintProjects/paint-regression/src/main/resources/221012 reference/Recordings.csv");
            diffs += compare_stub(baseline, testfile);

            System.out.println("\n\n🔢 Total differences detected: " + diffs);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}