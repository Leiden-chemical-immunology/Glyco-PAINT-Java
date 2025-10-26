package paint.shared.utils;

import org.apache.commons.csv.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static paint.shared.constants.PaintConstants.RECORDINGS_CSV;

/**
 * Utility class providing methods for working with CSV files, including operations
 * such as counting rows based on conditions, concatenating multiple CSV files, and
 * adding or updating specific columns. Designed to perform common CSV manipulations
 * in an efficient and reusable manner.
 */
public class CsvUtils {

    /** Prevents instantiation of this utility class. */
    private CsvUtils() {
        // prevent instantiation
    }

    // =====================================================================
    // == COUNTING UTILITIES ===============================================
    // =====================================================================

    /**
     * Counts the number of records in a CSV file where the "Process Flag" column
     * has a value indicating it is processed. Valid values for processed include
     * "true", "yes", "y", and "1" (case-insensitive).
     *
     * @param filePath the path to the CSV file to be processed
     * @return the number of records marked as processed; returns 0 if the "Process Flag"
     *         column is not found or an error occurs while processing the file
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
                    String val = record.get(processFlagKey);
                    if (Miscellaneous.isBooleanTrue(val)) {
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
     * Concatenates multiple CSV files into a single CSV file, with an option to delete the input files
     * after processing. The method ensures that the output file contains a single header row
     * (retrieved from the first valid input file) and appends all records from the input files.
     *
     * @param inputFiles a list of paths to input CSV files to be concatenated
     * @param outputFile the path to the resulting output CSV file
     * @param deleteInputs a flag indicating whether to delete the input files after successful processing
     * @throws IOException if an I/O error occurs during reading or writing files
     */
    public static void concatenateCsvFiles(List<Path> inputFiles, Path outputFile, boolean deleteInputs) throws IOException {
        boolean headerWritten = false;
        CSVPrinter printer = null;
        List<Path> processed = new ArrayList<>();

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            for (Path inputFile : inputFiles) {
                if (!Files.exists(inputFile)) {
                    PaintLogger.warnf("Skipping missing file: %s", inputFile);
                    continue;
                }

                try (Reader reader = Files.newBufferedReader(inputFile);
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

                    processed.add(inputFile);
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
     * Concatenates CSV files located in subdirectories of a specified project path, using a given file name
     * and a list of directory names. The resulting concatenated CSV file is written to the main project path
     * with the same file name.
     *
     * @param projectPath     the base directory containing the experiment subdirectories
     * @param fileName        the name of the CSV file to look for in each experiment subdirectory
     * @param experimentNames a list of experiment subdirectory names that contain the target CSV file
     * @throws IOException if an I/O error occurs during reading or writing the CSV files
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
     * Adds or updates a "Case" column in CSV files within specified experiment directories.
     * If a "Case" column already exists, its values are overwritten; if it does not exist,
     * it is added to the end of each record. Updated files replace the original files.
     *
     * @param root the root directory containing experiment subdirectories
     * @param fileName the name of the CSV file to be processed within each experiment subdirectory
     * @param experimentNames a list of experiment subdirectory names to locate the target CSV file
     * @param caseName the value to be assigned or updated in the "Case" column for each record
     * @throws IOException if an I/O error occurs during file processing
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

            CSVFormat readFormat = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build();

            try (Reader reader = Files.newBufferedReader(csvPath);
                 CSVParser parser = new CSVParser(reader, readFormat);
                 BufferedWriter writer = Files.newBufferedWriter(tempPath)) {

                Map<String, Integer> headerMap = parser.getHeaderMap();
                if (headerMap == null || headerMap.isEmpty()) {
                    PaintLogger.warnf("CsvUtils.addCase(): missing or invalid header in %s", csvPath);
                    continue;
                }

                boolean hasCaseColumn = headerMap.containsKey("Case");
                List<String> headers = new ArrayList<>(headerMap.keySet());
                if (!hasCaseColumn) headers.add("Case");

                // Define write format only once, now with explicit header
                CSVFormat writeFormat = CSVFormat.DEFAULT.builder()
                        .setHeader(headers.toArray(new String[0]))
                        .build();

                try (CSVPrinter printer = new CSVPrinter(writer, writeFormat)) {
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
                }

                Files.move(tempPath, csvPath, StandardCopyOption.REPLACE_EXISTING);
                PaintLogger.debugf("Updated 'Case' column in %s", csvPath);
            }
        }
    }

    // =====================================================================
    // == DEMO ==============================================================
    // =====================================================================

    /**
     * The main method serves as the entry point for the program execution.
     * It demonstrates the usage of utility methods such as adding a "Case" column
     * to CSV files and concatenating CSV files within experiment directories.
     *
     * @param args the command-line arguments passed to the program; not utilized in this implementation
     * @throws IOException if an I/O error occurs during file processing
     */
    public static void main(String[] args) throws IOException {
        Path root = Paths.get("/Users/hans/Paint Test Project/Sweep");
        List<String> exps = Arrays.asList("221012", "AnyName");

        // Example: Add case
        addCase(root, RECORDINGS_CSV, exps, "Case 1");

        // Example: Concatenate CSVs
        concatenateNamedCsvFiles(root, RECORDINGS_CSV, exps);
    }
}