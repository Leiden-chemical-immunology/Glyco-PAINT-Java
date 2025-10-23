package paint.fiji.utils;

import generatesquares.GenerateSquaresHeadless;
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
 * Utility for flattening sweep result directories by concatenating experiment-level CSV files.
 * <p>
 * It merges per-experiment CSV files (<code>SQUARES_CSV</code>, <code>TRACKS_CSV</code>,
 * <code>RECORDINGS_CSV</code>, and <code>EXPERIMENT_INFO_CSV</code>) from all subdirectories
 * into the corresponding CSV files in the parameter-level directory.
 * </p>
 */
public class SweepFlattener {

    /**
     * Scans all sweep parameter directories (those starting with '[')
     * and flattens their subdirectories by concatenating CSV files.
     * <p>
     * After flattening, it calls {@link GenerateSquaresHeadless#run(Path, List)}
     * to process all parameter directories.
     * </p>
     *
     * @param sweepPath      the root sweep directory containing parameter subdirectories
     * @param experimentNames list of experiment subdirectory names to include
     * @param deleteSubdirs   whether to delete the experiment subdirectories after flattening
     * @throws IOException if reading or writing CSV files fails
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

        PaintLogger.infof();
        PaintLogger.infof("Completed Sweep");
    }

    /**
     * Entry point for manual invocation of the sweep flattener.
     *
     * @param args command-line arguments (unused)
     * @throws IOException if I/O operations fail during flattening
     */
    public static void main(String[] args) throws IOException {
        Path sweepPath = Paths.get("/Users/hans/Paint Test Project/Sweep");
        flattenSweep(sweepPath, Arrays.asList("221012", "AnyName"), false);
    }
}