/*==============================================================================
 *  Class:        RecordingOverrideApplier.java
 *  Package:      paint.viewer.override
 *
 *  PURPOSE:
 *    Applies recording-level threshold overrides (density ratio, variability,
 *    R², neighbour mode) to in-memory RecordingEntry objects when the Viewer
 *    loads a project or when the user requests override import.
 *
 *  DESCRIPTION:
 *    This utility reads a pre-generated CSV file ("Recording Override.csv")
 *    located in the <project>/Viewer directory. Each row contains updated
 *    threshold values for a particular recording. The applier:
 *
 *       1. Loads overrides from the CSV via Tablesaw.
 *       2. Builds a composite-key map for fast lookup.
 *       3. Updates each RecordingEntry object with the overridden values.
 *
 *    No disk writes occur here — this class strictly mutates in-memory models.
 *    Writing override files is handled separately by RecordingOverrideWriter.
 *
 *  KEY FEATURES:
 *    • Lightweight override loader and applier.
 *    • Composite key lookup for fast matching.
 *    • Purely updates in-memory recording threshold fields.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-11-17
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
==============================================================================*/

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
 * Applies recording-level overrides to a collection of RecordingEntry objects.
 * <p>
 * Each override row specifies:
 * <ul>
 *     <li>experimentName</li>
 *     <li>recordingName</li>
 *     <li>minRequiredDensityRatio</li>
 *     <li>minRequiredRSquared</li>
 *     <li>maxAllowableVariability</li>
 *     <li>neighbourMode</li>
 * </ul>
 * Matching is performed using a composite key: {@code experimentName + "§" + recordingName}.
 */
public final class RecordingOverrideApplier {

    // ────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT
    // ────────────────────────────────────────────────────────────

    /**
     * Loads and applies all recording overrides to the given list of
     * RecordingEntry objects. If the override CSV does not exist,
     * the method exits without modifying anything.
     *
     * @param recordingEntries list of in-memory RecordingEntry objects
     * @param projectPath      project root folder containing /Viewer
     */
    public static void applyRecordingOverrides(List<RecordingEntry> recordingEntries,
            Path projectPath) {

        Path csvPath = projectPath.resolve("Viewer").resolve("Recording Override.csv");

        if (!Files.exists(csvPath)) {
            PaintLogger.warnf("Recording Override.csv not found → no overrides applied.");
            return;
        }

        List<RecordingOverride> overrides = loadRecordingOverride(csvPath);
        applyInternal(recordingEntries, overrides);
    }

    // ────────────────────────────────────────────────────────────
    // INTERNAL APPLY
    // ────────────────────────────────────────────────────────────

    /**
     * Applies overrides already loaded from CSV. This method performs:
     * <ol>
     *     <li>Composite-key indexing</li>
     *     <li>In-place mutation of RecordingEntry thresholds</li>
     * </ol>
     *
     * @param entries   recording entries to update
     * @param overrides list of overrides parsed from CSV
     */
    private static void applyInternal(List<RecordingEntry> entries,
            List<RecordingOverride> overrides) {

        // Map: "exp§rec" → RecordingOverride
        Map<String, RecordingOverride> map = new HashMap<>();

        for (RecordingOverride override : overrides) {
            map.put(key(override.getExperimentName(), override.getRecordingName()), override);
        }

        int applied = 0;

        for (RecordingEntry entry : entries) {

            String k = key(entry.getExperimentName(), entry.getRecordingName());
            RecordingOverride override = map.get(k);

            if (override != null) {

                // BEFORE applying, capture old values for logging
                double oldDensityRatio   = entry.getRecording().getMinRequiredDensityRatio();
                double oldRSquared       = entry.getRecording().getMinRequiredRSquared();
                double oldVariability    = entry.getRecording().getMaxAllowableVariability();
                String oldNeighbourMode  = entry.getRecording().getNeighbourMode();

                // APPLY overrides
                entry.getRecording().setMinRequiredDensityRatio(override.getMinRequiredDensityRatio());
                entry.getRecording().setMinRequiredRSquared(override.getMinRequiredRSquared());
                entry.getRecording().setMaxAllowableVariability(override.getMaxAllowableVariability());
                entry.getRecording().setNeighbourMode(override.getNeighbourMode());

                applied++;

                // LOG full detail for this recording
                PaintLogger.infof(
                        "Recording override applied: %s / %s\n" +
                                "                    DensityRatio:   %.4f → %.4f\n" +
                                "                    R²:             %.4f → %.4f\n" +
                                "                    Variability:    %.4f → %.4f\n" +
                                "                    NeighbourMode:  %s → %s",
                        entry.getExperimentName(),
                        entry.getRecordingName(),
                        oldDensityRatio,
                        override.getMinRequiredDensityRatio(),
                        oldRSquared,
                        override.getMinRequiredRSquared(),
                        oldVariability,
                        override.getMaxAllowableVariability(),
                        oldNeighbourMode,
                        override.getNeighbourMode()
                );
            }
        }

        PaintLogger.infof("Recording overrides applied: " + applied);
    }

    // ────────────────────────────────────────────────────────────
    // CSV LOADING
    // ────────────────────────────────────────────────────────────

    /**
     * Loads all rows from the `Recording Override.csv` file into a list of
     * RecordingOverride objects.
     *
     * @param csvFile path to Recording Override.csv
     * @return list of parsed RecordingOverride objects
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
     * Builds composite key used for override lookups.
     */
    private static String key(String experimentName, String recordingName) {
        return experimentName + "§" + recordingName;
    }

    /** Private constructor — utility class. */
    private RecordingOverrideApplier() {}
}