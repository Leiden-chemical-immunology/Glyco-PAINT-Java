package tools;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Test: For the first 100 tracks in the OLD file, find all tracks in the NEW file
 * that have the same Square (Nr/Number), Nr Spots, Nr Gaps, Longest Gap (integers)
 * and nearly identical numeric values for Track Duration, Displacement,
 * Max/Median Speed, Total Distance, X/Y Location.
 *
 * Prints Track Ids of all matches and summary counts.
 */
public class CompareAllTracksTest {

    private static final double XY_TOLERANCE = 0.5;
    private static final double DURATION_TOLERANCE = 0.1;
    private static final double SPEED_TOLERANCE = 0.5;
    private static final double DISPLACEMENT_TOLERANCE = 0.5;

    public static void main(String[] args) {
        Path oldCsv = Paths.get("/Users/hans/Paint/Paint Data - v39/Regular Probes/Paint Regular Probes - 20 Squares/221012/All Tracks.csv");
        Path newCsv = Paths.get("/Users/hans/Paint Test Project/221012/All Tracks Java.csv");

        try {
            List<Map<String, String>> oldRows = readCsv(oldCsv);
            List<Map<String, String>> newRows = readCsv(newCsv);

            System.out.printf("Loaded %,d OLD tracks and %,d NEW tracks%n", oldRows.size(), newRows.size());

            // -------- sort (use the correct column names per file) --------
            oldRows.sort(comparator("Square Nr", "Nr Spots", "Nr Gaps", "Longest Gap"));
            newRows.sort(comparator("Square Number", "Number of Spots", "Number of Gaps", "Longest Gap"));

            // -------- build NEW index (key uses NEW column names) --------
            Map<String, List<Map<String, String>>> newIndex = new HashMap<>();
            for (Map<String, String> row : newRows) {
                String key = makeIntKey(
                        row.get("Square Number"),
                        row.get("Number of Spots"),
                        row.get("Number of Gaps"),
                        row.get("Longest Gap"));
                newIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
            }

            System.out.printf("New index buckets: %,d%n", newIndex.size());

            int limit = Math.min(100, oldRows.size());
            int foundCount = 0;
            int uniqueCount = 0;

            System.out.println("\n=== Matching first " + limit + " OLD tracks ===");
            for (int i = 0; i < limit; i++) {
                Map<String, String> old = oldRows.get(i);

                int square  = parseIntSafe(old.get("Square Nr"));  // <— old uses Square Nr
                int nSpots  = parseIntSafe(old.get("Nr Spots"));
                int nGaps   = parseIntSafe(old.get("Nr Gaps"));
                int longest = parseIntSafe(old.get("Longest Gap"));

                double xOld  = parseDoubleSafe(old.get("Track X Location"));
                double yOld  = parseDoubleSafe(old.get("Track Y Location"));
                double durOld = parseDoubleSafe(old.get("Track Duration"));
                double dispOld = parseDoubleSafe(old.get("Track Displacement"));
                double maxSpeedOld = parseDoubleSafe(old.get("Track Max Speed"));
                double medSpeedOld = parseDoubleSafe(old.get("Track Median Speed"));
                double distOld = parseDoubleSafe(old.get("Total Distance"));
                String oldId = old.getOrDefault("Track Id", "");

                String key = square + "|" + nSpots + "|" + nGaps + "|" + longest;
                List<Map<String, String>> candidates = newIndex.getOrDefault(key, Collections.emptyList());

                List<String> matchIds = new ArrayList<>();

                for (Map<String, String> cand : candidates) {
                    double xNew  = parseDoubleSafe(cand.get("Track X Location"));
                    double yNew  = parseDoubleSafe(cand.get("Track Y Location"));
                    double durNew = parseDoubleSafe(cand.get("Track Duration"));
                    double dispNew = parseDoubleSafe(cand.get("Track Displacement"));
                    double maxSpeedNew = parseDoubleSafe(cand.get("Track Max Speed"));
                    double medSpeedNew = parseDoubleSafe(cand.get("Track Median Speed"));
                    double distNew = parseDoubleSafe(cand.get("Total Distance"));

                    if (Math.abs(xOld - xNew) <= XY_TOLERANCE &&
                            Math.abs(yOld - yNew) <= XY_TOLERANCE &&
                            Math.abs(distOld - distNew) <= XY_TOLERANCE &&
                            Math.abs(durOld - durNew) <= DURATION_TOLERANCE &&
                            Math.abs(dispOld - dispNew) <= DISPLACEMENT_TOLERANCE &&
                            Math.abs(maxSpeedOld - maxSpeedNew) <= SPEED_TOLERANCE &&
                            Math.abs(medSpeedOld - medSpeedNew) <= SPEED_TOLERANCE) {

                        matchIds.add(cand.getOrDefault("Track Id", "?"));
                    }
                }

                int matches = matchIds.size();
                if (matches > 0) foundCount++;
                if (matches == 1) uniqueCount++;

                System.out.printf(Locale.US,
                                  "OLD #%3d (Id=%s, Sq=%3d, Spots=%3d, Gaps=%3d, Longest=%3d, Dur=%.2f, Dist=%.2f, Disp=%.2f, Max=%.2f, Med=%.2f, X=%.2f, Y=%.2f) → %2d match(es): %s%n",
                                  i, oldId, square, nSpots, nGaps, longest, durOld, distOld, dispOld, maxSpeedOld, medSpeedOld, xOld, yOld, matches,
                                  matchIds.isEmpty() ? "—" : matchIds);
            }

            System.out.printf("%nSummary:%n");
            System.out.printf("  ≥1 match found: %d/%d%n", foundCount, limit);
            System.out.printf("  Exactly one match: %d/%d%n", uniqueCount, limit);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- Helper Methods ----------------

    private static Comparator<Map<String, String>> comparator(String... cols) {
        return (a, b) -> {
            for (String col : cols) {
                int va = parseIntSafe(a.get(col));
                int vb = parseIntSafe(b.get(col));
                int cmp = Integer.compare(va, vb);
                if (cmp != 0) return cmp;
            }
            return 0;
        };
    }

    private static String makeIntKey(String... vals) {
        StringBuilder sb = new StringBuilder();
        for (String v : vals) {
            if (sb.length() > 0) sb.append('|');
            sb.append(parseIntSafe(v));
        }
        return sb.toString();
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
            int count = 0;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                Map<String, String> map = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    map.put(headers[i].trim(), i < parts.length ? parts[i].trim() : "");
                }
                rows.add(map);
                if (++count % 10000 == 0) {
                    System.out.println("Read " + count + " rows from " + path.getFileName());
                }
            }
        }
        return rows;
    }
}