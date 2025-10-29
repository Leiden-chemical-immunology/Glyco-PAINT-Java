/******************************************************************************
 *  Class:        RecordingOverrideWriter.java
 *  Package:      paint.viewer.logic
 *
 *  PURPOSE:
 *    Manages persistence of recording-level parameter overrides in the
 *    PAINT viewer, enabling density ratio, variability, R² threshold,
 *    and neighbour mode adjustments to be stored, updated, and restored
 *    across sessions.
 *
 *  DESCRIPTION:
 *    The {@code RecordingOverrideWriter} class writes and maintains a
 *    structured CSV file named {@code Recording Override.csv} that
 *    contains control parameter overrides for each recording. Each
 *    entry represents a distinct configuration of user-applied square
 *    control parameters, written with a timestamp and recording name.
 *
 *    The writer supports three levels of application scope:
 *      • Recording — Applies overrides to the currently active recording.
 *      • Experiment — Applies overrides to all recordings in an experiment.
 *      • Project — Applies overrides globally to all recordings in a project.
 *
 *    The writer automatically creates the Viewer directory if missing,
 *    validates the CSV header, replaces existing entries for the same
 *    recording, and performs atomic write operations to ensure file safety.
 *
 *  KEY FEATURES:
 *    • Persists per-recording parameter overrides in a consistent CSV format.
 *    • Automatically creates and maintains headers and directory structure.
 *    • Supports Recording, Experiment, and Project scopes.
 *    • Performs atomic writes to prevent data corruption.
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

package paint.viewer.logic;

import paint.shared.objects.Track;
import paint.shared.utils.CalculateTau;
import paint.shared.utils.PaintLogger;
import paint.viewer.utils.RecordingEntry;
import paint.viewer.shared.SquareControlParams;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.utils.CalculateTau.calculateTau;
import static paint.shared.utils.SharedSquareUtils.getTracksFromSelectedSquares;

/**
 * Handles the writing of per-recording override configurations for the
 * PAINT viewer. The {@code RecordingOverrideWriter} manages the creation
 * and updating of a CSV file that stores user-defined control parameters
 * (density ratio, variability, R², and neighbour mode) for each recording.
 *
 * <p>Each record is timestamped and replaces any previous entry for the
 * same recording. The file is stored in the {@code Viewer} directory
 * under the project root.
 *
 * <p>This class mirrors {@link paint.viewer.logic.SquareOverrideWriter}
 * but operates at the recording scope instead of per-square granularity.
 */
public class ViewerOverrideWriter {
    private final Path csvFilePath;

    /**
     * Constructs a new {@code RecordingOverrideWriter} for the specified
     * project. Ensures that the Viewer directory exists and initializes
     * the target file path for the override CSV.
     *
     * @param projectPath the root project path where the Viewer directory resides
     */
    public RecordingOverrideWriter(Path projectPath) {
        Path viewerPath = projectPath.resolve("Viewer");
        if (Files.notExists(viewerPath)) {
            try {
                Files.createDirectories(viewerPath);          // creates all missing parents too
            } catch (IOException e) {
            }
        }
        this.csvFilePath = viewerPath.resolve("Recording Override.csv");
    }

