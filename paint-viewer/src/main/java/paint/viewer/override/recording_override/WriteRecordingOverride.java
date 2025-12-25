/*==============================================================================
 *  Class:        WriteRecordingOverride.java
 *  Package:      paint.viewer.override.recording_override
 *
 *  PURPOSE:
 *    Persist per-recording threshold override settings created in the PAINT
 *    Viewer, so user-modified filtering parameters can be restored in later
 *    Viewer sessions or applied in batch across an experiment or project.
 *
 *  DESCRIPTION:
 *    This class manages the CSV file:
 *
 *        <project>/Viewer/Recording Override.csv
 *
 *    Each CSV row represents one recording-level override identified by the
 *    composite key (experimentName, recordingName), storing:
 *
 *        experimentName,
 *        recordingName,
 *        timestamp,
 *        minRequiredDensityRatio,
 *        maxAllowableVariability,
 *        minRequiredRSquared,
 *        neighbourMode
 *
 *    The writer supports three scopes:
 *      • Recording  – update only the currently selected recording
 *      • Experiment – update all recordings in the same experiment
 *      • Project    – update every recording in the project
 *
 *    Updates are non-destructive: existing entries for the same composite key
 *    are replaced; all other rows are preserved. Writes are performed via a
 *    temporary file and a replace/move operation to reduce the chance of
 *    leaving a partially-written CSV.
 *
 *  KEY FEATURES:
 *    • Persistent storage of viewer threshold overrides.
 *    • Validates CSV header; rebuilds file if malformed.
 *    • Non-destructive updates via composite-key map.
 *    • Safe write strategy using temporary file replacement.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-12-25
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
==============================================================================*/

package paint.viewer.override.recording_override;

import paint.shared.utils.PaintLogger;
import paint.viewer.model.SquareControlParams;
import paint.viewer.model.RecordingEntry;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

import static paint.shared.constants.PaintStringConstants.*;

/**
 * Handles persistence of per-recording filter overrides in the PAINT Viewer.
 * <p>
 * Overrides are stored in:
 * <pre>
 *     Viewer/Recording Override.csv
 * </pre>
 * Each entry maps a {@code (experimentName, recordingName)} pair to a set of
 * filter threshold values chosen by the user. This enables restoring these
 * thresholds between Viewer sessions and applying batch updates across scopes.
 */
public class WriteRecordingOverride {

    /** Path to: Viewer/Recording Override.csv */
    private final Path csvFilePath;

    /**
     * Column headers for the CSV (must match the reader expectations elsewhere).
     * The file is considered invalid if the header does not match, and will be
     * rebuilt from scratch on the next write.
     */
    private static final String[] HEADER = {
            EXPERIMENT_NAME,
            RECORDING_NAME,
            TIME_STAMP,
            MIN_REQUIRED_DENSITY_RATIO,
            MAX_ALLOWABLE_VARIABILITY,
            MIN_REQUIRED_R_SQUARED,
            NEIGHBOUR_MODE
    };

    /**
     * Builds the composite key used for identifying override rows.
     *
     * @param exp experiment name
     * @param rec recording name
     * @return composite key: {@code exp + "§" + rec}
     */
    private static String key(String exp, String rec) {
        return exp + "§" + rec;
    }

    /**
     * Creates a new writer and ensures the {@code Viewer/} folder exists.
     *
     * @param projectPath root of the PAINT project
     */
    public WriteRecordingOverride(Path projectPath) {
        Path viewerPath = projectPath.resolve("Viewer");

        try {
            Files.createDirectories(viewerPath);
        } catch (IOException e) {
            PaintLogger.warnf("Failed to create Viewer directory: %s", e.getMessage());
        }

        this.csvFilePath = viewerPath.resolve("Recording Override.csv");
    }

    /**
     * Applies and writes overrides for the selected scope:
     *
     * <ul>
     *   <li>{@code "Recording"}  – update only the current recording</li>
     *   <li>{@code "Experiment"} – update all recordings in the same experiment</li>
     *   <li>{@code "Project"}    – update every recording in the viewer</li>
     * </ul>
     *
     * Overrides are written non-destructively: existing rows for the same
     * {@code (experimentName, recordingName)} pair are replaced; all other entries
     * are preserved.
     *
     * @param scope            one of: "Recording", "Experiment", "Project"
     * @param params           current UI threshold parameters to persist
     * @param recordingEntries all loaded recording entries in the viewer
     * @param currentIndex     index of the currently selected recording entry
     */
    public void writeRecordingOverridesToFile(
            String scope,
            SquareControlParams params,
            List<RecordingEntry> recordingEntries,
            int currentIndex
    ) {
        String timestamp = LocalDateTime.now().toString();

        if ("Recording".equals(scope)) {
            RecordingEntry re = recordingEntries.get(currentIndex);
            writeOrUpdate(re.getExperimentName(), re.getRecordingName(), params, timestamp);
            updateMemory(re, params);
            return;
        }

        if ("Experiment".equals(scope)) {
            String experiment = recordingEntries.get(currentIndex).getExperimentName();

            for (RecordingEntry re : recordingEntries) {
                if (re.getExperimentName().equals(experiment)) {
                    writeOrUpdate(re.getExperimentName(), re.getRecordingName(), params, timestamp);
                    updateMemory(re, params);
                }
            }
            return;
        }

        if ("Project".equals(scope)) {
            for (RecordingEntry re : recordingEntries) {
                writeOrUpdate(re.getExperimentName(), re.getRecordingName(), params, timestamp);
                updateMemory(re, params);
            }
        }
    }

