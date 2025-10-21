package paint.shared.utils;

import org.apache.commons.csv.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static paint.shared.constants.PaintConstants.RECORDINGS_CSV;

/**
 * Comprehensive CSV utility class for the Paint project.
 * <p>
 * Features:
 * <ul>
 *   <li>Concatenate multiple CSV files into one output file</li>
 *   <li>Concatenate all CSVs in a directory by regex</li>
 *   <li>Concatenate same-named CSVs from experiment subfolders</li>
 *   <li>Add or update a "Case" column in experiment CSVs</li>
 *   <li>Count how many rows are marked as processed</li>
 * </ul>
 * <p>
 * Uses <b>Apache Commons CSV</b> for robust parsing and writing.
 */
public class CsvUtils {

    private CsvUtils() {
        // prevent instantiation
    }

    // =====================================================================
    // == COUNTING UTILITIES ===============================================
    // =====================================================================

    /**
     * Counts rows in a CSV file whose "Process Flag" column contains a truthy value
     * (true, yes, y, or 1).
     *
     * @param filePath path to the CSV file
     * @return number of processed rows, or 0 if file missing or invalid
     */
    public static int countProcessed(Path filePath) {
        int count = 0;

        try {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build();

            try (CSVParser parser = CSVParser.parse(filePath.toFile(),
                                                    StandardCharsets.UTF_8, format)) {

                String processFlagKey = null;
                for (String header : parser.getHeaderMap().keySet()) {
                    if (header.trim().equalsIgnoreCase("Process Flag")) {
                        processFlagKey = header;
                        break;
                    }
                }

                if (processFlagKey == null) {
                    return 0;
                }

                for (CSVRecord record : parser) {
                    String val = record.get(processFlagKey).trim().toLowerCase();
                    if (val.equals("true") || val.equals("yes") || val.equals("y") || val.equals("1")) {
                        count++;
                    }
                }
            }
        } catch (IOException e) {
            PaintLogger.errorf("CsvUtils.countProcessed() failed for %s: %s", filePath, e.getMessage());
        }

        return count;
    }

    // =====================================================================
    // == CONCATENATION UTILITIES ==========================================
    // =====================================================================

    /**
     * Concatenates multiple CSV files into one output file.
     * <p>
     * Writes the header only once (from the first file). Optionally deletes
     * input files after successful concatenation.
     *
     * @param inputFiles   input CSV files
     * @param outputFile   destination CSV file
     * @param deleteInputs whether to delete input files after success
     * @throws IOException if reading or writing fails
     */
    public static void concatenateCsvFiles(List<Path> inputFiles, Path outputFile, boolean deleteInputs) throws IOException {
        boolean headerWritten = false;
        CSVPrinter printer = null;
        List<Path> processed = new ArrayList<>();

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            for (Path input : inputFiles) {
                if (!Files.exists(input)) {
                    throw new IOException("Missing input file: " + input);
                }

                try (Reader reader = Files.newBufferedReader(input);
                     CSVParser parser = CSVFormat.DEFAULT.builder()
                             .setHeader()
                             .setSkipHeaderRecord(true)
                             .build()
                             .parse(reader)) {

                    if (!headerWritten) {
                        printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                                .setHeader(parser.getHeaderMap().keySet().toArray(new String[0]))
                                .build());
                        headerWritten = true;
                    }

                    for (CSVRecord record : parser) {
                        printer.printRecord(record);
                    }

                    processed.add(input);
                }
            }

