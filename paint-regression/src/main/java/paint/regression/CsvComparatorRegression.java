package paint.regression;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static paint.shared.constants.PaintColumnNames.RECORDING_NAME;
import static paint.shared.constants.PaintColumnNames.SQUARE_NUMBER;

/**
 * CSV regression comparator with clean dual logging.
 */
public class CsvComparatorRegression {

    // ============================================================
    //  GLOBAL MODE: RELAXED vs STRICT
    // ============================================================
    private static boolean relaxedComparison = true; // YOU CAN TOGGLE THIS

    // ============================================================
    //  LOGGER
    // ============================================================
    private static final class Logger {
        private final PrintStream console;
        private PrintStream file;
        private boolean fileLoggingEnabled = true;   // ★ NEW

        Logger(PrintStream console) {
            this.console = console;
        }

        void enableFileLogging(Path logDir) throws IOException {
            String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            Path logFile = logDir.resolve("paint-regression-" + timestamp + ".log");
            Files.createDirectories(logFile.getParent());
            this.file = new PrintStream(Files.newOutputStream(logFile));
            println("🧾 Logging to: " + logFile.toAbsolutePath());
            println("");
        }

        // ★ NEW
        void disableFileLogging() {
            fileLoggingEnabled = false;
        }

        // ★ NEW
        void enableFileLogging() {
            fileLoggingEnabled = true;
        }

        void println(String msg) {
            console.println(msg);
            if (file != null && fileLoggingEnabled) {
                file.println(msg);
            }
        }

        void printf(String fmt, Object... args) {
            console.printf(fmt, args);
            if (file != null && fileLoggingEnabled) {
                file.printf(fmt, args);
            }
        }

        void flush() {
            console.flush();
            if (file != null && fileLoggingEnabled) {
                file.flush();
            }
        }
    }

    private static final Logger LOGGER = new Logger(System.out);

    public static void enableDualLogging(Path logDir) throws IOException {
        LOGGER.enableFileLogging(logDir);
    }

    // ============================================================
    //  IGNORE COLUMNS
    // ============================================================
    private static final Set<String> IGNORE_COLUMNS = new HashSet<String>(Arrays.asList(
            "Run Time",
            "Time Stamp",
            "Row Number",
            "Column Number",
            "Label Number",
            "Median Median Speed",
            "Median Mean Speed",
            "Max Median Speed",
            "Max Mean Speed",
            "Density Ratio"
            ));

    private static final Set<String> IGNORE_CASE_FIELDS = new HashSet<String>(Arrays.asList(
            "Visible",
            "Square Manually Excluded",
            "Image Excluded"
    ));


    // ============================================================
    //  RELATIVE NUMERIC TOLERANCES (per field)
    // ============================================================
    private static final Map<String, Double> RELATIVE_TOLERANCE = new HashMap<String, Double>();
    static {
        RELATIVE_TOLERANCE.put("Tau",                   0.01);
        RELATIVE_TOLERANCE.put("Density",               0.02);
        RELATIVE_TOLERANCE.put("Density Ratio",         0.02);
        RELATIVE_TOLERANCE.put("R Squared",             0.01);
        RELATIVE_TOLERANCE.put("Variability",           0.02);
        RELATIVE_TOLERANCE.put("Median Displacement",   0.02);
        RELATIVE_TOLERANCE.put("Max Displacement",      0.02);
        RELATIVE_TOLERANCE.put("Total Displacement",    0.02);
        RELATIVE_TOLERANCE.put("Median Max Speed",      0.02);
        RELATIVE_TOLERANCE.put("Max Max Speed",         0.02);
        RELATIVE_TOLERANCE.put("Median Median Speed",   0.02);
        RELATIVE_TOLERANCE.put("Max Mean Speed",        0.02);
        RELATIVE_TOLERANCE.put("Max Track Duration",    0.02);
        RELATIVE_TOLERANCE.put("Total Track Duration",  0.02);
        RELATIVE_TOLERANCE.put("Median Track Duration", 0.02);
        RELATIVE_TOLERANCE.put("Density Ratio Ori",     0.02);

        // Add more fields here if needed
    }


