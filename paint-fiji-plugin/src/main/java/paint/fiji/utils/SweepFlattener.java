package paint.fiji.utils;

import paint.generatesquares.GenerateSquaresHeadless;
import org.apache.commons.io.FileUtils;
import paint.shared.utils.PaintLogger;

import static paint.shared.constants.PaintConstants.*;
import static paint.shared.utils.CsvUtils.addCase;
import static paint.shared.utils.CsvUtils.concatenateNamedCsvFiles;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The SweepFlattener class provides functionality to process and flatten
 * subdirectories within a given sweep directory. It is designed to scan
 * parameter directories, concatenate specific CSV files, and optionally
 * remove subdirectories after processing.
 *
 * This class also integrates and utilizes the GenerateSquaresHeadless operations
 * during the sweep flattening process.
 */
public class SweepFlattener {

    /**
     * Flattens the structure of a directory containing parameter directories and experiment subdirectories
     * by processing and consolidating data into CSV files, running the Generate Squares pipeline,
     * and optionally deleting experiment subdirectories.
     *
     * @param sweepPath the root directory path containing the parameter directories to process
     * @param experimentNames a list of names of the experiment subdirectories to process within each parameter directory
     * @param deleteSubdirs a flag indicating whether to delete experiment subdirectories after processing
     * @throws IOException if I/O operations fail during the flattening process
     */
    public static void flattenSweep(Path sweepPath, List<String> experimentNames, boolean deleteSubdirs) throws IOException {
        if (!Files.isDirectory(sweepPath)) {
            throw new IOException("Not a directory: " + sweepPath);
        }

        // List to collect all parameter directories
        List<String> paramDirsFound = new ArrayList<>();

        // Look for directories starting with '['
        try (DirectoryStream<Path> paramPaths = Files.newDirectoryStream(sweepPath, "[[]*")) {
            for (Path paramPath : paramPaths) {
                if (!Files.isDirectory(paramPath)) {
                    continue;
                }

                System.out.println("Flattening: " + paramPath.getFileName());
                paramDirsFound.add(paramPath.getFileName().toString());

                if (experimentNames.isEmpty()) {
                    System.out.println("  (no subdirectories found)");
                    continue;
                }

                // Run Generate Squares
                try {
                    GenerateSquaresHeadless.run(paramPath, experimentNames);
                } catch (Exception e) {
                }

                // Add the case field to all files
                try {
                    String caseName = paramPath.getFileName().toString();

                    addCase(paramPath, SQUARES_CSV, experimentNames, caseName);
                    addCase(paramPath, TRACKS_CSV, experimentNames, caseName);
                    addCase(paramPath, RECORDINGS_CSV, experimentNames, caseName);
                    addCase(paramPath, EXPERIMENT_INFO_CSV, experimentNames, caseName);
                } catch (IOException e) {
                    System.err.println("  Error adding case field in " + paramPath + ": " + e.getMessage());
                }

                // Concatenate the four CSVs
                try {
                    concatenateNamedCsvFiles(paramPath, SQUARES_CSV, experimentNames);
                    concatenateNamedCsvFiles(paramPath, TRACKS_CSV, experimentNames);
                    concatenateNamedCsvFiles(paramPath, RECORDINGS_CSV, experimentNames);
                    concatenateNamedCsvFiles(paramPath, EXPERIMENT_INFO_CSV, experimentNames);
                } catch (IOException e) {
                    System.err.println("  Error concatenating files in " + paramPath + ": " + e.getMessage());
                }

                // Optionally delete experiment subdirectories
                if (deleteSubdirs) {
                    for (String sub : experimentNames) {
                        FileUtils.deleteDirectory(paramPath.resolve(sub).toFile());
                    }
                }
            }
        }

        // Concatenate results across all parameter directories
        concatenateNamedCsvFiles(sweepPath, SQUARES_CSV, paramDirsFound);
        concatenateNamedCsvFiles(sweepPath, TRACKS_CSV, paramDirsFound);
        concatenateNamedCsvFiles(sweepPath, RECORDINGS_CSV, paramDirsFound);
        concatenateNamedCsvFiles(sweepPath, EXPERIMENT_INFO_CSV, paramDirsFound);

        PaintLogger.blankline();
        PaintLogger.infof("Completed Sweep");
    }

    /**
     * The main entry point for the application. This method initializes the process
     * to flatten a directory structure based on predefined parameters.
     *
     * @param args command-line arguments passed to the program; currently unused
     * @throws IOException if I/O operations fail during the execution
     */
    public static void main(String[] args) throws IOException {
        Path sweepPath = Paths.get("/Users/hans/Paint Test Project/Sweep");
        flattenSweep(sweepPath, Arrays.asList("221012", "AnyName"), false);
    }
}