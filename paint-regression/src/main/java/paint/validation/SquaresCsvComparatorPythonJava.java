/******************************************************************************
 *  Class:        SquaresCsvComparatorUnified.java
 *  Package:      validation
 *
 *  PURPOSE:
 *    Provides a unified CSV comparison framework for validating and aligning
 *    Paint-generated “Squares” data across different systems (e.g., Python vs. Java).
 *    Performs normalization, precision analysis, field-level comparison, and
 *    tolerance optimization.
 *
 *  DESCRIPTION:
 *    This class reads, normalizes, and compares two datasets to ensure data
 *    consistency. It detects numeric and textual differences, computes relative
 *    deviations, evaluates per-field tolerance ranges, and generates multiple
 *    CSV-based validation outputs for detailed analysis and summary reporting.
 *
 *  RESPONSIBILITIES:
 *    • Normalize datasets into a shared schema
 *    • Compare fields across Paint versions (old vs. new)
 *    • Generate per-field numeric and textual difference reports
 *    • Compute shared numeric precision and deviation tolerances
 *    • Summarize selection discrepancies between datasets
 *
 *  USAGE EXAMPLE:
 *    SquaresCsvComparatorUnified.main(new String[]{});
 *
 *  DEPENDENCIES:
 *    - java.io
 *    - java.nio.file
 *    - java.util
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-27
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SquaresCsvComparatorPythonJava {

    /**
     * A static and final map that serves as a key-value data structure
     * for storing mappings of strings. The map is initialized as a
     * LinkedHashMap to ensure that the order of insertion is preserved.
     * <p>
     * This map is intended to be immutable, meaning it cannot be modified
     * after its initialization. It is used to represent predefined mappings
     * between string keys and string values, likely for configuration,
     * settings, or other mapping-based use cases within the context of
     * the application.
     */
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

    /**
     * A constant list that defines names of numeric fields used in specific calculations, analyses,
     * or statistical operations. The list contains descriptive string identifiers associated with
     * numeric data attributes related to specialized domains such as tracking, displacement, diffusion,
     * density, and variability metrics.
     */
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
            "Median Diffusion Coefficient Ext"
    );

    /**
     * Quick lookup set version of numeric fields.
     */
    private static final Set<String> NUMERIC_FIELDS = new HashSet<>(NUMERIC_FIELDS_LIST);

    // These fields are used for counting and collecting results
    Set<String> squaresWithDiffs = new HashSet<>();

    /**
     * Default rounding precision if nothing better is detected.
     */
    private static final Map<String, Integer> ROUNDING_MAP = new HashMap<>();

    static {
        for (String f : NUMERIC_FIELDS) {
            ROUNDING_MAP.put(f, 3);
        }
        ROUNDING_MAP.put("Variability", 1);
        ROUNDING_MAP.put("Tau", 2);
        ROUNDING_MAP.put("Density Ratio", 2);
        ROUNDING_MAP.put("R Squared", 2);
    }

    /**
     * Default per-field tolerance in % (used for "WITHIN X%" comparison).
     */
    private static final Map<String, Double> TOLERANCE_MAP = new HashMap<>();

    static {
        for (String f : NUMERIC_FIELDS) {
            TOLERANCE_MAP.put(f, 5.0);
        }
    }

    /**
     * Shared numeric precision determined from both files.
     */
    private static final Map<String, Integer> EFFECTIVE_PRECISION_MAP = new HashMap<>();

    // ---------------------------- Main ----------------------------

    /**
     * The main method orchestrates the entire workflow for comparing and validating
     * CSV data related to square structures. It performs tasks such as reading CSVs,
     * normalizing data, calculating precision levels, generating comparison files,
     * and optimizing tolerance levels.
     *
     * @param args Command-line arguments passed to the program.
     */
    public static void main(String[] args) {
        try {
            // Prepare output directory under ~/Downloads/Validate/Squares
            Path outDir = Paths.get(System.getProperty("user.home"), "Downloads", "Validate", "Squares");
            Files.createDirectories(outDir);

            // Define file paths for old/new CSVs
            Path oldCsv = Paths.get("/Users/hans/Paint Test Project/221012 - Python/All Squares.csv");
            Path newCsv = Paths.get("/Users/hans/Paint Test Project/221012/Squares.csv");

            // Step 1: Read both CSVs
            System.out.println("Reading CSVs...");
            List<Map<String, String>> oldRows = readCsv(oldCsv);
            List<Map<String, String>> newRows = readCsv(newCsv);
            System.out.printf("   OLD: %d rows%n   NEW: %d rows%n", oldRows.size(), newRows.size());

            // Step 2: Normalize both versions to a unified structure
            System.out.println("Normalizing...");
            List<Map<String, String>> normOld = normalizeOld(oldRows);
            List<Map<String, String>> normNew = normalizeNew(newRows);

            // Step 3: Determine shared numeric precision for each field
            EFFECTIVE_PRECISION_MAP.clear();
            EFFECTIVE_PRECISION_MAP.putAll(computeEffectivePrecisions(normOld, normNew, NUMERIC_FIELDS));
            System.out.println("Effective numeric precision (shared):");
            EFFECTIVE_PRECISION_MAP.forEach((k, v) -> System.out.printf("   %-35s -> %d%n", k, v));

            // Step 4: Write normalized versions for manual inspection
            Path normOldPath = outDir.resolve("Squares Validation - Old Normalized.csv");
            Path normNewPath = outDir.resolve("Squares Validation - New Normalized.csv");
            System.out.println("Writing normalized outputs...");
            writeCsv(normOld, normOldPath);
            writeCsv(normNew, normNewPath);
            System.out.println("   -> " + normOldPath);
            System.out.println("   -> " + normNewPath);

            // Step 5: Compare normalized data
            System.out.println("Comparing (status per field)...");
            Path comparisonCsv = outDir.resolve("Squares Validation - Comparison.csv");
            compareStatus(normOld, normNew, outDir);

            // Step 6: Write detailed numeric difference table
            System.out.println("Writing detailed numeric diff...");
            Path detailedCsv = outDir.resolve("Squares Validation - Detailed.csv");
            writeDetailed(normOld, normNew, detailedCsv);

            // Step 7: Write overview of selected (included) squares
            System.out.println("Writing selected overview...");
            Path selectedCsv = outDir.resolve("Squares Validation - Selected Overview.csv");
            writeSelectedOverview(normOld, normNew, selectedCsv);

            // Step 8: Optional short pause for file flush on some systems
            try {
                Thread.sleep(150);
            } catch (InterruptedException ignored) {
            }

            // Step 9: Suggest optimized tolerance levels based on data
            System.out.println("Optimizing tolerances...");
            Path tolCsv = outDir.resolve("Squares Validation - Tolerance Optimization.csv");
            optimizeTolerances(comparisonCsv, tolCsv);

            // Done
            System.out.println("All tasks complete.");
            System.out.println("Output directory: " + outDir.toAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------------------- IO ----------------------------

    /**
     * Reads a CSV file from the given path and parses its content into a list of maps.
     * Each map represents a row, where the keys are the column headers
     * and the values are the corresponding cell values.
     *
     * @param p the path to the CSV file to be read
     * @return a list of maps where each map corresponds to a row in the CSV file with key-value pairs of column headers to cell values
     * @throws IOException if there is an error opening or reading the file
     */
    private static List<Map<String, String>> readCsv(Path p) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String header = br.readLine();
            if (header == null) {
                return rows;
            }
            String[] h = header.split(",", -1);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
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

    /**
     * Writes a list of rows represented as key-value pairs into a CSV file at the specified location.
     * The method creates necessary directories if they do not exist and processes special formatting
     * requirements for numeric fields and headers.
     *
     * @param rows a list of maps where each map represents a row of the CSV. Keys are column names, and values are cell data.
     * @param file the path to the CSV file to be written. Directories in the path will be created if they do not exist.
     * @throws IOException if an I/O error occurs during file creation or writing.
     */
    private static void writeCsv(List<Map<String, String>> rows, Path file) throws IOException {
        if (rows.isEmpty()) {
            return;
        }
        Files.createDirectories(file.getParent());

        // Determine whether this is the "old" file (Python source)
        boolean isOldFile = file.getFileName().toString().toLowerCase(Locale.ROOT).contains("old");

        try (PrintWriter pw = new PrintWriter(file.toFile())) {
            // Write header
            List<String> header = new ArrayList<>();
            header.add("Recording Name");
            header.addAll(FIELD_MAP.keySet());
            header.add("Selected");
            pw.println(String.join(",", header));

            // Write data rows
            for (Map<String, String> r : rows) {
                List<String> vals = new ArrayList<>();
                for (String col : header) {
                    String val = r.getOrDefault(col, "");

                    // Handle numeric columns: round or blank-out invalid zero values
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
                                // Blank these zeros in old file
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

    /**
     * Escapes a string for safe usage in a CSV file. If the string contains
     * a comma or a double quote, it will be wrapped in double quotes,
     * and any double quotes within the string will be escaped by doubling them.
     * If the input is null, an empty string is returned.
     *
     * @param s the string to be escaped for CSV formatting; may be null
     * @return a CSV-safe version of the input string, or an empty string if the input is null
     */
    private static String escapeCsv(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // ---------------------------- Normalization ----------------------------

    /**
     * Normalizes and transforms a list of rows (represented as maps)
     * by cleaning and remapping fields, adjusting indices, and
     * computing selection flags.
     *
     * @param oldRows a list of maps representing rows of data to be normalized.
     *                Each map contains key-value pairs of the original data.
     * @return a list of maps where each map represents a normalized row of data
     *         with cleaned and remapped fields, adjusted indices, and computed
     *         selection flags.
     */
    public static List<Map<String, String>> normalizeOld(List<Map<String, String>> oldRows) {
        List<Map<String, String>> out = new ArrayList<>();
        for (Map<String, String> r : oldRows) {
            Map<String, String> n = new LinkedHashMap<>();

            // Normalize recording name (strip "-threshold-N")
            String rec = r.getOrDefault("Ext Recording Name", "");
            n.put("Recording Name", rec.replaceAll("-threshold-\\d+$", "").trim());

            // Map and clean all fields
            for (String f : FIELD_MAP.keySet()) {
                String v = r.getOrDefault(f, "").trim();
                if (f.equals("Tau")) {
                    try {
                        if (!v.isEmpty() && Double.parseDouble(v) < 0) {
                            v = "";
                        }
                    } catch (Exception ignored) {
                    }
                }
                n.put(f, v);
            }

            // Adjust 1-based row/column indices to 0-based
            adjustIndex(n, "Row Nr");
            adjustIndex(n, "Col Nr");

            // Compute selection flag based on field criteria
            n.put("Selected", String.valueOf(isSelected(n)));
            out.add(n);
        }
        return out;
    }

    /**
     * Normalizes a list of maps by restructuring and updating each map's key-value pairs
     * based on predefined mappings and logic.
     *
     * @param newRows a list of maps representing rows of data to be normalized
     * @return a list of normalized maps with updated key-value pairs
     */
    public static List<Map<String, String>> normalizeNew(List<Map<String, String>> newRows) {
        List<Map<String, String>> out = new ArrayList<>();
        for (Map<String, String> r : newRows) {
            Map<String, String> n = new LinkedHashMap<>();
            n.put("Recording Name", r.getOrDefault("Recording Name", ""));
            for (Map.Entry<String, String> e : FIELD_MAP.entrySet()) {
                n.put(e.getKey(), r.getOrDefault(e.getValue(), ""));
            }
            n.put("Selected", String.valueOf(isSelected(n)));
            out.add(n);
        }
        return out;
    }

    /**
     * Converts a 1-based integer field to 0-based, if it parses as a number.
     *
     * @param m row map to mutate
     * @param k key whose value should be decremented (Row Nr / Col Nr)
     */
    private static void adjustIndex(Map<String, String> m, String k) {
        try {
            String v = m.get(k);
            if (v != null && !v.isEmpty()) {
                int i = (int) Double.parseDouble(v);
                m.put(k, String.valueOf(i - 1));
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Detects the shared numeric precision (decimal places) for all
     * numeric fields across two datasets by taking the minimum precision
     * observed per field.
     *
     * @param a       first dataset (normalized rows)
     * @param b       second dataset (normalized rows)
     * @param numeric set of field names to consider as numeric
     * @return map of field → effective precision
     */
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

    /**
     * Determines the maximum number of fractional digits present for a field
     * within a list of row maps.
     *
     * @param rows rows to scan
     * @param f    field name
     * @return the highest number of decimals seen; 0 if none
     */
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

    /**
     * Compares normalized old/new CSVs field-by-field and writes the
     * per-field comparison report, including differences, missing values,
     * and field-level summaries.
     *
     * @param oldN   normalized old data
     * @param newN   normalized new data
     * @param outDir directory where the comparison CSV is written
     * @throws IOException if writing the comparison file fails
     */
    private static void compareStatus(List<Map<String, String>> oldN, List<Map<String, String>> newN, Path outDir) throws IOException {
        Path out = outDir.resolve("Squares Validation - Comparison.csv");

        List<String[]> diffs = new ArrayList<>();
        int total = 0, diffCount = 0;
        Set<String> squaresWithDiffs = new HashSet<>();

        // Track field-level differences
        Map<String, Integer> diffByField = new HashMap<>();
        Map<String, Integer> missingByField = new HashMap<>();

        // Build lookup map for new dataset
        Map<String, Map<String, String>> newMap = new HashMap<>();
        for (Map<String, String> n : newN) {
            newMap.put(key(n), n);
        }

        // Iterate all old records and compare with matching new record
        for (Map<String, String> o : oldN) {
            total++;
            String k = key(o);
            Map<String, String> n = newMap.get(k);
            if (n == null) {
                continue;
            }

            for (String f : FIELD_MAP.keySet()) {
                String ov = o.getOrDefault(f, "");
                String nv = n.getOrDefault(f, "");

                // Handle optional 0/empty fields gracefully
                if (isOptionalZeroField(f) && (isZeroOrEmpty(ov) || isZeroOrEmpty(nv))) {
                    boolean ovEmpty = isZeroOrEmpty(ov);
                    boolean nvEmpty = isZeroOrEmpty(nv);
                    if (ovEmpty && nvEmpty) {
                        continue; // both empty → skip
                    }

                    // One side missing → record as "MISSING"
                    if (ovEmpty) {
                        ov = "";
                    }
                    if (nvEmpty) {
                        nv = "";
                    }
                    diffs.add(new String[]{o.get("Recording Name"), o.get("Square Nr"), f, ov, nv, "", "", "MISSING"});
                    missingByField.merge(f, 1, Integer::sum);
                    squaresWithDiffs.add(k);
                    continue;
                }

                // Numeric comparison logic
                if (NUMERIC_FIELDS.contains(f)) {
                    Double da = parseDouble(ov);
                    Double db = parseDouble(nv);

                    if ((da == null || isZeroOrEmpty(ov)) && (db == null || isZeroOrEmpty(nv))) {
                        continue;
                    }

                    if ((da == null || isZeroOrEmpty(ov)) || (db == null || isZeroOrEmpty(nv))) {
                        if (isZeroOrEmpty(ov)) {
                            ov = "";
                        }
                        if (isZeroOrEmpty(nv)) {
                            nv = "";
                        }
                        diffs.add(new String[]{o.get("Recording Name"), o.get("Square Nr"), f, ov, nv, "", "", "MISSING"});
                        missingByField.merge(f, 1, Integer::sum);
                        squaresWithDiffs.add(k);
                        continue;
                    }

                    // Compute relative deviation and status
                    double dev = relativeDeviation(ov, nv);
                    double tol = TOLERANCE_MAP.getOrDefault(f, 5.0);
                    int prec = EFFECTIVE_PRECISION_MAP.getOrDefault(f, ROUNDING_MAP.getOrDefault(f, 3));

                    if (Double.isNaN(dev)) {
                        continue;
                    }

                    String status;
                    if (Math.abs(da - db) < 1e-12) {
                        status = "EQUAL";
                    } else if (dev <= tol) {
                        status = "WITHIN " + tol + "%";
                    } else {
                        status = "DIFFERENT";
                        diffCount++;
                        diffByField.merge(f, 1, Integer::sum);
                        squaresWithDiffs.add(k);
                    }

                    diffs.add(new String[]{o.get("Recording Name"), o.get("Square Nr"), f,
                            ov, nv, String.valueOf(prec),
                            String.format(Locale.US, "%.3f", dev), status});
                }
            }

            // Compare "Selected" field between old/new
            String sOld = o.get("Selected");
            String sNew = n.get("Selected");
            if (!Objects.equals(sOld, sNew)) {
                diffs.add(new String[]{o.get("Recording Name"), o.get("Square Nr"),
                        "Selected", sOld, sNew, "", "", "DIFFERENT"});
                diffCount++;
                diffByField.merge("Selected", 1, Integer::sum);
                squaresWithDiffs.add(k);
            }

            if (total % 1000 == 0) {
                System.out.printf("   ...processed %,d%n", total);
            }
        }

        // Write the comparison file
        try (PrintWriter pw = new PrintWriter(out.toFile())) {
            pw.println("Recording Name,Square Nr,Field,Old Value,New Value,Precision Used,Relative Diff (%),Status");
            for (String[] r : diffs) {
                pw.println(String.join(",", r));
            }
            pw.println();
            pw.printf("SUMMARY,,,,,,,%nDifferences,%d%n", diffCount);

            // Overview of differences and missing values
            if (!diffByField.isEmpty()) {
                pw.println();
                pw.println("Field Difference Overview (Status=DIFFERENT):");
                pw.println("Field,Count");
                diffByField.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .forEach(e -> pw.printf("%s,%d%n", e.getKey(), e.getValue()));
            }
            if (!missingByField.isEmpty()) {
                pw.println();
                pw.println("Field Missing Overview (one side empty):");
                pw.println("Field,Count");
                missingByField.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .forEach(e -> pw.printf("%s,%d%n", e.getKey(), e.getValue()));
            }
        }

        // Console summary
        System.out.printf("✅ Compared %,d squares — %d differences in %d squares%n",
                          total, diffCount, squaresWithDiffs.size());
    }

    // ---------------------------- Detailed numeric diff ----------------------------

    /**
     * Writes a detailed numeric comparison for all numeric fields,
     * including absolute and percentage differences per field.
     *
     * @param oldN    normalized old data
     * @param newN    normalized new data
     * @param outFile output CSV file to create
     * @throws IOException if writing fails
     */
    private static void writeDetailed(List<Map<String, String>> oldN, List<Map<String, String>> newN, Path outFile) throws IOException {
        Map<String, Map<String, String>> newMap = new HashMap<>();
        for (Map<String, String> n : newN) {
            newMap.put(key(n), n);
        }

        List<List<String>> rows = new ArrayList<>();
        int matched = 0, total = oldN.size();

        for (Map<String, String> o : oldN) {
            Map<String, String> n = newMap.get(key(o));
            if (n == null) {
                continue;
            }
            matched++;

            List<String> line = new ArrayList<>();
            line.add(o.getOrDefault("Recording Name", ""));
            line.add(n.getOrDefault("Recording Name", ""));

            // For each numeric field, compute old/new/diff/diff%
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
                line.add(format1(diffPer)); // 1 decimal place for %
                line.add(""); // separator column
            }
            rows.add(line);
        }

        // Write output CSV with grouped columns per field
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
            for (List<String> r : rows) {
                pw.println(String.join(",", r));
            }
        }

        System.out.printf("Detailed numeric rows written: %d (matched from %d)%n", matched, total);
        System.out.println("Written: " + outFile.toAbsolutePath());
    }

    // ---------------------------- Selected Overview ----------------------------

    /**
     * Writes a compact overview comparing selection status (old vs new) per square,
     * including Tau, Density Ratio, and Variability values.
     *
     * @param oldN    normalized old data
     * @param newN    normalized new data
     * @param outFile output CSV file to create
     * @throws IOException if writing fails
     */
    private static void writeSelectedOverview(List<Map<String, String>> oldN, List<Map<String, String>> newN, Path outFile) throws IOException {
        Map<String, Map<String, String>> newMap = new HashMap<>();
        for (Map<String, String> n : newN) {
            newMap.put(key(n), n);
        }

        Set<String> allKeys = new TreeSet<>();
        for (Map<String, String> o : oldN) {
            allKeys.add(key(o));
        }
        for (Map<String, String> n : newN) {
            allKeys.add(key(n));
        }

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

    /**
     * Returns a value from a row map or an empty string if the map is null or key missing.
     *
     * @param m row map (may be null)
     * @param k key to extract
     * @return value or empty string
     */
    private static String val(Map<String, String> m, String k) {
        return m == null ? "" : m.getOrDefault(k, "");
    }

    // ---------------------------- Tolerance Optimization ----------------------------

    /**
     * Analyze the comparison report to estimate a tighter per-field tolerance
     * that still keeps at least 98% of cases within range.
     *
     * @param comparisonCsv input comparison CSV produced by {@link #compareStatus(List, List, Path)}
     * @param outCsv        output CSV summarizing suggested tolerances
     * @throws IOException if reading or writing fails
     */
    private static void optimizeTolerances(Path comparisonCsv, Path outCsv) throws IOException {
        if (!Files.exists(comparisonCsv)) {
            System.out.println("No comparison file found for tolerance optimization.");
            return;
        }

        Map<String, List<Double>> diffsByField = new LinkedHashMap<>();

        // Read all numeric differences from comparison file
        try (BufferedReader br = Files.newBufferedReader(comparisonCsv, java.nio.charset.StandardCharsets.UTF_8)) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("SUMMARY")) {
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length < 7) {
                    continue;
                }
                String field = parts[2].trim();
                String rel = parts[6].trim();
                if (rel.isEmpty() || rel.equalsIgnoreCase("NaN")) {
                    continue;
                }
                try {
                    double d = Double.parseDouble(rel);
                    diffsByField.computeIfAbsent(field, k -> new ArrayList<>()).add(d);
                } catch (Exception ignored) {
                }
            }
        }

        if (diffsByField.isEmpty()) {
            System.out.println("No numeric deviations detected — skipping optimization.");
            return;
        }

        // Test tolerance levels from 5% down to 0.5%
        double[] testLevels = {5.0, 4.0, 3.0, 2.0, 1.0, 0.5};
        double targetKeep = 98.0;

        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outCsv, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("Field,Total,Equal@5%,OptimalTolerance(%),Equal@Optimal(%)");

            for (Map.Entry<String, List<Double>> e : diffsByField.entrySet()) {
                String field = e.getKey();
                List<Double> diffs = e.getValue();
                diffs.removeIf(d -> !Double.isFinite(d));
                if (diffs.isEmpty()) {
                    continue;
                }

                int total = diffs.size();
                double within5 = percentWithin(diffs, 5.0);
                double bestTol = 5.0, bestKeep = within5;

                // Find the smallest tolerance that still keeps ≥98% within range
                for (double tol : testLevels) {
                    double keep = percentWithin(diffs, tol);
                    if (keep < targetKeep) {
                        break;
                    }
                    bestTol = tol;
                    bestKeep = keep;
                }

                pw.printf(Locale.US, "%s,%d,%.2f,%.2f,%.2f%n", field, total, within5, bestTol, bestKeep);
            }
        }

        System.out.println("Tolerance optimization summary written: " + outCsv.toAbsolutePath());
    }

    /**
     * Percentage of values whose absolute magnitude is within the given tolerance.
     *
     * @param vals list of numeric deviations (percent)
     * @param tol  tolerance threshold (percent)
     * @return percent of entries with |value| ≤ tol
     */
    private static double percentWithin(List<Double> vals, double tol) {
        long ok = vals.stream().filter(v -> Math.abs(v) <= tol).count();
        return 100.0 * ok / vals.size();
    }

    /**
     * Builds a unique composite key for a row: {@code "Recording Name - Square Nr"}.
     *
     * @param r row map
     * @return composite key
     */
    private static String key(Map<String, String> r) {
        return r.getOrDefault("Recording Name", "") + " - " + r.getOrDefault("Square Nr", "");
    }

    /**
     * Parses a string to {@link Double}, returning {@code null} for empty or NaN.
     *
     * @param s string to parse
     * @return parsed Double or {@code null}
     */
    private static Double parseDouble(String s) {
        if (s == null || s.isEmpty() || s.equalsIgnoreCase("NaN")) {
            return null;
        }
        try {
            double v = Double.parseDouble(s);
            return Double.isNaN(v) ? null : v;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses a string to primitive {@code double}; missing/invalid becomes 0.0.
     *
     * @param s input string
     * @return parsed value or 0.0
     */
    private static double toDouble(String s) {
        Double d = parseDouble(s);
        return d == null ? 0.0 : d;
    }

    /**
     * Formats a number with 4 fractional digits (US locale).
     *
     * @param v value
     * @return formatted string
     */
    private static String format4(double v) {
        return String.format(Locale.US, "%.4f", v);
    }

    /**
     * Formats a number with 1 fractional digit (US locale).
     *
     * @param v value
     * @return formatted string
     */
    private static String format1(double v) {
        return String.format(Locale.US, "%.1f", v);
    }

    /**
     * Computes the relative deviation (percent) between two numeric values given as strings.
     * Returns NaN if either value is missing/invalid. If old is zero and new is non-zero,
     * returns {@link Double#POSITIVE_INFINITY}.
     *
     * @param oldVal old value (string)
     * @param newVal new value (string)
     * @return relative deviation in percent, NaN if not applicable
     */
    private static double relativeDeviation(String oldVal, String newVal) {
        Double oldNum = parseDouble(oldVal);
        Double newNum = parseDouble(newVal);
        if (oldNum == null || newNum == null) {
            return Double.NaN;
        }
        if (oldNum == 0.0 && newNum == 0.0) {
            return 0.0;
        }
        if (oldNum == 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.abs((newNum - oldNum) / oldNum) * 100.0;
    }

    /**
     * Evaluates whether a row meets the "selected" criteria:
     * <ul>
     *     <li>Density Ratio ≥ 2.0</li>
     *     <li>Variability &lt; 10.0</li>
     *     <li>R Squared &gt; 0.1</li>
     * </ul>
     *
     * @param r row map
     * @return {@code true} if selected, otherwise {@code false}
     */
    private static boolean isSelected(Map<String, String> r) {
        Double dr  = parseDouble(r.get("Density Ratio"));
        Double var = parseDouble(r.get("Variability"));
        Double r2  = parseDouble(r.get("R Squared"));
        return dr != null && var != null && r2 != null && dr >= 2.0 && var < 10.0 && r2 > 0.1;
    }

    /**
     * Indicates whether a field is treated as an "optional zero" numeric field.
     * For these fields, a zero or empty value on either side may be considered missing.
     *
     * @param f field name
     * @return {@code true} if field is optional-zero; otherwise {@code false}
     */
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

    /**
     * Determines if the given string is null/blank or parses to numeric zero.
     *
     * @param s string to test
     * @return {@code true} if empty or numerically zero; otherwise {@code false}
     */
    private static boolean isZeroOrEmpty(String s) {
        if (s == null || s.trim().isEmpty()) {
            return true;
        }
        try {
            return Double.parseDouble(s.trim()) == 0.0;
        } catch (Exception e) {
            return false;
        }
    }
}