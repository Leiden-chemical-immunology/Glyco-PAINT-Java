package paint.regression.clean;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static paint.shared.constants.PaintColumnNames.RECORDING_NAME;
import static paint.shared.constants.PaintColumnNames.SQUARE_NUMBER;

/**
 * CSV regression comparator with clean dual logging.
 */
public class CsvComparatorRegression {

    // GLOBAL MODE: RELAXED vs STRICT
    private static boolean relaxedComparison = true;

    private static final RegressionLogger LOGGER = new RegressionLogger(System.out);

    public static void enableDualLogging(Path logDir) throws IOException {
        LOGGER.enableFileLogging(logDir);
    }

    public static void setRelaxedComparison(boolean relaxed) {
        relaxedComparison = relaxed;
    }

    // ============================================================
    //  PUBLIC API
    // ============================================================
    public static int compareFiles(Path oldCsv, Path newCsv) throws IOException {

        LOGGER.println("📥 Reading baseline file...");
        List<Map<String, String>> oldRows = readCsv(oldCsv);
        LOGGER.println("   → " + oldRows.size() + " rows loaded.");

        LOGGER.println("📥 Reading test file...");
        List<Map<String, String>> newRows = readCsv(newCsv);
        LOGGER.println("   → " + newRows.size() + " rows loaded.");

        if (oldRows.size() == newRows.size()) {
            LOGGER.printf("✅ Same number of rows (%d). Ready for strict 1:1 comparison.%n",
                          oldRows.size());
        } else {
            LOGGER.printf("⚠️  Different row counts: baseline=%d, test=%d%n",
                          oldRows.size(), newRows.size());
        }

        Map<String, List<Map<String, String>>> oldMulti = toMultiMap(oldRows);
        Map<String, List<Map<String, String>>> newMulti = toMultiMap(newRows);

        Set<String> allKeys = new TreeSet<>(oldMulti.keySet());
        allKeys.addAll(newMulti.keySet());

        LOGGER.println("\n🔎 Starting detailed comparison...");
        LOGGER.println("   → Total unique (Recording Name [+ Square Nr]) keys: " + allKeys.size());
        LOGGER.println("");

        List<String[]> diffs = new ArrayList<>();
        int diffCount = 0;

        int processed = 0;
        int totalKeys = allKeys.size();

        for (String key : allKeys) {
            List<Map<String, String>> ol = oldMulti.get(key);
            List<Map<String, String>> nl = newMulti.get(key);
            if (ol == null) ol = Collections.emptyList();
            if (nl == null) nl = Collections.emptyList();

            int max = Math.max(ol.size(), nl.size());

            for (int i = 0; i < max; i++) {
                Map<String, String> o = (i < ol.size()) ? ol.get(i) : null;
                Map<String, String> n = (i < nl.size()) ? nl.get(i) : null;

                if (o == null && n != null) {
                    String rec = safe(n.get(RECORDING_NAME));
                    String sq = n.containsKey(SQUARE_NUMBER) ? safe(n.get(SQUARE_NUMBER)) : "";
                    diffs.add(new String[]{rec, sq, String.valueOf(i + 1), "", "", "", "Extra in NEW"});
                    diffCount++;
                    continue;
                }
                if (o != null && n == null) {
                    String rec = safe(o.get(RECORDING_NAME));
                    String sq = o.containsKey(SQUARE_NUMBER) ? safe(o.get(SQUARE_NUMBER)) : "";
                    diffs.add(new String[]{rec, sq, String.valueOf(i + 1), "", "", "", "Missing in NEW"});
                    diffCount++;
                    continue;
                }

                Set<String> fields = new LinkedHashSet<>();
                fields.addAll(o.keySet());
                fields.addAll(n.keySet());

                String rec = safe(o.get(RECORDING_NAME));
                String sq = o.containsKey(SQUARE_NUMBER) ? safe(o.get(SQUARE_NUMBER)) : "";

                for (String f : fields) {
                    if (f == null || f.trim().length() == 0) continue;
                    if (RegressionRules.isIgnoredColumn(f, relaxedComparison)) continue;

                    String ov = RegressionRules.clean(o.get(f));
                    String nv = RegressionRules.clean(n.get(f));

                    if (RegressionRules.isIgnoreCaseField(f)) {
                        if (ov.equalsIgnoreCase(nv)) continue;
                    }

                    if (RegressionRules.emptyAndZeroEquiv(ov, nv)) continue;

                    if (RegressionRules.isIgnoreCaseField(f)) {
                        if (ov.equalsIgnoreCase(nv)) continue;
                    }

                    if (RegressionRules.valuesEqual(ov, nv)) continue;

                    Double od = RegressionRules.parseDouble(ov);
                    Double nd = RegressionRules.parseDouble(nv);

                    // Skip if one numeric side is missing (strict + relaxed)
                    if (RegressionRules.numericMissingSkipDifference(od, nd)) {
                        continue;
                    }

                    if (od != null && nd != null) {
                        nd = RegressionRules.correctedValueIfTrackDependent(f, od, nd, o, n);
                        if (RegressionRules.numericEqualWithTolerance(f, od, nd, relaxedComparison)) {
                            continue;
                        }
                    }

                    String status = (od != null && nd != null)
                            ? "NUMERIC DIFFERENCE"
                            : "TEXT DIFFERENCE";

                    diffs.add(new String[]{rec, sq, String.valueOf(i + 1), f, ov, nv, status});
                    diffCount++;
                }
            }

            processed++;
            if (totalKeys <= 20) {
                LOGGER.println("   ✓ Compared: " + key);
            } else if (processed % 100 == 0) {
                LOGGER.printf("   ...processed %d/%d keys%n", processed, totalKeys);
            }
        }

        if (!diffs.isEmpty()) {
            printGroupedDifferences(diffs);
        } else {
            LOGGER.println("\n✅ No differences detected.");
        }

        return diffCount;
    }

