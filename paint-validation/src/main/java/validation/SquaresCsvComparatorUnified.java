package validation;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * ============================================================================
 *  SquaresCsvComparatorUnified.java
 *  Numeric-only comparator + normalizer + detailed diff + selected overview
 *  Output: ~/Downloads/Validate/Squares/
 * ============================================================================
 */
public class SquaresCsvComparatorUnified {

    // ---------------------------- Field Maps ----------------------------

    /** Old->New column mapping (for normalization) */
    private static final Map<String, String> FIELD_MAP = new LinkedHashMap<>();
    static {
        FIELD_MAP.put("Row Nr", "Row Number");
        FIELD_MAP.put("Col Nr", "Column Number");
        FIELD_MAP.put("Nr Tracks", "Number of Tracks");
        FIELD_MAP.put("X0", "X0");
        FIELD_MAP.put("Y0", "Y0");
        FIELD_MAP.put("X1", "X1");
        FIELD_MAP.put("Y1", "Y1");
        FIELD_MAP.put("Variability", "Variability");
        FIELD_MAP.put("Density", "Density");
        FIELD_MAP.put("Density Ratio", "Density Ratio Ori");
        FIELD_MAP.put("Tau", "Tau");
        FIELD_MAP.put("R Squared", "R Squared");
        FIELD_MAP.put("Median Diffusion Coefficient", "Median Diffusion Coefficient");
        FIELD_MAP.put("Median Diffusion Coefficient Ext", "Median Diffusion Coefficient Ext");
        FIELD_MAP.put("Median Long Track Duration", "Median Long Track Duration");
        FIELD_MAP.put("Median Short Track Duration", "Median Short Track Duration");
        FIELD_MAP.put("Median Displacement", "Median Displacement");
        FIELD_MAP.put("Max Displacement", "Max Displacement");
        FIELD_MAP.put("Total Displacement", "Total Displacement");
        FIELD_MAP.put("Median Max Speed", "Median Max Speed");
        FIELD_MAP.put("Max Max Speed", "Max Max Speed");
        FIELD_MAP.put("Max Track Duration", "Max Track Duration");
        FIELD_MAP.put("Total Track Duration", "Total Track Duration");
        FIELD_MAP.put("Median Track Duration", "Median Track Duration");
        FIELD_MAP.put("Square Nr", "Square Number");
    }

    /** Numeric fields (only these are compared + detailed) */
    private static final List<String> NUMERIC_FIELDS_LIST = Arrays.asList(
            "Square Nr",
            "Nr Tracks",
            "Tau",
            "R Squared",
            "Median Track Duration",
            "Total Track Duration",
            "Median Displacement",
            "Density Ratio",
            "Variability",
            "Density",
            "Median Max Speed",
            "Max Track Duration",
            "Total Displacement",
            "Max Displacement",
            "Median Diffusion Coefficient",
            "Median Diffusion Coefficient Ext",
            "Median Long Track Duration",
            "Median Short Track Duration"
    );
    private static final Set<String> NUMERIC_FIELDS = new HashSet<>(NUMERIC_FIELDS_LIST);

    /** Default rounding precision if nothing better is detected */
    private static final Map<String, Integer> ROUNDING_MAP = new HashMap<>();
    static {
        for (String f : NUMERIC_FIELDS) ROUNDING_MAP.put(f, 3);
        ROUNDING_MAP.put("Variability", 1);
        ROUNDING_MAP.put("Tau", 2);
        ROUNDING_MAP.put("Density Ratio", 2);
        ROUNDING_MAP.put("R Squared", 2);
    }

    /** Per-field tolerance in % for Comparison.csv status */
    private static final Map<String, Double> TOLERANCE_MAP = new HashMap<>();
    static {
        // 5% default for most; you can tune per field later
        for (String f : NUMERIC_FIELDS) TOLERANCE_MAP.put(f, 5.0);
    }

    /** Effective precision detected from both files (min of the two) */
    private static final Map<String, Integer> EFFECTIVE_PRECISION_MAP = new HashMap<>();

