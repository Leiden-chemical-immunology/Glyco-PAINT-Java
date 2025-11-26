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
            "Time Stamp"
    ));

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
                    if (IGNORE_COLUMNS.contains(f)) continue;

                    String ov = clean(o.get(f));
                    String nv = clean(n.get(f));

                    if (valuesEqual(ov, nv)) continue;

                    Double od = parseDouble(ov);
                    Double nd = parseDouble(nv);
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


    public static void main(String[] args) {
        Path baseline;
        Path testfile;
        int diffs = 0;

        try {
            enableDualLogging(Paths.get("/Users/hans/Paint Test Project/221012/logs"));

            Path projectRoot = Paths.get(System.getProperty("user.dir"));

            testfile = Paths.get("/Users/hans/Paint Test Project/221012/Squares.csv");
            baseline = projectRoot.resolve("paint-regression/src/main/resources/221012 reference/Squares.csv");
            diffs += compare_stub(baseline, testfile);

            testfile = Paths.get("/Users/hans/Paint Test Project/221012/Recordings.csv");
            baseline = projectRoot.resolve("paint-regression/src/main/resources/221012 reference/Recordings.csv");
            diffs += compare_stub(baseline, testfile);

            LOGGER.println("\n\n🔢 Total differences detected: " + diffs);

            compareLatestLogWithPrevious(Paths.get("/Users/hans/Paint Test Project/221012/logs"));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}