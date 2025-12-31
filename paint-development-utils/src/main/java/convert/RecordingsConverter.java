/*=============================================================================
 *  Class:        RecordingsConverter.java
 *  Package:      convert
 *
 *  PURPOSE:
 *    Specialized converter for the recordings table.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-development-utils
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *=============================================================================*/

package convert;

import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public final class RecordingsConverter {

    private final Path inputFile;
    private final Path outputFile;

    public RecordingsConverter(Path inputDir) {
        this.inputFile = inputDir.resolve("All Recordings.csv");
        this.outputFile = inputDir.resolve("Recordings.csv");
    }

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

    private static final Map<String,String> NAME_MAP = new HashMap<>();
    static {
        NAME_MAP.put("Experiment Name",               "Experiment Name");
        NAME_MAP.put("Recording Name",                "Recording Name");
        NAME_MAP.put("Condition Number",              "Condition Nr");
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
        NAME_MAP.put("Run Time",                      "Run Time");
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

    private static final Map<String,Integer> INDEX_MAP = new HashMap<>();
    static {
        INDEX_MAP.put("Number of Tracks in Background", 15);
    }

    public List<Map<String,String>> convert(List<Map<String,String>> src) {

        List<Map<String,String>> out = new ArrayList<>();

        for (Map<String,String> inRow : src) {

            String[] raw = toArray(inRow);

            Map<String,String> row = new LinkedHashMap<>();

            for (String newCol : HEADER) {

                // 1. name-based
                if (NAME_MAP.containsKey(newCol)) {

                    String oldName = NAME_MAP.get(newCol);

                    if (inRow.containsKey(oldName)) {
                        String val = safe(inRow.get(oldName));

                        // SPECIAL CASE: fix Time Stamp format
                        if (newCol.equals("Time Stamp")) {
                            val = fixTimestamp(val);
                        }

                        row.put(newCol, val);
                        continue;
                    }
                }

                // 2. index-based
                if (INDEX_MAP.containsKey(newCol)) {
                    int idx = INDEX_MAP.get(newCol) - 1;
                    if (idx >= 0 && idx < raw.length) {
                        row.put(newCol, safe(raw[idx]));
                        continue;
                    }
                }

                // 3. default
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
        return (s == null ? "" : s.trim());
    }

    // ---------------------------------------------------------------------
    // TIMESTAMP NORMALIZER: "Wed Sep 3 18:16:25 2025" → "2025-09-03T18:16:25"
    // ---------------------------------------------------------------------
    private static final SimpleDateFormat OLD_FMT =
            new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy", Locale.ENGLISH);

    private static final SimpleDateFormat ISO_FMT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private static String fixTimestamp(String v) {
        if (v == null || v.trim().isEmpty()) return "";

        try {
            Date d = OLD_FMT.parse(v.trim());
            return ISO_FMT.format(d);
        } catch (ParseException e) {
            return v;   // leave untouched if unparseable
        }
    }

    // ---------------------------------------------------------------------

    public void run() throws Exception {
        System.out.println("Reading:  " + inputFile);
        List<Map<String,String>> src = CsvIO.readSimpleCsv(inputFile);

        System.out.println("Converting Recordings...");
        List<Map<String,String>> out = convert(src);

        System.out.println("Writing:  " + outputFile);
        CsvIO.writeSimpleCsv(outputFile, HEADER, out);

        System.out.println("✔ Recordings conversion complete.");
    }
}