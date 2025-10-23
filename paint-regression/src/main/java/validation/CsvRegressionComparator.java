package validation;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * The CsvRegressionComparator class provides the functionality to compare two CSV files line by line.
 * It generates a report summarizing the differences detected between the baseline and test CSV files.
 * Designed as a utility tool, it can be executed from the command line with appropriate parameters or used programmatically.
 */
public class CsvRegressionComparator {

    /**
     * The main method serves as the program entry point. It compares two CSV files
     * for regression differences, generates a comparison report, and outputs key
     * details to the console. The application expects two arguments: paths to the
     * baseline and test CSV files.
     *
     * @param args an array of command-line arguments where:
     *             args[0] is the path to the baseline CSV file
     *             args[1] is the path to the test CSV file
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

    /**
     * Constructs the default path where the regression comparison report will be saved.
     *
     * @return the default path for the regression difference report as a {@code Path} object,
     *         pointing to the "Squares Regression Differences.csv" file located in a "Validate/Squares"
     *         subdirectory under the user's "Downloads" folder.
     */
    private static Path defaultReportPath() {
        return Paths.get(System.getProperty("user.home"), "Downloads", "Validate", "Squares",
                         "Squares Regression Differences.csv");
    }

    // ----------------------------------------------------------------------
    /**
     * Compares two CSV files for differences based on their content, generates
     * a detailed comparison report in CSV format, and returns the total count
     * of detected differences. The comparison is performed row-wise and field-wise,
     * capturing any added, missing, or differing values between the two files.
     *
     * @param oldCsv the path to the baseline CSV file
     * @param newCsv the path to the test CSV file to compare against the baseline
     * @param outCsv the path where the generated comparison report will be saved
     * @return the total number of differences detected between the two CSV files
     * @throws IOException if an I/O error occurs while reading the input files
     *         or writing the output report
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
     * Converts a list of maps into a multi-map structure where each key in the resulting map
     * is derived by concatenating the "Recording Name" and "Square Nr" values from the input maps.
     * Each key maps to a list of the corresponding maps from the input list.
     *
     * @param rows a list of maps, where each map represents a row with string keys and values;
     *             expected to contain "Recording Name" and "Square Nr" keys.
     * @return a map where the keys are String combinations of "Recording Name" and "Square Nr" values,
     *         and the values are lists of maps with corresponding entries.
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
    /**
     * Cleans the given string input by trimming whitespace and replacing specific
     * invalid values ("nan", "null") with an empty string.
     *
     * @param s the input string to clean; may be {@code null}
     * @return a cleaned string with whitespace removed; returns an empty string if
     *         the input is {@code null} or contains invalid values ("nan", "null")
     */
    private static String clean(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.equalsIgnoreCase("nan") || t.equalsIgnoreCase("null")) return "";
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
        if (Objects.equals(a, b)) return true;
        Double da = parseDouble(a);
        Double db = parseDouble(b);
        if (da != null && db != null) return Double.compare(da, db) == 0;
        return false;
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
        if (s == null || s.isEmpty()) return null;
        try {
            double v = Double.parseDouble(s);
            return (Double.isNaN(v) ? null : v);
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
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}