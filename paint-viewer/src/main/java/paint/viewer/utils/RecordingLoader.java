package paint.viewer.utils;

import paint.shared.config.PaintConfig;
import paint.shared.io.ExperimentDataLoader;
import paint.shared.objects.Experiment;
import paint.shared.objects.Project;
import paint.shared.objects.Recording;
import paint.shared.utils.PaintLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.io.HelperIO.readAllRecordings;

/**
 * The RecordingLoader class provides functionality for loading and filtering
 * recordings from a project directory structure. It ensures that the recordings
 * meet specific process requirements and that necessary file dependencies exist.
 */
public class RecordingLoader {

    /**
     * Loads a list of recording entries from the specified project. The method iterates
     * through all experiment names in the project, reads recordings, and constructs
     * valid recording entries based on specific conditions and configurations.
     *
     * @param project the project from which recordings and their associated data will be loaded.
     *                This project is expected to contain a root path, experiment names,
     *                and relevant directories or files required for constructing recording entries.
     * @return a list of {@link RecordingEntry} objects representing the recordings and their
     *         associated metadata loaded from the project. The list will be empty if no valid
     *         recording entries are found.
     */
    public static List<RecordingEntry> loadFromProject(Project project) {

        List<RecordingEntry> recordingEntries = new ArrayList<>();

        for (String experimentName : project.getExperimentNames()) {

            Path experimentPath = project.getProjectRootPath().resolve(experimentName);

            List<Recording> recordings = readAllRecordings(experimentPath);
            if (recordings == null || recordings.isEmpty()) {
                continue;
            }

            for (Recording recording : recordings) {
                String recordingName = recording.getRecordingName();
                if (!recording.isProcessFlag()) {
                    continue;
                }

                // --- Image paths ---
                Path trackmateImagePath = experimentPath
                        .resolve("TrackMate Images")
                        .resolve(recordingName + ".jpg");

                if (!Files.exists(trackmateImagePath)) {
                    PaintLogger.errorf("Missing TrackMate image for '%s'", recordingName);
                    continue;
                }

                Path brightfieldDirPath = experimentPath.resolve("BrightField Images");
                Path brightfieldImagePath = null;

                if (!Files.isDirectory(brightfieldDirPath)) {
                    PaintLogger.errorf("Missing Brightfield directory '%s' image for recording '%s'",
                                       brightfieldDirPath, recordingName);
                    continue;
                }

                try {
                    for (Path p : (Iterable<Path>) Files.list(brightfieldDirPath)::iterator) {
                        String fileName = p.getFileName().toString();
                        if ((fileName.startsWith(recordingName + "-BF") || fileName.startsWith(recordingName))
                                && fileName.endsWith(".jpg")) {
                            brightfieldImagePath = p;
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (brightfieldImagePath == null) {
                    PaintLogger.errorf("Missing BrightField image for '%s'", recordingName);
                    continue;
                }

                // --- Thresholds from config ---
                // @formatter:off
                double minDensityRatio = PaintConfig.getDouble("Generate Squares", "Min Required Density Ratio", 2.0);
                double maxVariability  = PaintConfig.getDouble("Generate Squares", "Max Allowable Variability",  10.0);
                double minRSquared     = PaintConfig.getDouble("Generate Squares", "Min Required R Squared",     0.1);
                String neighbourMode   = PaintConfig.getString("Generate Squares", "Neighbour Mode",             "Free");
                // @formatter:on

                // --- Build final entry ---
                RecordingEntry entry = new RecordingEntry(
                        recording,
                        trackmateImagePath,
                        brightfieldImagePath,
                        experimentName,
                        minDensityRatio,
                        maxVariability,
                        minRSquared,
                        neighbourMode
                );

                recordingEntries.add(entry);
            }
        }

        return recordingEntries;
    }
}