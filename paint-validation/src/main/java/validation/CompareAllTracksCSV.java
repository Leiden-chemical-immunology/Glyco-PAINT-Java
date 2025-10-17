package validation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Compare two "All Tracks" CSV files (old vs new/Java).
 *
 * This version processes only the FIRST recording (for debugging).
 * Later we can easily loop through all recordings.
 */
public class CompareAllTracksCSV {

    // === Configuration toggles ===
    private static final class MatchConfig {

        // @formatter:off
        boolean useDuration      = true;
        boolean useDisplacement  = true;
        boolean useSpeed         = true;  // both max and median
        boolean useDistance      = true;
        boolean useXY            = true;
        boolean useConfinement   = true;
        // @formatter:on
    }

    // === Tolerances ===

    // @formatter:off
    private static final double XY_TOLERANCE           = 0.5;
    private static final double DURATION_TOLERANCE     = 0.1;
    private static final double SPEED_TOLERANCE        = 0.5;
    private static final double DISPLACEMENT_TOLERANCE = 0.5;
    private static final double DIST_TOLERANCE         = 0.5;
    private static final double CONFINEMENT_TOLERANCE  = 0.5;
    // @formatter:on

    public static void main(String[] args) {
        Path oldCsv = Paths.get("/Users/hans/Paint Test Project/221012 - Python/All Tracks.csv");
        Path newCsv = Paths.get("/Users/hans/Paint Test Project/221012/All Tracks Java.csv");
        Path outCsv = Paths.get("/Users/hans/Desktop/AllTracksComparison.csv");

        MatchConfig cfg = new MatchConfig(); // toggle tests here

        System.out.println("=== Matching Configuration ===");
        System.out.println("Duration:      " + cfg.useDuration);
        System.out.println("Displacement:  " + cfg.useDisplacement);
        System.out.println("Speed:         " + cfg.useSpeed);
        System.out.println("Distance:      " + cfg.useDistance);
        System.out.println("XY Location:   " + cfg.useXY);
        System.out.println("Confinement:   " + cfg.useConfinement);
        System.out.println("==============================\n");

        try {
            List<Map<String, String>> oldRows = readCsv(oldCsv);
            List<Map<String, String>> newRows = readCsv(newCsv);
            System.out.printf("Loaded %,d OLD tracks and %,d NEW tracks%n", oldRows.size(), newRows.size());

            // === Identify and process only FIRST recording ===
            String firstRecording = findFirstRecordingName(oldRows, "Ext Recording Name");
            if (firstRecording == null || firstRecording.isEmpty()) {
                System.err.println("No recording name found in OLD file.");
                return;
            }

            // Filter old rows
            List<Map<String, String>> oldSubset = filterByRecordingPrefix(oldRows, "Ext Recording Name", firstRecording);

            // Group new tracks by recording name
            Map<String, List<Map<String, String>>> newByRecording = new HashMap<>();
            for (Map<String, String> r : newRows) {
                String rec = r.getOrDefault("Recording Name", "").trim();
                newByRecording.computeIfAbsent(rec, k -> new ArrayList<>()).add(r);
            }

            // Try to find matching new recording name
            String bestRec = findMatchingRecordingPrefix(firstRecording, newByRecording.keySet());
            List<Map<String, String>> newSubset = newByRecording.getOrDefault(bestRec, Collections.emptyList());

            System.out.println("Processing only the first recording:");
            System.out.println("  OLD recording: " + firstRecording + " (" + oldSubset.size() + " rows)");
            System.out.println("  NEW recording: " + bestRec + " (" + newSubset.size() + " rows)\n");

            Set<String> usedNewTrackIds = new HashSet<>();
            int total = oldSubset.size();
            int matched = 0, unique = 0, multiple = 0;

            try (BufferedWriter bw = Files.newBufferedWriter(outCsv)) {

                // Header
                bw.write(String.join(",", Arrays.asList(
                        "Ext Recording Name",
                        "Track Id",
                        "Track Id Java",
                        "Square Nr",
                        "Nr Spots",
                        "Nr Gaps",
                        "Longest Gap",
                        "Track Duration",
                        "Track Displacement",
                        "Track Max Speed",
                        "Track Median Speed",
                        "Total Distance",
                        "Track X Location",
                        "Track Y Location",
                        "Confinement Ratio",
                        "Matches Found"
                )));
                bw.newLine();

                for (int i = 0; i < total; i++) {
                    Map<String, String> old = oldSubset.get(i);
                    String oldRec = old.getOrDefault("Ext Recording Name", "").trim();

                    List<Map<String, String>> candidates = newSubset;

                    // Old numeric values
                    int square     = parseIntSafe(old.get("Square Nr"));
                    int nSpots     = parseIntSafe(old.get("Nr Spots"));
                    int nGaps      = parseIntSafe(old.get("Nr Gaps"));
                    int longest    = parseIntSafe(old.get("Longest Gap"));
                    double durOld  = parseDoubleSafe(old.get("Track Duration"));
                    double dispOld = parseDoubleSafe(old.get("Track Displacement"));
                    double maxOld  = parseDoubleSafe(old.get("Track Max Speed"));
                    double medOld  = parseDoubleSafe(old.get("Track Median Speed"));
                    double distOld = parseDoubleSafe(old.get("Total Distance"));
                    double xOld    = parseDoubleSafe(old.get("Track X Location"));
                    double yOld    = parseDoubleSafe(old.get("Track Y Location"));
                    double confOld = parseDoubleSafe(old.get("Confinement Ratio"));
                    String oldId   = old.getOrDefault("Track Id", "");

                    List<Map<String, String>> matches = new ArrayList<>();

                    for (Map<String, String> cand : candidates) {
                        String newId = cand.getOrDefault("Track Id", "");
                        if (usedNewTrackIds.contains(newId)) continue;

                        int squareNew  = parseIntSafe(cand.get("Square Number"));
                        int nSpotsNew  = parseIntSafe(cand.get("Number of Spots"));
                        int nGapsNew   = parseIntSafe(cand.get("Number of Gaps"));
                        int longestNew = parseIntSafe(cand.get("Longest Gap"));

                        if (square != squareNew || nSpots != nSpotsNew || nGaps != nGapsNew || longest != longestNew)
                            continue;

                        double durNew  = parseDoubleSafe(cand.get("Track Duration"));
                        double dispNew = parseDoubleSafe(cand.get("Track Displacement"));
                        double maxNew  = parseDoubleSafe(cand.get("Track Max Speed"));
                        double medNew  = parseDoubleSafe(cand.get("Track Median Speed"));
                        double distNew = parseDoubleSafe(cand.get("Total Distance"));
                        double xNew    = parseDoubleSafe(cand.get("Track X Location"));
                        double yNew    = parseDoubleSafe(cand.get("Track Y Location"));
                        double confNew = parseDoubleSafe(cand.get("Confinement Ratio"));

                        boolean ok = true;
                        if (cfg.useDuration)
                            ok &= Math.abs(durOld - durNew) <= DURATION_TOLERANCE;
                        if (cfg.useDisplacement)
                            ok &= Math.abs(dispOld - dispNew) <= DISPLACEMENT_TOLERANCE;
                        if (cfg.useSpeed)
                            ok &= Math.abs(maxOld - maxNew) <= SPEED_TOLERANCE &&
                                    Math.abs(medOld - medNew) <= SPEED_TOLERANCE;
                        if (cfg.useDistance)
                            ok &= Math.abs(distOld - distNew) <= DIST_TOLERANCE;
                        if (cfg.useXY)
                            ok &= Math.abs(xOld - xNew) <= XY_TOLERANCE &&
                                    Math.abs(yOld - yNew) <= XY_TOLERANCE;
                        if (cfg.useConfinement)
                            ok &= Math.abs(confOld - confNew) <= CONFINEMENT_TOLERANCE;

                        if (ok) matches.add(cand);
                    }

                    int matchCount = matches.size();
                    if (matchCount == 1) {
                        unique++;
                        usedNewTrackIds.add(matches.get(0).get("Track Id"));
                    } else if (matchCount > 1) {
                        multiple++;
                    }
                    if (matchCount > 0) {
                        matched++;
                    }

                    Map<String, String> match = (matchCount == 1) ? matches.get(0) : null;

                    // Write output row
                    List<String> row = new ArrayList<>();
                    row.add(escapeCsv(oldRec));
                    row.add(escapeCsv(oldId));
                    row.add(match != null ? escapeCsv(match.get("Track Id")) : "");
                    row.add(old.getOrDefault("Square Nr", ""));
                    row.add(old.getOrDefault("Nr Spots", ""));
                    row.add(old.getOrDefault("Nr Gaps", ""));
                    row.add(old.getOrDefault("Longest Gap", ""));
                    row.add(old.getOrDefault("Track Duration", ""));
                    row.add(old.getOrDefault("Track Displacement", ""));
                    row.add(old.getOrDefault("Track Max Speed", ""));
                    row.add(old.getOrDefault("Track Median Speed", ""));
                    row.add(old.getOrDefault("Total Distance", ""));
                    row.add(old.getOrDefault("Track X Location", ""));
                    row.add(old.getOrDefault("Track Y Location", ""));
                    row.add(old.getOrDefault("Confinement Ratio", ""));
                    row.add(String.valueOf(matchCount));

                    bw.write(String.join(",", row));
                    bw.newLine();

                    if ((i + 1) % 1000 == 0) {
                        System.out.printf("Processed %,d / %,d%n", i + 1, total);
                    }
                }

                // Summary
                int unmatched = total - matched;
                System.out.println("\n===== SUMMARY (First Recording Only) =====");
                System.out.printf("Total old tracks: %,d%n", total);
                System.out.printf("Matched (≥1):     %,d%n", matched);
                System.out.printf("Unique matches:   %,d%n", unique);
                System.out.printf("Multiple matches: %,d%n", multiple);
                System.out.printf("Unmatched:        %,d%n", unmatched);
                System.out.println("\n📄 Output written to: " + outCsv.toAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Helpers ---

    private static String findFirstRecordingName(List<Map<String, String>> rows, String col) {
        for (Map<String, String> r : rows) {
            String name = r.getOrDefault(col, "").trim();
            if (!name.isEmpty()) return name;
        }
        return null;
    }

    private static List<Map<String, String>> filterByRecordingPrefix(List<Map<String, String>> rows, String col, String prefix) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> r : rows) {
            String name = r.getOrDefault(col, "").trim();
            if (name.startsWith(prefix)) result.add(r);
        }
        return result;
    }

    private static String findMatchingRecordingPrefix(String oldRec, Set<String> newNames) {
        for (String newRec : newNames) {
            if (oldRec.startsWith(newRec)) return newRec;
        }
        return "";
    }

    private static int parseIntSafe(String s) {
        if (s == null || s.isEmpty()) return -1;
        try {
            double d = Double.parseDouble(s.trim());
            return (int) Math.round(d);
        } catch (Exception e) {
            return -1;
        }
    }

    private static double parseDoubleSafe(String s) {
        if (s == null || s.isEmpty()) return Double.NaN;
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return Double.NaN;
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
                    map.put(headers[i].trim(), i < parts.length ? parts[i].trim() : "");
                }
                rows.add(map);
            }
        }
        return rows;
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
}