package paint.fiji.trackmate;

import paint.shared.config.PaintConfig;
import paint.shared.config.SweepConfig;
import paint.shared.utils.PaintLogger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
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
        File sweepFile = projectPath.resolve("Sweep Config.json").toFile();
        if (!sweepFile.exists()) {
            PaintLogger.infof("No sweep configuration found at %s", sweepFile.getAbsolutePath());
            return false;
        }

        SweepConfig sweepConfig = new SweepConfig(sweepFile.getAbsolutePath());
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