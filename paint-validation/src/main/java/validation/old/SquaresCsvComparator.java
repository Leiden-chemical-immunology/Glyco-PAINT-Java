package validation.old;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * ============================================================================
 *  SquaresCsvComparator.java
 *  Version: 4.0 — Precision & Tolerance-Aligned Edition
 *  Author: Herr Doctor
 *
 *  PURPOSE
 *  ---------------------------------------------------------------------------
 *  Compare two “All Squares” CSVs (Python vs Java) with:
 *   • Automatic normalization
 *   • Effective precision alignment
 *   • Percentage-based tolerance per numeric field
 *   • “Selected” logic consistency
 *   • Detailed CSV reporting
 *
 *  OUTPUT FILES
 *  ---------------------------------------------------------------------------
 *  ~/Downloads/Validate/Squares/
 *    ├── Squares Validation - Comparison.csv
 *    ├── Squares Validation - Old Normalized.csv
 *    ├── Squares Validation - New Normalized.csv
 *    └── Squares Validation - Selected Overview.csv
 * ============================================================================
 */
public class SquaresCsvComparator {

    // ----------------------------------------------------------------------
    // FIELD DEFINITIONS
    // ----------------------------------------------------------------------

    /** Mapping of old→new column names (for normalization). */
    private static final Map<String, String> FIELD_MAP = new LinkedHashMap<>();
    static {
        FIELD_MAP.put("Row Nr", "Row Number");
        FIELD_MAP.put("Col Nr", "Column Number");
        FIELD_MAP.put("Nr Tracks", "Number of Tracks");
        FIELD_MAP.put("X0", "X0");
        FIELD_MAP.put("Y0", "Y0");
        FIELD_MAP.put("X1", "X1");
        FIELD_MAP.put("Y1", "Y1");
        FIELD_MAP.put("Variability", "Variability");
        FIELD_MAP.put("Density", "Density");
        FIELD_MAP.put("Density Ratio", "Density Ratio Ori");
        FIELD_MAP.put("Tau", "Tau");
        FIELD_MAP.put("R Squared", "R Squared");
        FIELD_MAP.put("Median Diffusion Coefficient", "Median Diffusion Coefficient");
        FIELD_MAP.put("Median Diffusion Coefficient Ext", "Median Diffusion Coefficient Ext");
        FIELD_MAP.put("Median Long Track Duration", "Median Long Track Duration");
        FIELD_MAP.put("Median Short Track Duration", "Median Short Track Duration");
        FIELD_MAP.put("Median Displacement", "Median Displacement");
        FIELD_MAP.put("Max Displacement", "Max Displacement");
        FIELD_MAP.put("Total Displacement", "Total Displacement");
        FIELD_MAP.put("Median Max Speed", "Median Max Speed");
        FIELD_MAP.put("Max Max Speed", "Max Max Speed");
        FIELD_MAP.put("Max Track Duration", "Max Track Duration");
        FIELD_MAP.put("Total Track Duration", "Total Track Duration");
        FIELD_MAP.put("Median Track Duration", "Median Track Duration");
        FIELD_MAP.put("Square Nr", "Square Number");
    }

    /** Numeric fields eligible for tolerance and precision handling. */
    private static final Set<String> NUMERIC_FIELDS = new HashSet<>(Arrays.asList(
            "X0","Y0","X1","Y1","Variability","Density","Density Ratio","Tau","R Squared",
            "Median Diffusion Coefficient","Median Diffusion Coefficient Ext",
            "Median Long Track Duration","Median Short Track Duration",
            "Median Displacement","Max Displacement","Total Displacement",
            "Median Max Speed","Max Max Speed","Max Track Duration",
            "Total Track Duration","Median Track Duration"
    ));

    /** Default rounding precision if no better info is found. */
    private static final Map<String, Integer> ROUNDING_MAP = new HashMap<>();
    static {
        for (String f : NUMERIC_FIELDS) ROUNDING_MAP.put(f, 3);
        ROUNDING_MAP.put("Variability", 1);
        ROUNDING_MAP.put("Tau", 2);
        ROUNDING_MAP.put("Density Ratio", 2);
        ROUNDING_MAP.put("R Squared", 2);
    }

