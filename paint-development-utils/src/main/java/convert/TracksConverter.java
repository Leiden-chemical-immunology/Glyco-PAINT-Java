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

    // Old → New header mapping
    private static final Map<String,String> MAP = new LinkedHashMap<>();
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

    // Output header: Unique Key, Experiment Name, then mapped fields
    public List<String> getOutputHeader() {
        List<String> header = new ArrayList<>();
        header.add("Unique Key");
        header.add("Experiment Name");
        for (Map.Entry<String,String> e : MAP.entrySet()) {
            if (!"Unique Key".equals(e.getKey())) {
                header.add(e.getValue());
            }
        }
        return header;
    }

    // -----------------------------------------------------------
    // Convert logic
    // -----------------------------------------------------------
    public List<Map<String,String>> convert(List<Map<String,String>> src) {
        List<Map<String,String>> out = new ArrayList<>();

        for (Map<String,String> r : src) {

            String unique = r.get("Unique Key");
            String experimentName = "";

            if (unique != null && unique.length() >= 6) {
                experimentName = unique.substring(0, 6);
            }

            Map<String,String> row = new LinkedHashMap<>();

            // 1. Unique Key — EXACT COPY (no trim)
            row.put("Unique Key", unique);

            // 2. Constructed Experiment Name
            row.put("Experiment Name", experimentName);

            // 3. Remaining mapped fields
            for (Map.Entry<String,String> e : MAP.entrySet()) {

                String oldH = e.getKey();
                String newH = e.getValue();

                if ("Unique Key".equals(oldH)) {
                    continue; // already handled
                }

                String val = r.get(oldH);

                // Strip "-Threshold..." from Recording Name
                if (newH.equals("Recording Name")) {
                    row.put(newH, stripThresholdSuffix(val));
                    continue;
                }

                // INTEGER normalization
                if (newH.equals("Number of Spots") ||
                        newH.equals("Number of Gaps") ||
                        newH.equals("Longest Gap")) {

                    row.put(newH, toIntOrEmpty(val));
                    continue;
                }

                // DOUBLE normalization
                if (newH.equals("Track Duration") ||
                        newH.equals("Track X Location") ||
                        newH.equals("Track Y Location") ||
                        newH.equals("Track Displacement") ||
                        newH.equals("Track Max Speed") ||
                        newH.equals("Track Median Speed") ||
                        newH.equals("Diffusion Coefficient") ||
                        newH.equals("Diffusion Coefficient Ext") ||
                        newH.equals("Total Distance") ||
                        newH.equals("Confinement Ratio")) {

                    row.put(newH, toDoubleOrEmpty(val));
                    continue;
                }

                // Default
                row.put(newH, safe(val));
            }

            out.add(row);
        }

        return out;
    }

    // -----------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private static String toIntOrEmpty(String v) {
        if (v == null) return "";
        v = v.trim();
        if (v.isEmpty()) return "";
        try {
            double d = Double.parseDouble(v);
            int i = (int) d;
            return Integer.toString(i);
        } catch (Exception ex) {
            return "";
        }
    }

    private static String toDoubleOrEmpty(String v) {
        if (v == null) return "";
        v = v.trim();
        if (v.isEmpty()) return "";
        try {
            double d = Double.parseDouble(v);
            return Double.toString(d);
        } catch (Exception ex) {
            return "";
        }
    }

    private static String stripThresholdSuffix(String v) {
        if (v == null) return "";
        v = v.trim();
        if (v.isEmpty()) return v;

        int idx = v.toLowerCase().lastIndexOf("-threshold");
        if (idx == -1) return v;

        return v.substring(0, idx).trim();
    }

    // -----------------------------------------------------------
    // Runner
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