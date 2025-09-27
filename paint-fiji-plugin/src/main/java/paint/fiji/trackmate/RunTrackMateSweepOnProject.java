package paint.fiji.trackmate;

import paint.shared.config.PaintConfig;
import paint.shared.config.SweepConfig;
import paint.shared.utils.PaintLogger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Executes TrackMate on a project while sweeping over configuration parameters.
 * Results are written into per-value subdirectories under Sweep/, with a summary CSV.
 */
public class RunTrackMateSweepOnProject {

    /**
     * Runs TrackMate on the given project with sweep parameters if present.
     *
     * @param projectPath     base project path
     * @param imagesPath      path to the image root
     * @param experimentNames experiments to process
     * @return true if all sweeps completed successfully, false if any failed
     * @throws IOException if sweep config cannot be read
     */
    public static boolean runWithSweep(Path projectPath,
                                       Path imagesPath,
                                       List<String> experimentNames) throws IOException {
        Path sweepFile = projectPath.resolve("Sweep Config.json");
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

        for (Map.Entry<String, List<Number>> entry : sweeps.entrySet()) {
            String parameter = entry.getKey();
            List<Number> values = entry.getValue();

            String originalValue = PaintConfig.getString("TrackMate", parameter, "undefined");

            for (Number val : values) {
                PaintLogger.infof("Running sweep for %s = %s", parameter, val);

                // Update PaintConfig with correct type
                if (val.doubleValue() == val.intValue()) {
                    PaintConfig.setInt("TrackMate", parameter, val.intValue());
                } else {
                    PaintConfig.setDouble("TrackMate", parameter, val.doubleValue());
                }

                Path sweepDir = projectPath.resolve("Sweep").resolve(parameter).resolve(val.toString());
                sweepDir.toFile().mkdirs();

                // --- Copy each experiment's Experiment Info.csv ---
                for (String expName : experimentNames) {
                    Path expSrc = projectPath.resolve(expName).resolve("Experiment Info.csv");
                    Path expDstDir = sweepDir.resolve(expName);
                    Path expDst = expDstDir.resolve("Experiment Info.csv");
                    try {
                        if (Files.exists(expSrc)) {
                            Files.createDirectories(expDstDir);
                            Files.copy(expSrc, expDst, StandardCopyOption.REPLACE_EXISTING);
                            //vPaintLogger.infof("Copied Experiment Info.csv for %s to %s", expName, expDst);
                        } else {
                            PaintLogger.warningf("Experiment Info.csv not found for %s at %s", expName, expSrc);
                        }
                    } catch (IOException e) {
                        PaintLogger.errorf("Failed to copy Experiment Info.csv for %s: %s", expName, e.getMessage());
                    }
                }

                boolean status = RunTrackMateOnProjectCore.runProject(
                        projectPath, imagesPath, experimentNames, null, sweepDir);

                summaryRows.add(new String[]{
                        parameter, val.toString(), sweepDir.toString(),
                        status ? "SUCCESS" : "FAILED"
                });

                if (!status) {
                    overallStatus = false;
                }
            }

            // Restore original PaintConfig value
            try {
                int intVal = Integer.parseInt(originalValue);
                PaintConfig.setInt("TrackMate", parameter, intVal);
            } catch (NumberFormatException e1) {
                try {
                    double dblVal = Double.parseDouble(originalValue);
                    PaintConfig.setDouble("TrackMate", parameter, dblVal);
                } catch (NumberFormatException e2) {
                    PaintConfig.setString("TrackMate", parameter, originalValue);
                }
            }
            PaintLogger.infof("Restored %s to %s", parameter, originalValue);
        }

        // Write summary CSV
        Path summaryFile = projectPath.resolve("sweep_summary.csv");
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(summaryFile.toFile()),
                CSVFormat.DEFAULT.withHeader("Parameter", "Value", "Result Directory", "Status"))) {
            for (String[] row : summaryRows) {
                printer.printRecord((Object[]) row);
            }
        }
        PaintLogger.infof("Sweep summary written to %s", summaryFile);

        return overallStatus;
    }
}