    // Rounding precision per numeric field
    // Example: 3 = compare rounded to 3 decimals
    private static final Map<String, Integer> ROUNDING_PRECISION = new HashMap<String, Integer>();
    static {;
        ROUNDING_PRECISION.put("R Squared",                         2);
        ROUNDING_PRECISION.put("Tau",                               0);
        ROUNDING_PRECISION.put("Density",                           1);
        ROUNDING_PRECISION.put("Density Ratio",                     1);
        ROUNDING_PRECISION.put("R Squared",                         2);
        ROUNDING_PRECISION.put("Variability",                       2);
        ROUNDING_PRECISION.put("Median Displacement",               1);
        ROUNDING_PRECISION.put("Max Displacement",                  1);
        ROUNDING_PRECISION.put("Total Displacement",                1);
        ROUNDING_PRECISION.put("Median Max Speed",                  1);
        ROUNDING_PRECISION.put("Max Max Speed",                     1);
        ROUNDING_PRECISION.put("Median Median Speed",               1);
        ROUNDING_PRECISION.put("Max Mean Speed",                    1);
        ROUNDING_PRECISION.put("Max Track Duration",                1);
        ROUNDING_PRECISION.put("Total Track Duration",              1);
        ROUNDING_PRECISION.put("Median Track Duration",             1);
        ROUNDING_PRECISION.put("Density Ratio Ori",                 1);
        ROUNDING_PRECISION.put("Median Diffusion Coefficient",      2);
        ROUNDING_PRECISION.put("Median Diffusion Coefficient Ext",  2);
        // add more as needed
    }


    // ============================================================
    // STRICT-MODE RULE OVERRIDES
    // ============================================================
    private static final Set<String> STRICT_IGNORE_COLUMNS = new HashSet<String>(Arrays.asList(
            "Run Time",
            "Time Stamp"
            // FEWER FIELDS IGNORED
    ));

    private static final Map<String, Double> STRICT_RELATIVE_TOLERANCE = new HashMap<String, Double>();
    static {
        STRICT_RELATIVE_TOLERANCE.put("Tau",                   0.0001);
        STRICT_RELATIVE_TOLERANCE.put("Density",               0.0001);
        STRICT_RELATIVE_TOLERANCE.put("Density Ratio",         0.0001);
        STRICT_RELATIVE_TOLERANCE.put("R Squared",             0.0001);
        STRICT_RELATIVE_TOLERANCE.put("Variability",           0.0001);
        STRICT_RELATIVE_TOLERANCE.put("Median Displacement",   0.0001);
        STRICT_RELATIVE_TOLERANCE.put("Max Displacement",      0.0001);
        STRICT_RELATIVE_TOLERANCE.put("Total Displacement",    0.0001);
        STRICT_RELATIVE_TOLERANCE.put("Median Max Speed",      0.0001);
        STRICT_RELATIVE_TOLERANCE.put("Max Max Speed",         0.0001);
        STRICT_RELATIVE_TOLERANCE.put("Median Median Speed",   0.0001);
        STRICT_RELATIVE_TOLERANCE.put("Max Mean Speed",        0.0001);
        STRICT_RELATIVE_TOLERANCE.put("Max Track Duration",    0.0001);
        STRICT_RELATIVE_TOLERANCE.put("Total Track Duration",  0.0001);
        STRICT_RELATIVE_TOLERANCE.put("Median Track Duration", 0.0001);
        STRICT_RELATIVE_TOLERANCE.put("Density Ratio Ori",     0.0001);
    }

