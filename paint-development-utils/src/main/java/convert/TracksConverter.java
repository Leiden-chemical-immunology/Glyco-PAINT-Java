package convert;

import java.nio.file.*;
import java.util.*;

public final class TracksConverter implements CsvConverter {

    private final Path inputDir;
    private final Path inputFile;
    private final Path outputFile;

    public TracksConverter(Path inputDir) {
        this.inputDir = inputDir;
        this.inputFile = inputDir.resolve("All Tracks.csv");
        this.outputFile = inputDir.resolve("Tracks.csv");
    }

    // Old → New header mapping (Experiment Name is constructed)
    private static final Map<String,String> MAP = new LinkedHashMap<String,String>();
    static {
        MAP.put("Unique Key",                 "Unique Key");
        MAP.put("Ext Recording Name",         "Recording Name");
        MAP.put("Track Id",                   "Track Id");
        MAP.put("Nr Spots",                   "Number of Spots");
        MAP.put("Nr Gaps",                    "Number of Gaps");
        MAP.put("Longest Gap",                "Longest Gap");
        MAP.put("Track Duration",             "Track Duration");
        MAP.put("Track X Location",           "Track X Location");
        MAP.put("Track Y Location",           "Track Y Location");
        MAP.put("Track Displacement",         "Track Displacement");
        MAP.put("Track Max Speed",            "Track Max Speed");
        MAP.put("Track Median Speed",         "Track Median Speed");
        MAP.put("Diffusion Coefficient",      "Diffusion Coefficient");
        MAP.put("Diffusion Coefficient Ext",  "Diffusion Coefficient Ext");
        MAP.put("Total Distance",             "Total Distance");
        MAP.put("Confinement Ratio",          "Confinement Ratio");
        MAP.put("Square Nr",                  "Square Number");
        MAP.put("Label Nr",                   "Label Number");
    }

    // -----------------------------------------------------------
    // Output header: Unique Key, Experiment Name, rest...
    // -----------------------------------------------------------
    public List<String> getOutputHeader() {
        List<String> header = new ArrayList<String>();

        header.add("Unique Key");        // 1
        header.add("Experiment Name");   // 2 (constructed)

        for (Map.Entry<String,String> e : MAP.entrySet()) {
            if ("Unique Key".equals(e.getKey())) continue;
            header.add(e.getValue());
        }
        return header;
    }

    // -----------------------------------------------------------
    // Convert logic
    // -----------------------------------------------------------
    public List<Map<String,String>> convert(List<Map<String,String>> src) {
        List<Map<String,String>> out = new ArrayList<Map<String,String>>();

        for (Map<String,String> r : src) {

            String unique = r.get("Unique Key");
            String experimentName = "";
            if (unique != null && unique.length() >= 6) {
                experimentName = unique.substring(0, 6);
            }

            Map<String,String> row = new LinkedHashMap<String,String>();

            // 1. Unique Key
            row.put("Unique Key", unique);

            // 2. Constructed Experiment Name
            row.put("Experiment Name", experimentName);

            // 3. Remaining fields
            for (Map.Entry<String,String> e : MAP.entrySet()) {
                String oldH = e.getKey();
                String newH = e.getValue();
                if ("Unique Key".equals(oldH)) continue;
                row.put(newH, r.get(oldH));
            }

            out.add(row);
        }

        return out;
    }

    // -----------------------------------------------------------
    // High-level runner
    // -----------------------------------------------------------
    public void run() throws Exception {
        System.out.println("Reading:  " + inputFile);
        List<Map<String,String>> src = CsvIO.readCsv(inputFile);

        System.out.println("Converting Tracks (" + src.size() + " rows)...");
        List<Map<String,String>> out = convert(src);

        System.out.println("Writing:  " + outputFile);
        CsvIO.writeCsv(outputFile, getOutputHeader(), out);

        System.out.println("✔ Tracks conversion complete.");
    }

}