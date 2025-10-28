/******************************************************************************
 *  Class:        CsvUtils.java
 *  Package:      paint.shared.utils
 *
 *  PURPOSE:
 *    Provides reusable utility methods for handling CSV files within the
 *    PAINT data processing framework.
 *
 *  DESCRIPTION:
 *    The {@code CsvUtils} class offers static helper methods for performing
 *    frequent CSV operations, including counting flagged records, concatenating
 *    multiple CSV files, and adding or updating specific columns.
 *
 *    The class uses Apache Commons CSV for reliable CSV parsing and writing,
 *    and integrates with the PAINT logging system for consistent output.
 *
 *  KEY FEATURES:
 *    • Count CSV records based on conditional column values.
 *    • Concatenate multiple CSV files with header management.
 *    • Add or overwrite "Case" columns in experiment result files.
 *    • Supports optional deletion of input files after concatenation.
 *    • UTF-8 encoding, safe file handling, and explicit exception control.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.shared.utils;

import org.apache.commons.csv.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static paint.shared.constants.PaintConstants.RECORDINGS_CSV;

/**
 * Utility class for common CSV file operations such as concatenation,
 * record counting, and column modification.
 * <p>
 * This class is stateless and cannot be instantiated.
 */
public final class CsvUtils {

    /** Prevents instantiation of this utility class. */
    private CsvUtils() {
        // Prevent instantiation
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // COUNTING UTILITIES
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Counts the number of records in a CSV file where the column
     * {@code "Process Flag"} has a value considered as {@code true}.
     * <p>
     * Accepted truthy values are: {@code "true"}, {@code "yes"}, {@code "y"},
     * and {@code "1"} (case-insensitive).
     *
     * @param filePath path to the CSV file
     * @return number of records marked as processed;
     *         returns {@code 0} if no valid "Process Flag" column is found
     */
    public static int countProcessed(Path filePath) {
        int count = 0;

        try {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build();

            try (CSVParser parser = CSVParser.parse(filePath.toFile(),
                                                    StandardCharsets.UTF_8,
                                                    format)) {

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
            PaintLogger.errorf("CsvUtils.countProcessed() failed for %s: %s",
                               filePath, e.getMessage());
        }

        return count;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // CONCATENATION UTILITIES
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Concatenates multiple CSV files into a single CSV file, preserving headers.
     * <p>
     * The output file includes one header row (from the first valid input file)
     * and all data rows from subsequent files. Optionally, the input files can be
     * deleted after successful concatenation.
     *
     * @param inputFiles   list of input CSV file paths
     * @param outputFile   target output file path
     * @param deleteInputs if {@code true}, delete input files after processing
     * @throws IOException if any I/O error occurs during reading or writing
     */
    public static void concatenateCsvFiles(List<Path> inputFiles,
                                           Path outputFile,
                                           boolean deleteInputs) throws IOException {

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
     * Concatenates CSV files located in subdirectories of a given project path.
     * <p>
     * Each subdirectory corresponds to an experiment and contains a CSV file
     * with the specified {@code fileName}. The resulting concatenated CSV file
     * is written to the root project directory.
     *
     * @param projectPath     base directory containing experiment subdirectories
     * @param fileName        CSV filename to concatenate from each subdirectory
     * @param experimentNames list of experiment directory names
     * @throws IOException if an I/O error occurs during processing
     */
    public static void concatenateNamedCsvFiles(Path projectPath,
                                                String fileName,
                                                List<String> experimentNames) throws IOException {
        List<Path> inputs = new ArrayList<>();
        for (String exp : experimentNames) {
            inputs.add(projectPath.resolve(exp).resolve(fileName));
        }

        Path output = projectPath.resolve(fileName);
        concatenateCsvFiles(inputs, output, false);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // CASE COLUMN UPDATER
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Adds or updates a {@code "Case"} column in CSV files within the specified
     * experiment subdirectories.
     * <p>
     * If the column exists, its values are overwritten. If not, it is appended
     * to the end of the record. Each processed file is atomically replaced
     * after writing the updated version.
     *
     * @param root            base path containing experiment subdirectories
     * @param fileName        target CSV filename within each subdirectory
     * @param experimentNames list of experiment subdirectory names
     * @param caseName        value to assign to the "Case" column
     * @throws IOException if an I/O error occurs during processing
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

                boolean      hasCaseColumn = headerMap.containsKey("Case");
                List<String> headers       = new ArrayList<>(headerMap.keySet());
                if (!hasCaseColumn) {
                    headers.add("Case");
                }

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
                        if (!hasCaseColumn) {
                            row.add(caseName);
                        }
                        printer.printRecord(row);
                    }
                    printer.flush();
                }

                Files.move(tempPath, csvPath, StandardCopyOption.REPLACE_EXISTING);
                PaintLogger.debugf("Updated 'Case' column in %s", csvPath);
            }
        }
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // DEMO / MAIN DRIVER
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Demonstrates the use of CSV utility methods:
     * adding a "Case" column and concatenating experiment CSV files.
     *
     * @param args command-line arguments (unused)
     * @throws IOException if an I/O error occurs during processing
     */
    public static void main(String[] args) throws IOException {
        Path root = Paths.get("/Users/hans/Paint Test Project/Sweep");
        List<String> exps = Arrays.asList("221012", "AnyName");

        // Example: Add "Case" column
        addCase(root, RECORDINGS_CSV, exps, "Case 1");

        // Example: Concatenate experiment CSV files
        concatenateNamedCsvFiles(root, RECORDINGS_CSV, exps);
    }
}