    // ============================================================
    //  GROUPED REPORT
    // ============================================================
    private static void printGroupedDifferences(List<String[]> diffs) {
        Map<String, Map<String, List<String[]>>> grouped =
                new LinkedHashMap<String, Map<String, List<String[]>>>();

        for (String[] row : diffs) {
            String rec = row[0];
            String sq = row[1].length() == 0 ? "—" : row[1];
            Map<String, List<String[]>> perRec = grouped.get(rec);
            if (perRec == null) {
                perRec = new LinkedHashMap<String, List<String[]>>();
                grouped.put(rec, perRec);
            }
            List<String[]> perSq = perRec.get(sq);
            if (perSq == null) {
                perSq = new ArrayList<String[]>();
                perRec.put(sq, perSq);
            }
            perSq.add(row);
        }

        int globalFieldWidth = 2;
        for (String[] row : diffs) {
            if (row[3] != null && row[3].length() + 2 > globalFieldWidth) {
                globalFieldWidth = row[3].length() + 2;
            }
        }

        LOGGER.println("\n🔎 Differences grouped by Square");
        LOGGER.println("───────────────────────────────");

        int total = 0;
        Set<String> squaresWithDiffs = new TreeSet<String>();

        for (Map.Entry<String, Map<String, List<String[]>>> recEntry : grouped.entrySet()) {
            LOGGER.println("Recording: " + recEntry.getKey());
            for (Map.Entry<String, List<String[]>> sqEntry : recEntry.getValue().entrySet()) {
                String sq = sqEntry.getKey();
                if (!sq.equals("—")) squaresWithDiffs.add(sq);

                LOGGER.println("  ▫ Square " + sq + ":");
                for (String[] row : sqEntry.getValue()) {
                    LOGGER.printf(
                            "     - %-" + globalFieldWidth + "s: '%s' vs '%s' (%s)%n",
                            row[3], row[4], row[5], row[6]
                    );
                    total++;
                }
            }
            LOGGER.println("");
        }

        LOGGER.printf("%n📊 Total differences listed: %d%n", total);
        if (!squaresWithDiffs.isEmpty()) {
            LOGGER.printf("🟧 Squares with at least one difference: %d (%s)%n",
                          squaresWithDiffs.size(),
                          join(squaresWithDiffs, ", "));
        }
    }

