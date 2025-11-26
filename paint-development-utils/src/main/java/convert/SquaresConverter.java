package convert;

import java.nio.file.Path;
import java.util.*;

public final class SquaresConverter implements CsvConverter {

    private final Path inputDir;
    private final Path inputFile;
    private final Path outputFile;

    public SquaresConverter(Path inputDir) {
        this.inputDir = inputDir;
        this.inputFile = inputDir.resolve("All Squares.csv");
        this.outputFile = inputDir.resolve("Squares.csv");
    }

    // --------------------------------------------------------------------
    // FINAL NEW HEADER ORDER (column 3 in your table)
    // --------------------------------------------------------------------
    private static final List<String> HEADER = Arrays.asList(
            "Unique Key",
            "Experiment Name",
            "Recording Name",
            "Square Number",
            "Row Number",
            "Column Number",
            "Label Number",
            "Cell ID",
            "Visible",
            "Square Manually Excluded",
            "Image Excluded",
            "X0",
            "Y0",
            "X1",
            "Y1",
            "Number of Tracks",
            "Variability",
            "Density",
            "Density Ratio",
            "Density Ratio Ori",
            "Tau",
            "R Squared",
            "Median Diffusion Coefficient",
            "Median Diffusion Coefficient Ext",
            "Median Displacement",
            "Max Displacement",
            "Total Displacement",
            "Median Max Speed",
            "Max Max Speed",
            "Median Mean Speed",
            "Max Mean Speed",
            "Max Track Duration",
            "Total Track Duration",
            "Median Track Duration"
    );

    // --------------------------------------------------------------------
    // NEW COLUMN → OLD POSITION (your column 4)
    // --------------------------------------------------------------------
    private static final Map<String,Integer> INDEX_MAP = new LinkedHashMap<String,Integer>();
    static {
        INDEX_MAP.put("Unique Key",                           1);
        INDEX_MAP.put("Experiment Name",                      4);
        INDEX_MAP.put("Recording Name",                       3);
        INDEX_MAP.put("Square Number",                        8);
        INDEX_MAP.put("Row Number",                          15);
        INDEX_MAP.put("Column Number",                       16);
        INDEX_MAP.put("Label Number",                        17);
        INDEX_MAP.put("Cell ID",                             18);
        INDEX_MAP.put("Visible",                             25);
        INDEX_MAP.put("Square Manually Excluded",            47);
        INDEX_MAP.put("Image Excluded",                      48);
        INDEX_MAP.put("X0",                                  21);
        INDEX_MAP.put("Y0",                                  22);
        INDEX_MAP.put("X1",                                  23);
        INDEX_MAP.put("Y1",                                  24);
        INDEX_MAP.put("Number of Tracks",                    20);
        INDEX_MAP.put("Variability",                         26);
        INDEX_MAP.put("Density",                             27);
        INDEX_MAP.put("Density Ratio",                       28);
        INDEX_MAP.put("Density Ratio Ori",                   28);
        INDEX_MAP.put("Tau",                                 29);
        INDEX_MAP.put("R Squared",                           30);
        INDEX_MAP.put("Median Diffusion Coefficient",        31);
        INDEX_MAP.put("Median Diffusion Coefficient Ext",    33);
        INDEX_MAP.put("Median Displacement",                 37);
        INDEX_MAP.put("Max Displacement",                    38);
        INDEX_MAP.put("Total Displacement",                  39);
        INDEX_MAP.put("Median Max Speed",                    40);
        INDEX_MAP.put("Max Max Speed",                       41);
        INDEX_MAP.put("Median Mean Speed",                   42);
        INDEX_MAP.put("Max Mean Speed",                      43);
        INDEX_MAP.put("Max Track Duration",                  44);
        INDEX_MAP.put("Total Track Duration",                45);
        INDEX_MAP.put("Median Track Duration",               46);
    }

    @Override
    public List<String> getOutputHeader() {
        return HEADER;
    }

