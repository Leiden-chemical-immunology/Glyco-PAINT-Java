package paint.validation.old;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static paint.shared.constants.PaintConstants.SQUARES_CSV;

/**
 * ============================================================================
 *  SquaresSidebySideComparator.java
 *  Part of the Paint Regression module.
 *
 *  <p><b>Purpose:</b><br>
 *  Compares legacy (Python) and new (Java) Squares CSVs side by side,
 *  aligning matching rows and applying tolerance-based field comparison.
 *  Handles numeric rounding, missing-value normalization, and per-field
 *  difference reporting.
 *  </p>
 *
 *  <p><b>Key Rules:</b></p>
 *  <ul>
 *    <li>Removes trailing "-threshold-N" from Recording Name in the old file.</li>
 *    <li>Sets Tau &lt; 0 in old file to empty.</li>
 *    <li>A square is selected if all the following are true:</li>
 *    <li>&nbsp;&nbsp;&bull;&nbsp;Density Ratio &gt;= 2</li>
 *    <li>&nbsp;&nbsp;&bull;&nbsp;Variability &lt; 10</li>
 *    <li>&nbsp;&nbsp;&bull;&nbsp;R&nbsp;Squared &gt; 0.1</li>
 *  </ul>
 *
 *  <p>Generates several diagnostic CSVs in the validation output directory.</p>
 *
 *  <p><b>Author:</b> Hans&nbsp;Bakker<br>
 *  <b>Module:</b> paint-regression</p>
 * ============================================================================
 */
