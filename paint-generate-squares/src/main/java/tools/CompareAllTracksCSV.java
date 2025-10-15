package tools;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Compare two "All Tracks" CSV files (old vs new/Java).
 *
 * Same logic as CompareAllTracksTest, but writes results to CSV.
 * For each of the first 100 OLD tracks:
 *   - find matching NEW tracks within the same recording (prefix rule)
 *   - match based on Square, Nr Spots, Nr Gaps, Longest Gap, and numeric tolerances
 *   - record number of matches (0, 1, >1) instead of just true/false
 */
public class CompareAllTracksCSV {

    private static final double XY_TOLERANCE = 0.5;
    private static final double DURATION_TOLERANCE = 0.1;
    private static final double SPEED_TOLERANCE = 0.5;
    private static final double DISPLACEMENT_TOLERANCE = 0.5;
    private static final double DIST_TOLERANCE = 0.5;

    public static void main(String[] args) {
        Path oldCsv = Paths.get("/Users/hans/Paint/Paint Data - v39/Regular Probes/Paint Regular Probes - 20 Squares/221012/All Tracks.csv");
        Path newCsv = Paths.get("/Users/hans/Paint Test Project/221012/All Tracks Java.csv");
        Path outCsv = Paths.get("/Users/hans/Desktop/AllTracksComparison_100.csv");

        try {
            List<Map<String, String>> oldRows = readCsv(oldCsv);
            List<Map<String, String>> newRows = readCsv(newCsv);

            System.out.printf("Loaded %,d OLD tracks and %,d NEW tracks%n", oldRows.size(), newRows.size());

            // Group new tracks by Recording Name
            Map<String, List<Map<String, String>>> newByRecording = new HashMap<>();
            for (Map<String, String> r : newRows) {
                String rec = r.getOrDefault("Recording Name", "").trim();
                newByRecording.computeIfAbsent(rec, k -> new ArrayList<>()).add(r);
            }

            Set<String> usedNewTrackIds = new HashSet<>();

            try (BufferedWriter bw = Files.newBufferedWriter(outCsv)) {

                // Write header
                bw.write(String.join(",", Arrays.asList(
                        "Ext Recording Name", "Recording Name",
                        "Track Id", "Track Id Java",
                        "Square Nr",
                        "Nr Spots", "Nr Gaps", "Longest Gap",
                        "Track Duration", "Track Displacement",
                        "Track Max Speed", "Track Median Speed",
                        "Total Distance", "Track X Location", "Track Y Location",
                        "Matches Found"
                )));
                bw.newLine();

                int limit = Math.min(100, oldRows.size());
                int matched = 0;

                for (int i = 0; i < limit; i++) {
                    Map<String, String> old = oldRows.get(i);
                    String oldRec = old.getOrDefault("Ext Recording Name", "").trim();

                    // Find base recording prefix match
                    String bestRec = findMatchingRecordingPrefix(oldRec, newByRecording.keySet());
                    List<Map<String, String>> candidates = newByRecording.getOrDefault(bestRec, Collections.emptyList());

                    // Extract old numeric fields
                    int square = parseIntSafe(old.get("Square Nr"));
                    int nSpots = parseIntSafe(old.get("Nr Spots"));
                    int nGaps = parseIntSafe(old.get("Nr Gaps"));
                    int longest = parseIntSafe(old.get("Longest Gap"));
                    double durOld = parseDoubleSafe(old.get("Track Duration"));
                    double dispOld = parseDoubleSafe(old.get("Track Displacement"));
                    double maxOld = parseDoubleSafe(old.get("Track Max Speed"));
                    double medOld = parseDoubleSafe(old.get("Track Median Speed"));
                    double distOld = parseDoubleSafe(old.get("Total Distance"));
                    double xOld = parseDoubleSafe(old.get("Track X Location"));
                    double yOld = parseDoubleSafe(old.get("Track Y Location"));
                    String oldId = old.getOrDefault("Track Id", "");

                    List<Map<String, String>> matches = new ArrayList<>();

                    for (Map<String, String> cand : candidates) {
                        String newId = cand.getOrDefault("Track Id", "");
                        if (usedNewTrackIds.contains(newId)) continue;

                        int squareNew = parseIntSafe(cand.get("Square Number"));
                        int nSpotsNew = parseIntSafe(cand.get("Number of Spots"));
                        int nGapsNew = parseIntSafe(cand.get("Number of Gaps"));
                        int longestNew = parseIntSafe(cand.get("Longest Gap"));

                        if (square != squareNew || nSpots != nSpotsNew || nGaps != nGapsNew || longest != longestNew)
                            continue;

                        double durNew = parseDoubleSafe(cand.get("Track Duration"));
                        double dispNew = parseDoubleSafe(cand.get("Track Displacement"));
                        double maxNew = parseDoubleSafe(cand.get("Track Max Speed"));
                        double medNew = parseDoubleSafe(cand.get("Track Median Speed"));
                        double distNew = parseDoubleSafe(cand.get("Total Distance"));
                        double xNew = parseDoubleSafe(cand.get("Track X Location"));
                        double yNew = parseDoubleSafe(cand.get("Track Y Location"));

                        if (Math.abs(durOld - durNew) <= DURATION_TOLERANCE &&
                                Math.abs(dispOld - dispNew) <= DISPLACEMENT_TOLERANCE &&
                                Math.abs(maxOld - maxNew) <= SPEED_TOLERANCE &&
                                Math.abs(medOld - medNew) <= SPEED_TOLERANCE &&
                                Math.abs(distOld - distNew) <= DIST_TOLERANCE &&
                                Math.abs(xOld - xNew) <= XY_TOLERANCE &&
                                Math.abs(yOld - yNew) <= XY_TOLERANCE) {
                            matches.add(cand);
                        }
                    }

                    int matchCount = matches.size();
                    if (matchCount == 1)
                        usedNewTrackIds.add(matches.get(0).get("Track Id"));
                    if (matchCount > 0)
                        matched++;

                    Map<String, String> match = (matchCount == 1) ? matches.get(0) : null;

                    // Write row
                    List<String> row = new ArrayList<>();
                    row.add(escapeCsv(oldRec));                                      // Ext Recording Name
                    row.add(match != null ? escapeCsv(bestRec) : "");                // Recording Name (new)
                    row.add(escapeCsv(oldId));                                       // Track Id (old)
                    row.add(match != null ? escapeCsv(match.get("Track Id")) : "");  // Track Id Java (new)
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
                    row.add(String.valueOf(matchCount));

                    bw.write(String.join(",", row));
                    bw.newLine();

                    if ((i + 1) % 10 == 0)
                        System.out.printf("Processed %,d / %,d%n", i + 1, limit);
                }

                System.out.printf("%n✅ Finished: %d matched (≥1) of %,d%n", matched, limit);
                System.out.println("📄 Output written to: " + outCsv.toAbsolutePath());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Helper functions ---

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
        if (s.contains(",") || s.contains("\""))
            return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
}