    @Override
    public List<Map<String,String>> convert(List<Map<String,String>> src) {

        List<Map<String,String>> out = new ArrayList<Map<String,String>>();

        for (Map<String,String> inRow : src) {

            // Skip completely empty rows
            if (isEffectivelyEmptyRow(inRow)) {
                continue;
            }

            String[] raw = inRow.values().toArray(new String[inRow.size()]);
            Map<String,String> row = new LinkedHashMap<String,String>();

            for (String newCol : HEADER) {

                int oldPos = INDEX_MAP.get(newCol);
                int idx = oldPos - 1;

                String val = (idx < raw.length ? raw[idx] : "");
                val = (val == null ? "" : val.trim());

                // SPECIAL: Unique Key cleanup
                if (newCol.equals("Unique Key")) {
                    row.put("Unique Key", stripThresholdForUniqueKey(val));
                    continue;
                }

                // SPECIAL: Recording Name cleanup
                if (newCol.equals("Recording Name")) {
                    row.put("Recording Name", stripThresholdFromRecordingName(val));
                    continue;
                }

                // SPECIAL: Label Number must be a valid integer
                if (newCol.equals("Label Number")) {
                    row.put("Label Number", parseOrZero(val));
                    continue;
                }

                // Normal mapping
                row.put(newCol, val);
            }

            out.add(row);
        }

        System.out.println("ROWS OUT: " + out.size());
        for (int i = 0; i < out.size(); i++) {
            Map<String,String> r = out.get(i);
            boolean empty = isEffectivelyEmptyRow(r);
            if (empty) {
                System.out.println("⚠ EMPTY OUTPUT ROW at index " + i);
            }
        }

        return out;
    }

    private static String parseOrZero(String s) {
        if (s == null) return "0";
        String t = s.trim();
        if (t.isEmpty()) return "0";
        try {
            Integer.parseInt(t);
            return t;  // valid integer
        } catch (Exception e) {
            return "0";  // invalid → force 0
        }
    }

    public void run() throws Exception {
        System.out.println("Reading:  " + inputFile);
        List<Map<String,String>> src = CsvIO.readCsv(inputFile);

        System.out.println("Converting Squares...");
        List<Map<String,String>> out = convert(src);

        System.out.println("Writing:  " + outputFile);
        CsvIO.writeCsv(outputFile, HEADER, out);

        System.out.println("✔ Squares conversion complete.");
    }

    private static boolean isEffectivelyEmptyRow(Map<String,String> row) {
        boolean hasRealValue = false;

        for (String v : row.values()) {
            if (v == null) continue;

            String t = v.trim();

            // Empty string → ignore
            if (t.isEmpty()) continue;

            // A "0" in Label Number or any numeric column is NOT meaningful → ignore
            if (t.equals("0")) continue;

            // Any other value means this row is a real data row
            hasRealValue = true;
            break;
        }

        return !hasRealValue;
    }

    /**
     * For UNIQUE KEY:
     *  "221012-Exp-1-A1-1-threshold-5 - 0"  → "221012-Exp-1-A1-1-0"
     *  "111111-Exp-11-A1-Threshold - 5 -1"  → "111111-Exp-11-A1-1"
     */
    private static String stripThresholdForUniqueKey(String v) {
        if (v == null) return "";
        v = v.trim();
        if (v.isEmpty()) return v;

        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "^(.*?)-threshold\\s*-\\s*\\d+\\s*-\\s*(\\d{1,3})\\s*$",
                java.util.regex.Pattern.CASE_INSENSITIVE
        ).matcher(v);

        if (m.matches()) {
            String base = m.group(1).trim();  // before "-threshold..."
            String last = m.group(2);         // final number
            return base + "-" + last;
        }
        return v;
    }

    /**
     * For RECORDING NAME:
     *  "221012-Exp-1-A1-1-threshold-5"      → "221012-Exp-1-A1-1"
     *  "111111-Exp-11-A1-Threshold - 5"    → "111111-Exp-11-A1"
     */
    private static String stripThresholdFromRecordingName(String v) {
        if (v == null) return "";
        v = v.trim();
        if (v.isEmpty()) return v;

        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "^(.*?)-threshold\\s*-\\s*\\d+\\s*$",
                java.util.regex.Pattern.CASE_INSENSITIVE
        ).matcher(v);

        if (m.matches()) {
            return m.group(1).trim();
        }
        return v;
    }
}