    // ---------------------------- Main ----------------------------

    public static void main(String[] args) {
        try {
            Path outDir = Paths.get(System.getProperty("user.home"), "Downloads", "Validate", "Squares");
            Files.createDirectories(outDir);

            Path oldCsv = Paths.get("/Users/hans/Paint Test Project/221012 - Python/All Squares.csv");
            Path newCsv = Paths.get("/Users/hans/Paint Test Project/221012/All Squares Java.csv");

            System.out.println("Reading CSVs...");
            List<Map<String, String>> oldRows = readCsv(oldCsv);
            List<Map<String, String>> newRows = readCsv(newCsv);
            System.out.printf("   OLD: %d rows%n   NEW: %d rows%n", oldRows.size(), newRows.size());

            System.out.println("Normalizing...");
            List<Map<String, String>> normOld = normalizeOld(oldRows);
            List<Map<String, String>> normNew = normalizeNew(newRows);

            EFFECTIVE_PRECISION_MAP.clear();
            EFFECTIVE_PRECISION_MAP.putAll(computeEffectivePrecisions(normOld, normNew, NUMERIC_FIELDS));
            System.out.println("Effective numeric precision (shared):");
            EFFECTIVE_PRECISION_MAP.forEach((k, v) -> System.out.printf("   %-35s -> %d%n", k, v));

            Path normOldPath = outDir.resolve("Squares Validation - Old Normalized.csv");
            Path normNewPath = outDir.resolve("Squares Validation - New Normalized.csv");
            System.out.println("Writing normalized outputs...");
            writeCsv(normOld, normOldPath);
            writeCsv(normNew, normNewPath);
            System.out.println("   -> " + normOldPath);
            System.out.println("   -> " + normNewPath);

            System.out.println("Comparing (status per field)...");
            Path comparisonCsv = outDir.resolve("Squares Validation - Comparison.csv");
            compareStatus(normOld, normNew, outDir);

            System.out.println("Writing detailed numeric diff...");
            Path detailedCsv = outDir.resolve("Squares Validation - Detailed.csv");
            writeDetailed(normOld, normNew, detailedCsv);

            System.out.println("Writing selected overview...");
            Path selectedCsv = outDir.resolve("Squares Validation - Selected Overview.csv");
            writeSelectedOverview(normOld, normNew, selectedCsv);

            // small wait ensures FS flush on some environments
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}

            System.out.println("Optimizing tolerances...");
            Path tolCsv = outDir.resolve("Squares Validation - Tolerance Optimization.csv");
            optimizeTolerances(comparisonCsv, tolCsv);

            System.out.println("All tasks complete.");
            System.out.println("Output directory: " + outDir.toAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------------------- IO ----------------------------

    private static List<Map<String, String>> readCsv(Path p) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String header = br.readLine();
            if (header == null) return rows;
            String[] h = header.split(",", -1);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",", -1);
                Map<String, String> m = new LinkedHashMap<>();
                for (int i = 0; i < h.length; i++) {
                    m.put(h[i].trim(), i < parts.length ? parts[i].trim() : "");
                }
                rows.add(m);
            }
        }
        return rows;
    }

    private static void writeCsv(List<Map<String, String>> rows, Path file) throws IOException {
        if (rows.isEmpty()) return;
        Files.createDirectories(file.getParent());

        boolean isOldFile = file.getFileName().toString().toLowerCase(Locale.ROOT).contains("old");

        try (PrintWriter pw = new PrintWriter(file.toFile())) {
            List<String> header = new ArrayList<>();
            header.add("Recording Name");
            header.addAll(FIELD_MAP.keySet());
            header.add("Selected");
            pw.println(String.join(",", header));

            for (Map<String, String> r : rows) {
                List<String> vals = new ArrayList<>();
                for (String col : header) {
                    String val = r.getOrDefault(col, "");

                    if (NUMERIC_FIELDS.contains(col)) {
                        Double num = parseDouble(val);
                        if (num != null) {
                            if (isOldFile && num == 0.0 && Arrays.asList(
                                    "R Squared",
                                    "Median Short Track Duration",
                                    "Median Long Track Duration",
                                    "Total Displacement",
                                    "Total Track Duration",
                                    "Variability",
                                    "Density",
                                    "Density Ratio"
                            ).contains(col)) {
                                val = "";
                            } else {
                                int prec = EFFECTIVE_PRECISION_MAP.getOrDefault(
                                        col, ROUNDING_MAP.getOrDefault(col, 3));
                                double f = Math.pow(10, prec);
                                double rounded = Math.round(num * f) / f;
                                val = String.format(Locale.US, "%." + prec + "f", rounded);
                            }
                        }
                    }

                    vals.add(escapeCsv(val));
                }
                pw.println(String.join(",", vals));
            }
        }
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    // ---------------------------- Normalization ----------------------------

    public static List<Map<String, String>> normalizeOld(List<Map<String, String>> oldRows) {
        List<Map<String, String>> out = new ArrayList<>();
        for (Map<String, String> r : oldRows) {
            Map<String, String> n = new LinkedHashMap<>();
            String rec = r.getOrDefault("Ext Recording Name", "");
            n.put("Recording Name", rec.replaceAll("-threshold-\\d+$", "").trim());
            for (String f : FIELD_MAP.keySet()) {
                String v = r.getOrDefault(f, "").trim();
                if (f.equals("Tau")) {
                    try { if (!v.isEmpty() && Double.parseDouble(v) < 0) v = ""; } catch (Exception ignored) {}
                }
                n.put(f, v);
            }
            adjustIndex(n, "Row Nr");
            adjustIndex(n, "Col Nr");
            n.put("Selected", String.valueOf(isSelected(n)));
            out.add(n);
        }
        return out;
    }

    public static List<Map<String, String>> normalizeNew(List<Map<String, String>> newRows) {
        List<Map<String, String>> out = new ArrayList<>();
        for (Map<String, String> r : newRows) {
            Map<String, String> n = new LinkedHashMap<>();
            n.put("Recording Name", r.getOrDefault("Recording Name", ""));
            for (Map.Entry<String, String> e : FIELD_MAP.entrySet())
                n.put(e.getKey(), r.getOrDefault(e.getValue(), ""));
            n.put("Selected", String.valueOf(isSelected(n)));
            out.add(n);
        }
        return out;
    }

    private static void adjustIndex(Map<String, String> m, String k) {
        try {
            String v = m.get(k);
            if (v != null && !v.isEmpty()) {
                int i = (int) Double.parseDouble(v);
                m.put(k, String.valueOf(i - 1));
            }
        } catch (Exception ignored) {}
    }

    private static Map<String, Integer> computeEffectivePrecisions(
            List<Map<String, String>> a, List<Map<String, String>> b, Set<String> numeric) {
        Map<String, Integer> res = new HashMap<>();
        for (String f : numeric) {
            int pa = detectPrecision(a, f);
            int pb = detectPrecision(b, f);
            res.put(f, Math.min(pa, pb));
        }
        return res;
    }

    private static int detectPrecision(List<Map<String, String>> rows, String f) {
        int best = 0;
        for (Map<String, String> r : rows) {
            String s = r.get(f);
            if (s != null && s.matches("^-?\\d+\\.\\d+$")) {
                int p = s.length() - s.indexOf('.') - 1;
                best = Math.max(best, p);
            }
        }
        return best;
    }

    // ---------------------------- Comparison (status) ----------------------------

    private static void compareStatus(List<Map<String, String>> oldN, List<Map<String, String>> newN, Path outDir) throws IOException {
        Path out = outDir.resolve("Squares Validation - Comparison.csv");
        List<String[]> diffs = new ArrayList<>();
        int total = 0, diffCount = 0;

        Map<String, Map<String, String>> newMap = new HashMap<>();
        for (Map<String, String> n : newN) newMap.put(key(n), n);

        for (Map<String, String> o : oldN) {
            total++;
            String k = key(o);
            Map<String, String> n = newMap.get(k);
            if (n == null) continue;

            for (String f : FIELD_MAP.keySet()) {
                String ov = o.getOrDefault(f, "");
                String nv = n.getOrDefault(f, "");

                // Treat 0 ↔ "" as equivalent for sparse numeric fields
                if (isOptionalZeroField(f) && (isZeroOrEmpty(ov) || isZeroOrEmpty(nv))) {
                    boolean ovEmpty = isZeroOrEmpty(ov);
                    boolean nvEmpty = isZeroOrEmpty(nv);

                    // If both are effectively empty → no difference, skip entirely
                    if (ovEmpty && nvEmpty) continue;

                    // Otherwise normalize display (avoid showing 0/NaN)
                    if (ovEmpty) ov = "";
                    if (nvEmpty) nv = "";

                    diffs.add(new String[]{
                            o.get("Recording Name"),
                            o.get("Square Nr"),
                            f,
                            ov,
                            nv,
                            "",
                            "",
                            "MISSING"
                    });
                    continue;
                }

                if (NUMERIC_FIELDS.contains(f)) {
                    Double da = parseDouble(ov);
                    Double db = parseDouble(nv);

                    // If both empty or zero-equivalent → skip
                    if ((da == null || isZeroOrEmpty(ov)) && (db == null || isZeroOrEmpty(nv))) {
                        continue;
                    }

                    // If one empty and one not → mark missing
                    if ((da == null || isZeroOrEmpty(ov)) || (db == null || isZeroOrEmpty(nv))) {
                        if (isZeroOrEmpty(ov)) ov = "";
                        if (isZeroOrEmpty(nv)) nv = "";
                        diffs.add(new String[]{
                                o.get("Recording Name"),
                                o.get("Square Nr"),
                                f,
                                ov,
                                nv,
                                "",
                                "",
                                "MISSING"
                        });
                        continue;
                    }

                    // Both numeric → compute deviation
                    double dev = relativeDeviation(ov, nv);
                    double tol = TOLERANCE_MAP.getOrDefault(f, 5.0);
                    int prec = EFFECTIVE_PRECISION_MAP.getOrDefault(f, ROUNDING_MAP.getOrDefault(f, 3));

                    String status;
                    if (Double.isNaN(dev)) {
                        continue; // skip NaN
                    } else if (Math.abs(da - db) < 1e-12) {
                        status = "EQUAL";
                    } else if (dev <= tol) {
                        status = "WITHIN " + tol + "%";
                    } else {
                        status = "DIFFERENT";
                        diffCount++;
                    }

                    diffs.add(new String[]{
                            o.get("Recording Name"),
                            o.get("Square Nr"),
                            f,
                            ov,
                            nv,
                            String.valueOf(prec),
                            String.format(Locale.US, "%.3f", dev),
                            status
                    });
                }
            }

            // Compare "Selected"
            String sOld = o.get("Selected");
            String sNew = n.get("Selected");
            if (!Objects.equals(sOld, sNew)) {
                diffs.add(new String[]{
                        o.get("Recording Name"),
                        o.get("Square Nr"),
                        "Selected",
                        sOld,
                        sNew,
                        "",
                        "",
                        "DIFFERENT"
                });
                diffCount++;
            }

            if (total % 1000 == 0) System.out.printf("   ...processed %,d%n", total);
        }

        try (PrintWriter pw = new PrintWriter(out.toFile())) {
            pw.println("Recording Name,Square Nr,Field,Old Value,New Value,Precision Used,Relative Diff (%),Status");
            for (String[] r : diffs) pw.println(String.join(",", r));
            pw.println();
            pw.printf("SUMMARY,,,,,,,%nDifferences,%d%n", diffCount);
        }
        System.out.printf("✅ Compared %,d squares — %d differences%n", total, diffCount);
    }

    // ---------------------------- Detailed numeric diff ----------------------------

    private static void writeDetailed(List<Map<String, String>> oldN, List<Map<String, String>> newN, Path outFile) throws IOException {
        Map<String, Map<String, String>> newMap = new HashMap<>();
        for (Map<String, String> n : newN) newMap.put(key(n), n);

        List<List<String>> rows = new ArrayList<>();
        int matched = 0, total = oldN.size();

        for (Map<String, String> o : oldN) {
            Map<String, String> n = newMap.get(key(o));
            if (n == null) continue;
            matched++;

            List<String> line = new ArrayList<>();
            line.add(o.getOrDefault("Recording Name", ""));
            line.add(n.getOrDefault("Recording Name", ""));

            for (String field : NUMERIC_FIELDS_LIST) {
                String ov = o.getOrDefault(field, "");
                String nv = n.getOrDefault(field, "");

                double oldNum = toDouble(ov);
                double newNum = toDouble(nv);
                double diff = newNum - oldNum;
                double diffPer = (oldNum == 0.0) ? 0.0 : (diff / oldNum) * 100.0;

                line.add(ov);
                line.add(nv);
                line.add(format4(diff));
                line.add(format1(diffPer)); // one decimal for percentages
                line.add(""); // separator
            }
            rows.add(line);
        }

        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outFile))) {
            List<String> h = new ArrayList<>();
            h.add("Recording Name");
            h.add("Recording Name Java");
            for (String f : NUMERIC_FIELDS_LIST) {
                h.add(f);
                h.add(f + " Java");
                h.add(f + " Diff");
                h.add(f + " DiffPer");
                h.add(""); // separator
            }
            pw.println(String.join(",", h));
            for (List<String> r : rows) pw.println(String.join(",", r));
        }

        System.out.printf("Detailed numeric rows written: %d (matched from %d)%n", matched, total);
        System.out.println("Written: " + outFile.toAbsolutePath());
    }

    // ---------------------------- Selected Overview ----------------------------

    private static void writeSelectedOverview(List<Map<String, String>> oldN, List<Map<String, String>> newN, Path outFile) throws IOException {
        Map<String, Map<String, String>> newMap = new HashMap<>();
        for (Map<String, String> n : newN) newMap.put(key(n), n);

        Set<String> allKeys = new TreeSet<>();
        for (Map<String, String> o : oldN) allKeys.add(key(o));
        for (Map<String, String> n : newN) allKeys.add(key(n));

        try (PrintWriter pw = new PrintWriter(outFile.toFile())) {
            pw.println("Recording Name,Square Nr,Selected(Old),Selected(New),Both,Only Old,Only New,Tau(Old),Tau(New),DensityRatio(Old),DensityRatio(New),Variability(Old),Variability(New)");
            for (String k : allKeys) {
                String[] parts = k.split(" - ", 2);
                String rec = parts[0], sq = parts.length > 1 ? parts[1] : "";
                Map<String, String> o = oldN.stream().filter(r -> key(r).equals(k)).findFirst().orElse(null);
                Map<String, String> n = newMap.get(k);
                boolean so = o != null && isSelected(o);
                boolean sn = n != null && isSelected(n);
                pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                          rec, sq, so, sn,
                          (so && sn) ? "true" : "", (so && !sn) ? "true" : "", (!so && sn) ? "true" : "",
                          val(o, "Tau"), val(n, "Tau"), val(o, "Density Ratio"), val(n, "Density Ratio"), val(o, "Variability"), val(n, "Variability"));
            }
        }
        System.out.println("Selected overview written: " + outFile.toAbsolutePath());
    }

    private static String val(Map<String, String> m, String k) { return m == null ? "" : m.getOrDefault(k, ""); }

    // ---------------------------- Tolerance Optimization ----------------------------

    private static void optimizeTolerances(Path comparisonCsv, Path outCsv) throws IOException {
        if (!Files.exists(comparisonCsv)) {
            System.out.println("No comparison file found for tolerance optimization.");
            return;
        }

        Map<String, List<Double>> diffsByField = new LinkedHashMap<>();

        try (BufferedReader br = Files.newBufferedReader(comparisonCsv, java.nio.charset.StandardCharsets.UTF_8)) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("SUMMARY")) continue;
                String[] parts = line.split(",", -1);
                if (parts.length < 7) continue;
                String field = parts[2].trim();
                String rel = parts[6].trim();
                if (rel.isEmpty() || rel.equalsIgnoreCase("NaN")) continue;
                try {
                    double d = Double.parseDouble(rel);
                    diffsByField.computeIfAbsent(field, k -> new ArrayList<>()).add(d);
                } catch (Exception ignored) {}
            }
        }

        if (diffsByField.isEmpty()) {
            System.out.println("No numeric deviations detected — skipping optimization.");
            return;
        }

        double[] testLevels = {5.0, 4.0, 3.0, 2.0, 1.0, 0.5};
        double targetKeep = 98.0;

        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outCsv, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("Field,Total,Equal@5%,OptimalTolerance(%),Equal@Optimal(%)");

            for (Map.Entry<String, List<Double>> e : diffsByField.entrySet()) {
                String field = e.getKey();
                List<Double> diffs = e.getValue();
                diffs.removeIf(d -> !Double.isFinite(d));
                if (diffs.isEmpty()) continue;

                int total = diffs.size();
                double within5 = percentWithin(diffs, 5.0);
                double bestTol = 5.0, bestKeep = within5;

                for (double tol : testLevels) {
                    double keep = percentWithin(diffs, tol);
                    if (keep < targetKeep) break;
                    bestTol = tol;
                    bestKeep = keep;
                }

                pw.printf(Locale.US, "%s,%d,%.2f,%.2f,%.2f%n", field, total, within5, bestTol, bestKeep);
            }
        }

        System.out.println("Tolerance optimization summary written: " + outCsv.toAbsolutePath());
    }

    private static double percentWithin(List<Double> vals, double tol) {
        long ok = vals.stream().filter(v -> Math.abs(v) <= tol).count();
        return 100.0 * ok / vals.size();
    }

    // ---------------------------- Helpers ----------------------------

    private static String key(Map<String, String> r) {
        return r.getOrDefault("Recording Name", "") + " - " + r.getOrDefault("Square Nr", "");
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isEmpty() || s.equalsIgnoreCase("NaN")) return null;
        try { double v = Double.parseDouble(s); return Double.isNaN(v) ? null : v; }
        catch (Exception e) { return null; }
    }

    private static double toDouble(String s) { Double d = parseDouble(s); return d == null ? 0.0 : d; }

    private static String format4(double v) { return String.format(Locale.US, "%.4f", v); }

    private static String format1(double v) { return String.format(Locale.US, "%.1f", v); }

    private static double relativeDeviation(String oldVal, String newVal) {
        Double oldNum = parseDouble(oldVal);
        Double newNum = parseDouble(newVal);
        if (oldNum == null || newNum == null) return Double.NaN;
        if (oldNum == 0.0 && newNum == 0.0) return 0.0;
        if (oldNum == 0.0) return Double.POSITIVE_INFINITY;
        return Math.abs((newNum - oldNum) / oldNum) * 100.0;
    }
    private static boolean isSelected(Map<String, String> r) {
        Double dr = parseDouble(r.get("Density Ratio"));
        Double var = parseDouble(r.get("Variability"));
        Double r2  = parseDouble(r.get("R Squared"));
        return dr != null && var != null && r2 != null && dr >= 2.0 && var < 10.0 && r2 > 0.1;
    }

    private static boolean isOptionalZeroField(String f) {
        return Arrays.asList(
                "Variability",
                "Density",
                "Density Ratio",
                "R Squared",
                "Median Short Track Duration",
                "Median Long Track Duration",
                "Total Displacement",
                "Total Track Duration",
                "Tau"
        ).contains(f);
    }

    private static boolean isZeroOrEmpty(String s) {
        if (s == null || s.trim().isEmpty()) return true;
        try { return Double.parseDouble(s.trim()) == 0.0; }
        catch (Exception e) { return false; }
    }
}