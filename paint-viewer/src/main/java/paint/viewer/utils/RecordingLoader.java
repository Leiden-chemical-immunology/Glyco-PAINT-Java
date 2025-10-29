/******************************************************************************
 *  Class:        RecordingLoader.java
 *  Package:      paint.viewer.utils
 *
 *  PURPOSE:
 *    Loads and validates recording data and associated images from a PAINT
 *    project directory, constructing {@link RecordingEntry} instances that
 *    represent complete experiment recordings.
 *
 *  DESCRIPTION:
 *    The {@code RecordingLoader} iterates over all experiment folders within
 *    a project, loads each experiment using {@link paint.shared.io.ExperimentDataLoader},
 *    and constructs {@link RecordingEntry} objects for all recordings that meet
 *    process and file-availability requirements.
 *
 *    Each recording entry combines metadata, images (TrackMate and Brightfield),
 *    and configuration thresholds loaded from {@link paint.shared.config.PaintConfig}.
 *    Invalid or incomplete recordings are skipped with diagnostic logging.
 *
 *  KEY FEATURES:
 *    • Loads complete experiments with squares and track data.
 *    • Validates existence of TrackMate and Brightfield images.
 *    • Reads density, variability, and R² thresholds from configuration.
 *    • Constructs structured {@link RecordingEntry} objects for UI or analysis use.
 *    • Provides detailed logging via {@link paint.shared.utils.PaintLogger}.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-10-29
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

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
 * Provides functionality for loading and filtering recordings from a project directory.
 * <p>
 * The {@code RecordingLoader} ensures that required images and metadata exist for each
 * recording, applying configuration-defined thresholds to build {@link RecordingEntry}
 * instances ready for visualization or further processing.
 * </p>
 */
public class RecordingLoader {

    /**
     * Loads all {@link RecordingEntry} instances from the specified {@link Project}.
     * <p>
     * This method iterates over all experiment directories, loads their full content
     * (including squares and tracks), and constructs valid entries only for recordings
     * meeting both process requirements and file availability conditions.
     * </p>
     *
     * @param project the project context containing experiments, recordings, and file data
     * @return list of valid {@link RecordingEntry} instances; empty if no valid recordings found
     */
    public static List<RecordingEntry> loadFromProject(Project project) {

        List<RecordingEntry> recordingEntries = new ArrayList<>();

        for (String experimentName : project.getExperimentNames()) {

            Path experimentPath = project.getProjectRootPath().resolve(experimentName);

            // ✅ Load full experiment (recordings + tracks + squares)
            Experiment experiment = ExperimentDataLoader.loadExperiment(
                    project.getProjectRootPath(),
                    experimentName,
                    true // matureProject: includes squares + tracks
            );

            if (experiment == null || experiment.getRecordings().isEmpty()) {
                continue;
            }

            for (Recording recording : experiment.getRecordings()) {
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