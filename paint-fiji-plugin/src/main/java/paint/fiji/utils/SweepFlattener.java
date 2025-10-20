package paint.fiji.utils;

import paint.shared.utils.CsvConcatenator;
import static generatesquares.GenerateSquaresRunner.run;
import static paint.shared.utils.CsvCaseAdder.addCase;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Flattens sweep result directories by concatenating experiment-level CSV files
 * ("All Squares Java.csv", "All Tracks Java.csv", "All Recordings Java.csv", "Experiment Info Java.csv")
 * from all subdirectories into the corresponding files in the parameter-level directory.
 */
public class SweepFlattener {

    /**
     * Scans all sweep parameter directories (those starting with '[')
     * and flattens their subdirectories by concatenating CSV files.
     *
     * After flattening, it calls GenerateSquaresRunner.run(sweepRoot, paramDirsFound)
     * to process all parameter directories.
     *
     * @param sweepRoot     Path to the root Sweep directory
     * @param deleteSubdirs Whether to delete the subdirectories after flattening
     * @throws IOException if reading or writing fails
     */
    public static void flattenSweep(Path sweepRoot, List<String> experimentNames, boolean deleteSubdirs) throws IOException {
        if (!Files.isDirectory(sweepRoot)) {
            throw new IOException("Not a directory: " + sweepRoot);
        }

        // List to collect all parameter directories
        List<String> paramDirsFound = new ArrayList<>();

        // Look for directories starting with '['
        try (DirectoryStream<Path> paramDirs = Files.newDirectoryStream(sweepRoot, "[[]*")) {
            for (Path paramDir : paramDirs) {
                if (!Files.isDirectory(paramDir)) continue;

                System.out.println("Flattening: " + paramDir.getFileName());
                paramDirsFound.add(paramDir.getFileName().toString());


                if (experimentNames.isEmpty()) {
                    System.out.println("  (no subdirectories found)");
                    continue;
                }

                // Run Generate Squares
                try {
                    run(paramDir, experimentNames);
                } catch (Exception e) {
                }

                // Add the case field to all files
                try {
                    String caseName = paramDir.getFileName().toString();

                    addCase(paramDir, "All Squares Java.csv", experimentNames, caseName);
                    addCase(paramDir, "All Tracks Java.csv", experimentNames, caseName);
                    addCase(paramDir, "All Recordings Java.csv", experimentNames, caseName);
                    addCase(paramDir, "Experiment Info.csv", experimentNames, caseName);
                } catch (IOException e) {
                    System.err.println("  Error concatenating files in " + paramDir + ": " + e.getMessage());
                }

                // Concatenate the four CSVs
                try {
                    CsvConcatenator.concatenateNamedCsvFiles(paramDir, "All Squares Java.csv", experimentNames);
                    CsvConcatenator.concatenateNamedCsvFiles(paramDir, "All Tracks Java.csv", experimentNames);
                    CsvConcatenator.concatenateNamedCsvFiles(paramDir, "All Recordings Java.csv", experimentNames);
                    CsvConcatenator.concatenateNamedCsvFiles(paramDir, "Experiment Info.csv", experimentNames);
                } catch (IOException e) {
                    System.err.println("  Error concatenating files in " + paramDir + ": " + e.getMessage());
                }

                // Optionally delete experiment subdirectories
                if (deleteSubdirs) {
                    for (String sub : experimentNames) {
                        deleteRecursively(paramDir.resolve(sub));
                    }
                }
            }
        }

        CsvConcatenator.concatenateNamedCsvFiles(sweepRoot, "All Squares Java.csv", paramDirsFound);
        CsvConcatenator.concatenateNamedCsvFiles(sweepRoot, "All Tracks Java.csv", paramDirsFound);
        CsvConcatenator.concatenateNamedCsvFiles(sweepRoot, "All Recordings Java.csv", paramDirsFound);
        CsvConcatenator.concatenateNamedCsvFiles(sweepRoot, "Experiment Info.csv", paramDirsFound);

    }

    private static void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteRecursively(entry);
                }
            }
        }
        Files.deleteIfExists(path);
    }

    public static void main(String[] args) throws IOException {
        Path sweepPath = Paths.get("/Users/hans/Paint Test Project/Sweep");
        flattenSweep(sweepPath, Arrays.asList("221012", "AnyName"),false);
    }
}