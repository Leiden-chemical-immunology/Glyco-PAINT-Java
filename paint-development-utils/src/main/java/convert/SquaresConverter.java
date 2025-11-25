package convert;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class SquaresConverter implements CsvConverter {

    private final Path inputDir;
    private final Path inputFile;
    private final Path outputFile;

    public SquaresConverter(Path inputDir) {
        this.inputDir = inputDir;
        this.inputFile = inputDir.resolve("All Squares.csv");
        this.outputFile = inputDir.resolve("Squares.csv");
    }

    // --------------------------------------------------------------------
    // FINAL NEW HEADER ORDER (column 3 in your table)
    // --------------------------------------------------------------------
    private static final List<String> HEADER = Arrays.asList(
            "Unique Key",
            "Experiment Name",
            "Recording Name",
            "Square Number",
            "Row Number",
            "Column Number",
            "Label Number",
            "Cell ID",
            "Visible",
            "Square Manually Excluded",
            "Image Excluded",
            "X0",
            "Y0",
            "X1",
            "Y1",
            "Number of Tracks",
            "Variability",
            "Density",
            "Density Ratio",
            "Density Ratio Ori",
            "Tau",
            "R Squared",
            "Median Diffusion Coefficient",
            "Median Diffusion Coefficient Ext",
            "Median Displacement",
            "Max Displacement",
            "Total Displacement",
            "Median Max Speed",
            "Max Max Speed",
            "Median Mean Speed",
            "Max Mean Speed",
            "Max Track Duration",
            "Total Track Duration",
            "Median Track Duration"
    );

    // --------------------------------------------------------------------
    // NEW COLUMN → OLD POSITION (your column 4)
    // --------------------------------------------------------------------
    private static final Map<String,Integer> INDEX_MAP = new LinkedHashMap<String,Integer>();
    static {
        INDEX_MAP.put("Unique Key",                           1);
        INDEX_MAP.put("Experiment Name",                      4);
        INDEX_MAP.put("Recording Name",                       3);
        INDEX_MAP.put("Square Number",                        8);
        INDEX_MAP.put("Row Number",                          15);
        INDEX_MAP.put("Column Number",                       16);
        INDEX_MAP.put("Label Number",                        17);
        INDEX_MAP.put("Cell ID",                             18);
        INDEX_MAP.put("Visible",                             25);
        INDEX_MAP.put("Square Manually Excluded",            47);
        INDEX_MAP.put("Image Excluded",                      48);
        INDEX_MAP.put("X0",                                  21);
        INDEX_MAP.put("Y0",                                  22);
        INDEX_MAP.put("X1",                                  23);
        INDEX_MAP.put("Y1",                                  24);
        INDEX_MAP.put("Number of Tracks",                    20);
        INDEX_MAP.put("Variability",                         26);
        INDEX_MAP.put("Density",                             27);
        INDEX_MAP.put("Density Ratio",                       28);
        INDEX_MAP.put("Density Ratio Ori",                   28);
        INDEX_MAP.put("Tau",                                 29);
        INDEX_MAP.put("R Squared",                           30);
        INDEX_MAP.put("Median Diffusion Coefficient",        31);
        INDEX_MAP.put("Median Diffusion Coefficient Ext",    33);
        INDEX_MAP.put("Median Displacement",                 37);
        INDEX_MAP.put("Max Displacement",                    38);
        INDEX_MAP.put("Total Displacement",                  39);
        INDEX_MAP.put("Median Max Speed",                    40);
        INDEX_MAP.put("Max Max Speed",                       41);
        INDEX_MAP.put("Median Mean Speed",                   42);
        INDEX_MAP.put("Max Mean Speed",                      43);
        INDEX_MAP.put("Max Track Duration",                  44);
        INDEX_MAP.put("Total Track Duration",                45);
        INDEX_MAP.put("Median Track Duration",               46);
    }

    @Override
    public List<String> getOutputHeader() {
        return HEADER;
    }

    @Override
    public List<Map<String,String>> convert(List<Map<String,String>> src) {

        List<Map<String,String>> out = new ArrayList<Map<String,String>>();

        for (Map<String,String> inRow : src) {

            // convert row to array in original column order
            String[] raw = inRow.values().toArray(new String[inRow.size()]);

            Map<String,String> row = new LinkedHashMap<String,String>();

            for (String newCol : HEADER) {
                int oldPos = INDEX_MAP.get(newCol);
                int idx = oldPos - 1;

                String val = (idx < raw.length ? raw[idx] : "");
                row.put(newCol, val == null ? "" : val.trim());
            }

            out.add(row);
        }

        return out;
    }

    public void run() throws Exception {
        System.out.println("Reading:  " + inputFile);
        List<Map<String,String>> src = CsvIO.readCsv(inputFile);

        System.out.println("Converting Squares...");
        List<Map<String,String>> out = convert(src);

        System.out.println("Writing:  " + outputFile);
        CsvIO.writeCsv(outputFile, HEADER, out);

        System.out.println("✔ Squares conversion complete.");
    }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get("/Users/hans/Downloads/221012 Python Reference - v39");
        new SquaresConverter(dir).run();
    }
}