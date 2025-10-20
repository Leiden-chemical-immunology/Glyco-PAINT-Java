package validation;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * ============================================================================
 *  TracksCsvComparator.java
 *  Version: 3.0 — Documented Edition
 *
 *  PURPOSE
 *  ---------------------------------------------------------------------------
 *  Compare and validate two "All Tracks" CSV exports (e.g., Python vs Java)
 *  produced by the PAINT/TrackMate pipeline.
 *
 *  The comparator:
 *    1. Normalizes both CSVs (unifies headers, trims values, aligns naming)
 *    2. Sorts by Recording Name + Track Id
 *    3. Performs multi-phase matching:
 *         - Phase 1: strict per-field tolerance-based matching
 *         - Phase 2: diagnostic best-fit analysis for remaining unmatched tracks
 *         - Phase 3: summary report with matched/unmatched IDs
 *    4. Writes detailed comparison, diagnostics, normalized, and summary CSVs
 *
 *  OUTPUT FILES
 *  ---------------------------------------------------------------------------
 *  ~/Downloads/Validate/Tracks/
 *    ├── Tracks Validation - Comparison.csv
 *    ├── Tracks Validation - Comparison Diagnostics.csv
 *    ├── Tracks Validation - Comparison Summary.csv
 *    ├── Tracks Validation - Old Normalised.csv
 *    └── Tracks Validation - New Normalised.csv
 *
 *  COMPARISON LOGIC
 *  ---------------------------------------------------------------------------
 *  Matching is done per Recording Name (threshold suffix removed).
 *  Each old track is compared to potential new tracks using a configurable
 *  set of numerical tolerances. If only one new track matches → unique match.
 *  Multiple matches are recorded and scored in diagnostics.
 *
 *  Phase 2 diagnostics rank unmatched tracks using a root-mean-square
 *  deviation metric across selected columns, limited by MAX_ACCEPTABLE_SCORE.
 * ============================================================================
 */
public class TracksCsvComparator {

    /**
     * Configuration for which comparison features are active.
     */
    private static final class MatchConfig {

        // @formatter:off
        final boolean useDuration     = true;
        final boolean useDisplacement = true;
        final boolean useSpeed        = true;
        final boolean useDistance     = true;
        final boolean useXY           = true;
        final boolean useConfinement  = true;
        // @formatter:on
    }

    // --- Field-level tolerances (units correspond to column meaning) ---
    private static final double XY_TOLERANCE           = 0.5;
    private static final double DURATION_TOLERANCE     = 0.1;
    private static final double SPEED_TOLERANCE        = 0.5;
    private static final double DISPLACEMENT_TOLERANCE = 0.5;
    private static final double DIST_TOLERANCE         = 0.5;
    private static final double CONFINEMENT_TOLERANCE  = 0.5;

    /** Maximum allowed RMS score for a diagnostic match to be considered acceptable. */
    private static final double MAX_ACCEPTABLE_SCORE   = 40;