public class SquaresSidebySideComparator {

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
        // "Selected" is computed; not part of FIELD_MAP on purpose
    }

    private static final Set<String> DOUBLE_FIELDS = new HashSet<>(Arrays.asList(
            "X0", "Y0", "X1", "Y1",
            "Variability", "Density", "Density Ratio",
            "Tau", "R Squared",
            "Median Diffusion Coefficient", "Median Diffusion Coefficient Ext",
            "Median Long Track Duration", "Median Short Track Duration",
            "Median Displacement", "Max Displacement", "Total Displacement",
            "Median Max Speed", "Max Max Speed",
            "Max Track Duration", "Total Track Duration", "Median Track Duration"
    ));

    private static final Map<String, Integer> ROUNDING_MAP = new HashMap<>();
    static {
        ROUNDING_MAP.put("X0", 4);
        ROUNDING_MAP.put("Y0", 4);
        ROUNDING_MAP.put("X1", 4);
        ROUNDING_MAP.put("Y1", 4);
        ROUNDING_MAP.put("Variability", 1);
        ROUNDING_MAP.put("Density", 3);
        ROUNDING_MAP.put("Density Ratio", 2);
        ROUNDING_MAP.put("Tau", 2);
        ROUNDING_MAP.put("R Squared", 2);
        ROUNDING_MAP.put("Median Diffusion Coefficient", 1);
        ROUNDING_MAP.put("Median Diffusion Coefficient Ext", 1);
        ROUNDING_MAP.put("Median Long Track Duration", 0);
        ROUNDING_MAP.put("Median Short Track Duration", 0);
        ROUNDING_MAP.put("Median Displacement", 0);
        ROUNDING_MAP.put("Max Displacement", 0);
        ROUNDING_MAP.put("Total Displacement", 0);
        ROUNDING_MAP.put("Median Max Speed", 0);
        ROUNDING_MAP.put("Max Max Speed", 0);
        ROUNDING_MAP.put("Max Track Duration", 0);
        ROUNDING_MAP.put("Total Track Duration", 0);
        ROUNDING_MAP.put("Median Track Duration", 0);
    }

    private static final Map<String, Integer> EFFECTIVE_PRECISION_MAP = new HashMap<>();

    // ----------------------------------------------------------------------
    private static List<Map<String, String>> readCsv(Path path) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return rows;
            String[] headers = headerLine.split(",", -1);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",", -1);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                    row.put(headers[i].trim(), values[i].trim());
                }
                rows.add(row);
            }
        }
        return rows;
    }

    // ----------------------------------------------------------------------
    private static List<Map<String, String>> normalizeOld(List<Map<String, String>> oldRows) {
        List<Map<String, String>> norm = new ArrayList<>();
        for (Map<String, String> row : oldRows) {
            Map<String, String> n = new LinkedHashMap<>();
            n.put("Recording Name", row.getOrDefault("Ext Recording Name", ""));

            // Remove trailing "-threshold-<number>"
            String recName = n.get("Recording Name");
            if (recName != null) {
                recName = recName.replaceAll("-threshold-\\d+$", "").trim();
                n.put("Recording Name", recName);
            }

            for (String oldCol : FIELD_MAP.keySet()) {
                String val = row.getOrDefault(oldCol, "").trim();

                // Tau normalization
                if (oldCol.equalsIgnoreCase("Tau")) {
                    try {
                        if (!val.isEmpty() && Double.parseDouble(val) < 0)
                            val = "";
                    } catch (NumberFormatException ignored) {}
                }

                // 0-to-empty normalization
                if (oldCol.equalsIgnoreCase("R Squared")
                        || oldCol.equalsIgnoreCase("Median Long Track Duration")
                        || oldCol.equalsIgnoreCase("Median Short Track Duration")
                        || oldCol.equalsIgnoreCase("Total Track Duration")
                        || oldCol.equalsIgnoreCase("Density")
                        || oldCol.equalsIgnoreCase("Density Ratio")
                        || oldCol.equalsIgnoreCase("Variability")
                        || oldCol.equalsIgnoreCase("Total Displacement")) {
                    try {
                        if (!val.isEmpty() && Double.parseDouble(val) == 0.0)
                            val = "";
                    } catch (NumberFormatException ignored) {}
                }

                n.put(oldCol, val);
            }

            // Normalize Row/Col to 0-based
            try {
                String rowVal = n.get("Row Nr");
                if (rowVal != null && !rowVal.isEmpty()) {
                    int rowNum = (int) Double.parseDouble(rowVal);
                    n.put("Row Nr", String.valueOf(rowNum - 1));
                }
            } catch (NumberFormatException ignored) {}
            try {
                String colVal = n.get("Col Nr");
                if (colVal != null && !colVal.isEmpty()) {
                    int colNum = (int) Double.parseDouble(colVal);
                    n.put("Col Nr", String.valueOf(colNum - 1));
                }
            } catch (NumberFormatException ignored) {}

            // Compute Selected
            n.put("Selected", String.valueOf(isSelected(n)));

            norm.add(n);
        }
        return norm;
    }

    private static List<Map<String, String>> normalizeNew(List<Map<String, String>> newRows) {
        List<Map<String, String>> norm = new ArrayList<>();
        for (Map<String, String> row : newRows) {
            Map<String, String> n = new LinkedHashMap<>();
            n.put("Recording Name", row.getOrDefault("Recording Name", ""));
            for (Map.Entry<String, String> entry : FIELD_MAP.entrySet()) {
                String oldCol = entry.getKey();
                String newCol = entry.getValue();
                n.put(oldCol, row.getOrDefault(newCol, ""));
            }
            // NEW is already 0-based; nothing to adjust for row/col
            // Compute Selected
            n.put("Selected", String.valueOf(isSelected(n)));

            norm.add(n);
        }
        return norm;
    }

    // ----------------------------------------------------------------------
    private static Map<String, Integer> computeEffectivePrecisions(
            List<Map<String, String>> oldRows,
            List<Map<String, String>> newRows,
            Set<String> numericFields) {

        Map<String, Integer> precisionMap = new HashMap<>();
        for (String field : numericFields) {
            int oldPrec = detectPrecision(oldRows, field);
            int newPrec = detectPrecision(newRows, field);
            precisionMap.put(field, Math.min(oldPrec, newPrec));
        }
        return precisionMap;
    }

    private static int detectPrecision(List<Map<String, String>> rows, String field) {
        int best = 0;
        for (Map<String, String> row : rows) {
            String s = row.get(field);
            if (s != null) {
                s = s.trim();
                if (s.matches("^-?\\d+\\.\\d+$")) {
                    int p = s.length() - s.indexOf('.') - 1;
                    if (p > best) best = p;
                }
            }
        }
        return best;
    }

    // ----------------------------------------------------------------------
    private static String compositeKey(Map<String, String> row) {
        return row.getOrDefault("Recording Name", "").trim() + " - " +
                row.getOrDefault("Square Nr", "").trim();
    }

    private static void writeCsv(List<Map<String, String>> rows, Path file) throws IOException {
        if (rows.isEmpty()) return;
        Files.createDirectories(file.getParent());
        try (PrintWriter pw = new PrintWriter(file.toFile())) {
            // Ensure "Selected" exists and appears last in header (after FIELD_MAP)
            List<String> header = new ArrayList<>();
            header.add("Recording Name");
            header.addAll(FIELD_MAP.keySet());
            if (!header.contains("Selected")) header.add("Selected");
            pw.println(String.join(",", header));

            for (Map<String, String> r : rows) {
                List<String> vals = new ArrayList<>();
                for (String c : header) {
                    String val = r.get(c);
                    if (val == null) val = "";
                    String out = val;

                    if (DOUBLE_FIELDS.contains(c)) {
                        Double num = parseDouble(val);
                        if (num != null) {
                            int digits = EFFECTIVE_PRECISION_MAP.getOrDefault(
                                    c, ROUNDING_MAP.getOrDefault(c, 3));
                            double factor = Math.pow(10, digits);
                            double rounded = Math.round(num * factor) / factor;
                            out = String.format(Locale.US, "%." + digits + "f", rounded);
                        }
                    }
                    vals.add(escapeCsv(out));
                }
                pw.println(String.join(",", vals));
            }
        }
    }

    // ----------------------------------------------------------------------
    private static boolean compareDoubles(String field, String a, String b) {
        String ta = (a == null) ? "" : a.trim();
        String tb = (b == null) ? "" : b.trim();

        boolean aSpecial = isZeroNaNOrEmpty(ta);
        boolean bSpecial = isZeroNaNOrEmpty(tb);
        if (aSpecial && bSpecial) return true;

        if (field.equalsIgnoreCase("Tau")) {
            try {
                if (!ta.isEmpty() && Double.parseDouble(ta) < 0 && isZeroNaNOrEmpty(tb))
                    return true;
            } catch (NumberFormatException ignored) {}
        }

        Double dx = parseDouble(ta);
        Double dy = parseDouble(tb);
        if (dx != null && dy != null) {
            int d = EFFECTIVE_PRECISION_MAP.getOrDefault(field, ROUNDING_MAP.getOrDefault(field, 3));
            double f = Math.pow(10, d);
            double rx = Math.round(dx * f) / f;
            double ry = Math.round(dy * f) / f;
            return Double.compare(rx, ry) == 0;
        }
        return false;
    }

    private static boolean isZeroNaNOrEmpty(String s) {
        if (s == null) return true;
        String t = s.trim();
        if (t.isEmpty()) return true;
        if (t.equalsIgnoreCase("NaN")) return true;
        try {
            double v = Double.parseDouble(t);
            return Math.abs(v) < 1e-12 || Double.isNaN(v);
        } catch (Exception e) {
            return false;
        }
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isEmpty() || s.equalsIgnoreCase("NaN")) return null;
        try {
            double v = Double.parseDouble(s);
            return Double.isNaN(v) ? null : v;
        } catch (Exception e) {
            return null;
        }
    }

    /** Selected := (Density Ratio >= 2) AND (Variability < 10) AND (R Squared > 0.1) */
    private static boolean isSelected(Map<String, String> row) {
        Double densityRatio = parseDouble(row.get("Density Ratio"));
        Double variability  = parseDouble(row.get("Variability"));
        Double rSquared     = parseDouble(row.get("R Squared"));
        if (densityRatio == null || variability == null || rSquared == null) return false;
        return densityRatio >= 2.0 && variability < 10.0 && rSquared > 0.1;
    }

    private static String quote(String s) {
        if (s == null) return "";
        String t = s.replace("\"", "\"\"");
        if (t.contains(",") || t.contains("\"") || t.contains(" ")) return "\"" + t + "\"";
        return t;
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }


    // ----------------------------------------------------------------------
    private static void compare(List<Map<String, String>> oldNorm, List<Map<String, String>> newNorm) throws IOException {
        Path reportFile = Paths.get("/Users/Hans/Desktop/Squares Validation - Comparison.csv");

        // Sort inputs
        Comparator<Map<String, String>> sorter = Comparator
                .comparing((Map<String, String> r) -> r.get("Recording Name"))
                .thenComparing(r -> {
                    String s = r.get("Square Nr");
                    try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
                });
        oldNorm.sort(sorter);
        newNorm.sort(sorter);

        Map<String, Map<String, String>> newMap = new HashMap<>();
        for (Map<String, String> n : newNorm) newMap.put(compositeKey(n), n);

        List<String[]> reportRows = new ArrayList<>();

        int matched = 0, mismatched = 0, missing = 0, extra = 0, selectedMismatches = 0;

        for (Map<String, String> oldRow : oldNorm) {
            String key = compositeKey(oldRow);
            Map<String, String> newRow = newMap.get(key);

            if (newRow == null) {
                reportRows.add(new String[]{ oldRow.get("Recording Name"), oldRow.get("Square Nr"),
                        "", "", "", "", "", "Missing in NEW" });
                missing++;
                continue;
            }

            boolean diff = false;

            // Compare numeric & mapped fields
            for (String field : FIELD_MAP.keySet()) {
                String oldVal = oldRow.getOrDefault(field, "");
                String newVal = newRow.getOrDefault(field, "");

                boolean equal = DOUBLE_FIELDS.contains(field)
                        ? compareDoubles(field, oldVal, newVal)
                        : Objects.equals(oldVal, newVal);

                if (!equal) {
                    diff = true;
                    int prec = EFFECTIVE_PRECISION_MAP.getOrDefault(field, ROUNDING_MAP.getOrDefault(field, -1));

                    // Relative difference
                    String relDiffStr = "";
                    Double dx = parseDouble(oldVal);
                    Double dy = parseDouble(newVal);
                    if (dx != null && dy != null && Math.abs(dx) > 1e-12) {
                        double relDiff = Math.abs((dy - dx) / dx) * 100.0;
                        relDiffStr = String.format(Locale.US, "%.2f", relDiff);
                    }

                    reportRows.add(new String[]{
                            oldRow.get("Recording Name"),
                            oldRow.get("Square Nr"),
                            field,
                            oldVal,
                            newVal,
                            String.valueOf(prec),
                            relDiffStr,
                            "DIFFERENT"
                    });
                }
            }

            // Compare Selected flag
            String selOld = String.valueOf(isSelected(oldRow));
            String selNew = String.valueOf(isSelected(newRow));
            if (!selOld.equalsIgnoreCase(selNew)) {
                diff = true;
                selectedMismatches++;
                reportRows.add(new String[]{
                        oldRow.get("Recording Name"),
                        oldRow.get("Square Nr"),
                        "Selected",
                        selOld,
                        selNew,
                        "",         // precision N/A
                        "",         // relative diff N/A
                        "DIFFERENT"
                });
            }

            if (diff) mismatched++; else matched++;
        }

        // Extra rows in NEW
        Set<String> oldKeys = new HashSet<>();
        for (Map<String, String> o : oldNorm) oldKeys.add(compositeKey(o));
        for (Map<String, String> n : newNorm) {
            if (!oldKeys.contains(compositeKey(n))) {
                reportRows.add(new String[]{
                        n.get("Recording Name"), n.get("Square Nr"),
                        "", "", "", "", "", "Extra in NEW"
                });
                extra++;
            }
        }

        // Sort report rows by key
        reportRows.sort(Comparator
                                .comparing((String[] r) -> r[0] == null ? "" : r[0])
                                .thenComparing(r -> {
                                    try { return Integer.parseInt(r[1]); } catch (Exception e) { return 0; }
                                }));

        // Write report
        try (PrintWriter report = new PrintWriter(reportFile.toFile())) {
            report.println("Recording Name,Square Nr,Field,Old Value,New Value,Precision Used,Relative Difference (%),Status");
            for (String[] row : reportRows) {
                List<String> escaped = new ArrayList<>();
                for (String v : row) escaped.add(quote(v));
                report.println(String.join(",", escaped));
            }
            report.println();
            report.println("SUMMARY,,,,,,,");
            report.printf("Matched,%d,,,,,,%n", matched);
            report.printf("Mismatched,%d,,,,,,%n", mismatched);
            report.printf("Missing,%d,,,,,,%n", missing);
            report.printf("Extra,%d,,,,,,%n", extra);
            report.printf("Selected mismatches,%d,,,,,,%n", selectedMismatches);
            report.flush();
        }

        // Console summary
        System.out.printf("✅ Comparison complete. Report: %s%n", reportFile);
        System.out.printf("Matched: %d, Mismatched: %d, Missing: %d, Extra: %d, Selected mismatches: %d%n",
                          matched, mismatched, missing, extra, selectedMismatches);
    }

    // ADD at the bottom of the file, before the final closing brace of the class:

    // ----------------------------------------------------------------------
    /**
     * Write an overview CSV listing for each square:
     * Selected (old/new), Tau, Density Ratio, and Variability,
     * plus combined selection state (Both/OnlyOld/OnlyNew).
     */
    private static void writeSelectedOverview(List<Map<String, String>> oldNorm,
                                              List<Map<String, String>> newNorm) throws IOException {
        Path overviewFile = Paths.get("/Users/Hans/Desktop/Squares Validation - Selected Overview.csv");

        Comparator<Map<String, String>> sorter = Comparator
                .comparing((Map<String, String> r) -> r.get("Recording Name"))
                .thenComparing(r -> {
                    String s = r.get("Square Nr");
                    try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
                });
        oldNorm.sort(sorter);
        newNorm.sort(sorter);

        Map<String, Map<String, String>> newMap = new HashMap<>();
        for (Map<String, String> n : newNorm) newMap.put(compositeKey(n), n);

        Set<String> allKeys = new TreeSet<>();
        for (Map<String, String> o : oldNorm) allKeys.add(compositeKey(o));
        for (Map<String, String> n : newNorm) allKeys.add(compositeKey(n));

        try (PrintWriter pw = new PrintWriter(overviewFile.toFile())) {
            pw.println("Recording Name,Square Nr,"
                               + "Selected (Old),Selected (New),Both Selected,Only Old Selected,Only New Selected,"
                               + "Tau (Old),Tau (New),Density Ratio (Old),Density Ratio (New),Variability (Old),Variability (New)");

            for (String key : allKeys) {
                String rec = key.split(" - ")[0];
                String sq  = key.split(" - ").length > 1 ? key.split(" - ")[1] : "";

                Map<String, String> oldRow = oldNorm.stream()
                        .filter(r -> compositeKey(r).equals(key)).findFirst().orElse(null);
                Map<String, String> newRow = newMap.get(key);

                boolean selOldBool = oldRow != null && isSelected(oldRow);
                boolean selNewBool = newRow != null && isSelected(newRow);

                String selOld = oldRow != null ? String.valueOf(selOldBool) : "";
                String selNew = newRow != null ? String.valueOf(selNewBool) : "";

                // Combined flags
                String bothSelected = (selOldBool && selNewBool) ? "true" : "";
                String onlyOldSelected = (selOldBool && !selNewBool) ? "true" : "";
                String onlyNewSelected = (!selOldBool && selNewBool) ? "true" : "";

                String tauOld = oldRow != null ? oldRow.getOrDefault("Tau", "") : "";
                String tauNew = newRow != null ? newRow.getOrDefault("Tau", "") : "";

                String drOld = oldRow != null ? oldRow.getOrDefault("Density Ratio", "") : "";
                String drNew = newRow != null ? newRow.getOrDefault("Density Ratio", "") : "";

                String varOld = oldRow != null ? oldRow.getOrDefault("Variability", "") : "";
                String varNew = newRow != null ? newRow.getOrDefault("Variability", "") : "";

                pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                          quote(rec), quote(sq),
                          quote(selOld), quote(selNew),
                          quote(bothSelected), quote(onlyOldSelected), quote(onlyNewSelected),
                          quote(tauOld), quote(tauNew),
                          quote(drOld), quote(drNew),
                          quote(varOld), quote(varNew));
            }
        }

        System.out.printf("📊 Selected overview written: %s%n", overviewFile);
    }

    // ----------------------------------------------------------------------
    public static void main(String[] args) throws IOException {

        Path downloadsPath = Paths.get(System.getProperty("user.home"), "Downloads");
        Path validatePath  = downloadsPath.resolve("Validate").resolve("Squares");

        try {
            Files.createDirectories(validatePath);
        } catch (IOException ignored) {
        }

        Path oldCsv = Paths.get("/Users/hans/Paint Test Project/221012 - Python/" + SQUARES_CSV);
        Path newCsv = Paths.get("/Users/hans/Paint Test Project/221012/" + SQUARES_CSV);

        List<Map<String, String>> oldRows = readCsv(oldCsv);
        List<Map<String, String>> newRows = readCsv(newCsv);

        List<Map<String, String>> normOld = normalizeOld(oldRows);
        List<Map<String, String>> normNew = normalizeNew(newRows);

        Path normOldPath = validatePath.resolve("Squares Validation - Old normalized.csv");
        Path normNewPath = validatePath.resolve("Squares Validation - New Normalized.csv");

        // Effective precision must be computed BEFORE writing normalized CSVs (so rounding matches)
        EFFECTIVE_PRECISION_MAP.putAll(computeEffectivePrecisions(normOld, normNew, DOUBLE_FIELDS));

        System.out.println("🔍 Effective precisions per field:");
        for (Map.Entry<String, Integer> e : EFFECTIVE_PRECISION_MAP.entrySet()) {
            System.out.printf("   %-35s → %d%n", e.getKey(), e.getValue());
        }

        writeCsv(normOld, normOldPath);
        writeCsv(normNew, normNewPath);

        System.out.println("🧩 Normalized files written:");
        System.out.println("   - " + normOldPath);
        System.out.println("   - " + normNewPath);

        compare(normOld, normNew);
        writeSelectedOverview(normOld, normNew);
    }

}