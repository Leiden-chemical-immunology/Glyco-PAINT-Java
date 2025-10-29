/******************************************************************************
 *  Class:        RunTrackMateOnProjectSweep.java
 *  Package:      paint.fiji.trackmate
 *
 *  PURPOSE:
 *    Executes the Paint TrackMate analysis pipeline across multiple experiments
 *    under varying parameter configurations ("sweep mode"). This allows
 *    systematic exploration of parameter sensitivity by iteratively running
 *    TrackMate under different settings.
 *
 *  DESCRIPTION:
 *    • Loads the Sweep configuration from Paint's JSON configuration file.
 *    • Iterates over all active TrackMate parameters marked for sweeping.
 *    • Creates separate subdirectories for each parameter–value combination.
 *    • Rewrites Paint configuration files dynamically for each sweep run.
 *    • Executes the TrackMate workflow for all experiments under the new setup.
 *    • Optionally flattens the sweep results for summary analysis.
 *
 *  RESPONSIBILITIES:
 *    • Manage configuration cloning and parameter substitution per sweep run.
 *    • Invoke {@link RunTrackMateOnProject} for each parameter combination.
 *    • Maintain logging and recovery of the original Paint configuration.
 *
 *  USAGE EXAMPLE:
 *    boolean ok = RunTrackMateOnProjectSweep.runWithSweep(
 *                     Paths.get("/Paint Project"),
 *                     Paths.get("/Volumes/Images"),
 *                     Arrays.asList("221108", "221122"));
 *
 *  DEPENDENCIES:
 *    – paint.fiji.trackmate.RunTrackMateOnProject
 *    – paint.shared.config.PaintConfig
 *    – paint.shared.config.SweepConfig
 *    – paint.shared.utils.PaintLogger
 *    – paint.fiji.utils.SweepFlattener
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

package paint.fiji.trackmate;

import org.apache.commons.io.FileUtils;
import paint.shared.config.PaintConfig;
import paint.shared.config.SweepConfig;
import paint.shared.utils.PaintLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static paint.fiji.utils.SweepFlattener.flattenSweep;
import static paint.shared.constants.PaintConstants.*;

/**
 * Coordinates a "parameter sweep" execution for TrackMate across multiple
 * experiments. For each active sweep parameter, the Paint configuration
 * is duplicated and modified, and the entire project is reprocessed.
 * <p>
 * This class enables systematic parameter-space exploration for TrackMate
 * within Paint’s experiment framework.
 * </p>
 */
public class RunTrackMateOnProjectSweep {

