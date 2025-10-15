package tools;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * ============================================================================
 * MatchAllTracks.java
 *
 * Matches tracks between old "All Tracks" and new "All Tracks Java" CSVs.
 * - Groups by recording name
 * - Old uses Ext Recording Name (may contain suffix)
 * - Prefix-matching between Ext Recording Name and Recording Name
 * - Requires Nr Spots, Nr Gaps, and Longest Gap to be identical
 * - Compares numeric fields for best match
 * - Progress shown per recording, with summary at end
 * ============================================================================
 */
public class MatchAllTracks {

    private static final int TEST_LIMIT = 100;   // compare only first N per recording (for debugging)
    private static final int SEARCH_WINDOW = 100; // ±window for local search
    private static final double PERFECT_TOLERANCE = 1e-6;

    /** Hardcoded mapping: old column → new column (from Tracks Match.csv). */
    private static Map<String, String> getMapping() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Nr Spots", "Number of Spots");
        map.put("Nr Gaps", "Number of Gaps");
        map.put("Longest Gap", "Longest Gap");
        map.put("Track Duration", "Track Duration");
        map.put("Track X Location", "Track X Location");
        map.put("Track Y Location", "Track Y Location");
        map.put("Track Displacement", "Track Displacement");
        map.put("Track Max Speed", "Track Max Speed");
        map.put("Track Median Speed", "Track Median Speed");
        return map;
    }

    public static void main(String[] args) {
        // Adjust paths here:
        Path oldCsv = Paths.get("/Users/hans/Paint/Paint Data - v39/Regular Probes/Paint Regular Probes - 20 Squares/221012/All Tracks.csv");
        Path newCsv = Paths.get("/Users/hans/Paint Test Project/221012/All Tracks Java.csv");
        Path outCsv = Paths.get("/Users/hans/Desktop/TrackMatches_ByRecording.csv");

        try {
            Map<String, String> mapping = getMapping();
            System.out.println("🔍 Loading CSV files...");
            List<Map<String, String>> oldTracks = readCsv(oldCsv);
            List<Map<String, String>> newTracks = readCsv(newCsv);
            System.out.printf("✅ Loaded %d old tracks, %d new tracks%n", oldTracks.size(), newTracks.size());

            // Group tracks by recording
            Map<String, List<Map<String, String>>> oldByExt = groupByKey(oldTracks, "Ext Recording Name");
            Map<String, List<Map<String, String>>> newByRec = groupByKey(newTracks, "Recording Name");

            System.out.printf("📁 Found %d recordings in old (Ext), %d in new%n",
                              oldByExt.size(), newByRec.size());

            // Match recordings by prefix
            Map<String, String> recordingMatch = new LinkedHashMap<>();
            for (String newRec : newByRec.keySet()) {
                String match = findMatchingOldRecording(newRec, oldByExt.keySet());
                if (match != null) {
                    recordingMatch.put(newRec, match);
                }
                else {
                    System.out.printf("⚠️  No match found for new recording '%s'%n", newRec);
                }
            }

            try (BufferedWriter bw = Files.newBufferedWriter(outCsv)) {
                // Header
                List<String> header = new ArrayList<>();
                header.add("Recording Name");
                header.add("Old Track ID");
                header.add("Best Match Track ID");
                header.add("Total Difference");
                header.add("PerfectMatch");
                for (String oldField : mapping.keySet()) {
                    header.add(oldField + " Diff");
                }
                bw.write(String.join(",", header));
                bw.newLine();

                Map<String, Integer> totalPerRec = new LinkedHashMap<>();
                Map<String, Integer> perfectPerRec = new LinkedHashMap<>();

                for (Map.Entry<String, String> recMap : recordingMatch.entrySet()) {
                    String newRec = recMap.getKey();
                    String oldRec = recMap.getValue();

                    System.out.printf("🎬 Processing recording: old='%s' ↔ new='%s'%n", oldRec, newRec);

                    List<Map<String, String>> oldList = oldByExt.get(oldRec);
                    List<Map<String, String>> newList = newByRec.get(newRec);

                    if (newList == null || oldList == null) {
                        System.out.printf("⚠️  Missing tracks for recording pair %s ↔ %s%n", oldRec, newRec);
                        continue;
                    }

                    sortTracks(oldList, "Track X Location", "Track Y Location", "Track Duration");
                    sortTracks(newList, "Track X Location (µm)", "Track Y Location (µm)", "Track Duration (s)");

                    int limit = Math.min(TEST_LIMIT, oldList.size());
                    long startTime = System.currentTimeMillis();
                    int perfectCount = 0;

                    for (int i = 0; i < limit; i++) {
                        Map<String, String> oldRow = oldList.get(i);
                        String oldId = oldRow.getOrDefault("Track Id", String.valueOf(i));

                        double bestDistance = Double.POSITIVE_INFINITY;
                        Map<String, String> bestNew = null;
                        Map<String, Double> bestDiffs = new LinkedHashMap<>();

                        int start = Math.max(0, i - SEARCH_WINDOW);
                        int end = Math.min(newList.size(), i + SEARCH_WINDOW);

                        for (int j = start; j < end; j++) {
                            Map<String, String> newRow = newList.get(j);
                            double totalDiff = 0.0;
                            Map<String, Double> diffs = new LinkedHashMap<>();
                            boolean valid = true;

                            // --- Stage 1: integer identity check ---
                            String[] oldKeys = {"Nr Spots", "Nr Gaps", "Longest Gap"};
                            String[] newKeys = {"Number of Spots", "Number of Gaps", "Longest Gap"};

                            for (int k = 0; k < oldKeys.length; k++) {
                                String sOld = oldRow.get(oldKeys[k]);
                                String sNew = newRow.get(newKeys[k]);
                                if (sOld == null || sNew == null) {
                                    valid = false;
                                    break;
                                }
                                try {
                                    int iOld = Integer.parseInt(sOld.trim());
                                    int iNew = Integer.parseInt(sNew.trim());
                                    if (iOld != iNew) {
                                        valid = false;
                                        break;
                                    }
                                } catch (NumberFormatException e) {
                                    valid = false;
                                    break;
                                }
                            }
                            if (!valid) continue;

                            // --- Stage 2: numeric comparison ---
                            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                                String oldCol = entry.getKey();
                                String newCol = entry.getValue();

                                if (oldCol.equals("Nr Spots") || oldCol.equals("Nr Gaps") || oldCol.equals("Longest Gap"))
                                    continue; // skip identity fields

                                Double dOld = tryParseDouble(oldRow.get(oldCol));
                                Double dNew = tryParseDouble(newRow.get(newCol));

                                if (dOld == null || dNew == null) {
                                    valid = false;
                                    break;
                                }

                                double diff = Math.abs(dOld - dNew);
                                diffs.put(oldCol, diff);
                                totalDiff += diff;
                            }

                            if (!valid) continue;
                            if (totalDiff < bestDistance) {
                                bestDistance = totalDiff;
                                bestNew = newRow;
                                bestDiffs = diffs;
                            }
                        }

                        boolean perfect = bestNew != null &&
                                bestDiffs.values().stream().allMatch(d -> d < PERFECT_TOLERANCE);
                        if (perfect) perfectCount++;

                        List<String> line = new ArrayList<>();
                        line.add(escapeCsv(newRec));
                        line.add(escapeCsv(oldId));
                        if (bestNew != null) {
                            line.add(escapeCsv(bestNew.getOrDefault("Track Id", "")));
                            line.add(String.format(Locale.US, "%.6f", bestDistance));
                            line.add(Boolean.toString(perfect));
                            for (String f : mapping.keySet()) {
                                Double d = bestDiffs.get(f);
                                line.add(d == null ? "" : String.format(Locale.US, "%.6f", d));
                            }
                        } else {
                            line.add("");
                            line.add("");
                            line.add("false");
                            for (int k = 0; k < mapping.size(); k++) line.add("");
                        }
                        bw.write(String.join(",", line));
                        bw.newLine();

                        if ((i + 1) % 10 == 0 || i == limit - 1) {
                            double progress = (i + 1) * 100.0 / limit;
                            long elapsed = System.currentTimeMillis() - startTime;
                            System.out.printf("   Progress %s: %.1f%% (%d/%d)  Elapsed: %.1fs%n",
                                              newRec, progress, i + 1, limit, elapsed / 1000.0);
                        }
                    }

                    totalPerRec.put(newRec, limit);
                    perfectPerRec.put(newRec, perfectCount);
                }

                // Summary
                bw.newLine();
                bw.write("Recording,TotalCompared,PerfectMatches,PercentPerfect");
                bw.newLine();
                for (String rec : totalPerRec.keySet()) {
                    int tot = totalPerRec.get(rec);
                    int perf = perfectPerRec.getOrDefault(rec, 0);
                    double pct = tot > 0 ? (100.0 * perf / tot) : 0.0;
                    bw.write(String.format(Locale.US, "%s,%d,%d,%.2f%n", rec, tot, perf, pct));
                }
            }

            System.out.println("✅ Matching complete with per-recording summaries.");
            System.out.println("📄 Output written to: " + outCsv.toAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Helpers ---

    private static String findMatchingOldRecording(String newRec, Set<String> oldNames) {
        for (String oldRec : oldNames) {
            if (oldRec.length() >= newRec.length() &&
                    oldRec.substring(0, newRec.length()).equalsIgnoreCase(newRec)) {
                return oldRec;
            }
        }
        return null;
    }

    private static Map<String, List<Map<String, String>>> groupByKey(List<Map<String, String>> list, String key) {
        Map<String, List<Map<String, String>>> grouped = new LinkedHashMap<>();
        for (Map<String, String> row : list) {
            String rec = row.getOrDefault(key, "UNKNOWN");
            grouped.computeIfAbsent(rec, k -> new ArrayList<>()).add(row);
        }
        return grouped;
    }

    private static void sortTracks(List<Map<String, String>> list, String xCol, String yCol, String durCol) {
        list.sort(Comparator
                          .comparing((Map<String, String> r) -> tryParseDouble(r.get(xCol)), Comparator.nullsLast(Double::compareTo))
                          .thenComparing(r -> tryParseDouble(r.get(yCol)), Comparator.nullsLast(Double::compareTo))
                          .thenComparing(r -> tryParseDouble(r.get(durCol)), Comparator.nullsLast(Double::compareTo)));
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

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}