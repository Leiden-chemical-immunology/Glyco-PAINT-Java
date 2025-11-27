/*==============================================================================
 *  Class:        RecordingOverrideWriter.java
 *  Package:      paint.viewer.override
 *
 *  PURPOSE:
 *    Persists per-recording override settings in the PAINT Viewer, allowing
 *    user-modified filter thresholds (density ratio, variability, R², neighbour
 *    mode) to be stored, updated, and restored across viewer sessions.
 *
 *  DESCRIPTION:
 *    This class writes and maintains the CSV file:
 *
 *        Viewer/Recording Override.csv
 *
 *    Each row corresponds to a specific recording and stores:
 *
 *        experimentName,
 *        recordingName,
 *        timestamp,
 *        minRequiredDensityRatio,
 *        maxAllowableVariability,
 *        minRequiredRSquared,
 *        neighbourMode
 *
 *    The writer supports:
 *       • Updating a single recording
 *       • Applying overrides to an entire experiment
 *       • Applying overrides project-wide
 *       • Maintaining existing overrides non-destructively
 *       • Atomic CSV writes using temporary replacement
 *
 *  KEY FEATURES:
 *    • Persistent per-recording threshold overrides.
 *    • Robust CSV header validation and recovery.
 *    • Non-destructive update logic.
 *    • Atomic write operations to avoid corruption.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-10-30
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
==============================================================================*/

package paint.viewer.override;

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
 *
 * Each entry maps a {@code (experimentName, recordingName)} pair to filter
 * threshold values chosen by the user. This enables restoring user-defined
 * thresholds between viewer sessions or applying batch updates.
 */
public class RecordingOverrideWriter {

    /** Path to: Viewer/Recording Override.csv */
    private final Path csvFilePath;

    /** Column headers for the CSV */
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
     * Builds a composite key for identifying override entries.
     */
    private static String key(String exp, String rec) {
        return exp + "§" + rec;
    }

    /**
     * Creates a new RecordingOverrideWriter and ensures the {@code Viewer/} folder exists.
     *
     * @param projectPath root of the PAINT project
     */
    public RecordingOverrideWriter(Path projectPath) {
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
     *   <li>"Recording"  – update only the current recording</li>
     *   <li>"Experiment" – update all recordings in the same experiment</li>
     *   <li>"Project"    – update every recording in the viewer</li>
     * </ul>
     *
     * Overrides are written non-destructively: existing rows for the same
     * (experimentName, recordingName) pair are replaced; all other entries kept.
     */
    public void applyAndWrite(
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
     * Writes a new override entry or updates an existing one for the given
     * recording. All other entries are preserved unchanged.
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
     * Loads the existing override rows from disk and returns them as a map
     * keyed by {@code experimentName§recordingName}.
     *
     * @return map of compositeKey → CSV row
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

            // Header is invalid → ignore file
            if (!lines.get(0).equalsIgnoreCase(expectedHeader)) {
                PaintLogger.warnf("Invalid Recording Override.csv header. Rebuilding.");
                return map;
            }

            // Parse entries
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
     * Writes all override entries back to disk using a temporary file and
     * atomic replace to guarantee consistency.
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
     * Updates the live {@link paint.shared.objects.Recording} associated with the entry,
     * ensuring that UI panels and filtering logic immediately reflect the new settings.
     */
    private void updateMemory(RecordingEntry re, SquareControlParams params) {
        re.getRecording().setMinRequiredDensityRatio(params.minRequiredDensityRatio);
        re.getRecording().setMaxAllowableVariability(params.maxAllowableVariability);
        re.getRecording().setMinRequiredRSquared(params.minRequiredRSquared);
        re.getRecording().setNeighbourMode(params.neighbourMode);
    }
}