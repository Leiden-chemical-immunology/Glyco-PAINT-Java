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
 *    - java.text
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

package paint.validation;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
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
    // Columns that should be ignored during field-by-field comparison
    private static final Set<String> IGNORE_COLUMNS = new HashSet<>(Arrays.asList(
            "Run Time", "Time Stamp", "Unique Key"
    ));

    // ----------------------------------------------------------------------
    // Dual logging: console + file
    private static PrintStream dualOut;

    private static void setupDualLogging(Path logDir) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        Path logFile = logDir.resolve("paint-regression-" + timestamp + ".log");
        Files.createDirectories(logFile.getParent());

        // Keep reference to original console output
        final PrintStream originalOut = System.out;
        final PrintStream fileOut = new PrintStream(Files.newOutputStream(logFile));

        dualOut = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                originalOut.write(b);  // use original, not System.out
                fileOut.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                originalOut.write(b, off, len);
                fileOut.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                originalOut.flush();
                fileOut.flush();
            }
        }, true);

        System.setOut(dualOut);
        System.setErr(dualOut);
        System.out.println("🧾 Logging to: " + logFile.toAbsolutePath());
        System.out.println();
    }

    // ----------------------------------------------------------------------
    /**
     * Compares two CSV files for differences based on their content, generates
     * a detailed comparison report, and returns the total number of detected differences.
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

                if (o == null && n != null) {
                    String rec = safe(n.get("Recording Name"));
                    String sq  = n.containsKey("Square Number") ? safe(n.get("Square Number")) : "";
                    diffs.add(new String[]{ rec, sq, String.valueOf(i + 1), "", "", "", "Extra in NEW" });
                    diffCount++;
                    continue;
                } else if (o != null && n == null) {
                    String rec = safe(o.get("Recording Name"));
                    String sq  = o.containsKey("Square Number") ? safe(o.get("Square Number")) : "";
                    diffs.add(new String[]{ rec, sq, String.valueOf(i + 1), "", "", "", "Missing in NEW" });
                    diffCount++;
                    continue;
                }

                // Compare common fields
                Set<String> fields = new LinkedHashSet<>();
                fields.addAll(o.keySet());
                fields.addAll(n.keySet());

                String rec = safe(o.get("Recording Name"));
                String sq  = o.containsKey("Square Number") ? safe(o.get("Square Number")) : "";

                for (String f : fields) {
                    if (f == null || f.trim().isEmpty()) continue;
                    if (IGNORE_COLUMNS.contains(f)) continue;

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

        // === Grouped Difference Summary (aligned globally) ===
        if (!diffs.isEmpty()) {
            // Group by recording and square
            Map<String, Map<String, List<String[]>>> grouped = new LinkedHashMap<>();
            for (String[] row : diffs) {
                String rec = row[0];
                String sq  = row[1].isEmpty() ? "—" : row[1];
                grouped.computeIfAbsent(rec, r -> new LinkedHashMap<>())
                        .computeIfAbsent(sq, s -> new ArrayList<>())
                        .add(row);
            }

            // Compute global max field name width for perfect alignment
            int globalFieldWidth = diffs.stream()
                    .map(row -> row[3] == null ? 0 : row[3].length())
                    .max(Integer::compareTo)
                    .orElse(0) + 2;

            System.out.println("\n🔎 Differences grouped by Square");
            System.out.println("───────────────────────────────");

            int total = 0;
            Set<String> squaresWithDiffs = new TreeSet<>();

            for (Map.Entry<String, Map<String, List<String[]>>> recEntry : grouped.entrySet()) {
                System.out.println("Recording: " + recEntry.getKey());
                for (Map.Entry<String, List<String[]>> sqEntry : recEntry.getValue().entrySet()) {
                    String sq = sqEntry.getKey();
                    if (!sq.equals("—")) squaresWithDiffs.add(sq);
                    List<String[]> entries = sqEntry.getValue();

                    System.out.println("  ▫ Square " + sq + ":");
                    for (String[] row : entries) {
                        String field = row[3];
                        String oldV  = row[4];
                        String newV  = row[5];
                        String stat  = row[6];
                        System.out.printf("     - %-" + globalFieldWidth + "s: '%s' vs '%s' (%s)%n",
                                          field, oldV, newV, stat);
                        total++;
                    }
                }
                System.out.println();
            }

            System.out.printf("%n📊 Total differences listed: %d%n", total);
            if (!squaresWithDiffs.isEmpty()) {
                System.out.printf("🟧 Squares with at least one difference: %d (%s)%n",
                                  squaresWithDiffs.size(),
                                  String.join(", ", squaresWithDiffs));
            }
        } else {
            System.out.println("\n✅ No differences detected.");
        }
        return diffCount;
    }

    // ----------------------------------------------------------------------
    private static String buildKey(Map<String, String> r) {
        String rec = safe(r.get("Recording Name"));
        String sq  = r.containsKey("Square Number") ? safe(r.get("Square Number")) : "";
        return sq.isEmpty() ? rec : rec + " - " + sq;
    }

    private static Map<String, List<Map<String, String>>> toMultiMap(List<Map<String, String>> rows) {
        Map<String, List<Map<String, String>>> mm = new TreeMap<>();
        for (Map<String, String> r : rows) {
            String key = buildKey(r);
            mm.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }
        return mm;
    }

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

    private static String clean(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.equalsIgnoreCase("nan") || t.equalsIgnoreCase("null")) return "";
        return t;
    }

    private static boolean valuesEqual(String a, String b) {
        if (Objects.equals(a, b)) return true;
        Double da = parseDouble(a);
        Double db = parseDouble(b);
        if (da != null && db != null) {
            double ra = Math.round(da * 1000.0) / 1000.0;
            double rb = Math.round(db * 1000.0) / 1000.0;
            return Double.compare(ra, rb) == 0;
        }
        return false;
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            double v = Double.parseDouble(s);
            return Double.isNaN(v) ? null : v;
        } catch (Exception e) {
            return null;
        }
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    // ----------------------------------------------------------------------
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
            setupDualLogging(Paths.get("/Users/hans/Paint Test Project/221012/logs"));

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