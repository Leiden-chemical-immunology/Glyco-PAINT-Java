package paint.fiji.utils;

import static generatesquares.GenerateSquaresHeadless.run;
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
 * Flattens sweep result directories by concatenating experiment-level CSV files
 * (SQUARES_CSV, TRACKS_CSV, RECORDINGS_CSV, "Experiment Info Java.csv")
 * from all subdirectories into the corresponding files in the parameter-level directory.
 */
public class SweepFlattener {

    /**
     * Scans all sweep parameter directories (those starting with '[')
     * and flattens their subdirectories by concatenating CSV files.
     * <p></p>
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
                    String caseName = paramPath.getFileName().toString();

                    addCase(paramPath, SQUARES_CSV, experimentNames, caseName);
                    addCase(paramPath, TRACKS_CSV, experimentNames, caseName);
                    addCase(paramPath, RECORDINGS_CSV, experimentNames, caseName);
                    addCase(paramPath, EXPERIMENT_INFO_CSV, experimentNames, caseName);
                } catch (IOException e) {
                    System.err.println("  Error concatenating files in " + paramPath + ": " + e.getMessage());
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
                        deleteRecursively(paramDir.resolve(sub));
                    }
                }
            }
        }

        concatenateNamedCsvFiles(sweepPath, SQUARES_CSV, paramDirsFound);
        concatenateNamedCsvFiles(sweepPath, TRACKS_CSV, paramDirsFound);
        concatenateNamedCsvFiles(sweepPath, RECORDINGS_CSV, paramDirsFound);
        concatenateNamedCsvFiles(sweepPath, EXPERIMENT_INFO_CSV, paramDirsFound);

        PaintLogger.infof();
        PaintLogger.infof("Completed Sweep");

    }

    public static void main(String[] args) throws IOException {
        Path sweepPath = Paths.get("/Users/hans/Paint Test Project/Sweep");
        flattenSweep(sweepPath, Arrays.asList("221012", "AnyName"),false);
    }
}