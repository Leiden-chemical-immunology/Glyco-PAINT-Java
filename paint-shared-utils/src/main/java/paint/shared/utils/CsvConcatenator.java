package paint.shared.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for concatenating multiple CSV files into a single output file.
 * <p>
 * Features:
 * <ul>
 *     <li>Combines multiple CSV files into one, including the header only once.</li>
 *     <li>Supports directory scanning with regex filtering.</li>
 *     <li>Can automatically delete input files after successful concatenation.</li>
 *     <li>Provides convenience methods for project-specific track and experiment file handling.</li>
 * </ul>
 * <p>
 * This class relies on <b>Apache Commons CSV</b> for robust parsing and writing of CSV files.
 */
public class CsvConcatenator {

    /**
     * Concatenates multiple CSV files into a single output file.
     * <p>
     * The first input file's header is written to the output. All subsequent files' headers
     * are skipped automatically. If {@code deleteInputs} is {@code true}, input files are deleted
     * only after all files have been successfully concatenated.
     *
     * @param inputFiles   list of input CSV file paths
     * @param outputFile   output CSV file path
     * @param deleteInputs whether to delete input files after concatenation
     * @throws IOException if any file cannot be read or written
     */
    public static void concatenateCsvFiles(List<Path> inputFiles, Path outputFile, boolean deleteInputs) throws IOException {
        boolean headerWritten = false;
        CSVPrinter printer = null;
        List<Path> successfullyProcessed = new ArrayList<>();

        // Open a buffered writer for the output CSV file
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            for (Path inputFile : inputFiles) {
                // Check file existence before processing
                if (!Files.exists(inputFile)) {
                    throw new IOException("Missing input file: " + inputFile);
                }

                // Parse input CSV file
                try (
                        Reader reader = Files.newBufferedReader(inputFile);
                        CSVParser parser = CSVFormat.DEFAULT.builder()
                                .setHeader()                // Use the first record as the header
                                .setSkipHeaderRecord(true)  // Skip the header during iteration
                                .build()
                                .parse(reader)
                ) {
                    // Write the header only once (from the first file)
                    if (!headerWritten) {
                        printer = new CSVPrinter(writer,
                                                 CSVFormat.DEFAULT.builder()
                                                         .setHeader(parser.getHeaderMap().keySet().toArray(new String[0]))
                                                         .build());
                        headerWritten = true;
                    }

                    // Write all records from the current CSV file
                    for (CSVRecord record : parser) {
                        printer.printRecord(record);
                    }

                    // Keep track of successfully processed files
                    successfullyProcessed.add(inputFile);

                } catch (IOException e) {
                    throw new IOException("Error reading file: " + inputFile, e);
                }
            }

            // Flush the writer to ensure data integrity
            if (printer != null) {
                printer.flush();
            }
        }

        // Delete all input files only if concatenation completed successfully
        if (deleteInputs) {
            for (Path file : successfullyProcessed) {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    System.err.println("Warning: Could not delete " + file + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Concatenates all CSV files in a given directory whose names match the provided regular expression.
     * <p>
     * The matched files are sorted lexicographically before concatenation. Optionally deletes
     * input files after successful concatenation.
     *
     * @param inputDir     directory containing input CSV files
     * @param outputFile   output CSV file path
     * @param regex        regular expression used to match filenames
     * @param deleteInputs whether to delete input files after concatenation
     * @throws IOException if directory cannot be read or files cannot be written
     */
    public static void concatenateCsvFilesInDirectory(Path inputDir, Path outputFile, String regex, boolean deleteInputs) throws IOException {
        List<Path> files = new ArrayList<>();

        // Collect files matching the regex
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir)) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                if (name.matches(regex)) {
                    files.add(p);
                }
            }
        }

        // Sort files alphabetically for deterministic ordering
        Collections.sort(files);

        // Perform concatenation
        concatenateCsvFiles(files, outputFile, deleteInputs);
    }


    /**
     * Concatenates the same-named CSV file from multiple experiment subdirectories into one file
     * located at the project root.
     * <p>
     * This is typically used to merge CSV results from multiple experiments into a single dataset in the root
     *
     * @param projectPath     root directory of the project containing experiment subfolders
     * @param fileName        name of the CSV file to concatenate (e.g., {@code "All Tracks Java.csv"})
     * @param experimentNames list of experiment subfolder names
     * @throws IOException if any file cannot be read or written
     */
    public static void concatenateNamedCsvFiles(Path projectPath, String fileName, List<String> experimentNames) throws IOException {
        List<Path> inputFiles = new ArrayList<>();

        // Build full paths for all input files
        for (String experiment : experimentNames) {
            inputFiles.add(projectPath.resolve(experiment).resolve(fileName));
        }

        // Output file is written to the project root
        Path outputFile = projectPath.resolve(fileName);
        concatenateCsvFiles(inputFiles, outputFile, false);
    }
}