    /**
     * Applies the provided parameters and writes overrides based on the
     * specified scope. The scope determines whether overrides are applied
     * at the Recording, Experiment, or Project level.
     *
     * @param scope        the override scope ("Recording", "Experiment", or "Project")
     * @param params       the parameter set containing density ratio, variability,
     *                     R², and neighbour mode values
     * @param recordings   the list of available recording entries in the current session
     * @param currentIndex the index of the currently active recording
     */
    public void applyAndWrite(String               scope,
                              SquareControlParams  params,
                              List<RecordingEntry> recordings,
                              int                  currentIndex) {
        String timestamp = LocalDateTime.now().toString();

        if ("Recording".equals(scope)) {
            RecordingEntry recordingEntry = recordings.get(currentIndex);
            writeOverrideRecord(recordingEntry.getRecordingName(), params, timestamp);
            update(recordingEntry, params);

        } else if ("Experiment".equals(scope)) {
            RecordingEntry currentRecordingEntry = recordings.get(currentIndex);
            for (RecordingEntry recordingEntry : recordings) {
                if (recordingEntry.getExperimentName().equals(currentRecordingEntry.getExperimentName())) {
                    writeOverrideRecord(recordingEntry.getRecordingName(), params, timestamp);
                    update(recordingEntry, params);
                }
            }

        } else if ("Project".equals(scope)) {
            for (RecordingEntry recordingEntry : recordings) {
                writeOverrideRecord(recordingEntry.getRecordingName(), params, timestamp);
                update(recordingEntry, params);
            }
        }
    }

    /**
     * Writes a single override record into the CSV file using the provided
     * recording name, parameters, and timestamp. Existing entries for the
     * same recording are replaced. Headers are automatically added if
     * missing or invalid.
     *
     * @param recordingName the name of the recording to which the override applies
     * @param params        the parameter values to be persisted
     * @param timestamp     the ISO-formatted timestamp of the override entry
     */
    private void writeOverrideRecord(String              recordingName,
                                     SquareControlParams params,
                                     String              timestamp) {
        PaintLogger.infof(
                "Override for '%s': MinRequiredDensityRatio=%.0f, MaxAllowableVariability=%.0f, MinRequiredRSquared=%.2f, NeighbourMode=%s",
                recordingName, params.minRequiredDensityRatio, params.maxAllowableVariability, params.minRequiredRSquared, params.neighbourMode
        );

        try {
            List<String> lines = new ArrayList<>();
            if (Files.exists(csvFilePath)) {
                lines = Files.readAllLines(csvFilePath);
            }

            // Initialize header if missing or malformed
            if (lines.isEmpty() || !lines.get(0).startsWith("recordingName,")) {
                lines.clear();
                lines.add("recordingName,timestamp,MinRequiredDensityRatio,MaxAllowableVariability,minRequiredRSquared,neighbourMode");
            }

            String prefix  = recordingName + ",";
            String newLine = recordingName + "," + timestamp + "," +
                    params.minRequiredDensityRatio + "," + params.maxAllowableVariability + "," +
                    params.minRequiredRSquared + "," + params.neighbourMode;

            boolean replaced = false;
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).startsWith(prefix)) {
                    lines.set(i, newLine);
                    replaced = true;
                    break;
                }
            }

            if (!replaced) {
                lines.add(newLine);
            }

            Path tmpFilePath = csvFilePath.resolveSibling(csvFilePath.getFileName().toString() + ".tmp");Files.write(tmpFilePath, lines);
            // Atomic write using a temporary file for reliability
            Files.write(tmpFilePath, lines);
            Files.move(tmpFilePath, csvFilePath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Updates the in-memory recording entry based on the provided parameters
     * and recalculates Tau and R² values from the selected squares.
     *
     * @param recordingEntry the recording entry to update
     * @param params         the parameter set containing updated control values
     */
    private void update(RecordingEntry      recordingEntry,
                        SquareControlParams params) {

        List<Track> tracksFromSelectedSquares = getTracksFromSelectedSquares(
                recordingEntry.getRecording().getSquaresOfRecording());

        CalculateTau.CalculateTauResult results = calculateTau(
                tracksFromSelectedSquares, params.minRequiredRSquared);

        recordingEntry.getRecording().setTau(results.getTau());
        recordingEntry.getRecording().setRSquared(results.getRSquared());
        recordingEntry.setMaxAllowableVariability(params.maxAllowableVariability);
        recordingEntry.setMinRequiredDensityRatio(params.minRequiredDensityRatio);
        recordingEntry.setNeighbourMode(params.neighbourMode);
        recordingEntry.setMinRequiredRSquared(params.minRequiredRSquared);
    }
}