    /** Percentage-based tolerances per field (relative deviation in %). */
    private static final Map<String, Double> TOLERANCE_MAP = new HashMap<>();
    static {

        // @format:off
        TOLERANCE_MAP.put("Variability",                      5.0);
        TOLERANCE_MAP.put("Density",                          5.0);
        TOLERANCE_MAP.put("Density Ratio",                    5.0);
        TOLERANCE_MAP.put("Tau",                              5.0);
        TOLERANCE_MAP.put("R Squared",                        5.0);
        TOLERANCE_MAP.put("Median Diffusion Coefficient",     5.0);
        TOLERANCE_MAP.put("Median Diffusion Coefficient Ext", 5.0);
        TOLERANCE_MAP.put("Median Long Track Duration",       5.0);
        TOLERANCE_MAP.put("Median Short Track Duration",      5.0);
        TOLERANCE_MAP.put("Total Track Duration",             5.0);
        TOLERANCE_MAP.put("Median Displacement",              5.0);
        TOLERANCE_MAP.put("Max Displacement",                 5.0);
        TOLERANCE_MAP.put("Total Displacement",               5.0);
        TOLERANCE_MAP.put("Median Max Speed",                 5.0);
        TOLERANCE_MAP.put("Max Max Speed",                    5.0);
        TOLERANCE_MAP.put("Max Track Duration",               5.0);
        TOLERANCE_MAP.put("Total Track Duration",             5.0);
        TOLERANCE_MAP.put("Median Track Duration",            5.0);
        // @format:on
    }

    /** Computed effective precision per numeric field. */
    private static final Map<String, Integer> EFFECTIVE_PRECISION_MAP = new HashMap<>();

    // ----------------------------------------------------------------------
    // MAIN ENTRY
    // ----------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        Path outDir = Paths.get(System.getProperty("user.home"), "Downloads", "Validate", "Squares");
        Files.createDirectories(outDir);

        Path oldCsv = Paths.get("/Users/hans/Paint Test Project/221012 - Python/All Squares.csv");
        Path newCsv = Paths.get("/Users/hans/Paint Test Project/221012/All Squares Java.csv");

        System.out.println("🔍 Reading CSVs...");
        List<Map<String,String>> oldRows = readCsv(oldCsv);
        List<Map<String,String>> newRows = readCsv(newCsv);
        System.out.printf("   OLD: %d rows%n   NEW: %d rows%n", oldRows.size(), newRows.size());

        System.out.println("🧩 Normalizing...");
        List<Map<String,String>> normOld = normalizeOld(oldRows);
        List<Map<String,String>> normNew = normalizeNew(newRows);

        // 🔹 Compute shared effective precision for all numeric fields
        EFFECTIVE_PRECISION_MAP.clear();
        EFFECTIVE_PRECISION_MAP.putAll(computeEffectivePrecisions(normOld, normNew, NUMERIC_FIELDS));

        System.out.println("🔢 Effective numeric precision (shared):");
        EFFECTIVE_PRECISION_MAP.forEach((k,v)->System.out.printf("   %-35s → %d%n", k,v));

        // 🔹 Write normalized files using shared precision
        Path normOldPath = outDir.resolve("Squares Validation - Old Normalized.csv");
        Path normNewPath = outDir.resolve("Squares Validation - New Normalized.csv");
        System.out.println("💾 Writing normalized outputs (aligned precision)...");
        writeCsv(normOld, normOldPath);
        writeCsv(normNew, normNewPath);
        System.out.println("   → " + normOldPath);
        System.out.println("   → " + normNewPath);

        // 🔹 Continue with comparison
        compare(normOld, normNew, outDir);
        writeSelectedOverview(normOld, normNew, outDir);

        Path comparisonCsv = outDir.resolve("Squares Validation - Comparison.csv");
        Path tolCsv = outDir.resolve("Squares Validation - Tolerance Optimization.csv");

        // 🔹 Ensure comparison file is fully flushed before optimization
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {}

        optimizeTolerances(comparisonCsv, tolCsv);

