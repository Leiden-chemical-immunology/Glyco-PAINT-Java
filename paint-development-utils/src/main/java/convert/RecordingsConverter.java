package convert;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class RecordingsConverter implements CsvConverter {

    private final Path inputDir;
    private final Path inputFile;
    private final Path outputFile;

    public RecordingsConverter(Path inputDir) {
        this.inputDir = inputDir;
        this.inputFile = inputDir.resolve("All Recordings.csv");
        this.outputFile = inputDir.resolve("Recordings.csv");
    }

    // ---------------------------------------------------------------
    // FINAL NEW HEADER ORDER
    // ---------------------------------------------------------------
    private static final List<String> HEADER = Arrays.asList(
            "Experiment Name",
            "Recording Name",
            "Condition Number",
            "Replicate Number",
            "Probe Name",
            "Probe Type",
            "Cell Type",
            "Adjuvant",
            "Concentration",
            "Process Flag",
            "Threshold",
            "Number of Spots",
            "Number of Tracks",
            "Number of Tracks in Background",
            "Number of Squares in Background",
            "Average Tracks in Background",
            "Number of Spots in All Tracks",
            "Number of Frames",
            "Run Time",
            "Time Stamp",
            "Exclude",
            "Tau",
            "R Squared",
            "Density",
            "Min Required Density Ratio",
            "Min Required R Squared",
            "Max Allowable Variability",
            "Neighbour Mode"
    );

    // ---------------------------------------------------------------
    // NAME → NAME mappings (name-based lookups)
    // ---------------------------------------------------------------
    private static final Map<String,String> NAME_MAP = new HashMap<String,String>();
    static {
        NAME_MAP.put("Experiment Name",               "Experiment Name");
        NAME_MAP.put("Recording Name",                "Recording Name");
        NAME_MAP.put("Condition Number",              "Condition Nr");     // FIXED
        NAME_MAP.put("Replicate Number",              "Replicate Nr");
        NAME_MAP.put("Probe Name",                    "Probe");
        NAME_MAP.put("Probe Type",                    "Probe Type");
        NAME_MAP.put("Cell Type",                     "Cell Type");
        NAME_MAP.put("Adjuvant",                      "Adjuvant");
        NAME_MAP.put("Concentration",                 "Concentration");
        NAME_MAP.put("Process Flag",                  "Process");
        NAME_MAP.put("Threshold",                     "Threshold");
        NAME_MAP.put("Number of Spots",               "Nr Spots");
        NAME_MAP.put("Number of Tracks",              "Nr Tracks");
        NAME_MAP.put("Number of Frames",              "Recording Size");
        NAME_MAP.put("Run Time",                      "Run Time");         // FIXED: from old column 16
        NAME_MAP.put("Time Stamp",                    "Time Stamp");
        NAME_MAP.put("Number of Spots in All Tracks", "Nr Spots in All Tracks");
        NAME_MAP.put("Min Required R Squared",        "Min Required R Squared");
        NAME_MAP.put("Min Required Density Ratio",    "Min Required Density Ratio");
        NAME_MAP.put("Max Allowable Variability",     "Max Allowable Variability");
        NAME_MAP.put("Exclude",                       "Exclude");
        NAME_MAP.put("Neighbour Mode",                "Neighbour Mode");
        NAME_MAP.put("Tau",                           "Tau");
        NAME_MAP.put("Density",                       "Density");
        NAME_MAP.put("R Squared",                     "R Squared");
    }

    // ---------------------------------------------------------------
    // INDEX-BASED MAPPINGS (1-based indexes from old Python export)
    // ---------------------------------------------------------------
    private static final Map<String,Integer> INDEX_MAP = new HashMap<String,Integer>();
    static {
        // These three exist in your table but do not have name-based equivalents:
        INDEX_MAP.put("Number of Tracks in Background", 15);
        INDEX_MAP.put("Average Tracks in Background",   16);  // check if needed
        INDEX_MAP.put("Number of Squares in Background",17);  // check if needed
    }

    @Override
    public List<String> getOutputHeader() {
        return HEADER;
    }

    @Override
    public List<Map<String,String>> convert(List<Map<String,String>> src) {

        List<Map<String,String>> out = new ArrayList<Map<String,String>>();

        for (Map<String,String> inRow : src) {

            String[] raw = toArray(inRow); // old row as array

            Map<String,String> row = new LinkedHashMap<String,String>();

            for (String newCol : HEADER) {

                // 1. Try name-based mapping first
                if (NAME_MAP.containsKey(newCol)) {
                    String oldName = NAME_MAP.get(newCol);
                    if (inRow.containsKey(oldName)) {
                        row.put(newCol, safe(inRow.get(oldName)));
                        continue;
                    }
                }

                // 2. Try index-based mapping
                if (INDEX_MAP.containsKey(newCol)) {
                    int idx = INDEX_MAP.get(newCol) - 1;
                    if (idx >= 0 && idx < raw.length) {
                        row.put(newCol, safe(raw[idx]));
                        continue;
                    }
                }

                // 3. Nothing found → default "0"
                row.put(newCol, "0");
            }

            out.add(row);
        }

        return out;
    }

    private static String[] toArray(Map<String,String> row) {
        return row.values().toArray(new String[row.size()]);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    public void run() throws Exception {
        System.out.println("Reading:  " + inputFile);
        List<Map<String,String>> src = CsvIO.readCsv(inputFile);

        System.out.println("Converting Recordings...");
        List<Map<String,String>> out = convert(src);

        System.out.println("Writing:  " + outputFile);
        CsvIO.writeCsv(outputFile, HEADER, out);

        System.out.println("✔ Recordings conversion complete.");
    }


}