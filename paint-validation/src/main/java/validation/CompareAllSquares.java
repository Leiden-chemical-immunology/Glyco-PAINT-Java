package validation;

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
 *   For numeric columns (forced list below):
 *     <oldName>, <oldName> Java, <oldName> Diff, <oldName> DiffPer, [blank]
 *   For text columns:
 *     <oldName>, <oldName> Java
 *
 * Diff    = old - new
 * DiffPer = 100 * Diff / old   (blank if old == 0 or not numeric)
 * DiffPer is formatted with exactly one decimal digit (e.g. 1.0, -2.0, 0.0)
 */
public class CompareAllSquares {

    public static void main(String[] args) {
        // --- Adjust these paths for your system ---
        // Path oldCsv = Paths.get("/Users/hans/Paint/Paint Data - v39/Regular Probes/Paint Regular Probes - 20 Squares/221012/All Squares.csv");
        Path oldCsv = Paths.get("/Users/hans/Paint Test Project/221012 Pythn/All Squares.csv");
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

            // ---- Build headers, using forced numeric list ----
            Set<String> FORCE_NUMERIC = getForcedNumericOldColumnNames();

            List<String> headers = new ArrayList<>();
            for (Mapping m : mappings) {
                String base = m.oldCol;
                boolean numeric = FORCE_NUMERIC.contains(base);

                headers.add(base);
                headers.add(base + " Java");
                if (numeric) {
                    headers.add(base + " Diff");
                    headers.add(base + " DiffPer");
                    headers.add(""); // spacer
                }
            }

            try (BufferedWriter bw = Files.newBufferedWriter(outCsv)) {
                bw.write(String.join(",", headers));
                bw.newLine();

                int n = Math.min(oldRows.size(), newRows.size());
                System.out.printf("🔍 Comparing %,d rows with %,d mapped columns%n", n, mappings.size());

                for (int i = 0; i < n; i++) {
                    Map<String, String> oldRow = oldRows.get(i);
                    Map<String, String> newRow = newRows.get(i);
                    List<String> row = new ArrayList<>();

                    for (Mapping m : mappings) {
                        String sOld = oldRow.getOrDefault(m.oldCol, "");
                        String sNew = newRow.getOrDefault(m.newCol, "");
                        boolean numeric = FORCE_NUMERIC.contains(m.oldCol);

                        if (!numeric) {
                            // Text-only
                            row.add(escapeCsv(sOld));
                            row.add(escapeCsv(sNew));
                            continue;
                        }

                        Double dOld = tryParseDouble(sOld);
                        Double dNew = tryParseDouble(sNew);

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

                        // --- Misc specific rules ---
                        if ((m.oldCol.equalsIgnoreCase("Total Displacement") ||
                                m.newCol.equalsIgnoreCase("Total Track Duration") ||
                                m.newCol.equalsIgnoreCase("Median Short Track Duration") ||
                                m.newCol.equalsIgnoreCase("Median Long Track Duration") ||
                                m.newCol.equalsIgnoreCase("Density Ratio") ||
                                m.newCol.equalsIgnoreCase("Max Track Duration") ||
                                m.newCol.equalsIgnoreCase("Density") ||
                                m.newCol.equalsIgnoreCase("Variability") ||
                                m.newCol.equalsIgnoreCase("Nr Tracks"))) {
                            if (dOld == null || dNew == null || dOld == 0) {
                                row.add("");
                                row.add(escapeCsv(sNew));
                                row.add("");
                                row.add("");
                                row.add("");
                                continue;
                            }
                        }

                        if (dOld != null && dNew != null) {
                            int prec = precisionFor(m.oldCol);
                            double dOldRounded = round(dOld, prec);
                            double dNewRounded = round(dNew, prec);

                            double absDiff = dOldRounded - dNewRounded;
                            diff = format(absDiff, prec);
                            if (dOldRounded != 0.0) {
                                double relDiff = (absDiff / dOldRounded) * 100.0;
                                rel = formatOneDecimal(relDiff);
                            }
                        }

                        row.add(escapeCsv(sOld));
                        row.add(escapeCsv(sNew));
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
        list.add(new Mapping("Density Ratio",                   "Density Ratio Ori"));
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

    /**
     * Force these old-column names to be treated as numeric (so Diff/DiffPer are always emitted),
     * even if the first row is blank.
     */
    private static Set<String> getForcedNumericOldColumnNames() {
        return new LinkedHashSet<>(Arrays.asList(
                "Square Nr",
                "Nr Tracks",
                "Tau",
                "R Squared",
                "Median Track Duration",
                "Total Track Duration",
                "Median Displacement",
                "Density Ratio",
                "Density Ratio Ori",
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
        ));
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

    /** Always one decimal place (e.g. 0.0, 2.5, -12.0). */
    private static String formatOneDecimal(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "";
        return String.format(Locale.US, "%.1f", v);
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static double round(double value, int decimals) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return value;
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    /** Returns number of decimals to round for a given old column name. */
    private static int precisionFor(String col) {
        col = col.toLowerCase(Locale.ROOT);
        if (col.contains("tau"))
            return 0;
        if (col.contains("ratio") || col.contains("density"))
            return 3;
        if (col.contains("speed") || col.contains("displacement") || col.contains("duration") || col.contains("variability"))
            return 2;
        if (col.contains("squared"))
            return 2;
        return 1; // default
    }
}