    // ---------------------------------------------------------------------
    /**
     * Main entry point. Reads both CSVs, runs all phases, and writes output files.
     */
    public static void main(String[] args) {

        Path downloadsPath = Paths.get(System.getProperty("user.home"), "Downloads");
        Path validatePath  = downloadsPath.resolve("Validate").resolve(Paths.get("Tracks"));

        try {
            Files.createDirectories(validatePath);
        } catch (IOException ignored) {
        }

        // Define input and output file locations
        Path oldCsv     = Paths.get("/Users/hans/Paint Test Project/221012 - Python/Tracks.csv");
        Path newCsv     = Paths.get("/Users/hans/Paint Test Project/221012/Tracks Java.csv");

        Path outCsv     = validatePath.resolve("Tracks Validation - Comparison.csv");
        Path diagCsv    = validatePath.resolve("Tracks Validation - Comparison Diagnostics.csv");
        Path summaryCsv = validatePath.resolve("Tracks Validation - Comparison Summary.csv");
        Path oldNormCsv = validatePath.resolve("Tracks Validation - Old Normalised.csv");
        Path newNormCsv = validatePath.resolve("Tracks Validation - New Normalised.csv");

        MatchConfig cfg = new MatchConfig();

        try {
            // === Step 1: Read & normalize ===
            List<Map<String, String>> oldRows = readCsv(oldCsv);
            List<Map<String, String>> newRows = readCsv(newCsv);

            List<Map<String, String>> oldNorm = normalizeAndSort(oldRows, "Ext Recording Name");
            List<Map<String, String>> newNorm = normalizeAndSort(newRows, "Recording Name");
            writeNormalizedCsv(oldNorm, oldNormCsv);
            writeNormalizedCsv(newNorm, newNormCsv);

            // === Step 2: Group by recording name ===
            Map<String, List<Map<String, String>>> oldByRec = groupBy(oldNorm, "Ext Recording Name");
            Map<String, List<Map<String, String>>> newByRec = groupBy(newNorm, "Recording Name");

            // Build a recording name mapping ignoring threshold suffixes
            Map<String, String> recMapping = new LinkedHashMap<>();
            for (String oldRec : oldByRec.keySet()) {
                String oldNormName = normalizeRecordingName(oldRec);
                String match = findClosestRecording(oldNormName, newByRec.keySet());
                if (match != null) recMapping.put(oldRec, match);
            }

            if (recMapping.isEmpty()) {
                System.err.println("❌ No matching recordings found.");
                return;
            }

            System.out.println("\n=== Recording mappings ===");
            for (Map.Entry<String, String> e : recMapping.entrySet())
                System.out.println("  " + e.getKey() + "  →  " + e.getValue());
            System.out.println();

            // === Step 3: Phase 1 — strict direct matching ===
            Set<String> usedNewIds = new HashSet<>();
            List<Map<String, String>> unmatched = new ArrayList<>();
            List<Map<String, String>> multipleMatches = new ArrayList<>();
            Set<String> multipleMatchIds = new HashSet<>();
            List<String> perfectIds = new ArrayList<>();
            List<String> reasonableIds = new ArrayList<>();
            List<String> unmatchedIds = new ArrayList<>();

            int total = 0, matched = 0, unique = 0, multiple = 0;

            try (BufferedWriter bw = Files.newBufferedWriter(outCsv)) {
                // Write header row for comparison file
                bw.write(String.join(",", Arrays.asList(
                        "Ext Recording Name","Track Id","Track Id Java","Square Nr","Nr Spots","Nr Gaps","Longest Gap",
                        "Track Duration","Track Displacement","Track Max Speed","Track Median Speed","Total Distance",
                        "Track X Location","Track Y Location","Confinement Ratio","Matches Found"
                )));
                bw.newLine();

                // Process per recording mapping
                for (Map.Entry<String, String> entry : recMapping.entrySet()) {
                    String oldRec = entry.getKey();
                    String newRec = entry.getValue();
                    List<Map<String, String>> oldSubset = oldByRec.get(oldRec);
                    List<Map<String, String>> newSubset = newByRec.get(newRec);
                    if (oldSubset == null || newSubset == null) continue;

                    System.out.printf("→ Processing %s (%d old, %d new)%n", oldRec, oldSubset.size(), newSubset.size());
                    for (int i = 0; i < oldSubset.size(); i++) {
                        Map<String, String> old = oldSubset.get(i);
                        total++;

                        List<Map<String, String>> matches = findMatches(cfg, old, newSubset, usedNewIds);
                        int count = matches.size();

                        if (count == 1) {
                            matched++; unique++;
                            usedNewIds.add(matches.get(0).get("Track Id"));
                            perfectIds.add(old.get("Track Id"));
                        } else if (count > 1) {
                            matched++; multiple++;
                            multipleMatches.addAll(matches);
                            for (Map<String, String> m : matches)
                                multipleMatchIds.add(m.get("Track Id"));
                            unmatched.add(old);
                            unmatchedIds.add(old.get("Track Id"));
                        } else {
                            unmatched.add(old);
                            unmatchedIds.add(old.get("Track Id"));
                        }

                        Map<String, String> match = (count == 1) ? matches.get(0) : null;
                        List<String> row = new ArrayList<>();
                        row.add(escapeCsv(old.get("Ext Recording Name")));
                        row.add(escapeCsv(old.get("Track Id")));
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
                        row.add(String.valueOf(count));
                        bw.write(String.join(",", row));
                        bw.newLine();

                        // 👇 periodic progress reporting every 1000 tracks
                        if ((i + 1) % 1000 == 0) {
                            System.out.printf("   ...processed %,d / %,d tracks in %s%n",
                                              i + 1, oldSubset.size(), oldRec);
                        }
                    }
                    System.out.printf("   ✓ Finished %s%n", oldRec);
                }
            }

            // Phase 1 summary
            System.out.println("\n=== Phase 1 Summary ===");
            System.out.printf("Total old tracks: %,d%n", total);
            System.out.printf("Matched (≥1):     %,d%n", matched);
            System.out.printf("Unique matches:   %,d%n", unique);
            System.out.printf("Multiple matches: %,d%n", multiple);
            System.out.printf("Unmatched:        %,d%n", unmatched.size());
            System.out.println("📄 Written: " + outCsv.toAbsolutePath());

            // === Step 4: Phase 2 — diagnostic scoring for remaining unmatched ===
            if (!unmatched.isEmpty()) {
                System.out.println("\nRunning diagnostics for unmatched tracks...");
                try (BufferedWriter bw = Files.newBufferedWriter(diagCsv)) {
                    bw.write(String.join(",", Arrays.asList(
                            "Recording","Old Track ID","Best Match ID","Duration Δ","Displacement Δ","Max Speed Δ",
                            "Median Speed Δ","Total Distance Δ","X Δ","Y Δ","Confinement Δ","Total Score"
                    )));
                    bw.newLine();

                    Map<String, List<Map<String, String>>> unmatchedByRec = groupBy(unmatched, "Ext Recording Name");
                    for (Map.Entry<String, List<Map<String, String>>> e : unmatchedByRec.entrySet()) {
                        String oldRec = e.getKey();
                        String normOld = normalizeRecordingName(oldRec);
                        String newRec = findClosestRecording(normOld, newByRec.keySet());
                        if (newRec == null) continue;

                        List<Map<String, String>> candidates = new ArrayList<>();
                        for (Map<String, String> cand : newByRec.get(newRec)) {
                            if (multipleMatchIds.contains(cand.get("Track Id")))
                                candidates.add(cand);
                        }

                        for (Map<String, String> old : e.getValue()) {
                            Map<String, Object> best = findBestCandidate(old, candidates);
                            bw.write(String.join(",", Arrays.asList(
                                    escapeCsv(oldRec),
                                    escapeCsv(old.getOrDefault("Track Id", "")),
                                    escapeCsv((String) best.get("BestId")),
                                    fmt(best.get("durD")), fmt(best.get("dispD")), fmt(best.get("maxD")),
                                    fmt(best.get("medD")), fmt(best.get("distD")),
                                    fmt(best.get("xD")), fmt(best.get("yD")), fmt(best.get("confD")),
                                    fmt(best.get("score"))
                            )));
                            bw.newLine();
                        }
                    }
                }
                System.out.println("📊 Diagnostics written: " + diagCsv.toAbsolutePath());

            }

            // 🔹 Ensure diagnostic file is fully flushed before optimization
            try {
                Thread.sleep(200); // short wait for filesystem sync
            } catch (InterruptedException ignored) {}

            // === Step 4b: Iterative tolerance optimization (impact analysis) ===
            Path tolCsv = validatePath.resolve("Tracks Validation - Tolerance Optimization.csv");
            if (Files.exists(diagCsv)) {
                System.out.println("\n🔬 Running tolerance impact analysis (5% baseline → tighter thresholds)...");
                optimizeTolerances(diagCsv, tolCsv);
                System.out.println("📈 Tolerance optimization summary written: " + tolCsv.toAbsolutePath());
            } else {
                System.out.println("ℹ️ Skipping tolerance optimization — no diagnostic file found.");
            }


            // === Step 5: Global summary ===
            summarize(perfectIds, reasonableIds, unmatchedIds, diagCsv, summaryCsv, total);

            System.out.println("\n✅ All done!");
            System.out.println("📄 Summary: " + summaryCsv.toAbsolutePath());
            System.out.println("📄 Normalized old: " + oldNormCsv.toAbsolutePath());
            System.out.println("📄 Normalized new: " + newNormCsv.toAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------------
    /** Strip threshold suffix ("-threshold-N") from recording name. */
    private static String normalizeRecordingName(String name) {
        if (name == null) return "";
        name = name.trim();
        int idx = name.indexOf("-threshold");
        if (idx > 0) name = name.substring(0, idx);
        return name;
    }

    /** Find best matching recording name among candidates (ignoring threshold). */
    private static String findClosestRecording(String oldRecNorm, Set<String> candidates) {
        for (String cand : candidates) {
            String normCand = normalizeRecordingName(cand);
            if (oldRecNorm.equalsIgnoreCase(normCand)
                    || oldRecNorm.startsWith(normCand)
                    || normCand.startsWith(oldRecNorm))
                return cand;
        }
        return null;
    }

    // ---------------------------------------------------------------------
    /** Standard column order used in normalized CSVs. */
    private static final List<String> COMPARE_COLUMNS = Arrays.asList(
            "Recording Name",
            "Track Id",
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
            "Confinement Ratio"
    );

    /** Mappings from alternative column names → standard column names. */
    private static final Map<String, String> COLUMN_MAP = new HashMap<>();
    static {
        COLUMN_MAP.put("Square Number", "Square Nr");
        COLUMN_MAP.put("Number of Spots", "Nr Spots");
        COLUMN_MAP.put("Number of Gaps", "Nr Gaps");
    }

    // ---------------------------------------------------------------------
    /**
     * Normalize column names, recording names, and track IDs.
     * Then sort by Recording Name + Track Id.
     */
    private static List<Map<String, String>> normalizeAndSort(List<Map<String, String>> rows,
                                                              String recordingColumn) {
        if (rows.isEmpty()) return rows;

        for (Map<String, String> row : rows) {
            // unify recording name
            String recName = row.getOrDefault(recordingColumn, "").trim().replace('\u00A0', ' ');
            int idx = recName.toLowerCase(Locale.ROOT).indexOf("-threshold");
            if (idx > 0) recName = recName.substring(0, idx);
            row.put("Recording Name", recName);

            // normalize alternate column names
            for (Map.Entry<String, String> e : COLUMN_MAP.entrySet()) {
                if (row.containsKey(e.getKey()) && !row.containsKey(e.getValue())) {
                    row.put(e.getValue(), row.get(e.getKey()));
                }
            }

            // ensure Track Id field exists
            String id = row.getOrDefault("Track Id", "").trim();
            row.put("Track Id", id);
        }

        // sort deterministically
        rows.sort((a, b) -> {
            String ra = a.getOrDefault("Recording Name", "").toLowerCase(Locale.ROOT);
            String rb = b.getOrDefault("Recording Name", "").toLowerCase(Locale.ROOT);
            int cmp = ra.compareTo(rb);
            if (cmp != 0) return cmp;
            double ia = parseDoubleSafe(a.getOrDefault("Track Id", ""));
            double ib = parseDoubleSafe(b.getOrDefault("Track Id", ""));
            if (Double.isFinite(ia) && Double.isFinite(ib))
                return Double.compare(ia, ib);
            return a.getOrDefault("Track Id", "").compareTo(b.getOrDefault("Track Id", ""));
        });

        System.out.println("✅ Normalized + sorted by Recording Name + Track Id");
        return rows;
    }

    // ---------------------------------------------------------------------
    /** Write normalized CSV to disk for verification and inspection. */
    private static void writeNormalizedCsv(List<Map<String, String>> sortedRows, Path out) throws IOException {
        if (sortedRows.isEmpty()) return;

        List<String> headers = new ArrayList<>(COMPARE_COLUMNS);

        try (BufferedWriter bw = Files.newBufferedWriter(out)) {
            bw.write(String.join(",", headers));
            bw.newLine();

            for (Map<String, String> row : sortedRows) {
                List<String> vals = new ArrayList<>();
                for (String h : headers)
                    vals.add(escapeCsv(row.getOrDefault(h, "")));
                bw.write(String.join(",", vals));
                bw.newLine();
            }
        }

        System.out.println("📄 Normalized harmonized file written: " + out.getFileName());
        System.out.println("  → Columns: " + headers);

        // preview first few entries
        for (int i = 0; i < Math.min(3, sortedRows.size()); i++) {
            Map<String, String> r = sortedRows.get(i);
            System.out.printf("    %s | %s%n",
                              r.getOrDefault("Recording Name", ""), r.getOrDefault("Track Id", ""));
        }
    }

    // --- Matching + metrics ---
    private static List<Map<String, String>> findMatches(MatchConfig cfg, Map<String, String> old,
                                                         List<Map<String, String>> candidates, Set<String> used) {
        List<Map<String, String>> matches = new ArrayList<>();

        // Extract numerical fields from "old" track
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

        // Iterate over all potential candidates in the same recording
        for (Map<String, String> cand : candidates) {
            String newId = cand.getOrDefault("Track Id", "");
            if (used.contains(newId)) continue;  // Skip if already assigned

            // Compare integer-like columns exactly
            int squareNew   = parseIntSafe(cand.get("Square Number"));
            int nSpotsNew   = parseIntSafe(cand.get("Number of Spots"));
            int nGapsNew    = parseIntSafe(cand.get("Number of Gaps"));
            int longestNew  = parseIntSafe(cand.get("Longest Gap"));
            if (square != squareNew || nSpots != nSpotsNew || nGaps != nGapsNew || longest != longestNew)
                continue; // Not comparable structure

            // Compare numeric columns within tolerance
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
        return matches;
    }

    // ---------------------------------------------------------------------
    /**
     * Compute the best possible match (lowest RMS score) between an unmatched
     * "old" track and a set of "new" candidates, based on relative deviation
     * weighted by field tolerances.
     *
     * @param old        reference track
     * @param candidates candidate new tracks
     * @return map containing best candidate ID, per-field deviations, and score
     */
    private static Map<String, Object> findBestCandidate(
            Map<String, String> old, List<Map<String, String>> candidates) {

        double bestScore = Double.POSITIVE_INFINITY;
        Map<String, String> bestCand = null;
        double durD=0, dispD=0, maxD=0, medD=0, distD=0, xD=0, yD=0, confD=0;

        for (Map<String, String> cand : candidates) {
            // Normalized differences divided by their respective tolerance
            double dur  = diff(old, cand, "Track Duration", DURATION_TOLERANCE);
            double disp = diff(old, cand, "Track Displacement", DISPLACEMENT_TOLERANCE);
            double max  = diff(old, cand, "Track Max Speed", SPEED_TOLERANCE);
            double med  = diff(old, cand, "Track Median Speed", SPEED_TOLERANCE);
            double dist = diff(old, cand, "Total Distance", DIST_TOLERANCE);
            double x    = diff(old, cand, "Track X Location", XY_TOLERANCE);
            double y    = diff(old, cand, "Track Y Location", XY_TOLERANCE);
            double conf = diff(old, cand, "Confinement Ratio", CONFINEMENT_TOLERANCE);

            // RMS score across all differences
            double score = Math.sqrt((dur*dur + disp*disp + max*max + med*med +
                    dist*dist + x*x + y*y + conf*conf) / 8.0);

            if (Double.isFinite(score) && score < bestScore && score <= MAX_ACCEPTABLE_SCORE) {
                bestScore = score;
                bestCand = cand;
                durD=dur; dispD=disp; maxD=max; medD=med; distD=dist; xD=x; yD=y; confD=conf;
            }
        }

        // Prepare result summary
        Map<String, Object> out = new LinkedHashMap<>();
        if (bestCand != null) {
            out.put("BestId", bestCand.get("Track Id"));
            out.put("score", String.format(Locale.US, "%.3f", bestScore));
        } else {
            out.put("BestId", "-");
            out.put("score", "No acceptable match");
        }
        out.put("durD", durD);
        out.put("dispD", dispD);
        out.put("maxD", maxD);
        out.put("medD", medD);
        out.put("distD", distD);
        out.put("xD", xD);
        out.put("yD", yD);
        out.put("confD", confD);
        return out;
    }

    // ---------------------------------------------------------------------
    /**
     * Generate global summary CSV after all comparison phases.
     *
     * @param perfectIds     list of perfect matches (phase 1)
     * @param reasonableIds  list of reasonable matches (phase 2)
     * @param unmatchedIds   list of unmatched IDs
     * @param diagCsv        path to diagnostic CSV
     * @param summaryCsv     output summary file
     * @param total          total number of old tracks
     */
    private static void summarize(List<String> perfectIds, List<String> reasonableIds,
                                  List<String> unmatchedIds, Path diagCsv,
                                  Path summaryCsv, int total) throws IOException {

        int phase1Perfect = perfectIds.size();
        int phase2Reasonable = 0;

        // Scan diagnostic file for acceptable RMS scores
        if (Files.exists(diagCsv)) {
            try (BufferedReader br = Files.newBufferedReader(diagCsv)) {
                String header = br.readLine();
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",", -1);
                    if (parts.length < 12) continue;
                    String oldId = parts[1].trim();
                    String bestId = parts[2].trim();
                    String scoreStr = parts[11].trim();
                    if (!bestId.equals("-") && !scoreStr.startsWith("No")) {
                        try {
                            double score = Double.parseDouble(scoreStr);
                            if (score <= MAX_ACCEPTABLE_SCORE) {
                                reasonableIds.add(oldId);
                                unmatchedIds.remove(oldId);
                                phase2Reasonable++;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        // Print to console
        System.out.println("\n===== GLOBAL SUMMARY =====");
        System.out.printf("Total old tracks:                %,d%n", total);
        System.out.printf("Perfect matches (Phase 1):       %,d%n", phase1Perfect);
        System.out.printf("Reasonable matches (Phase 2 ≤ %.1f): %,d%n",
                          MAX_ACCEPTABLE_SCORE, phase2Reasonable);
        System.out.printf("Still unmatched:                 %,d%n", unmatchedIds.size());
        System.out.println("=========================================");

        // Write summary CSV with category per Track ID
        try (BufferedWriter bw = Files.newBufferedWriter(summaryCsv)) {
            bw.write("Category,Track ID");
            bw.newLine();
            for (String id : perfectIds)   bw.write("Perfect," + id + "\n");
            for (String id : reasonableIds) bw.write("Reasonable," + id + "\n");
            for (String id : unmatchedIds)  bw.write("Unmatched," + id + "\n");
        }
        System.out.println("📄 Detailed ID list written: " + summaryCsv.toAbsolutePath());
    }

    // ---------------------------------------------------------------------
    /** Compute normalized difference divided by tolerance (used in RMS scoring). */
    private static double diff(Map<String, String> a, Map<String, String> b, String field, double tol) {
        double va = parseDoubleSafe(a.get(field));
        double vb = parseDoubleSafe(b.get(field));
        if (Double.isNaN(va) || Double.isNaN(vb)) return Double.POSITIVE_INFINITY;
        return Math.abs(va - vb) / tol;
    }

    /** Safe numeric parsing — returns NaN for empty or invalid input. */
    private static double parseDoubleSafe(String s) {
        if (s == null || s.isEmpty()) return Double.NaN;
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return Double.NaN; }
    }

    /** Safe integer parsing with fallback to -1. */
    private static int parseIntSafe(String s) {
        if (s == null || s.isEmpty()) return -1;
        try { return (int) Math.round(Double.parseDouble(s.trim())); }
        catch (Exception e) { return -1; }
    }

    // ---------------------------------------------------------------------
    /**
     * Generic CSV reader returning list of maps (columnName → value).
     * No quoting is processed beyond trimming — suitable for standard CSVs.
     */
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
        System.out.println("Loaded " + rows.size() + " rows from " + path.getFileName());
        return rows;
    }

    /** Escape CSV fields containing commas or quotes. */
    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) {
            s = s.replace("\"", "\"\"");
            return "\"" + s + "\"";
        }
        return s;
    }

    /** Group list of maps by the value in a given column. */
    private static Map<String, List<Map<String, String>>> groupBy(List<Map<String, String>> rows, String col) {
        Map<String, List<Map<String, String>>> grouped = new LinkedHashMap<>();
        for (Map<String, String> r : rows) {
            String key = r.getOrDefault(col, "").trim();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }
        return grouped;
    }

    /** Format numbers to 3 decimals (US locale) for diagnostic CSVs. */
    private static String fmt(Object o) {
        if (o == null) return "";
        if (o instanceof Double) return String.format(Locale.US, "%.3f", (Double) o);
        return o.toString();
    }

    /** Evaluate how much count changes when tightening per-field tolerance thresholds. */
    private static void optimizeTolerances(Path diagCsv, Path outCsv) throws IOException {
        if (!Files.exists(diagCsv)) {
            System.out.println("⚠️ No diagnostics available for optimization step.");
            return;
        }

        Map<String, List<Double>> deviationsByField = new LinkedHashMap<>();

        try (BufferedReader br = Files.newBufferedReader(diagCsv)) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length < 12) continue;
                deviationsByField.computeIfAbsent("Duration Δ", k -> new ArrayList<>()).add(parseDoubleSafe(parts[3]));
                deviationsByField.computeIfAbsent("Displacement Δ", k -> new ArrayList<>()).add(parseDoubleSafe(parts[4]));
                deviationsByField.computeIfAbsent("Max Speed Δ", k -> new ArrayList<>()).add(parseDoubleSafe(parts[5]));
                deviationsByField.computeIfAbsent("Median Speed Δ", k -> new ArrayList<>()).add(parseDoubleSafe(parts[6]));
                deviationsByField.computeIfAbsent("Total Distance Δ", k -> new ArrayList<>()).add(parseDoubleSafe(parts[7]));
                deviationsByField.computeIfAbsent("X Δ", k -> new ArrayList<>()).add(parseDoubleSafe(parts[8]));
                deviationsByField.computeIfAbsent("Y Δ", k -> new ArrayList<>()).add(parseDoubleSafe(parts[9]));
                deviationsByField.computeIfAbsent("Confinement Δ", k -> new ArrayList<>()).add(parseDoubleSafe(parts[10]));
            }
        }

        double[] testLevels = {5.0, 4.0, 3.0, 2.0, 1.0, 0.5};
        double targetKeep = 98.0; // percent of matches to retain

        try (PrintWriter pw = new PrintWriter(outCsv.toFile())) {
            pw.println("Field,Total,Equal@5%,OptimalTolerance(%),Equal@Optimal(%)");

            for (Map.Entry<String, List<Double>> e : deviationsByField.entrySet()) {
                String field = e.getKey();
                List<Double> devs = e.getValue();
                devs.removeIf(d -> !Double.isFinite(d));
                if (devs.isEmpty()) continue;
                int total = devs.size();

                double equalAt5 = percentWithin(devs, 5.0);
                double bestTol = 5.0;
                double bestKeep = equalAt5;

                for (double tol : testLevels) {
                    double keep = percentWithin(devs, tol);
                    if (keep < targetKeep) break;
                    bestTol = tol;
                    bestKeep = keep;
                }

                pw.printf(Locale.US, "%s,%d,%.2f,%.2f,%.2f%n",
                          field, total, equalAt5, bestTol, bestKeep);
            }
        }
    }

    /** Helper: compute fraction of deviations within tolerance. */
    private static double percentWithin(List<Double> devs, double tol) {
        long ok = devs.stream().filter(d -> Math.abs(d) <= tol).count();
        return 100.0 * ok / devs.size();
    }
}