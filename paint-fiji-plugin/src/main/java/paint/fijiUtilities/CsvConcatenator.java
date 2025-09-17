package paint.fijiUtilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CsvConcatenator {

    /**
     * Concatenates all CSV files in a directory into one output file.
     * <p>
     * The header row is kept only once (from the first file encountered).
     * After each file is processed, the original is deleted.
     *
     * @param inputDir   the directory containing the CSV files to concatenate
     * @param outputFile the path of the output CSV file that will be written
     * @throws IOException if an error occurs while reading or writing files
     */
    public static void concatenateCsvFiles(Path inputDir, Path outputFile) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            boolean headerWritten = false;

            // Collect files with regex: 6 digits + "-Exp-" + anything + "-tracks.csv"
            // Exclude if the name contains "threshold" (remnant of the first Glyco-PAINT version)
            List<Path> files = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir)) {
                for (Path p : stream) {
                    String name = p.getFileName().toString();
                    if (name.matches("\\d{6}-Exp-.*-tracks\\.csv")
                            && !name.toLowerCase().contains("threshold")) {
                        files.add(p);
                    }
                }
            }

            // Sort alphabetically for reproducibility
            Collections.sort(files);

            for (Path csvFile : files) {
                try (BufferedReader reader = Files.newBufferedReader(csvFile)) {
                    String line;
                    boolean isFirstLine = true;

                    while ((line = reader.readLine()) != null) {
                        if (isFirstLine) {
                            isFirstLine = false;
                            if (headerWritten) {
                                continue; // skip header if already written
                            } else {
                                headerWritten = true;
                            }
                        }
                        writer.write(line);
                        writer.newLine();
                    }
                }

                // ✅ Delete original after processing
                try {
                    Files.delete(csvFile);
                } catch (IOException e) {
                    System.err.println("Warning: Could not delete " + csvFile + ": " + e.getMessage());
                }
            }
        }
    }


    // Example usage
    public static void main(String[] args) throws IOException {
        Path inputDir = Paths.get("/Users/hans/Paint Test Project/221012");
        Path outputFile = Paths.get("/Users/hans/Paint Test Project/221012/All Tracks Java.csv");

        concatenateCsvFiles(inputDir, outputFile);
        System.out.println("Merged CSV written to " + outputFile);
    }
}