/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.viewer.override.recording_override;

import static paint.shared.constants.PaintStringConstants.*;

import paint.shared.utils.PaintLogger;
import paint.viewer.model.RecordingEntry;
import tech.tablesaw.api.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Imports and applies recording-level threshold overrides to a collection of
 * {@link RecordingEntry} objects.
 * <p>
 * Overrides are loaded from <project>/Viewer/Recording Override.csv. Each row
 * specifies the updated threshold values for a single recording. Rows are matched
 * to {@link RecordingEntry} objects using a composite key:
 * {@code experimentName + "§" + recordingName}.
 * <p>
 * This class performs in-memory mutation only (no file writes).
 */
public final class ImportRecordingOverride {

    // ────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT
    // ────────────────────────────────────────────────────────────

    /**
     * Loads and applies all recording overrides to the given list of
     * {@link RecordingEntry} objects.
     * <p>
     * If <project>/Viewer/Recording Override.csv does not exist, this method
     * returns immediately without modifying anything.
     *
     * @param recordingEntries list of in-memory {@link RecordingEntry} objects to update
     * @param projectPath      project root folder containing the {@code Viewer} directory
     */
    public static void importRecordingOverrides(List<RecordingEntry> recordingEntries, Path projectPath) {

        Path csvPath = projectPath.resolve("Viewer").resolve("Recording Override.csv");

        if (!Files.exists(csvPath)) {
            PaintLogger.infof("No Recording Override.csv present - no overrides to apply.");
            return;
        }

        List<RecordingOverride> overrides = loadRecordingOverride(csvPath);
        applyInternal(recordingEntries, overrides);
    }

    // ────────────────────────────────────────────────────────────
    // INTERNAL APPLY
    // ────────────────────────────────────────────────────────────

    /**
     * Applies overrides that were already loaded from CSV.
     * <p>
     * Implementation notes:
     * <ul>
     *   <li>Builds a composite-key map for O(1) override lookup.</li>
     *   <li>Mutates {@link RecordingEntry} recording threshold fields in place.</li>
     *   <li>Logs only fields whose values changed.</li>
     * </ul>
     *
     * @param entries   recording entries to update
     * @param overrides overrides parsed from CSV
     */
    private static void applyInternal(List<RecordingEntry> entries, List<RecordingOverride> overrides) {

        Map<String, RecordingOverride> map = new HashMap<>();

        for (RecordingOverride override : overrides) {
            map.put(key(override.getExperimentName(), override.getRecordingName()), override);
        }


        for (RecordingEntry entry : entries) {

            String experimentName = entry.getExperimentName();
            String recordingName  = entry.getRecordingName();

            RecordingOverride override = map.get(key(experimentName, recordingName));

            if (override != null) {

                // Capture current values for change-only logging
                double oldDensityRatio   = entry.getRecording().getMinRequiredDensityRatio();
                double oldRSquared       = entry.getRecording().getMinRequiredRSquared();
                double oldVariability    = entry.getRecording().getMaxAllowableVariability();
                String oldNeighbourMode  = entry.getRecording().getNeighbourMode();

                // Apply overrides (in-memory only)
                entry.getRecording().setMinRequiredDensityRatio(override.getMinRequiredDensityRatio());
                entry.getRecording().setMinRequiredRSquared(override.getMinRequiredRSquared());
                entry.getRecording().setMaxAllowableVariability(override.getMaxAllowableVariability());
                entry.getRecording().setNeighbourMode(override.getNeighbourMode());

                // Log what changed for this recording
                PaintLogger.infof(
                        "Applied Recording override on %s",
                        entry.getRecordingName());

                if (oldDensityRatio != override.getMinRequiredDensityRatio()) {
                    PaintLogger.infof("                    DensityRatio:   %-6.2f → %.2f",
                                      oldDensityRatio, override.getMinRequiredDensityRatio());
                }

                if (oldRSquared != override.getMinRequiredRSquared()) {
                    PaintLogger.infof("                    R²:             %-6.2f → %.2f",
                                      oldRSquared,
                                      override.getMinRequiredRSquared());
                }

                if (oldVariability != override.getMaxAllowableVariability()) {
                    PaintLogger.infof("                    Variability:    %-6.2f → %.2f",
                                      oldVariability,
                                      override.getMaxAllowableVariability());
                }

                if (!oldNeighbourMode.equals(override.getNeighbourMode())) {
                    PaintLogger.infof("                    Neighbour Mode: %6s → %s",
                                      oldNeighbourMode,
                                      override.getNeighbourMode());
                }
                PaintLogger.blankline();
            }
        }
    }

    // ────────────────────────────────────────────────────────────
    // CSV LOADING
    // ────────────────────────────────────────────────────────────

    /**
     * Loads all rows from {@code Recording Override.csv} into a list of
     * {@link RecordingOverride} objects.
     *
     * @param csvFile path to {@code Recording Override.csv}
     * @return list of parsed {@link RecordingOverride} objects (possibly empty)
     */
    public static List<RecordingOverride> loadRecordingOverride(Path csvFile) {

        List<RecordingOverride> list = new ArrayList<>();

        try {
            Table table = Table.read().csv(csvFile.toString());

            for (int i = 0; i < table.rowCount(); i++) {
                RecordingOverride recordingOverride = new RecordingOverride();

                recordingOverride.setExperimentName(          table.column(EXPERIMENT_NAME).get(i).toString());
                recordingOverride.setRecordingName(           table.column(RECORDING_NAME).get(i).toString());
                recordingOverride.setMinRequiredDensityRatio( Double.parseDouble(table.column(MIN_REQUIRED_DENSITY_RATIO).get(i).toString()));
                recordingOverride.setMinRequiredRSquared(     Double.parseDouble(table.column(MIN_REQUIRED_R_SQUARED).get(i).toString()));
                recordingOverride.setMaxAllowableVariability( Double.parseDouble(table.column(MAX_ALLOWABLE_VARIABILITY).get(i).toString()));
                recordingOverride.setNeighbourMode(           table.column(NEIGHBOUR_MODE).get(i).toString());

                list.add(recordingOverride);
            }
        } catch (Exception ex) {
            PaintLogger.errorf( "Error reading Recording Override.csv → " + ex.getMessage());
        }

        return list;
    }

    /**
     * Builds the composite key used for override lookups.
     *
     * @param experimentName experiment name
     * @param recordingName  recording name
     * @return composite key in the format {@code experimentName + "§" + recordingName}
     */
    private static String key(String experimentName, String recordingName) {
        return experimentName + "§" + recordingName;
    }

    /** Utility class; not instantiable. */
    private ImportRecordingOverride() {}
}