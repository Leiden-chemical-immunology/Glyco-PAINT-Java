package validation;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * ============================================================================
 *  SquaresCsvRegressionComparator.java
 *  Version: 2.2 (console progress reporting)
 *
 *  PURPOSE
 *    Regression-compare two already-normalized "All Squares" CSVs.
 *
 *  Features:
 *   • Full processing even if files are identical (no short-circuit)
 *   • Deterministic multi-map comparison
 *   • Progress output for large files
 *   • Prints summary and key stats to console
 * ============================================================================
 */
public class SquaresCsvRegressionComparator {

    public static void main(String[] args) {
        try {
            if (args.length != 2) {
                System.err.println("Usage: java validation.SquaresCsvRegressionComparator <baseline.csv> <test.csv>");
                System.exit(2);
            }

            Path baseline = Paths.get(args[0]).toAbsolutePath().normalize();
            Path testfile = Paths.get(args[1]).toAbsolutePath().normalize();
            Path report   = defaultReportPath();

            System.out.println("🔍 Squares CSV Regression Comparator");
            System.out.println("------------------------------------");
            System.out.println("Baseline file : " + baseline);
            System.out.println("Test file     : " + testfile);
            System.out.println("Report output : " + report);
            System.out.println();

            Files.createDirectories(report.getParent());

            int diffs = compareFiles(baseline, testfile, report);

            System.out.println("\n✅ Regression comparison complete.");
            System.out.println("📄 Report written to: " + report.toAbsolutePath());
            System.out.println("🔢 Total differences detected: " + diffs);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Path defaultReportPath() {
        return Paths.get(System.getProperty("user.home"), "Downloads", "Validate", "Squares",
                         "Squares Regression Differences.csv");
    }

    // ----------------------------------------------------------------------
    public static int compareFiles(Path oldCsv, Path newCsv, Path outCsv) throws IOException {
        System.out.println("📥 Reading baseline file...");
        List<Map<String, String>> oldRows = readCsv(oldCsv);
        System.out.println("   → " + oldRows.size() + " rows loaded.");

        System.out.println("📥 Reading test file...");
        List<Map<String, String>> newRows = readCsv(newCsv);
        System.out.println("   → " + newRows.size() + " rows loaded.");

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

        for (String key : allKeys) {
            List<Map<String, String>> ol = oldMulti.getOrDefault(key, Collections.emptyList());
            List<Map<String, String>> nl = newMulti.getOrDefault(key, Collections.emptyList());

//            sortBySignature(ol);
//            sortBySignature(nl);

            int max = Math.max(ol.size(), nl.size());

            for (int i = 0; i < max; i++) {
                Map<String, String> o = (i < ol.size()) ? ol.get(i) : null;
                Map<String, String> n = (i < nl.size()) ? nl.get(i) : null;

                if (o == null && n != null) {
                    String rec = safe(n.get("Recording Name"));
                    String sq  = safe(n.get("Square Nr"));
                    diffs.add(new String[]{ rec, sq, String.valueOf(i+1), "", "", "", "Extra in NEW" });
                    diffCount++;
                    continue;
                } else if (o != null && n == null) {
                    String rec = safe(o.get("Recording Name"));
                    String sq  = safe(o.get("Square Nr"));
                    diffs.add(new String[]{ rec, sq, String.valueOf(i+1), "", "", "", "Missing in NEW" });
                    diffCount++;
                    continue;
                }

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
                    String status = (od != null && nd != null) ? "NUMERIC DIFFERENCE" : "TEXT DIFFERENCE";

                    diffs.add(new String[]{ rec, sq, String.valueOf(i+1), f, ov, nv, status });
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

//    private static void sortBySignature(List<Map<String, String>> list) {
//        list.sort(Comparator.comparing(SquaresCsvRegressionComparator::rowSignature));
//    }
//
//    private static String rowSignature(Map<String, String> r) {
//        List<String> parts = new ArrayList<>();
//        List<String> keys = new ArrayList<>(r.keySet());
//        Collections.sort(keys);
//        for (String k : keys) parts.add(k + "=" + clean(r.get(k)));
//        return String.join("|", parts);
//    }

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
        if (da != null && db != null) return Double.compare(da, db) == 0;
        return false;
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            double v = Double.parseDouble(s);
            return (Double.isNaN(v) ? null : v);
        } catch (Exception e) {
            return null;
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}