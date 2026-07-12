/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.viewer.model;

import paint.shared.io.ExperimentDataLoader;
import paint.shared.objects.Experiment;
import paint.shared.objects.Project;
import paint.shared.objects.Recording;
import paint.shared.utils.PaintLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
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
                if (!recording.isProcessFlagSet()) {
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

                try (java.util.stream.Stream<Path> stream = Files.list(brightfieldDirPath)) {
                    for (Iterator<Path> it = stream.iterator(); it.hasNext(); ) {
                        Path p = it.next();
                        String fileName = p.getFileName().toString();

                        // Match "<recording name>.jpg", "<recording name>-BF.jpg" and similar.
                        //
                        // The name must not be followed by a digit: recording names end in a
                        // number, so a plain startsWith() lets recording "…-A4-1" match the file
                        // of "…-A4-10" and display the wrong brightfield image. (The old
                        // "-BF" clause here was dead: startsWith(name + "-BF") can only be true
                        // when startsWith(name) already is.)
                        boolean isMatch = false;
                        if (fileName.endsWith(".jpg") && fileName.startsWith(recordingName)) {
                            String rest = fileName.substring(recordingName.length());
                            isMatch = rest.isEmpty() || !Character.isDigit(rest.charAt(0));
                        }

                        if (isMatch) {
                            brightfieldImagePath = p;
                            break;
                        }
                    }
                } catch (Exception e) {
                    PaintLogger.errorf("Error scanning Brightfield directory: %s", e.getMessage());
                }

                if (brightfieldImagePath == null) {
                    PaintLogger.warnf("Missing Brightfield image for recording '%s'", recordingName);
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