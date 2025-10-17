package validation;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Diagnostic build of CompareAllTracksCSV
 * ---------------------------------------
 * Adds per-recording timing, per-track best-score printouts,
 * and sanity checks for candidate mapping + thresholds.
 */
public class CompareAllTracksCSV {

    private static final class MatchConfig {
        boolean useDuration = true;
        boolean useDisplacement = true;
        boolean useSpeed = true;
        boolean useDistance = true;
        boolean useXY = true;
        boolean useConfinement = true;
    }

    // --- tolerances and limits ---
    private static final double XY_TOLERANCE           = 0.5;
    private static final double DURATION_TOLERANCE     = 0.1;
    private static final double SPEED_TOLERANCE        = 0.5;
    private static final double DISPLACEMENT_TOLERANCE = 0.5;
    private static final double DIST_TOLERANCE         = 0.5;
    private static final double CONFINEMENT_TOLERANCE  = 0.5;
    private static final double MAX_ACCEPTABLE_SCORE   = 40;

    public static void main(String[] args) {
        Path oldCsv  = Paths.get("/Users/hans/Paint Test Project/221012 - Python/All Tracks.csv");
        Path newCsv  = Paths.get("/Users/hans/Paint Test Project/221012/All Tracks Java.csv");
        Path outCsv  = Paths.get("/Users/hans/Desktop/All Tracks - Comparison.csv");
        Path diagCsv = Paths.get("/Users/hans/Desktop/All Tracks - Comparison-Diagnostics.csv");
        Path summaryCsv = Paths.get("/Users/hans/Desktop/All Tracks - Comparison-Summary.csv");

        MatchConfig cfg = new MatchConfig();

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

            // --- Create normalized minimal versions of the old/new input files ---
            Path oldNormCsv = Paths.get("/Users/hans/Desktop/All Tracks - Old Normalized.csv");
            Path newNormCsv = Paths.get("/Users/hans/Desktop/All Tracks - New Normalized.csv");

            System.out.println("Creating normalized comparison extracts...");

            List<String> fields = Arrays.asList(
                    "Ext Recording Name","Track Id","Square Nr","Nr Spots","Nr Gaps","Longest Gap",
                    "Track Duration","Track Displacement","Track Max Speed","Track Median Speed",
                    "Total Distance","Track X Location","Track Y Location","Confinement Ratio"
            );

            List<String> fieldsNew = Arrays.asList(
                    "Recording Name","Track Id","Square Number","Number of Spots","Number of Gaps","Longest Gap",
                    "Track Duration","Track Displacement","Track Max Speed","Track Median Speed",
                    "Total Distance","Track X Location","Track Y Location","Confinement Ratio"
            );

            try (BufferedWriter bwOld = Files.newBufferedWriter(oldNormCsv);
                 BufferedWriter bwNew = Files.newBufferedWriter(newNormCsv)) {

                bwOld.write(String.join(",", fields));
                bwOld.newLine();
                for (Map<String, String> r : oldRows) {
                    List<String> vals = new ArrayList<>();
                    for (String f : fields) vals.add(escapeCsv(r.getOrDefault(f, "")));
                    bwOld.write(String.join(",", vals));
                    bwOld.newLine();
                }

                bwNew.write(String.join(",", fieldsNew));
                bwNew.newLine();
                for (Map<String, String> r : newRows) {
                    List<String> vals = new ArrayList<>();
                    for (String f : fieldsNew) vals.add(escapeCsv(r.getOrDefault(f, "")));
                    bwNew.write(String.join(",", vals));
                    bwNew.newLine();
                }
            }

            System.out.println("📄 Wrote compact input extracts:");
            System.out.println("    OLD: " + oldNormCsv.toAbsolutePath());
            System.out.println("    NEW: " + newNormCsv.toAbsolutePath());
            System.out.println();

            Map<String, List<Map<String, String>>> oldByRec = groupBy(oldRows, "Ext Recording Name");
            Map<String, List<Map<String, String>>> newByRec = groupBy(newRows, "Recording Name");

            Map<String, String> recMapping = new LinkedHashMap<>();
            for (String oldRec : oldByRec.keySet()) {
                String oldNorm = normalizeRecordingName(oldRec);
                String match = findClosestRecording(oldNorm, newByRec.keySet());
                if (match != null) recMapping.put(oldRec, match);
            }

            if (recMapping.isEmpty()) {
                System.err.println("❌ No recording mappings found.");
                return;
            }

            System.out.println("Recording mappings (threshold-insensitive):");
            for (Map.Entry<String, String> e : recMapping.entrySet()) {
                String oldRec = e.getKey();
                String newRec = e.getValue();
                int oldCount = oldByRec.get(oldRec).size();
                int newCount = newByRec.get(newRec).size();
                System.out.printf("  %-40s → %-40s (%4d old | %4d new)%n", oldRec, newRec, oldCount, newCount);
            }
            System.out.println();

            Set<String> usedNewIds = new HashSet<>();
            List<Map<String, String>> unmatched = new ArrayList<>();
            List<Map<String, String>> multipleMatches = new ArrayList<>();
            Set<String> multipleMatchIds = new HashSet<>();

            List<String> perfectIds = new ArrayList<>();
            List<String> reasonableIds = new ArrayList<>();
            List<String> unmatchedIds = new ArrayList<>();

            int total = 0, matched = 0, unique = 0, multiple = 0;

            try (BufferedWriter bw = Files.newBufferedWriter(outCsv)) {
                bw.write(String.join(",", Arrays.asList(
                        "Ext Recording Name","Track Id","Track Id Java","Square Nr","Nr Spots","Nr Gaps","Longest Gap",
                        "Track Duration","Track Displacement","Track Max Speed","Track Median Speed","Total Distance",
                        "Track X Location","Track Y Location","Confinement Ratio","Matches Found","Best Score"
                )));
                bw.newLine();

                for (Map.Entry<String, String> entry : recMapping.entrySet()) {
                    String oldRec = entry.getKey();
                    String newRec = entry.getValue();
                    List<Map<String, String>> oldSubset = oldByRec.get(oldRec);
                    List<Map<String, String>> newSubset = newByRec.get(newRec);
                    if (oldSubset == null || newSubset == null) continue;

                    long start = System.currentTimeMillis();
                    System.out.printf("▶ Processing %s ...%n", oldRec);

                    for (int i = 0; i < oldSubset.size(); i++) {
                        Map<String, String> old = oldSubset.get(i);
                        total++;

                        List<Map<String, String>> matches = findMatches(cfg, old, newSubset, usedNewIds);
                        int count = matches.size();
                        String bestId = (count >= 1) ? matches.get(0).get("Track Id") : "-";
                        String bestScoreStr = (matches.isEmpty()) ? "NA" : matches.get(0).getOrDefault("_bestScore", "NA");

                        if (count == 1) {
                            matched++; unique++;
                            usedNewIds.add(bestId);
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

                        List<String> row = new ArrayList<>();
                        row.add(escapeCsv(oldRec));
                        row.add(escapeCsv(old.get("Track Id")));
                        row.add(bestId);
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
                        row.add(bestScoreStr);
                        bw.write(String.join(",", row));
                        bw.newLine();

                        // Diagnostic console output for first few hundred only
                        if (i < 20 || i % 500 == 0) {
                            System.out.printf("  Track %s → best %s (score=%s)%n",
                                              old.get("Track Id"), bestId, bestScoreStr);
                        }
                    }

                    long dur = System.currentTimeMillis() - start;
                    System.out.printf("⏱  %s finished in %.2f s%n%n", oldRec, dur / 1000.0);
                }
            }

            System.out.println("📄 AllTracksComparison.csv written to: " + outCsv.toAbsolutePath());
            System.out.printf("Phase 1 summary: total=%d  matched=%d  unique=%d  multiple=%d  unmatched=%d%n%n",
                              total, matched, unique, multiple, unmatched.size());

            // --- phase 2 diagnostics (same recording restriction) ---
            if (!unmatched.isEmpty()) {
                System.out.println("Running phase 2 diagnostics per recording...");
                runDiagnosticsPerRecording(unmatched, newByRec, multipleMatchIds, diagCsv);
            }

            summarize(perfectIds, reasonableIds, unmatchedIds, diagCsv, summaryCsv, total);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------------
    private static String normalizeRecordingName(String name) {
        if (name == null) return "";
        int idx = name.indexOf("-threshold");
        if (idx > 0) name = name.substring(0, idx);
        return name.trim();
    }

    private static String findClosestRecording(String oldRecNorm, Set<String> candidates) {
        for (String cand : candidates) {
            String norm = normalizeRecordingName(cand);
            if (oldRecNorm.equalsIgnoreCase(norm) || oldRecNorm.startsWith(norm) || norm.startsWith(oldRecNorm))
                return cand;
        }
        return null;
    }

    // ---------------------------------------------------------------------
    private static List<Map<String, String>> findMatches(MatchConfig cfg,
                                                         Map<String, String> old,
                                                         List<Map<String, String>> candidates,
                                                         Set<String> used) {
        double bestScore = Double.POSITIVE_INFINITY;
        Map<String, String> bestCand = null;

        double durOld  = parseDoubleSafe(old.get("Track Duration"));
        double dispOld = parseDoubleSafe(old.get("Track Displacement"));
        double maxOld  = parseDoubleSafe(old.get("Track Max Speed"));
        double medOld  = parseDoubleSafe(old.get("Track Median Speed"));
        double distOld = parseDoubleSafe(old.get("Total Distance"));
        double xOld    = parseDoubleSafe(old.get("Track X Location"));
        double yOld    = parseDoubleSafe(old.get("Track Y Location"));
        double confOld = parseDoubleSafe(old.get("Confinement Ratio"));

        for (Map<String, String> cand : candidates) {
            String newId = cand.getOrDefault("Track Id", "");
            // comment this line if you suspect over-filtering:
            // if (used.contains(newId)) continue;

            double durNew  = parseDoubleSafe(cand.get("Track Duration"));
            double dispNew = parseDoubleSafe(cand.get("Track Displacement"));
            double maxNew  = parseDoubleSafe(cand.get("Track Max Speed"));
            double medNew  = parseDoubleSafe(cand.get("Track Median Speed"));
            double distNew = parseDoubleSafe(cand.get("Total Distance"));
            double xNew    = parseDoubleSafe(cand.get("Track X Location"));
            double yNew    = parseDoubleSafe(cand.get("Track Y Location"));
            double confNew = parseDoubleSafe(cand.get("Confinement Ratio"));

            double durD  = Math.abs(durOld  - durNew)  / DURATION_TOLERANCE;
            double dispD = Math.abs(dispOld - dispNew) / DISPLACEMENT_TOLERANCE;
            double maxD  = Math.abs(maxOld  - maxNew)  / SPEED_TOLERANCE;
            double medD  = Math.abs(medOld  - medNew)  / SPEED_TOLERANCE;
            double distD = Math.abs(distOld - distNew) / DIST_TOLERANCE;
            double xD    = Math.abs(xOld    - xNew)    / XY_TOLERANCE;
            double yD    = Math.abs(yOld    - yNew)    / XY_TOLERANCE;
            double confD = Math.abs(confOld - confNew) / CONFINEMENT_TOLERANCE;

            double score = Math.sqrt((durD*durD + dispD*dispD + maxD*maxD + medD*medD +
                    distD*distD + xD*xD + yD*yD + confD*confD) / 8.0);

            if (Double.isFinite(score) && score < bestScore && score <= MAX_ACCEPTABLE_SCORE) {
                bestScore = score;
                bestCand = cand;
            }
        }

        List<Map<String, String>> result = new ArrayList<>();
        if (bestCand != null) {
            bestCand.put("_bestScore", String.format(Locale.US, "%.3f", bestScore));
            result.add(bestCand);
        }
        return result;
    }

    // ---------------------------------------------------------------------
    private static void runDiagnosticsPerRecording(List<Map<String, String>> unmatched,
                                                   Map<String, List<Map<String, String>>> newByRec,
                                                   Set<String> multipleMatchIds,
                                                   Path diagCsv) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(diagCsv)) {
            bw.write(String.join(",", Arrays.asList(
                    "Recording","Old Track ID","Best Match ID","Total Score"
            )));
            bw.newLine();

            Map<String, List<Map<String, String>>> unmatchedByRec = groupBy(unmatched, "Ext Recording Name");
            for (String oldRec : unmatchedByRec.keySet()) {
                String newRec = findClosestRecording(normalizeRecordingName(oldRec), newByRec.keySet());
                if (newRec == null) continue;
                List<Map<String, String>> candidates = new ArrayList<>();
                for (Map<String, String> cand : newByRec.get(newRec))
                    if (multipleMatchIds.contains(cand.get("Track Id")))
                        candidates.add(cand);

                for (Map<String, String> old : unmatchedByRec.get(oldRec)) {
                    Map<String, Object> best = findBestCandidate(old, candidates);
                    bw.write(String.join(",", Arrays.asList(
                            escapeCsv(oldRec),
                            escapeCsv(old.get("Track Id")),
                            escapeCsv((String) best.get("BestId")),
                            fmt(best.get("score"))
                    )));
                    bw.newLine();
                }
            }
        }
        System.out.println("📊 Phase 2 diagnostics written to: " + diagCsv.toAbsolutePath());
    }

