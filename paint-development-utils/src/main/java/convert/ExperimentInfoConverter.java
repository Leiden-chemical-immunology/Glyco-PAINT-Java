package convert;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class ExperimentInfoConverter {

//    private final Path inputDir;
    private final Path inputFile;
    private final Path outputFile;

    public ExperimentInfoConverter(Path inputDir) {
//        this.inputDir = inputDir;
        this.inputFile = inputDir.resolve("Experiment Info - Python.csv");
        this.outputFile = inputDir.resolve("Experiment Info.csv");
    }

    // ---------------------------------------------------------------
    // FINAL new header order (column 3)
    // ---------------------------------------------------------------
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
            "Threshold"
    );

    // ---------------------------------------------------------------
    // NEW COLUMN → OLD POSITION (column 4)
    // (1-based indexes, because Python export is 1-based)
    // ---------------------------------------------------------------
    private static final Map<String,Integer> INDEX_MAP = new LinkedHashMap<>();
    static {
        INDEX_MAP.put("Experiment Name",     4);
        INDEX_MAP.put("Recording Name",      2);
        INDEX_MAP.put("Condition Number",    5);
        INDEX_MAP.put("Replicate Number",    6);
        INDEX_MAP.put("Probe Name",          7);
        INDEX_MAP.put("Probe Type",          8);
        INDEX_MAP.put("Cell Type",           9);
        INDEX_MAP.put("Adjuvant",           10);
        INDEX_MAP.put("Concentration",      11);
        INDEX_MAP.put("Process Flag",       13);
        INDEX_MAP.put("Threshold",          12);
    }

//    @Override
//    public List<String> getOutputHeader() {
//        return HEADER;
//    }

    public List<Map<String,String>> convert(List<Map<String,String>> src) {

        List<Map<String,String>> out = new ArrayList<>();

        for (Map<String,String> inRow : src) {

            // convert old row to array by index
            String[] raw = inRow.values().toArray(new String[inRow.size()]);

            Map<String,String> row = new LinkedHashMap<>();

            for (String newCol : HEADER) {
                int oldPos = INDEX_MAP.get(newCol);
                int idx = oldPos - 1;  // convert to 0-based
                String val = (idx < raw.length ? raw[idx] : "");
                row.put(newCol, val == null ? "" : val.trim());
            }

            out.add(row);
        }

        return out;
    }

    public void run() throws Exception {
        System.out.println("Reading:  " + inputFile);
        List<Map<String,String>> src = CsvIO.readSimpleCsv(inputFile);

        System.out.println("Converting Experiment Info...");
        List<Map<String,String>> out = convert(src);

        System.out.println("Writing:  " + outputFile);
        CsvIO.writeSimpleCsv(outputFile, HEADER, out);

        System.out.println("✔ Experiment Info conversion complete.");
    }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get("/Users/hans/Downloads/221012 Python Reference - v39");
        new ExperimentInfoConverter(dir).run();
    }
}