    // ====================================================================================
    // WRITE / UPDATE LOGIC
    // ====================================================================================

    /**
     * Writes a new override entry or updates an existing one for the given recording.
     * All other entries are preserved unchanged.
     *
     * @param experimentName experiment identifier
     * @param recordingName  recording identifier
     * @param params         threshold values to store
     * @param timestamp      ISO timestamp representing when the override was written
     */
    private void writeOrUpdate(
            String experimentName,
            String recordingName,
            SquareControlParams params,
            String timestamp
    ) {
        Map<String, String> map = loadExistingRows();

        String line =
                experimentName + "," +
                        recordingName  + "," +
                        timestamp      + "," +
                        params.minRequiredDensityRatio + "," +
                        params.maxAllowableVariability + "," +
                        params.minRequiredRSquared     + "," +
                        params.neighbourMode;

        map.put(key(experimentName, recordingName), line);
        writeAll(map);
    }

    // ====================================================================================
    // READ CSV INTO MAP
    // ====================================================================================

    /**
     * Loads the existing override rows from disk and returns them as a map keyed by
     * {@code experimentName§recordingName}.
     * <p>
     * If the CSV does not exist, an empty map is returned.
     * If the header is not exactly as expected, the file is treated as invalid and
     * ignored (empty map returned), so the next write recreates it.
     *
     * @return map of {@code compositeKey → CSV row}
     */
    private Map<String, String> loadExistingRows() {
        Map<String, String> map = new LinkedHashMap<>();

        if (!Files.exists(csvFilePath)) {
            return map;
        }

        try {
            List<String> lines = Files.readAllLines(csvFilePath);
            if (lines.isEmpty()) return map;

            String expectedHeader = String.join(",", HEADER);

            // Header is invalid → ignore file and rebuild on next write.
            if (!lines.get(0).equalsIgnoreCase(expectedHeader)) {
                PaintLogger.warnf("Invalid Recording Override.csv header. Rebuilding.");
                return map;
            }

            // Parse entries (store full line, indexed by composite key).
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", 3);
                if (parts.length < 2) continue;

                String exp = parts[0];
                String rec = parts[1];

                map.put(key(exp, rec), line);
            }

        } catch (IOException ex) {
            PaintLogger.errorf("Failed reading Recording Override.csv: %s", ex.getMessage());
        }

        return map;
    }

    // ====================================================================================
    // WRITE ALL ROWS (ATOMIC)
    // ====================================================================================

    /**
     * Writes all override entries back to disk using a temporary file and replace/move.
     * <p>
     * This reduces the risk of leaving a truncated CSV if the process is interrupted.
     *
     * @param map map of {@code compositeKey → CSV row}
     */
    private void writeAll(Map<String, String> map) {
        String header = String.join(",", HEADER);

        List<String> out = new ArrayList<>();
        out.add(header);
        out.addAll(map.values());

        Path tmp = csvFilePath.resolveSibling(csvFilePath.getFileName() + ".tmp");

        try {
            Files.write(tmp, out);
            Files.move(tmp, csvFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            PaintLogger.errorf("Failed writing Recording Override.csv: %s", e.getMessage());
        }
    }

    // ====================================================================================
    // UPDATE IN-MEMORY RECORDING ENTRY
    // ====================================================================================

    /**
     * Updates the in-memory {@link paint.shared.objects.Recording} referenced by the
     * provided {@link RecordingEntry} so the UI and filtering logic immediately reflect
     * the current override values.
     *
     * @param re     recording entry to update
     * @param params source threshold values (typically from the UI)
     */
    private void updateMemory(RecordingEntry re, SquareControlParams params) {
        re.getRecording().setMinRequiredDensityRatio(params.minRequiredDensityRatio);
        re.getRecording().setMaxAllowableVariability(params.maxAllowableVariability);
        re.getRecording().setMinRequiredRSquared(params.minRequiredRSquared);
        re.getRecording().setNeighbourMode(params.neighbourMode);
    }
}