    /**
     * Executes the TrackMate analysis workflow across multiple parameter
     * configurations defined in the sweep configuration file.
     * <p>
     * For each parameter–value combination, this method:
     * <ul>
     *   <li>Creates a clean subdirectory under the project’s "Sweep" folder.</li>
     *   <li>Copies and modifies the Paint configuration file.</li>
     *   <li>Runs all selected experiments under that parameter setting.</li>
     *   <li>Logs results and restores the original configuration afterward.</li>
     * </ul>
     *
     * @param projectPath     the root directory of the Paint project
     * @param imagesPath      the directory containing experiment image data
     * @param experimentNames list of experiments to include in the sweep
     * @return {@code true} if all sweep combinations complete successfully;
     *         {@code false} otherwise
     * @throws IOException if any file or directory operation fails
     */
    public static boolean runWithSweep(Path projectPath,
                                       Path imagesPath,
                                       List<String> experimentNames) throws IOException {

        // ---------------------------------------------------------------------
        // Phase 1 – Load sweep configuration
        // ---------------------------------------------------------------------
        Path sweepFile = projectPath.resolve(PAINT_SWEEP_CONFIGURATION_JSON);
        if (!Files.exists(sweepFile)) {
            PaintLogger.infof("No sweep configuration found at %s", sweepFile);
            return false;
        }

        SweepConfig sweepConfig = new SweepConfig(sweepFile.toString());
        Map<String, List<Number>> sweeps = sweepConfig.getActiveSweepValues("TrackMate Sweep");

        if (sweeps.isEmpty()) {
            PaintLogger.infof("Sweep enabled, but no active sweep parameters defined.");
            return false;
        }

        boolean overallStatus = true;
        List<String[]> summaryRows = new ArrayList<>();

        // ---------------------------------------------------------------------
        // Phase 2 – Generate sweep summary for logging
        // ---------------------------------------------------------------------
        List<String> sweepSummary = new ArrayList<>();
        sweepSummary.add("");
        try {
            for (Map.Entry<String, List<Number>> entry : sweeps.entrySet()) {
                String parameter = entry.getKey();       // parameter name
                List<Number> values = entry.getValue();  // list of values

                // Convert values to comma-separated string
                String valueList = values.stream()
                        .map(Object::toString)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("(none)");

                // Add a header line for the parameter
                sweepSummary.add(String.format("Parameter: %s", parameter));
                sweepSummary.add(String.format("Values:    %s", valueList));
                sweepSummary.add("");
            }

            // Add the experiments section at the end
            if (experimentNames != null && !experimentNames.isEmpty()) {
                String exps = String.join(", ", experimentNames);
                sweepSummary.add(String.format("Experiments: %s", exps));
            }
        } catch (Exception e) {
            PaintLogger.errorf("Error building sweep summary: %s", e.getMessage());
        }
        PaintLogger.doc("Sweep analysis to be performed", sweepSummary);

        // ---------------------------------------------------------------------
        // Phase 3 – Perform parameter sweep
        // ---------------------------------------------------------------------
        try {
            for (Map.Entry<String, List<Number>> entry : sweeps.entrySet()) {
                // Cycle through the parameters
                String parameter    = entry.getKey();       // Holds a parameter that we sweep
                List<Number> values = entry.getValue();     // Values holds the values

                // Store original parameter value for restoration
                String originalValue = PaintConfig.getString("TrackMate", parameter, "undefined");

                for (Number val : values) {
                    PaintLogger.infof("Running sweep for %s = %s", parameter, val);

                    // Create clean sweep directory
                    Path sweepPath = projectPath.resolve("Sweep").resolve("[" + parameter + "]-[" + val + "]");
                    FileUtils.deleteDirectory(sweepPath.toFile());
                    Files.createDirectories(sweepPath);

                    // Copy baseline PaintConfig.json
                    Path baselineConfig = projectPath.resolve(PAINT_CONFIGURATION_JSON);
                    Path configCopy     = sweepPath.resolve(PAINT_CONFIGURATION_JSON);
                    try {
                        Files.copy(baselineConfig, configCopy, StandardCopyOption.REPLACE_EXISTING);
                        PaintLogger.infof("Copied baseline PaintConfig.json to %s", configCopy);
                    } catch (IOException e) {
                        PaintLogger.errorf("Failed to copy PaintConfig.json to %s: %s", configCopy, e.getMessage());
                    }

                    // Reinitialize PaintConfig for this sweep
                    PaintConfig.reinitialise(sweepPath);

                    // Apply updated parameter value
                    if (val.doubleValue() == val.intValue()) {
                        PaintConfig.setInt("TrackMate", parameter, val.intValue());
                    } else {
                        PaintConfig.setDouble("TrackMate", parameter, val.doubleValue());
                    }

                    // Persist modified configuration
                    PaintConfig.instance().save();

                    // Copy Experiment Info.csv files into sweep directory
                    for (String expName : experimentNames) {
                        Path expSrc    = projectPath.resolve(expName).resolve(EXPERIMENT_INFO_CSV);
                        Path expDstDir = sweepPath.resolve(expName);
                        Path expDst    = expDstDir.resolve(EXPERIMENT_INFO_CSV);
                        try {
                            if (Files.exists(expSrc)) {
                                Files.createDirectories(expDstDir);
                                Files.copy(expSrc, expDst, StandardCopyOption.REPLACE_EXISTING);
                            } else {
                                PaintLogger.warnf("Experiment Info.csv not found for %s at %s", expName, expSrc);
                            }
                        } catch (IOException e) {
                            PaintLogger.errorf("Failed to copy Experiment Info.csv for %s: %s", expName, e.getMessage());
                        }
                    }

                    // Run TrackMate workflow for all experiments under current sweep
                    boolean status = RunTrackMateOnProject.runProject(projectPath, imagesPath, experimentNames, null, sweepPath);

                    summaryRows.add(new String[]{
                            parameter, val.toString(), sweepPath.toString(),
                            status ? "SUCCESS" : "FAILED"
                    });

                    if (!status) {
                        overallStatus = false;
                    }
                }

                // Optionally restore original value after each parameter sweep
                PaintConfig.setString("TrackMate", parameter, originalValue);
            }

        } finally {
            // Always restore PaintConfig context to the project root
            PaintConfig.reinitialise(projectPath);
            PaintLogger.infof("Restored PaintConfig to project root: %s", projectPath);
        }

        // ---------------------------------------------------------------------
        // Phase 4 – Flatten results (optional)
        // ---------------------------------------------------------------------
        if (overallStatus) {
            flattenSweep(projectPath.resolve("Sweep"), experimentNames, true);
        }

        return overallStatus;
    }

    /**
     * Command-line entry point for testing the sweep execution.
     */
    public static void main(String[] args) throws IOException {
        runWithSweep(
                Paths.get("/Users/hans/Paint Test Project"),
                Paths.get("/Volumes/Extreme Pro/Omero"),
                Arrays.asList("221012", "AnyName"));
    }
}