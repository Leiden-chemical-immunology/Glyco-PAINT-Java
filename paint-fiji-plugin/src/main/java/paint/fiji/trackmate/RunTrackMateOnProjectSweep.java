package paint.fiji.trackmate;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import paint.shared.config.PaintConfig;
import paint.shared.config.SweepConfig;
import paint.shared.utils.PaintLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import static paint.fiji.utils.SweepFlattener.flattenSweep;
import static paint.shared.constants.PaintConstants.*;

/**
 * Executes TrackMate on a project while sweeping over configuration parameters.
 * Results are written into per-value subdirectories under Sweep/, with a summary CSV.
 */
public class RunTrackMateOnProjectSweep {

    /**
     * Runs TrackMate on the given project with sweep parameters if enabled.
     *
     * @param projectPath     path to the project root
     * @param imagesPath      path to the image root
     * @param experimentNames experiments to process
     * @return                true if all sweeps completed successfully, false if any failed
     * @throws IOException    if sweep config cannot be read
     */
    public static boolean runWithSweep(Path projectPath,
                                       Path imagesPath,
                                       List<String> experimentNames) throws IOException {

        Path sweepFile = projectPath.resolve(PAINT_SWEEP_CONFIGURATION_JSON);
        if (!Files.exists(sweepFile)) {
            PaintLogger.infof("No sweep configuration found at %s", sweepFile);
            return false;
        }

        SweepConfig sweepConfig = new SweepConfig(sweepFile.toString());

        // 🔑 Require Sweep Settings.Sweep = true
        boolean sweepEnabled = sweepConfig.getBoolean("Sweep Settings", "Sweep", false);
        if (!sweepEnabled) {
            PaintLogger.infof("Sweep configuration present, but sweep mode disabled.");
            return false;
        }

        Map<String, List<Number>> sweeps = sweepConfig.getActiveSweepValues("TrackMate Sweep");
        if (sweeps.isEmpty()) {
            PaintLogger.infof("Sweep enabled, but no active sweep parameters defined.");
            return false;
        }

        // --- Sweep mode - we really have a viable sweep configuration ---

        // Sweeps is a Hash<ap that contains the parameter that need to be swept, e.g. MAX_FRAME_GAP
        // and the values those parameters take

        boolean overallStatus = true;
        List<String[]> summaryRows = new ArrayList<>();

        try {
            for (Map.Entry<String, List<Number>> entry : sweeps.entrySet()) {
                // Cycle through the parameters
                String parameter    = entry.getKey();       // Holds a parameter that we sweep
                List<Number> values = entry.getValue();     // Values holds the values

                String originalValue = PaintConfig.getString("TrackMate", parameter, "undefined");

                for (Number val : values) {
                    // Cycle through the values
                    PaintLogger.infof("Running sweep for %s = %s", parameter, val);

                    // Create the sweepPOath, delete if exists and recreate empty
                    Path sweepPath = projectPath.resolve("Sweep").resolve("[" + parameter + "]-[" + val + "]");
                    FileUtils.deleteDirectory(sweepPath.toFile());
                    Files.createDirectory(sweepPath);

                    // --- Copy baseline PaintConfig.json into sweep dir ---
                    Path baselineConfig = projectPath.resolve(PAINT_CONFIGURATION_JSON);
                    Path configCopy     = sweepPath.resolve(PAINT_CONFIGURATION_JSON);
                    try {
                        Files.copy(baselineConfig, configCopy, StandardCopyOption.REPLACE_EXISTING);
                        PaintLogger.infof("Copied baseline PaintConfig.json to %s", configCopy);
                    } catch (IOException e) {
                        PaintLogger.errorf("Failed to copy PaintConfig.json to %s: %s",
                                           configCopy, e.getMessage());
                    }

                    // Reinitialise PaintConfig to point at sweepDir
                    PaintConfig.reinitialise(sweepPath);

                    // Apply parameter update in sweep config
                    if (val.doubleValue() == val.intValue()) {
                        PaintConfig.setInt("TrackMate", parameter, val.intValue());
                    } else {
                        PaintConfig.setDouble("TrackMate", parameter, val.doubleValue());
                    }
                    // Save updated sweepDir JSON
                    PaintConfig.instance().save();

                    // --- Copy each experiment's Experiment Info.csv ---
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

                    // Run TrackMate in sweepDir
                    boolean status = RunTrackMateOnProject.runProject(projectPath, imagesPath, experimentNames, null, sweepPath);

                    summaryRows.add(new String[]{
                            parameter, val.toString(), sweepPath.toString(),
                            status ? "SUCCESS" : "FAILED"
                    });

                    if (!status) {
                        overallStatus = false;
                    }
                }
            }
        } finally {
            // Always restore PaintConfig to project root at the end
            PaintConfig.reinitialise(projectPath);
            PaintLogger.infof("Reinitialised original PaintConfig back to project root: %s", projectPath);
        }

        // Write summary CSV
        Path outPath = projectPath.resolve("Out");
        if (!Files.exists(outPath)) {
            Files.createDirectories(outPath);
        }
        Path summaryFile = outPath.resolve("Sweep Summary.csv");
        try (CSVPrinter printer = new CSVPrinter(
                new FileWriter(summaryFile.toFile()),
                CSVFormat.DEFAULT.builder()
                        .setHeader("Parameter", "Value", "Result Directory", "Status")
                        .build()
        )) {
            for (String[] row : summaryRows) {
                printer.printRecord((Object[]) row);
            }
        }
        PaintLogger.infof("Sweep summary written to %s", summaryFile);

        if (overallStatus) {
            flattenSweep(projectPath.resolve("Sweep"), experimentNames, true);
        }
        return overallStatus;
    }

    public static void main(String[] args) throws IOException {
        runWithSweep(
                Paths.get("/Users/hans/Paint Test Project"),
                Paths.get("/Volumes/Extreme Pro/Omero"),
                Arrays.asList("221012", "AnyName"));
    }
}