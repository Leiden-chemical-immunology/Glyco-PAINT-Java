/*==============================================================================
 *  Class:        RecordingLoader.java
 *  Package:      paint.viewer.model
 *
 *  PURPOSE:
 *    Loads and validates all experiment recordings for a PAINT project,
 *    constructing fully populated {@link RecordingEntry} objects that include
 *    metadata, square data, and preloaded image resources.
 *
 *  DESCRIPTION:
 *    The {@code RecordingLoader} iterates over experiment folders inside a
 *    {@link paint.shared.objects.Project}, loads each experiment using
 *    {@link paint.shared.io.ExperimentDataLoader}, and creates a
 *    {@link RecordingEntry} for each recording that:
 *
 *      • Has its process flag enabled.
 *      • Has a valid TrackMate overlay image.
 *      • Has a matching Brightfield reference image.
 *
 *    Recordings missing mandatory resources are skipped with diagnostic logging.
 *    TrackMate images are expected in:
 *
 *      <experiment>/TrackMate Images/<recording>.jpg
 *
 *    Brightfield images are expected in:
 *
 *      <experiment>/Brightfield Images/<recording>[-BF*].jpg
 *
 *  KEY FEATURES:
 *    • Loads experiments, recordings, squares (no tracks).
 *    • Validates all required image assets.
 *    • Returns a complete, ready-to-display list of {@link RecordingEntry}.
 *    • Uses {@link paint.shared.utils.PaintLogger} for structured diagnostics.
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
 *==============================================================================*/

package paint.viewer.model;

import paint.shared.io.ExperimentDataLoader;
import paint.shared.objects.Experiment;
import paint.shared.objects.Project;
import paint.shared.objects.Recording;
import paint.shared.utils.PaintLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and validates {@link RecordingEntry} objects for a full PAINT project.
 *
 * <p>The loader ensures:</p>
 * <ul>
 *   <li>Each recording has its process flag set.</li>
 *   <li>Required TrackMate and Brightfield images exist.</li>
 *   <li>Square data is loaded via {@link ExperimentDataLoader}.</li>
 * </ul>
 *
 * <p>Invalid or incomplete recordings are skipped, and diagnostic messages are
 * logged via {@link PaintLogger}. Returned entries are ready for direct use in
 * the PAINT viewer.</p>
 */
public class RecordingLoader {

    /**
     * Loads all valid {@link RecordingEntry} objects from a project.
     *
     * @param project the project containing experiment directories and metadata
     * @return list of validated {@link RecordingEntry} objects; empty if none valid
     */
    public static List<RecordingEntry> loadFromProject(Project project) {

        List<RecordingEntry> recordingEntries = new ArrayList<>();

        for (String experimentName : project.getExperimentNames()) {

            Path experimentPath = project.getProjectRootPath().resolve(experimentName);

            // ------------------------------------------------------------------
            // Load full experiment (squares only; tracks intentionally disabled)
            // ------------------------------------------------------------------
            Experiment experiment = ExperimentDataLoader.loadExperiment(
                    project.getProjectRootPath(),
                    experimentName,
                    true,   // load squares
                    false   // do not load tracks
            );

            if (experiment == null || experiment.getRecordings().isEmpty()) {
                PaintLogger.warnf("Experiment '%s' contains no valid recordings.", experimentName);
                continue;
            }

            for (Recording recording : experiment.getRecordings()) {

                String recordingName = recording.getRecordingName();

                // Skip recordings not flagged for processing
                if (!recording.isProcessFlag()) {
                    continue;
                }

                // ------------------------------------------------------------------
                // TrackMate image (*.jpg)
                // ------------------------------------------------------------------
                Path trackmateImagePath = experimentPath
                        .resolve("TrackMate Images")
                        .resolve(recordingName + ".jpg");

                if (!Files.exists(trackmateImagePath)) {
                    PaintLogger.errorf(
                            "Missing TrackMate image for recording '%s' (%s)",
                            recordingName, trackmateImagePath
                    );
                    continue;
                }

                // ------------------------------------------------------------------
                // Brightfield images (recording*.jpg or recording-BF*.jpg)
                // ------------------------------------------------------------------
                Path brightfieldDirPath = experimentPath.resolve("Brightfield Images");

                if (!Files.isDirectory(brightfieldDirPath)) {
                    PaintLogger.errorf(
                            "Missing Brightfield directory '%s' for recording '%s'",
                            brightfieldDirPath, recordingName
                    );
                    continue;
                }

                Path brightfieldImagePath = null;

                try {
                    for (Path p : (Iterable<Path>) Files.list(brightfieldDirPath)::iterator) {
                        String fileName = p.getFileName().toString();

                        // Accept "<recording>.jpg" OR "<recording>-BF*.jpg"
                        boolean isMatch =
                                (fileName.startsWith(recordingName + "-BF") ||
                                        fileName.startsWith(recordingName))
                                        && fileName.endsWith(".jpg");

                        if (isMatch) {
                            brightfieldImagePath = p;
                            break;
                        }
                    }
                } catch (Exception e) {
                    PaintLogger.errorf("Error scanning Brightfield directory: %s", e.getMessage());
                }

                if (brightfieldImagePath == null) {
                    PaintLogger.errorf("Missing Brightfield image for recording '%s'", recordingName);
                    continue;
                }

                // ------------------------------------------------------------------
                // Construct final entry
                // ------------------------------------------------------------------
                RecordingEntry entry = new RecordingEntry(
                        recording,
                        trackmateImagePath,
                        brightfieldImagePath,
                        experimentName
                );

                recordingEntries.add(entry);
            }
        }

        return recordingEntries;
    }
}