    // STRICT rounding rules (tighter or equal)
    private static final Map<String, Integer> STRICT_ROUNDING_PRECISION = new HashMap<String, Integer>();
    static {
        ROUNDING_PRECISION.put("R Squared",                         3);
        ROUNDING_PRECISION.put("Tau",                               3);
        ROUNDING_PRECISION.put("Density",                           3);
        ROUNDING_PRECISION.put("Density Ratio",                     3);
        ROUNDING_PRECISION.put("R Squared",                         3);
        ROUNDING_PRECISION.put("Variability",                       3);
        ROUNDING_PRECISION.put("Median Displacement",               3);
        ROUNDING_PRECISION.put("Max Displacement",                  3);
        ROUNDING_PRECISION.put("Total Displacement",                3);
        ROUNDING_PRECISION.put("Median Max Speed",                  3);
        ROUNDING_PRECISION.put("Max Max Speed",                     3);
        ROUNDING_PRECISION.put("Median Median Speed",               3);
        ROUNDING_PRECISION.put("Max Mean Speed",                    3);
        ROUNDING_PRECISION.put("Max Track Duration",                3);
        ROUNDING_PRECISION.put("Total Track Duration",              3);
        ROUNDING_PRECISION.put("Median Track Duration",             3);
        ROUNDING_PRECISION.put("Density Ratio Ori",                 3);
        ROUNDING_PRECISION.put("Median Diffusion Coefficient",      3);
        ROUNDING_PRECISION.put("Median Diffusion Coefficient Ext",  3);
        // Add more if necessary
    }