    // ---------------------------------------------------------------------
    private static Map<String, Object> findBestCandidate(Map<String, String> old, List<Map<String, String>> candidates) {
        double bestScore = Double.POSITIVE_INFINITY;
        Map<String, String> bestCand = null;

        for (Map<String, String> cand : candidates) {
            double score = diffScore(old, cand);
            if (score < bestScore && score <= MAX_ACCEPTABLE_SCORE) {
                bestScore = score;
                bestCand = cand;
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        if (bestCand != null) {
            res.put("BestId", bestCand.get("Track Id"));
            res.put("score", String.format(Locale.US, "%.3f", bestScore));
        } else {
            res.put("BestId", "-");
            res.put("score", "No acceptable match");
        }
        return res;
    }

    private static double diffScore(Map<String, String> a, Map<String, String> b) {
        double[] d = {
                diff(a,b,"Track Duration",DURATION_TOLERANCE),
                diff(a,b,"Track Displacement",DISPLACEMENT_TOLERANCE),
                diff(a,b,"Track Max Speed",SPEED_TOLERANCE),
                diff(a,b,"Track Median Speed",SPEED_TOLERANCE),
                diff(a,b,"Total Distance",DIST_TOLERANCE),
                diff(a,b,"Track X Location",XY_TOLERANCE),
                diff(a,b,"Track Y Location",XY_TOLERANCE),
                diff(a,b,"Confinement Ratio",CONFINEMENT_TOLERANCE)
        };
        double sum=0;
        for(double v:d) sum+=v*v;
        return Math.sqrt(sum/8.0);
    }

    private static double diff(Map<String, String> a, Map<String, String> b, String f, double tol) {
        double va=parseDoubleSafe(a.get(f)), vb=parseDoubleSafe(b.get(f));
        if(Double.isNaN(va)||Double.isNaN(vb)) return Double.POSITIVE_INFINITY;
        return Math.abs(va-vb)/tol;
    }

    private static void summarize(List<String> perfectIds, List<String> reasonableIds, List<String> unmatchedIds,
                                  Path diagCsv, Path summaryCsv, int total) throws IOException {
        System.out.println("\n===== GLOBAL SUMMARY =====");
        System.out.printf("Total tracks: %d%nPerfect matches: %d%nUnmatched phase 1: %d%n",
                          total, perfectIds.size(), unmatchedIds.size());
        System.out.println("==========================");
        try(BufferedWriter bw=Files.newBufferedWriter(summaryCsv)){
            bw.write("Category,Track ID\n");
            for(String id:perfectIds)bw.write("Perfect,"+id+"\n");
            for(String id:unmatchedIds)bw.write("Unmatched,"+id+"\n");
        }
        System.out.println("📄 Summary written to: " + summaryCsv.toAbsolutePath());
    }

    // --- helpers ----------------------------------------------------------
    private static double parseDoubleSafe(String s){
        if(s==null||s.isEmpty())return Double.NaN;
        try{return Double.parseDouble(s.trim());}catch(Exception e){return Double.NaN;}
    }
    private static List<Map<String,String>> readCsv(Path p)throws IOException{
        List<Map<String,String>> rows=new ArrayList<>();
        try(BufferedReader br=Files.newBufferedReader(p)){
            String h=br.readLine();if(h==null)return rows;
            String[] head=h.split(",",-1);String line;
            while((line=br.readLine())!=null){
                String[] parts=line.split(",",-1);
                Map<String,String> m=new LinkedHashMap<>();
                for(int i=0;i<head.length;i++)m.put(head[i].trim(),i<parts.length?parts[i].trim():"");
                rows.add(m);
            }
        }return rows;
    }
    private static Map<String,List<Map<String,String>>> groupBy(List<Map<String,String>> rows,String col){
        Map<String,List<Map<String,String>>> g=new LinkedHashMap<>();
        for(Map<String,String> r:rows){
            String k=r.getOrDefault(col,"").trim();
            g.computeIfAbsent(k,k2->new ArrayList<>()).add(r);
        }
        return g;
    }
    private static String escapeCsv(String s){
        if(s==null)return"";if(s.contains(",")||s.contains("\"")){s=s.replace("\"","\"\"");return"\""+s+"\"";}
        return s;
    }
    private static String fmt(Object o){
        if(o==null) return "";
        if(o instanceof Double) return String.format(Locale.US, "%.3f", (Double)o);
        return o.toString();
    }
}