package validation.old;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AllSquaresDetailedComparator {

    private static final List<String> NUMERIC_FIELDS = Arrays.asList(
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

    public static void main(String[] args) throws IOException {
        Path outDir = Paths.get(System.getProperty("user.home"), "Downloads", "Validate", "Squares");
        Files.createDirectories(outDir);

        Path oldCsv = Paths.get("/Users/hans/Paint Test Project/221012 - Python/All Squares.csv");
        Path newCsv = Paths.get("/Users/hans/Paint Test Project/221012/Squares.csv");
        Path outCsv = outDir.resolve("AllSquaresComparison.csv");

        System.out.println("🔍 Reading CSVs...");
        List<Map<String, String>> oldRows = readCsv(oldCsv);
        List<Map<String, String>> newRows = readCsv(newCsv);
        System.out.printf("   OLD: %d rows%n   NEW: %d rows%n", oldRows.size(), newRows.size());

        // 🧩 Normalize using existing logic from SquaresCsvComparator
        System.out.println("🧩 Normalizing via SquaresCsvComparator...");
        List<Map<String, String>> normOld = SquaresCsvComparator.normalizeOld(oldRows);
        List<Map<String, String>> normNew = SquaresCsvComparator.normalizeNew(newRows);

        System.out.println("\n🧮 Matching and comparing...");
        List<String[]> result = compare(normOld, normNew);

        System.out.println("💾 Writing output...");
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outCsv))) {
            pw.println(String.join(",", header()));
            for (String[] r : result) pw.println(String.join(",", r));
        }

        System.out.printf("✅ Compared %d matched squares — %d rows written%n", result.size(), result.size());
        System.out.println("📄 Output: " + outCsv.toAbsolutePath());
    }

    // ---------------------------------------------------------------------
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

    // ---------------------------------------------------------------------
    private static List<String[]> compare(List<Map<String, String>> oldRows, List<Map<String, String>> newRows) {
        Map<String, Map<String, String>> newMap = new HashMap<>();
        for (Map<String, String> n : newRows) newMap.put(key(n), n);

        List<String[]> out = new ArrayList<>();
        int missing = 0;
        int showFirst = 10;

        for (Map<String, String> o : oldRows) {
            String k = key(o);
            Map<String, String> n = newMap.get(k);
            if (n == null) {
                if (missing < showFirst) System.out.printf("⚠️ Missing key %s%n", k);
                missing++;
                continue;
            }

            List<String> row = new ArrayList<>();
            row.add(o.getOrDefault("Recording Name", ""));
            row.add(n.getOrDefault("Recording Name", ""));

            for (String field : NUMERIC_FIELDS) {
                String oldVal = o.getOrDefault(field, "");
                String newVal = n.getOrDefault(field, "");

                double oldNum = parseDouble(oldVal);
                double newNum = parseDouble(newVal);
                double diff = newNum - oldNum;
                double diffPer = (oldNum == 0) ? 0 : (diff / oldNum) * 100.0;

                row.add(oldVal);
                row.add(newVal);
                row.add(format(diff));
                row.add(formatPercent(diffPer));  // 🔹 use one-decimal formatting
                row.add(""); // separator column
            }

            out.add(row.toArray(new String[0]));
        }

        if (missing > 0)
            System.out.printf("🔎 Keys not found in NEW: %,d (printed first %d above)%n", missing, Math.min(missing, 10));

        System.out.printf("   ✅ Matched %,d of %,d old rows%n", out.size(), oldRows.size());
        return out;
    }

    // ---------------------------------------------------------------------
    private static String key(Map<String, String> r) {
        return r.getOrDefault("Recording Name", "") + "||" + r.getOrDefault("Square Nr", "");
    }

    private static double parseDouble(String s) {
        if (s == null || s.isEmpty()) return 0.0;
        try { return Double.parseDouble(s); }
        catch (Exception e) { return 0.0; }
    }

    private static String format(double v) {
        if (Double.isNaN(v)) return "";
        return String.format(Locale.US, "%.4f", v);
    }

    private static String formatPercent(double v) {
        if (Double.isNaN(v)) return "";
        return String.format(Locale.US, "%.1f", v); // 🔹 one decimal for percentages
    }

    private static List<String> header() {
        List<String> h = new ArrayList<>();
        h.add("Recording Name");
        h.add("Recording Name Java");
        for (String f : NUMERIC_FIELDS) {
            h.add(f);
            h.add(f + " Java");
            h.add(f + " Diff");
            h.add(f + " DiffPer");
            h.add(""); // empty separator column
        }
        return h;
    }
}