/******************************************************************************
 *  Class:        SweepFlattener.java
 *  Package:      paint.fiji.utils
 *
 *  PURPOSE:
 *    Flattens multi-parameter sweep directories by merging experimental CSVs,
 *    running downstream analysis (Generate Squares), and optionally removing
 *    intermediate experiment folders.
 *
 *  DESCRIPTION:
 *    • Iterates through all sweep parameter directories (e.g., “[Param]-[Value]”).
 *    • Runs {@link paint.generatesquares.GenerateSquaresHeadless} for each case.
 *    • Adds the “Case” column to each per-experiment CSV.
 *    • Concatenates results (Squares, Tracks, Recordings, Experiment Info)
 *      into unified CSVs both per-parameter and across all parameters.
 *    • Optionally removes processed experiment subdirectories.
 *
 *  RESPONSIBILITIES:
 *    • Automate post-sweep data consolidation.
 *    • Maintain deterministic output structure for all sweeps.
 *    • Integrate “Generate Squares” into the flattening pipeline.
 *
 *  USAGE EXAMPLE:
 *    Path sweepPath = Paths.get("/Users/hans/Paint Test Project/Sweep");
 *    SweepFlattener.flattenSweep(sweepPath, Arrays.asList("221012", "AnyName"), true);
 *
 *  DEPENDENCIES:
 *    – paint.generatesquares.GenerateSquaresHeadless
 *    – paint.shared.utils.CsvUtils
 *    – paint.shared.utils.PaintLogger
 *    – org.apache.commons.io.FileUtils
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.fiji.utils;

import org.apache.commons.io.FileUtils;
import paint.generatesquares.GenerateSquaresHeadless;
import paint.shared.utils.PaintLogger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static paint.shared.constants.PaintConstants.*;
import static paint.shared.utils.CsvUtils.addCase;
import static paint.shared.utils.CsvUtils.concatenateNamedCsvFiles;

/**
 * Automates flattening and consolidation of multi-parameter sweep data.
 * <p>
 * Scans a sweep root directory containing subdirectories for parameter cases,
 * processes each experiment, runs “Generate Squares” and concatenates CSV
 * outputs into flattened tables suitable for downstream Paint analysis.
 * </p>
 */
public final class SweepFlattener {

    /**
     * Private constructor to prevent instantiation.
     */
    private SweepFlattener() {
        // Utility class; prevent instantiation
    }

    /**
     * Flattens a sweep directory structure by processing each parameter subfolder.
     * <p>
     * Performs the following sequence:
     * <ol>
     *   <li>Identifies parameter directories under the given sweep root.</li>
     *   <li>Runs the {@link GenerateSquaresHeadless} process for each case.</li>
     *   <li>Adds “Case” columns to per-experiment CSVs.</li>
     *   <li>Concatenates the four main CSVs (Squares, Tracks, Recordings, Experiment Info).</li>
     *   <li>Optionally deletes subdirectories after successful processing.</li>
     *   <li>Finally, merges all parameter-level CSVs into a global flattened output.</li>
     * </ol>
     *
     * @param sweepPath       root directory containing the parameter subdirectories
     * @param experimentNames list of experiment subdirectories to process per parameter
     * @param deleteSubdirs   true to remove experiment folders after processing
     * @throws IOException if I/O operations fail during flattening
     */
    public static void flattenSweep(Path sweepPath,
                                    List<String> experimentNames,
                                    boolean deleteSubdirs) throws IOException {

        if (!Files.isDirectory(sweepPath)) {
            throw new IOException("Not a directory: " + sweepPath);
        }

        List<String> paramDirsFound = new ArrayList<>();

        // ---------------------------------------------------------------------
        // Step 1 – Iterate over parameter directories (e.g., “[Param]-[Value]”)
        // ---------------------------------------------------------------------
        try (DirectoryStream<Path> paramPaths = Files.newDirectoryStream(sweepPath, "[[]*")) {
            for (Path paramPath : paramPaths) {
                if (!Files.isDirectory(paramPath)) {
                    continue;
                }

                PaintLogger.infof("Flattening sweep case: %s", paramPath.getFileName());
                paramDirsFound.add(paramPath.getFileName().toString());

                if (experimentNames.isEmpty()) {
                    PaintLogger.warnf("No experiment subdirectories specified under %s", paramPath);
                    continue;
                }

                // -----------------------------------------------------------------
                // Step 2 – Run Generate Squares
                // -----------------------------------------------------------------
                try {
                    GenerateSquaresHeadless.run(paramPath, experimentNames);
                } catch (Exception e) {
                    PaintLogger.errorf("GenerateSquaresHeadless failed in %s: %s", paramPath, e.getMessage());
                }

                // -----------------------------------------------------------------
                // Step 3 – Add “Case” field to experiment-level CSVs
                // -----------------------------------------------------------------
                try {
                    String caseName = paramPath.getFileName().toString();
                    addCase(paramPath, SQUARES_CSV,       experimentNames, caseName);
                    addCase(paramPath, TRACKS_CSV,        experimentNames, caseName);
                    addCase(paramPath, RECORDINGS_CSV,    experimentNames, caseName);
                    addCase(paramPath, EXPERIMENT_INFO_CSV, experimentNames, caseName);
                } catch (IOException e) {
                    PaintLogger.errorf("  Error adding 'Case' field in %s: %s", paramPath, e.getMessage());
                }

                // -----------------------------------------------------------------
                // Step 4 – Concatenate per-parameter experiment CSVs
                // -----------------------------------------------------------------
                try {
                    concatenateNamedCsvFiles(paramPath, SQUARES_CSV,       experimentNames);
                    concatenateNamedCsvFiles(paramPath, TRACKS_CSV,        experimentNames);
                    concatenateNamedCsvFiles(paramPath, RECORDINGS_CSV,    experimentNames);
                    concatenateNamedCsvFiles(paramPath, EXPERIMENT_INFO_CSV, experimentNames);
                } catch (IOException e) {
                    PaintLogger.errorf("  Error concatenating files in %s: %s", paramPath, e.getMessage());
                }

                // -----------------------------------------------------------------
                // Step 5 – Optionally delete experiment subdirectories
                // -----------------------------------------------------------------
                if (deleteSubdirs) {
                    for (String sub : experimentNames) {
                        try {
                            FileUtils.deleteDirectory(paramPath.resolve(sub).toFile());
                        } catch (Exception e) {
                            PaintLogger.warnf("Failed to delete subdirectory %s: %s", sub, e.getMessage());
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // Step 6 – Global concatenation across all parameter directories
        // ---------------------------------------------------------------------
        concatenateNamedCsvFiles(sweepPath, SQUARES_CSV,       paramDirsFound);
        concatenateNamedCsvFiles(sweepPath, TRACKS_CSV,        paramDirsFound);
        concatenateNamedCsvFiles(sweepPath, RECORDINGS_CSV,    paramDirsFound);
        concatenateNamedCsvFiles(sweepPath, EXPERIMENT_INFO_CSV, paramDirsFound);

        PaintLogger.blankline();
        PaintLogger.infof("Completed Sweep flattening for %d parameter sets.", paramDirsFound.size());
    }

    /**
     * Standalone entry point for debugging and testing the sweep flattening process.
     *
     * @param args command-line arguments (unused)
     * @throws IOException if I/O operations fail during execution
     */
    public static void main(String[] args) throws IOException {
        Path sweepPath = Paths.get("/Users/hans/Paint Test Project/Sweep");
        flattenSweep(sweepPath, Arrays.asList("221012", "AnyName"), false);
    }
}