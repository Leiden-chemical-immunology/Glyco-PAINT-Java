package tools;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Compare two "All Squares" CSV files (old vs new/Java).
 *
 * Mapping order per pair: (oldColName, newColName)
 *   - oldColName -> column name in the old "All Squares.csv"
 *   - newColName -> column name in the new "All Squares Java.csv"
 *
 * Output columns:
 *   For numeric columns:
 *     <oldName>, <oldName> Java, <oldName> Diff, <oldName> DiffPer, [blank]
 *   For text columns:
 *     <oldName>, <oldName> Java
 *
 * Diff    = old - new
 * DiffPer = 100 * Diff / old   (blank if old == 0 or not numeric)
 */
public class CompareAllSquares {

    public static void main(String[] args) {
        // --- Adjust these paths for your system ---
        Path oldCsv = Paths.get("/Users/hans/Paint/Paint Data - v39/Regular Probes/Paint Regular Probes - 20 Squares/221012/All Squares.csv");
        Path newCsv = Paths.get("/Users/hans/Paint Test Project/221012/All Squares Java.csv");
        Path outCsv = Paths.get("/Users/hans/Desktop/AllSquaresComparison.csv");

        try {
            List<Mapping> mappings = getMappings(); // (oldCol, newCol)
            List<Map<String, String>> oldRows = readCsv(oldCsv);
            List<Map<String, String>> newRows = readCsv(newCsv);

            if (oldRows.isEmpty() || newRows.isEmpty()) {
                System.err.println("❌ One of the CSV files is empty.");
                return;
            }

            int n = Math.min(oldRows.size(), newRows.size());
            System.out.printf("🔍 Comparing %,d rows with %,d mapped columns%n", n, mappings.size());

            // ---- detect numeric columns ----
            Map<String, Boolean> isNumeric = new LinkedHashMap<>();
            Map<String, String> oldSample = oldRows.get(0);
            Map<String, String> newSample = newRows.get(0);

            for (Mapping m : mappings) {
                Double dOld = tryParseDouble(oldSample.getOrDefault(m.oldCol, ""));
                Double dNew = tryParseDouble(newSample.getOrDefault(m.newCol, ""));
                boolean numeric = (dOld != null || dNew != null);
                isNumeric.put(m.oldCol, numeric);
            }

            // ---- build headers ----
            List<String> headers = new ArrayList<>();
            for (Mapping m : mappings) {
                String base = m.oldCol;
                headers.add(base);
                headers.add(base + " Java");
                if (isNumeric.get(m.oldCol)) {
                    headers.add(base + " Diff");
                    headers.add(base + " DiffPer");
                    headers.add(""); // spacer
                }
            }

            try (BufferedWriter bw = Files.newBufferedWriter(outCsv)) {
                bw.write(String.join(",", headers));
                bw.newLine();

                for (int i = 0; i < n; i++) {
                    Map<String, String> oldRow = oldRows.get(i);
                    Map<String, String> newRow = newRows.get(i);
                    List<String> row = new ArrayList<>();

                    for (Mapping m : mappings) {
                        String sOld = oldRow.getOrDefault(m.oldCol, "");
                        String sNew = newRow.getOrDefault(m.newCol, "");

                        Double dOld = tryParseDouble(sOld);
                        Double dNew = tryParseDouble(sNew);

                        // --- Non-numeric → only values ---
                        if (!isNumeric.get(m.oldCol)) {
                            row.add(escapeCsv(sOld));
                            row.add(escapeCsv(sNew));
                            continue;
                        }

                        String diff = "";
                        String rel = "";

                        // --- Tau-specific rule ---
                        if (m.oldCol.equalsIgnoreCase("Tau")) {
                            if (dOld == null || dNew == null || dOld < 0 || dNew < 0) {
                                row.add("");
                                row.add(escapeCsv(sNew));
                                row.add("");
                                row.add("");
                                row.add("");
                                continue;
                            }
                        }

                        // --- R Squared-specific rule ---
                        if (m.oldCol.equalsIgnoreCase("R Squared")) {
                            if (dOld == null || dNew == null || dOld == 0) {
                                row.add("");
                                row.add(escapeCsv(sNew));
                                row.add("");
                                row.add("");
                                row.add("");
                                continue;
                            }
                        }

                        // =============================
                        // Other specific rule
                        // =============================
                        if ((m.oldCol.equalsIgnoreCase("Total Displacement") ||
                                m.newCol.equalsIgnoreCase("Total Track Duration") ||
                                m.newCol.equalsIgnoreCase("Median Short Track Duration") ||
                                m.newCol.equalsIgnoreCase("Median Long Track Duration") ||
                                m.newCol.equalsIgnoreCase("Density Ratio") ||
                                m.newCol.equalsIgnoreCase("Density") ||
                                m.newCol.equalsIgnoreCase("Variability") ||
                                m.newCol.equalsIgnoreCase("Nr Tracks") ))  {
                            if (dOld == null || dNew == null || dOld == 0) {
                                row.add("");
                                row.add(escapeCsv(sNew));
                                row.add(""); // diff
                                row.add(""); // rel
                                row.add(""); // spacer
                                continue;    // skip to next column
                            }
                        }

                        // --- Normal numeric rule ---
                        if (dOld != null && dNew != null) {
                            double absDiff = dOld - dNew;
                            diff = format(absDiff, 6);

                            if (dOld != 0.0) {
                                double relDiff = (absDiff / dOld) * 100.0;
                                rel = format(relDiff, 3);
                            }
                        }

                        row.add(escapeCsv(sOld)); // old
                        row.add(escapeCsv(sNew)); // new
                        row.add(diff);
                        row.add(rel);
                        row.add(""); // spacer
                    }

                    bw.write(String.join(",", row));
                    bw.newLine();
                }
            }

            System.out.println("✅ Comparison complete.");
            System.out.println("📄 Output written to: " + outCsv.toAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Define mappings: oldCsv (All Squares) → newCsv (All Squares Java) */
    private static List<Mapping> getMappings() {
        List<Mapping> list = new ArrayList<>();

        list.add(new Mapping("Ext Recording Name",              "Recording Name"));
        list.add(new Mapping("Square Nr",                       "Square Number"));
        list.add(new Mapping("Nr Tracks",                       "Number of Tracks"));
        list.add(new Mapping("Tau",                             "Tau"));
        list.add(new Mapping("R Squared",                       "R Squared"));
        list.add(new Mapping("Median Track Duration",           "Median Track Duration"));
        list.add(new Mapping("Total Track Duration",            "Total Track Duration"));
        list.add(new Mapping("Median Displacement",             "Median Displacement"));
        list.add(new Mapping("Density Ratio",                   "Density Ratio"));
        list.add(new Mapping("Variability",                     "Variability"));
        list.add(new Mapping("Density",                         "Density"));
        list.add(new Mapping("Median Max Speed",                "Median Max Speed"));
        list.add(new Mapping("Max Track Duration",              "Max Track Duration"));
        list.add(new Mapping("Total Displacement",              "Total Displacement"));
        list.add(new Mapping("Max Displacement",                "Max Displacement"));
        list.add(new Mapping("Median Diffusion Coefficient",    "Median Diffusion Coefficient"));
        list.add(new Mapping("Median Diffusion Coefficient Ext","Median Diffusion Coefficient Ext"));
        list.add(new Mapping("Median Long Track Duration",      "Median Long Track Duration"));
        list.add(new Mapping("Median Short Track Duration",     "Median Short Track Duration"));

        return list;
    }

    // =================== helpers ===================

    private static class Mapping {
        final String oldCol;  // column in old All Squares.csv
        final String newCol;  // column in new All Squares Java.csv

        Mapping(String oldCol, String newCol) {
            this.oldCol = oldCol;
            this.newCol = newCol;
        }
    }

    private static List<Map<String, String>> readCsv(Path path) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String headerLine = br.readLine();
            if (headerLine == null) return rows;
            String[] headers = headerLine.split(",", -1);

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                Map<String, String> map = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    String key = headers[i].trim();
                    String val = (i < parts.length) ? parts[i].trim() : "";
                    map.put(key, val);
                }
                rows.add(map);
            }
        }
        return rows;
    }

    private static Double tryParseDouble(String s) {
        try {
            if (s == null || s.isEmpty()) return null;
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String format(double v, int p) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "";
        return String.format(Locale.US, "%." + p + "f", v);
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}