    // ============================================================
    //  PUBLIC API
    // ============================================================
    public static int compareFiles(Path oldCsv, Path newCsv) throws IOException {

        LOGGER.println("📥 Reading baseline file...");
        List<Map<String,String>> oldRows = readCsv(oldCsv);
        LOGGER.println("   → " + oldRows.size() + " rows loaded.");

        LOGGER.println("📥 Reading test file...");
        List<Map<String,String>> newRows = readCsv(newCsv);
        LOGGER.println("   → " + newRows.size() + " rows loaded.");

        if (oldRows.size() == newRows.size()) {
            LOGGER.printf("✅ Same number of rows (%d). Ready for strict 1:1 comparison.%n",
                          oldRows.size());
        } else {
            LOGGER.printf("⚠️  Different row counts: baseline=%d, test=%d%n",
                          oldRows.size(), newRows.size());
        }

        Map<String,List<Map<String,String>>> oldMulti = toMultiMap(oldRows);
        Map<String,List<Map<String,String>>> newMulti = toMultiMap(newRows);

        Set<String> allKeys = new TreeSet<String>(oldMulti.keySet());
        allKeys.addAll(newMulti.keySet());

        LOGGER.println("\n🔎 Starting detailed comparison...");
        LOGGER.println("   → Total unique (Recording Name [+ Square Nr]) keys: " + allKeys.size());
        LOGGER.println("");

        List<String[]> diffs = new ArrayList<String[]>();
        int diffCount = 0;

        int processed = 0;
        int totalKeys = allKeys.size();

        // ========================================================
        // CORE COMPARISON LOOP
        // ========================================================
        for (String key : allKeys) {
            List<Map<String,String>> ol = oldMulti.get(key);
            List<Map<String,String>> nl = newMulti.get(key);
            if (ol == null) ol = Collections.emptyList();
            if (nl == null) nl = Collections.emptyList();

            int max = Math.max(ol.size(), nl.size());

            for (int i = 0; i < max; i++) {
                Map<String,String> o = (i < ol.size()) ? ol.get(i) : null;
                Map<String,String> n = (i < nl.size()) ? nl.get(i) : null;

                if (o == null && n != null) {
                    String rec = safe(n.get(RECORDING_NAME));
                    String sq  = n.containsKey(SQUARE_NUMBER) ? safe(n.get(SQUARE_NUMBER)) : "";
                    diffs.add(new String[]{rec, sq, String.valueOf(i+1), "", "", "", "Extra in NEW"});
                    diffCount++;
                    continue;
                }
                if (o != null && n == null) {
                    String rec = safe(o.get(RECORDING_NAME));
                    String sq  = o.containsKey(SQUARE_NUMBER) ? safe(o.get(SQUARE_NUMBER)) : "";
                    diffs.add(new String[]{rec, sq, String.valueOf(i+1), "", "", "", "Missing in NEW"});
                    diffCount++;
                    continue;
                }

                Set<String> fields = new LinkedHashSet<String>();
                fields.addAll(o.keySet());
                fields.addAll(n.keySet());

                String rec = safe(o.get(RECORDING_NAME));
                String sq  = o.containsKey(SQUARE_NUMBER) ? safe(o.get(SQUARE_NUMBER)) : "";

                for (String f : fields) {
                    if (f == null || f.trim().length() == 0) continue;
                    if (relaxedComparison) {
                        if (IGNORE_COLUMNS.contains(f)) continue;
                    } else {
                        if (STRICT_IGNORE_COLUMNS.contains(f)) continue;
                    }

                    String ov = clean(o.get(f));
                    String nv = clean(n.get(f));

                    // Case-insensitive match for selected fields
                    if (IGNORE_CASE_FIELDS.contains(f)) {
                        if (ov.equalsIgnoreCase(nv)) continue;
                    }

                    // NEW RULE: empty vs 0 / 0.0 / -1 / -1.0 → treat as equal
                    if (emptyAndZeroEquiv(ov, nv)) continue;

                    // Case-insensitive match for selected fields
                    if (IGNORE_CASE_FIELDS.contains(f)) {
                        if (ov.equalsIgnoreCase(nv)) continue;
                    }

                    // Normal numeric or text comparison
                    if (valuesEqual(ov, nv)) continue;

                    Double od = parseDouble(ov);
                    Double nd = parseDouble(nv);

                    // TRACK-BASED CORRECTION
                    if (od != null && nd != null) {
                        nd = correctedValueIfTrackDependent(f, od, nd, o, n);
                    }

                    // Apply rounding + relative tolerance per field
                    if (od != null && nd != null && numericEqualWithTolerance(f, od, nd)) {
                        continue;
                    }

                    String status = (od != null && nd != null)
                            ? "NUMERIC DIFFERENCE"
                            : "TEXT DIFFERENCE";

                    diffs.add(new String[]{rec, sq, String.valueOf(i+1), f, ov, nv, status});
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

        // ========================================================
        // SUMMARY
        // ========================================================
        if (!diffs.isEmpty()) {
            Map<String, Map<String,List<String[]>>> grouped =
                    new LinkedHashMap<String, Map<String,List<String[]>>>();

            for (String[] row : diffs) {
                String rec = row[0];
                String sq = row[1].length() == 0 ? "—" : row[1];
                Map<String,List<String[]>> perRec = grouped.get(rec);
                if (perRec == null) {
                    perRec = new LinkedHashMap<String,List<String[]>>();
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

            for (Map.Entry<String, Map<String,List<String[]>>> recEntry : grouped.entrySet()) {
                LOGGER.println("Recording: " + recEntry.getKey());
                for (Map.Entry<String,List<String[]>> sqEntry : recEntry.getValue().entrySet()) {
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
        } else {
            LOGGER.println("\n✅ No differences detected.");
        }

        return diffCount;
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

    private static String buildKey(Map<String,String> r) {
        String rec = safe(r.get(RECORDING_NAME));
        String sq  = r.containsKey(SQUARE_NUMBER) ? safe(r.get(SQUARE_NUMBER)) : "";
        return sq.length() == 0 ? rec : rec + " - " + sq;
    }

    private static Map<String, List<Map<String, String>>> toMultiMap(List<Map<String, String>> rows) {
        Map<String, List<Map<String, String>>> mm = new TreeMap<String, List<Map<String, String>>>();

        for (Map<String, String> r : rows) {
            String key = buildKey(r);

            // Ensure the bucket exists
            List<Map<String, String>> bucket = mm.get(key);
            if (bucket == null) {
                bucket = new ArrayList<Map<String, String>>();
                mm.put(key, bucket);
            }

            // Add the actual row map
            bucket.add(r);
        }

        return mm;
    }

    private static List<Map<String,String>> readCsv(Path path) throws IOException {
        List<Map<String,String>> rows = new ArrayList<Map<String,String>>();
        BufferedReader br = Files.newBufferedReader(path);
        try {
            String headerLine = br.readLine();
            if (headerLine == null) return rows;

            String[] headers =
                    Arrays.stream(headerLine.split(",", -1))
                          .map(String::trim)
                          .toArray(String[]::new);

            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() == 0) continue;

                String[] vals = line.split(",", -1);
                Map<String,String> row = new LinkedHashMap<String,String>();

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

    private static String clean(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.equalsIgnoreCase("nan")) return "";
        if (t.equalsIgnoreCase("null")) return "";
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

    private static boolean numericEqualWithTolerance(String field, double a, double b) {

        // Select the correct rule-set based on global flag
        Map<String, Double> tolMap = relaxedComparison ? RELATIVE_TOLERANCE : STRICT_RELATIVE_TOLERANCE;
        Map<String, Integer> roundMap = relaxedComparison ? ROUNDING_PRECISION : STRICT_ROUNDING_PRECISION;

        // 1) Rounding first
        Integer prec = roundMap.get(field);
        if (prec != null) {
            double ra = round(a, prec);
            double rb = round(b, prec);
            if (Double.compare(ra, rb) == 0) return true;
        }

        // 2) Relative tolerance
        Double relTol = tolMap.get(field);
        if (relTol != null) {
            double denom = Math.max(1e-9, Math.max(Math.abs(a), Math.abs(b)));
            double relErr = Math.abs(a - b) / denom;
            if (relErr <= relTol) return true;
        }

        return false;
    }

    private static double round(double v, int decimals) {
        double f = Math.pow(10, decimals);
        return Math.round(v * f) / f;
    }

    private static Double parseDouble(String s) {
        if (s == null || s.length() == 0) return null;
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



    // ============================================================
    //  LOG FILE COMPARISON
    // ============================================================
    public static void compareLatestLogWithPrevious(Path logDir) throws IOException {

        // ★ Turn OFF file logging — console only
        LOGGER.disableFileLogging();

        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(logDir, "paint-regression-*.log");
            List<Path> logs = new ArrayList<Path>();
            for (Path p : stream) logs.add(p);
            stream.close();

            if (logs.size() < 2) {
                LOGGER.println("ℹ️ Not enough log files to compare (" + logs.size() + " found).");
                return;
            }

            Collections.sort(logs);

            Path previous = logs.get(logs.size() - 2);
            Path latest   = logs.get(logs.size() - 1);

            LOGGER.println("");
            LOGGER.println("🧪 Comparing log files:");
            LOGGER.println("   Previous: " + previous.getFileName());
            LOGGER.println("   Latest  : " + latest.getFileName());
            LOGGER.println("");

            List<String> oldLines = Files.readAllLines(previous);
            List<String> newLines = Files.readAllLines(latest);

            int max = Math.max(oldLines.size(), newLines.size());
            int differences = 0;

            for (int i = 0; i < max; i++) {
                String oldL = (i < oldLines.size()) ? oldLines.get(i) : "";
                String newL = (i < newLines.size()) ? newLines.get(i) : "";

                if (!Objects.equals(oldL, newL)) {
                    LOGGER.printf("🔸 Line %d:%n", (i+1));
                    LOGGER.printf("     OLD: %s%n", oldL);
                    LOGGER.printf("     NEW: %s%n", newL);
                    differences++;
                }
            }

            if (differences == 0) {
                LOGGER.println("✅ No differences in log files.");
            } else {
                LOGGER.println("\n📊 Total log differences detected: " + differences);
            }

        } finally {
            // ★ Restore writing to file
            // LOGGER.enableFileLogging();
        }
    }


    /**
     * Adjust fields that scale with number of tracks.
     *
     * If old/new number of tracks differ, density values are expected
     * to scale proportionally with track count.
     */
    private static Double correctedValueIfTrackDependent(
            String field, Double oldVal, Double newVal,
            Map<String,String> oldRow, Map<String,String> newRow)
    {
        if (oldVal == null || newVal == null) return newVal;

        // Only apply to known fields
        if (!field.equals("Density") && !field.equals("Density Ratio Ori")) {
            return newVal;
        }

        // Read track counts
        Double oldTracks = parseDouble(oldRow.get("Number of Tracks"));
        Double newTracks = parseDouble(newRow.get("Number of Tracks"));
        if (oldTracks == null || newTracks == null) return newVal;

        if (oldTracks <= 0 || newTracks <= 0) return newVal;

        // If equal, no correction
        if (Objects.equals(oldTracks, newTracks)) return newVal;

        // Correction: scale new value to what it *would* be
        // had the number of tracks matched.
        double ratio = oldTracks / newTracks;
        double corrected = newVal * ratio;

        return corrected;
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
        // Normalize
        String x = (a == null ? "" : a.trim());
        String y = (b == null ? "" : b.trim());

        // If one is empty and the other is numeric 0 or -1
        if (x.isEmpty() && (
                y.equals("0")   || y.equals("0.0")   ||
                        y.equals("-1")  || y.equals("-1.0")  ||
                        y.equals("-2")  || y.equals("-2.0")  ||
                        y.equals("-3")  || y.equals("-3.0"))) {
            return true;
        }
        if (y.isEmpty() && (
                x.equals("0")   || x.equals("0.0")   ||
                        x.equals("-1")  || x.equals("-1.0")  ||
                        x.equals("-2")  || x.equals("-2.0")  ||
                        x.equals("-3")  || x.equals("-3.0"))) {
            return true;
        }

        return false;
    }


    public static void main(String[] args) {
        Path baseline;
        Path testfile;
        int diffs = 0;

        try {
            relaxedComparison = false;
            enableDualLogging(Paths.get("/Users/hans/Downloads/logs1"));

            Path projectRoot = Paths.get(System.getProperty("user.dir"));

            testfile = Paths.get("/Users/hans/Paint Test Project/221012/Squares.csv");
            baseline = projectRoot.resolve("paint-regression/src/main/resources/221012 reference/Squares.csv");
            diffs += compare_stub(baseline, testfile);

            testfile = Paths.get("/Users/hans/Paint Test Project/221012/Recordings.csv");
            baseline = projectRoot.resolve("paint-regression/src/main/resources/221012 reference/Recordings.csv");
            diffs += compare_stub(baseline, testfile);

            LOGGER.println("\n\n🔢 Total differences detected: " + diffs);

            compareLatestLogWithPrevious(Paths.get("/Users/hans/Downloads/logs1"));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }


        try {
            relaxedComparison = true;
            enableDualLogging(Paths.get("/Users/hans/Downloads/logs2"));

            testfile = Paths.get("/Users/hans/Paint Test Project/221012 - v39 - reprocessed/Squares.csv");
            baseline = Paths.get("/Users/hans/Paint Test Project/221012 - v39 - updated format/Squares.csv");
            diffs += compare_stub(baseline, testfile);

            testfile = Paths.get("/Users/hans/Paint Test Project/221012 - v39 - reprocessed/Recordings.csv");
            baseline = Paths.get("/Users/hans/Paint Test Project/221012 - v39 - updated format/Recordings.csv");;
            diffs += compare_stub(baseline, testfile);

            LOGGER.println("\n\n🔢 Total differences detected: " + diffs);

            compareLatestLogWithPrevious(Paths.get("/Users/hans/Downloads/logs2"));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}