    // ============================================================
    //  UTILITIES
    // ============================================================
    private static String join(Set<String> s, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String x : s) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(x);
        }
        return sb.toString();
    }

    private static String buildKey(Map<String, String> r) {
        String rec = safe(r.get(RECORDING_NAME));
        String sq = r.containsKey(SQUARE_NUMBER) ? safe(r.get(SQUARE_NUMBER)) : "";
        return sq.length() == 0 ? rec : rec + " - " + sq;
    }

    private static Map<String, List<Map<String, String>>> toMultiMap(List<Map<String, String>> rows) {
        Map<String, List<Map<String, String>>> mm = new TreeMap<String, List<Map<String, String>>>();

        for (Map<String, String> r : rows) {
            String key = buildKey(r);
            List<Map<String, String>> bucket = mm.get(key);
            if (bucket == null) {
                bucket = new ArrayList<Map<String, String>>();
                mm.put(key, bucket);
            }
            bucket.add(r);
        }

        return mm;
    }

    private static List<Map<String, String>> readCsv(Path path) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        BufferedReader br = Files.newBufferedReader(path);
        try {
            String headerLine = br.readLine();
            if (headerLine == null) return rows;

            String[] headers = headerLine.split(",", -1);
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim();
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() == 0) continue;

                String[] vals = line.split(",", -1);
                Map<String, String> row = new LinkedHashMap<>();

                for (int i = 0; i < headers.length; i++) {
                    String h = headers[i];
                    String v = (i < vals.length ? vals[i] : "");
                    row.put(h, v == null ? "" : v.trim());
                }
                rows.add(row);
            }
        } finally {
            br.close();
        }
        return rows;
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    // ============================================================
    //  CLI ENTRY POINT
    // ============================================================
    static int compare_stub(Path baseline, Path testfile) throws IOException {
        LOGGER.println("");
        LOGGER.println("");
        LOGGER.println("🔍 CSV Regression Comparator");
        LOGGER.println("------------------------------------");
        LOGGER.println("Baseline file : " + baseline);
        LOGGER.println("Test file     : " + testfile);
        LOGGER.println("");

        int diffs = compareFiles(baseline, testfile);

        LOGGER.println("\n✅ Regression comparison complete.");
        LOGGER.println("🔢 Differences detected: " + diffs);
        return diffs;
    }

    private static boolean emptyAndZeroEquiv(String a, String b) {
        // kept here only if you still want a direct call; otherwise use RegressionRules
        return RegressionRules.emptyAndZeroEquiv(a, b);
    }

    public static void main(String[] args) {
        Path baseline;
        Path testfile;
        int diffs = 0;

        try {
            // STRICT comparison for original reference
            setRelaxedComparison(false);
            Path logPath = Paths.get("/Users/hans/Downloads/logs1");
            enableDualLogging(logPath);

            Path projectRoot = Paths.get(System.getProperty("user.dir"));

            testfile = Paths.get("/Users/hans/Paint Test Project/221012/Squares.csv");
            baseline = projectRoot.resolve("paint-regression/src/main/resources/221012 reference/Squares.csv");
            diffs += compare_stub(baseline, testfile);

            testfile = Paths.get("/Users/hans/Paint Test Project/221012/Recordings.csv");
            baseline = projectRoot.resolve("paint-regression/src/main/resources/221012 reference/Recordings.csv");
            diffs += compare_stub(baseline, testfile);

            LOGGER.println("\n\n🔢 Total differences detected: " + diffs);

            LOGGER.compareLatestLogWithPrevious(logPath);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        try {
            // RELAXED comparison for v39 / new format
            setRelaxedComparison(true);
            Path logPath = Paths.get("/Users/hans/Downloads/logs2");
            enableDualLogging(logPath);

            testfile = Paths.get("/Users/hans/Paint Test Project/221012 - v39 - reprocessed/Squares.csv");
            baseline = Paths.get("/Users/hans/Paint Test Project/221012 - v39 - updated format/Squares.csv");
            diffs += compare_stub(baseline, testfile);

            testfile = Paths.get("/Users/hans/Paint Test Project/221012 - v39 - reprocessed/Recordings.csv");
            baseline = Paths.get("/Users/hans/Paint Test Project/221012 - v39 - updated format/Recordings.csv");
            diffs += compare_stub(baseline, testfile);

            LOGGER.println("\n\n🔢 Total differences detected: " + diffs);

            LOGGER.compareLatestLogWithPrevious(logPath);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}