            if (printer != null) printer.flush();
        }

        if (deleteInputs) {
            for (Path f : processed) {
                try {
                    Files.deleteIfExists(f);
                } catch (IOException e) {
                    PaintLogger.warnf("Could not delete %s: %s", f, e.getMessage());
                }
            }
        }
    }

    /**
     * Concatenates all CSV files in a directory whose names match a regex pattern.
     *
     * @param inputDir     directory containing CSV files
     * @param outputFile   path to output CSV
     * @param regex        regex pattern for filenames
     * @param deleteInputs delete files after success
     * @throws IOException on I/O error
     */
    public static void concatenateCsvFilesInDirectory(Path inputDir, Path outputFile,
                                                      String regex, boolean deleteInputs) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir)) {
            for (Path p : stream) {
                if (p.getFileName().toString().matches(regex)) {
                    files.add(p);
                }
            }
        }
        Collections.sort(files);
        concatenateCsvFiles(files, outputFile, deleteInputs);
    }

    /**
     * Concatenates a same-named CSV file (e.g. "Tracks.csv") from multiple
     * experiment subfolders into a single file at the project root.
     *
     * @param projectPath     root directory
     * @param fileName        CSV file name
     * @param experimentNames list of experiment subfolder names
     * @throws IOException on I/O error
     */
    public static void concatenateNamedCsvFiles(Path projectPath, String fileName,
                                                List<String> experimentNames) throws IOException {
        List<Path> inputs = new ArrayList<>();
        for (String exp : experimentNames) {
            inputs.add(projectPath.resolve(exp).resolve(fileName));
        }

        Path output = projectPath.resolve(fileName);
        concatenateCsvFiles(inputs, output, false);
    }

    // =====================================================================
    // == CASE COLUMN ADDER ===============================================
    // =====================================================================

    /**
     * Adds or updates a "Case" column with a given fixed value in CSV files
     * found in experiment subdirectories.
     *
     * @param root            root directory containing experiment folders
     * @param fileName        name of the CSV file to modify (e.g. RECORDINGS_CSV)
     * @param experimentNames list of experiment directory names
     * @param caseName        value to insert into the "Case" column
     * @throws IOException if reading or writing fails
     */
    public static void addCase(Path root,
                               String fileName,
                               List<String> experimentNames,
                               String caseName) throws IOException {

        for (String exp : experimentNames) {
            Path csvPath = root.resolve(exp).resolve(fileName);
            if (!Files.exists(csvPath)) {
                PaintLogger.warnf("CsvUtils.addCase(): file not found %s", csvPath);
                continue;
            }

            Path tempPath = csvPath.resolveSibling(fileName + ".tmp");

            try (Reader reader = Files.newBufferedReader(csvPath);
                 CSVParser parser = CSVFormat.DEFAULT
                         .withFirstRecordAsHeader()
                         .parse(reader);
                 BufferedWriter writer = Files.newBufferedWriter(tempPath)) {

                Map<String, Integer> headerMap = parser.getHeaderMap();
                boolean hasCaseColumn = headerMap.containsKey("Case");

                List<String> headers = new ArrayList<>(headerMap.keySet());
                if (!hasCaseColumn) headers.add("Case");

                CSVPrinter printer = new CSVPrinter(writer,
                                                    CSVFormat.DEFAULT.withHeader(headers.toArray(new String[0])));

                for (CSVRecord record : parser) {
                    List<String> row = new ArrayList<>();
                    for (String h : headerMap.keySet()) {
                        if (h.equals("Case")) {
                            row.add(caseName); // overwrite if exists
                        } else {
                            row.add(record.get(h));
                        }
                    }
                    if (!hasCaseColumn) row.add(caseName);
                    printer.printRecord(row);
                }

                printer.flush();
                printer.close();
            }

            Files.move(tempPath, csvPath, StandardCopyOption.REPLACE_EXISTING);
            PaintLogger.infof("Updated 'Case' column in %s", csvPath);
        }
    }

    // =====================================================================
    // == DEMO ==============================================================
    // =====================================================================

    /** Example usage. */
    public static void main(String[] args) throws IOException {
        Path root = Paths.get("/Users/hans/Paint Test Project/Sweep");
        List<String> exps = Arrays.asList("221012", "AnyName");

        // Example: Add case
        addCase(root, RECORDINGS_CSV, exps, "Case 1");

        // Example: Concatenate CSVs
        concatenateNamedCsvFiles(root, RECORDINGS_CSV, exps);
    }
}