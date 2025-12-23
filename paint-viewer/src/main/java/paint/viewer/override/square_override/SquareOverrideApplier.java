/*==============================================================================
 *  Class:        SquareOverrideApplier.java
 *  Package:      paint.viewer.override
 *
 *  PURPOSE:
 *    Applies per-square cell assignment overrides to in-memory Square objects
 *    during project loading or batch override processing. Overrides may be
 *    defined for any combination of experiment, recording, and square number.
 *
 *  DESCRIPTION:
 *    This utility reads "Square Override.csv" from the <project>/Viewer
 *    directory and applies the cellId values to Square objects held inside
 *    RecordingEntry instances or provided via direct square lists.
 *
 *    The override file contains rows structured as:
 *
 *        experimentName, recordingName, squareNumber, cellId, timestamp
 *
 *    Overrides are matched via a composite key:
 *
 *        experimentName + "§" + recordingName + "§" + squareNumber
 *
 *    Only in-memory mutation occurs here. The writer responsible for
 *    generating the override CSV is {@link paint.viewer.override.SquareOverrideWriter}.
 *
 *  KEY FEATURES:
 *    • Reads and applies overrides to Square objects.
 *    • Composite-key lookup for fast matching.
 *    • Supports applying overrides to RecordingEntry collections or raw Square lists.
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

package paint.viewer.override.square_override;

import static paint.shared.constants.PaintStringConstants.SQUARE_NUMBER;
import static paint.shared.constants.PaintStringConstants.CELL_ID;
import static paint.shared.constants.PaintStringConstants.TIME_STAMP;
import static paint.shared.constants.PaintStringConstants.EXPERIMENT_NAME;
import static paint.shared.constants.PaintStringConstants.RECORDING_NAME;

import paint.shared.objects.Square;
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
 * Loads and applies per-square override values to Square objects.
 * <p>
 * Each override row in the CSV sets a new {@code cellId} for one specific square
 * in a specific recording. Matching uses a stable composite key:
 * <pre>
 *   experimentName + "§" + recordingName + "§" + squareNumber
 * </pre>
 */
public final class SquareOverrideApplier {

    // ────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT (RecordingEntries + projectPath)
    // ────────────────────────────────────────────────────────────

    /**
     * Loads overrides (if present) and applies them to all Square objects
     * found inside the provided RecordingEntry list.
     *
     * @param recordingEntries all recordings whose squares should be updated
     * @param projectPath      the project root containing /Viewer/Square Override.csv
     */
    public static void applySquareOverrides(List<RecordingEntry> recordingEntries,
            Path projectPath) {

        Path csvPath = projectPath.resolve("Viewer").resolve("Square Override.csv");
        if (!Files.exists(csvPath)) {
            PaintLogger.warnf("Square Override.csv not found → no overrides applied.");
            return;
        }

        List<SquareOverride> overrides = loadSquareOverride(csvPath);

        // Flatten all squares from all recordings
        List<Square> allSquares = new ArrayList<>();
        for (RecordingEntry recordingEntry : recordingEntries) {
            allSquares.addAll(recordingEntry.getRecording().getSquaresOfRecording());
        }

        applyInternal(allSquares, overrides);
    }

    // ────────────────────────────────────────────────────────────
    // INTERNAL APPLY
    // ────────────────────────────────────────────────────────────

    /**
     * Applies overrides to the provided list of Square objects.
     * Only squares with a matching composite key are updated.
     *
     * @param squares   list of Square objects from one or more recordings
     * @param overrides parsed override objects from CSV
     */
    private static void applyInternal(List<Square> squares,
            List<SquareOverride> overrides) {

        // Map: key(exp, rec, square) → cellId
        Map<String, Integer> overrideCellIds = new HashMap<>();

        for (SquareOverride override : overrides) {
            String key = key(override.getExperimentName(),
                             override.getRecordingName(),
                             override.getSquareNumber());
            overrideCellIds.put(key, override.getCellId());
        }

        int applied = 0;

        for (Square square : squares) {

            String k = key(square.getExperimentName(),
                           square.getRecordingName(),
                           square.getSquareNumber());

            Integer newCellId = overrideCellIds.get(k);

            // Only apply if present and different
            if (newCellId != null && newCellId != square.getCellId()) {

                int oldCellId = square.getCellId();   // capture BEFORE change

                square.setCellId(newCellId);
                applied++;

                PaintLogger.infof(
                        "Updated square: %s / %s / #%3d | cellId %d → %d",
                        square.getExperimentName(),
                        square.getRecordingName(),
                        square.getSquareNumber(),
                        oldCellId,
                        newCellId
                );
            }
        }

        PaintLogger.infof("Square overrides applied: " + applied);
    }

    // ────────────────────────────────────────────────────────────
    // CSV LOADING
    // ────────────────────────────────────────────────────────────

    /**
     * Loads square overrides from "Square Override.csv".
     *
     * @param csvFile full path to the override file
     * @return a list of SquareOverride objects
     */
    public static List<SquareOverride> loadSquareOverride(Path csvFile) {

        List<SquareOverride> list = new ArrayList<>();

        try {
            Table table = Table.read().csv(csvFile.toString());

            for (int i = 0; i < table.rowCount(); i++) {

                SquareOverride squareOverride = new SquareOverride();

                squareOverride.setExperimentName(table.column(EXPERIMENT_NAME).get(i).toString());
                squareOverride.setRecordingName(table.column(RECORDING_NAME).get(i).toString());
                squareOverride.setSquareNumber(Integer.parseInt(table.column(SQUARE_NUMBER).get(i).toString()));
                squareOverride.setCellId(Integer.parseInt(table.column(CELL_ID).get(i).toString()));
                squareOverride.setTimestamp(table.column(TIME_STAMP).get(i).toString());

                list.add(squareOverride);
            }

        } catch (Exception ex) {
           PaintLogger.errorf("Error reading Square Override.csv → " + ex.getMessage());
        }

        return list;
    }

    /**
     * Builds a composite override lookup key.
     */
    private static String key(String experimentName,
            String recordingName,
            int squareId) {
        return experimentName + "§" + recordingName + "§" + squareId;
    }

    private SquareOverrideApplier() {}
}