        System.out.println("\n🎯 All tasks complete.");
    }

    // ----------------------------------------------------------------------
    // CSV I/O
    // ----------------------------------------------------------------------

    private static List<Map<String,String>> readCsv(Path p) throws IOException {
        List<Map<String,String>> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String header = br.readLine();
            if (header == null) return rows;
            String[] h = header.split(",",-1);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",",-1);
                Map<String,String> m = new LinkedHashMap<>();
                for (int i=0;i<h.length;i++) m.put(h[i].trim(), i<parts.length?parts[i].trim():"");
                rows.add(m);
            }
        }
        return rows;
    }

    /** Write normalized CSV using shared precision and zero→empty logic for old file. */
    private static void writeCsv(List<Map<String, String>> rows, Path file) throws IOException {
        if (rows.isEmpty()) return;
        Files.createDirectories(file.getParent());

        boolean isOldFile = file.getFileName().toString().toLowerCase(Locale.ROOT).contains("old");

        try (PrintWriter pw = new PrintWriter(file.toFile())) {
            List<String> header = new ArrayList<>();
            header.add("Recording Name");
            header.addAll(FIELD_MAP.keySet());
            header.add("Selected");
            pw.println(String.join(",", header));

            for (Map<String, String> r : rows) {
                List<String> vals = new ArrayList<>();
                for (String col : header) {
                    String val = r.getOrDefault(col, "");

                    // ---- Handle numeric formatting ----
                    if (NUMERIC_FIELDS.contains(col)) {
                        Double num = parseDouble(val);
                        if (num != null) {
                            // 🔹 OLD FILES: apply "0 → empty" rule for specific fields
                            if (isOldFile && num == 0.0 && Arrays.asList(
                                    "R Squared",
                                    "Median Short Track Duration",
                                    "Median Long Track Duration",
                                    "Total Displacement",
                                    "Total Track Duration"
                            ).contains(col)) {
                                val = "";
                            } else {
                                int prec = EFFECTIVE_PRECISION_MAP.getOrDefault(
                                        col, ROUNDING_MAP.getOrDefault(col, 3));
                                double f = Math.pow(10, prec);
                                double rounded = Math.round(num * f) / f;
                                val = String.format(Locale.US, "%." + prec + "f", rounded);
                            }
                        }
                    }

                    vals.add(escapeCsv(val));
                }
                pw.println(String.join(",", vals));
            }
        }
    }

    private static String escapeCsv(String s) {
        if (s==null) return "";
        if (s.contains(",")||s.contains("\"")) return "\"" + s.replace("\"","\"\"") + "\"";
        return s;
    }

    // ----------------------------------------------------------------------
    // NORMALIZATION + PRECISION
    // ----------------------------------------------------------------------

    public static List<Map<String,String>> normalizeOld(List<Map<String,String>> oldRows){
        List<Map<String,String>> out = new ArrayList<>();
        for (Map<String,String> r: oldRows){
            Map<String,String> n = new LinkedHashMap<>();
            String rec = r.getOrDefault("Ext Recording Name","");
            n.put("Recording Name", rec.replaceAll("-threshold-\\d+$","").trim());
            for (String f: FIELD_MAP.keySet()) {
                String v = r.getOrDefault(f,"").trim();
                if (f.equals("Tau")) {
                    try { if (!v.isEmpty() && Double.parseDouble(v)<0) v=""; } catch(Exception ignored){}
                }
                n.put(f,v);
            }
            adjustIndex(n,"Row Nr");
            adjustIndex(n,"Col Nr");
            n.put("Selected", String.valueOf(isSelected(n)));
            out.add(n);
        }
        return out;
    }

    public static List<Map<String,String>> normalizeNew(List<Map<String,String>> newRows){
        List<Map<String,String>> out = new ArrayList<>();
        for (Map<String,String> r: newRows){
            Map<String,String> n = new LinkedHashMap<>();
            n.put("Recording Name", r.getOrDefault("Recording Name",""));
            for (Map.Entry<String,String> e: FIELD_MAP.entrySet())
                n.put(e.getKey(), r.getOrDefault(e.getValue(),""));
            n.put("Selected", String.valueOf(isSelected(n)));
            out.add(n);
        }
        return out;
    }

    private static void adjustIndex(Map<String,String> m, String k){
        try {
            String v=m.get(k);
            if (v!=null&&!v.isEmpty()) {
                int i=(int)Double.parseDouble(v);
                m.put(k,String.valueOf(i-1));
            }
        } catch(Exception ignored){}
    }

    private static Map<String,Integer> computeEffectivePrecisions(
            List<Map<String,String>> a,List<Map<String,String>> b,Set<String> numeric){
        Map<String,Integer> res=new HashMap<>();
        for (String f:numeric){
            int pa=detectPrecision(a,f);
            int pb=detectPrecision(b,f);
            res.put(f,Math.min(pa,pb));
        }
        return res;
    }

    private static int detectPrecision(List<Map<String,String>> rows,String f){
        int best=0;
        for (Map<String,String> r: rows){
            String s=r.get(f);
            if (s!=null&&s.matches("^-?\\d+\\.\\d+$")){
                int p=s.length()-s.indexOf('.')-1;
                best=Math.max(best,p);
            }
        }
        return best;
    }

    // ----------------------------------------------------------------------
    // COMPARISON
    // ----------------------------------------------------------------------

    private static void compare(List<Map<String,String>> oldN,List<Map<String,String>> newN,Path outDir) throws IOException {
        Path out=outDir.resolve("Squares Validation - Comparison.csv");
        List<String[]> diffs=new ArrayList<>();
        int total=0,diffCount=0;

        Map<String,Map<String,String>> newMap=new HashMap<>();
        for (Map<String,String> n:newN) newMap.put(key(n),n);

        for (Map<String,String> o: oldN){
            total++;
            String k=key(o);
            Map<String,String> n=newMap.get(k);
            if (n==null) continue;

            for (String f : FIELD_MAP.keySet()) {
                String ov = o.getOrDefault(f, "");
                String nv = n.getOrDefault(f, "");

                if (NUMERIC_FIELDS.contains(f)) {
                    Double da = parseDouble(ov);
                    Double db = parseDouble(nv);
                    double dev = relativeDeviation(ov, nv);
                    double tol = TOLERANCE_MAP.getOrDefault(f, 5.0);
                    int prec = EFFECTIVE_PRECISION_MAP.getOrDefault(f, ROUNDING_MAP.get(f));

                    String status;
                    if (da == null && db == null) {
                        status = "BOTH EMPTY";
                    } else if (Double.isNaN(dev)) {
                        status = "NaN";
                    } else if (dev <= tol) {
                        status = "WITHIN " + tol + "%";
                    } else {
                        status = "DIFFERENT";
                        diffCount++;
                    }

                    diffs.add(new String[]{
                            o.get("Recording Name"),
                            o.get("Square Nr"),
                            f,
                            ov,
                            nv,
                            String.valueOf(prec),
                            String.format(Locale.US, "%.3f", dev),
                            status
                    });

                } else if (!Objects.equals(ov, nv)) {
                    diffs.add(new String[]{
                            o.get("Recording Name"),
                            o.get("Square Nr"),
                            f,
                            ov,
                            nv,
                            "",
                            "",
                            "TEXT DIFFERENCE"
                    });
                    diffCount++;
                }
            }
            String sOld=o.get("Selected");
            String sNew=n.get("Selected");
            if (!Objects.equals(sOld,sNew)){
                diffs.add(new String[]{o.get("Recording Name"),o.get("Square Nr"),"Selected",sOld,sNew,"","","DIFFERENT"});
                diffCount++;
            }

            if (total%1000==0) System.out.printf("   ...processed %,d%n",total);
        }

        try(PrintWriter pw=new PrintWriter(out.toFile())){
            pw.println("Recording Name,Square Nr,Field,Old Value,New Value,Precision Used,Relative Diff (%),Status");
            for(String[] r:diffs) pw.println(String.join(",",r));
            pw.println();
            pw.printf("SUMMARY,,,,,,,%nDifferences,%d%n",diffCount);
        }
        System.out.printf("✅ Compared %,d squares — %d differences%n",total,diffCount);
    }

    private static String key(Map<String,String> r){
        return r.getOrDefault("Recording Name","")+" - "+r.getOrDefault("Square Nr","");
    }

    private static boolean numericEqual(String f,String a,String b){
        Double da=parseDouble(a), db=parseDouble(b);
        if (da==null && db==null) return true;
        if (da==null || db==null) return false;
        int prec=EFFECTIVE_PRECISION_MAP.getOrDefault(f,ROUNDING_MAP.getOrDefault(f,3));
        double fac=Math.pow(10,prec);
        return Math.round(da*fac)==Math.round(db*fac);
    }

    private static double relativeDeviation(String a, String b) {
        Double da = parseDouble(a);
        Double db = parseDouble(b);

        // Both missing or equivalent to zero → no deviation
        if ((da == null || da == 0.0) && (db == null || db == 0.0)) return 0.0;

        // One side empty → treat as absolute deviation
        if (da == null || db == null) return 100.0; // arbitrary high deviation

        double absA = Math.abs(da);
        double absDiff = Math.abs(db - da);

        // Avoid division by zero, use absolute diff if old value is near zero
        if (absA < 1e-12) return absDiff * 100.0;

        return absDiff / absA * 100.0;
    }

    // ----------------------------------------------------------------------
    // UTILITIES
    // ----------------------------------------------------------------------

    private static Double parseDouble(String s){
        if (s==null||s.isEmpty()||s.equalsIgnoreCase("NaN")) return null;
        try { double v=Double.parseDouble(s); return Double.isNaN(v)?null:v; }
        catch(Exception e){return null;}
    }

    private static boolean isSelected(Map<String,String> r){
        Double dr=parseDouble(r.get("Density Ratio"));
        Double var=parseDouble(r.get("Variability"));
        Double r2=parseDouble(r.get("R Squared"));
        return dr!=null&&var!=null&&r2!=null&&dr>=2.0&&var<10.0&&r2>0.1;
    }

    private static void writeSelectedOverview(List<Map<String,String>> oldN,List<Map<String,String>> newN,Path outDir) throws IOException {
        Path f=outDir.resolve("Squares Validation - Selected Overview.csv");
        Map<String,Map<String,String>> newMap=new HashMap<>();
        for(Map<String,String> n:newN) newMap.put(key(n),n);

        Set<String> allKeys=new TreeSet<>();
        for(Map<String,String> o:oldN) allKeys.add(key(o));
        for(Map<String,String> n:newN) allKeys.add(key(n));

        try(PrintWriter pw=new PrintWriter(f.toFile())){
            pw.println("Recording Name,Square Nr,Selected(Old),Selected(New),Both,Only Old,Only New,Tau(Old),Tau(New),DensityRatio(Old),DensityRatio(New),Variability(Old),Variability(New)");
            for(String k:allKeys){
                String[] parts=k.split(" - ",2);
                String rec=parts[0], sq=parts.length>1?parts[1]:"";
                Map<String,String> o=oldN.stream().filter(r->key(r).equals(k)).findFirst().orElse(null);
                Map<String,String> n=newMap.get(k);
                boolean so=o!=null&&isSelected(o);
                boolean sn=n!=null&&isSelected(n);
                pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                          rec,sq,so,sn,
                          (so&&sn)?"true":"",(so&&!sn)?"true":"",(!so&&sn)?"true":"",
                          val(o,"Tau"),val(n,"Tau"),val(o,"Density Ratio"),val(n,"Density Ratio"),val(o,"Variability"),val(n,"Variability"));
            }
        }
        System.out.println("📊 Selected overview written: "+f);
    }

    private static String val(Map<String,String> m,String k){
        return m==null?"":m.getOrDefault(k,"");
    }

    /**
     * Evaluate how many values fall within various relative tolerance levels
     * to identify optimal per-field thresholds.
     */
    private static void optimizeTolerances(Path comparisonCsv, Path outCsv) throws IOException {
        if (!Files.exists(comparisonCsv)) {
            System.out.println("⚠️ No comparison file found for tolerance optimization.");
            return;
        }

        System.out.println("🔬 Analyzing tolerance levels across numeric fields...");

        Map<String, List<Double>> diffsByField = new LinkedHashMap<>();

        // --- Read comparison CSV and extract relative differences ---
        try (BufferedReader br = Files.newBufferedReader(comparisonCsv, java.nio.charset.StandardCharsets.UTF_8)) {
            String header = br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("SUMMARY")) continue;
                String[] parts = line.split(",", -1);
                if (parts.length < 7) continue;
                String field = parts[2].trim();
                String rel = parts[6].trim();
                if (rel.isEmpty() || rel.equalsIgnoreCase("NaN")) continue;
                try {
                    double d = Double.parseDouble(rel);
                    diffsByField.computeIfAbsent(field, k -> new ArrayList<>()).add(d);
                } catch (Exception ignored) {}
            }
        }

        if (diffsByField.isEmpty()) {
            System.out.println("ℹ️ No numeric deviations detected — skipping optimization.");
            return;
        }

        double[] testLevels = {5.0, 4.0, 3.0, 2.0, 1.0, 0.5};
        double targetKeep = 98.0; // target: retain at least this % of matches

        // --- Write optimization summary in UTF-8 ---
        try (PrintWriter pw = new PrintWriter(
                Files.newBufferedWriter(outCsv, java.nio.charset.StandardCharsets.UTF_8))) {

            pw.println("Field,Total,Equal@5%,OptimalTolerance(%),Equal@Optimal(%)");

            for (Map.Entry<String, List<Double>> e : diffsByField.entrySet()) {
                String field = e.getKey();
                List<Double> diffs = e.getValue();
                diffs.removeIf(d -> !Double.isFinite(d));
                if (diffs.isEmpty()) continue;

                int total = diffs.size();
                double equalAt5 = percentWithin(diffs, 5.0);
                double bestTol = 5.0;
                double bestKeep = equalAt5;

                for (double tol : testLevels) {
                    double keep = percentWithin(diffs, tol);
                    if (keep < targetKeep) break;
                    bestTol = tol;
                    bestKeep = keep;
                }

                pw.printf(Locale.US, "%s,%d,%.2f,%.2f,%.2f%n",
                          field, total, equalAt5, bestTol, bestKeep);
            }
        }

        System.out.println("📈 Tolerance optimization summary written: " + outCsv.toAbsolutePath());
    }

    /** Helper: compute fraction of deviations within given tolerance. */
    private static double percentWithin(List<Double> vals, double tol) {
        long ok = vals.stream().filter(v -> Math.abs(v) <= tol).count();
        return 100.0 * ok / vals.size();
    }
}