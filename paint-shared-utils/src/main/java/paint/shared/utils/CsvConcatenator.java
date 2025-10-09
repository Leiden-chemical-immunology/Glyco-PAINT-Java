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

public class CsvConcatenator {

    /**
     * Core method: concatenate a list of CSV files into one output file.
     * Header is included only once. Optionally deletes the input files after successful concatenation.
     */
    public static void concatenateCsvFiles(List<Path> inputFiles, Path outputFile, boolean deleteInputs) throws IOException {
        boolean headerWritten = false;
        CSVPrinter printer = null;
        List<Path> successfullyProcessed = new ArrayList<>();

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            for (Path inputFile : inputFiles) {
                if (!Files.exists(inputFile)) {
                    throw new IOException("Missing input file: " + inputFile);
                }

                try (
                        Reader reader = Files.newBufferedReader(inputFile);
                        CSVParser parser = CSVFormat.DEFAULT.builder()
                                .setHeader()
                                .setSkipHeaderRecord(true)
                                .build()
                                .parse(reader)
                ) {
                    if (!headerWritten) {
                        printer = new CSVPrinter(writer,
                                                 CSVFormat.DEFAULT.builder()
                                                         .setHeader(parser.getHeaderMap().keySet().toArray(new String[0]))
                                                         .build()
                        );
                        headerWritten = true;
                    }

                    for (CSVRecord record : parser) {
                        printer.printRecord(record);
                    }

                    successfullyProcessed.add(inputFile);

                } catch (IOException e) {
                    throw new IOException("Error reading file: " + inputFile, e);
                }
            }

            if (printer != null) {
                printer.flush();
            }
        }

        // Delete files only if all succeeded
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

    public static void concatenateCsvFilesInDirectory(Path inputDir, Path outputFile, String regex, boolean deleteInputs) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir)) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                if (name.matches(regex)) {
                    files.add(p);
                }
            }
        }
        Collections.sort(files);
        concatenateCsvFiles(files, outputFile, deleteInputs);
    }

    public static void concatenateTracksFilesInDirectory(Path inputDir, Path outputFile) throws IOException {
        String defaultRegex = "\\d{6}-Exp-.*-tracks\\.csv";
        concatenateCsvFilesInDirectory(inputDir, outputFile, defaultRegex, true);
    }

    public static void concatenateExperimentCsvFiles(Path projectPath, String fileName, List<String> experimentNames) throws IOException {
        List<Path> inputFiles = new ArrayList<>();
        for (String experiment : experimentNames) {
            inputFiles.add(projectPath.resolve(experiment).resolve(fileName));
        }
        Path outputFile = projectPath.resolve(fileName);
        concatenateCsvFiles(inputFiles, outputFile, false);
    }

    public static void main(String[] args) throws IOException {
        // Example 1: from directory with regex
        Path inputDir = Paths.get("/Users/hans/Paint Test Project/221012");
        Path outputFile = inputDir.resolve("All Tracks Java.csv");
        concatenateTracksFilesInDirectory(inputDir, outputFile);
        System.out.println("✔ Concatenated directory files to: " + outputFile);

        // Example 2: from experiment subfolders
        Path projectPath = Paths.get("/Users/hans/Paint Test Project");
        List<String> experimentNames = Arrays.asList("230417", "230418", "230419");
        String fileName = "All Tracks Java.csv";
        concatenateExperimentCsvFiles(projectPath, fileName, experimentNames);
        System.out.println("✔ Concatenated experiment files to: " + projectPath.resolve(fileName));
    }
}