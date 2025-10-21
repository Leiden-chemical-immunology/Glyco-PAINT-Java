package viewer.utils;

import paint.shared.config.PaintConfig;
import paint.shared.objects.Project;
import paint.shared.objects.Recording;
import paint.shared.utils.PaintLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.io.HelperIO.readAllRecordings;

/**
 * Loads {@link RecordingEntry} objects from a {@link Project} by scanning its experiment directories.
 * <p>
 * This class reads metadata and image paths for each {@link Recording} in a project,
 * validating the existence of required TrackMate and BrightField image files.
 * It also retrieves default analysis parameters (e.g. density ratio, variability, R² thresholds)
 * from {@link PaintConfig}.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Iterate through experiment directories in a {@link Project}</li>
 *   <li>Read all {@link Recording} definitions from each experiment folder</li>
 *   <li>Verify required images exist before creating {@link RecordingEntry} objects</li>
 *   <li>Attach configuration-derived filtering parameters</li>
 * </ul>
 *
 * <p><b>Note:</b> Missing files or invalid directories are logged using {@link PaintLogger}.</p>
 */
public class RecordingLoader {

    /**
     * Loads all valid {@link RecordingEntry} instances from a {@link Project}.
     * <p>
     * Each experiment directory under the project root is processed as follows:
     * <ol>
     *   <li>Reads all {@link Recording} objects using {@code readAllRecordings(experimentPath)}</li>
     *   <li>Filters out recordings marked as non-processable</li>
     *   <li>Verifies presence of required TrackMate and BrightField image files</li>
     *   <li>Retrieves control parameters from {@link PaintConfig}</li>
     *   <li>Builds and collects {@link RecordingEntry} objects</li>
     * </ol>
     *
     * @param project the {@link Project} containing experiment directories and recordings
     * @return a list of {@link RecordingEntry} objects representing valid recordings ready for display or analysis
     */
    public static List<RecordingEntry> loadFromProject(Project project) {

        List<RecordingEntry> entries = new ArrayList<>();

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
                double maxVariability  = PaintConfig.getDouble("Generate Squares", "Max Allowable Variability", 10.0);
                double minRSquared     = PaintConfig.getDouble("Generate Squares", "Min Required R Squared", 0.1);
                String neighbourMode   = PaintConfig.getString("Generate Squares", "Neighbour Mode", "Free");
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

                entries.add(entry);
            }
        }
        return entries;
    }
}