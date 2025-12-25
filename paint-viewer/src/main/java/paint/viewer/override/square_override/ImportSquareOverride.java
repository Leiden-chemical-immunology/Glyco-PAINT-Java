/*==============================================================================
 *  Class:        ImportSquareOverride.java
 *  Package:      paint.viewer.override.square_override
 *
 *  PURPOSE:
 *    Loads and applies per-square cell-assignment overrides (cellId) from the
 *    Viewer override CSV to in-memory Square objects. This is used when the
 *    Viewer loads a project or when override import is triggered.
 *
 *  DESCRIPTION:
 *    Reads the file:
 *
 *        <project>/Viewer/Square Override.csv
 *
 *    Each CSV row identifies a square by:
 *
 *        experimentName, recordingName, squareNumber
 *
 *    and provides an updated cellId plus a timestamp. Overrides are matched by
 *    a stable composite key:
 *
 *        experimentName + "§" + recordingName + "§" + squareNumber
 *
 *    This class performs in-memory mutation only; it does not write any files.
 *    Writing/maintaining the override CSV is handled elsewhere.
 *
 *  KEY FEATURES:
 *    • Fast composite-key lookup for per-square updates.
 *    • Applies overrides across one or many recordings via flattened square list.
 *    • Optional verbose logging per updated square.
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

package paint.viewer.override.square_override;

import static paint.shared.constants.PaintStringConstants.SQUARE_NUMBER;
import static paint.shared.constants.PaintStringConstants.CELL_ID;
import static paint.shared.constants.PaintStringConstants.TIME_STAMP;
import static paint.shared.constants.PaintStringConstants.EXPERIMENT_NAME;
import static paint.shared.constants.PaintStringConstants.RECORDING_NAME;

import paint.shared.objects.Square;
import paint.shared.utils.PaintLogger;
import paint.shared.utils.PaintRuntime;
import paint.viewer.model.RecordingEntry;

import tech.tablesaw.api.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Loads and applies per-square override values to {@link Square} objects.
 * <p>
 * Each override row in the CSV sets a new {@code cellId} for one specific square
 * in a specific recording. Matching uses a stable composite key:
 * <pre>
 *   experimentName + "§" + recordingName + "§" + squareNumber
 * </pre>
 * <p>
 * This class mutates in-memory squares only; it does not persist any output.
 */
public final class ImportSquareOverride {

    // ────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT (RecordingEntries + projectPath)
    // ────────────────────────────────────────────────────────────

    /**
     * Loads overrides (if present) and applies them to all {@link Square} objects
     * referenced by the provided {@link RecordingEntry} list.
     *
     * @param recordingEntries all recordings whose squares should be updated
     * @param projectPath      the project root containing {@code Viewer/Square Override.csv}
     */
    public static void importSquareOverrides(List<RecordingEntry> recordingEntries, Path projectPath) {

        Path csvPath = projectPath.resolve("Viewer").resolve("Square Override.csv");
        if (!Files.exists(csvPath)) {
            PaintLogger.infof("No Square Override.csv present - no overrides to apply.");
            return;
        }

        List<SquareOverride> overrides = loadSquareOverride(csvPath);

        // Flatten all squares from all recordings into one list so overrides can be applied in one pass.
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
     * Applies overrides to the provided list of {@link Square} objects.
     * Only squares with a matching composite key are updated.
     *
     * @param squares   list of Square objects from one or more recordings
     * @param overrides parsed override objects loaded from CSV
     */
    private static void applyInternal(List<Square> squares, List<SquareOverride> overrides) {

        // Map: key(exp, rec, squareNumber) → overridden cellId
        Map<String, Integer> overrideCellIds = new HashMap<>();

        for (SquareOverride override : overrides) {
            String key = key(override.getExperimentName(),
                             override.getRecordingName(),
                             override.getSquareNumber());
            overrideCellIds.put(key, override.getCellId());
        }

        int applied = 0;

        // Apply overrides in-place to the in-memory Square list.
        for (Square square : squares) {

            String experimentName = square.getExperimentName();
            String recordingName  = square.getRecordingName();
            int    squareNumber   = square.getSquareNumber();

            String key = key(experimentName, recordingName, squareNumber);

            Integer newCellId = overrideCellIds.get(key);

            // Only apply if an override exists and the value actually changes.
            if (newCellId != null && newCellId != square.getCellId()) {
                int oldCellId  = square.getCellId();   // capture BEFORE change

                square.setCellId(newCellId);
                applied++;

                if (PaintRuntime.isVerbose()) {
                    PaintLogger.infof(
                            "Updated square: %s / %s / #%3d | cellId %2d → %2d",
                            square.getExperimentName(),
                            square.getRecordingName(),
                            square.getSquareNumber(),
                            oldCellId,
                            newCellId);
                }
            }
        }

        // Summary logging (grouped per recording) when any updates were applied.
        if (applied > 0) {
            Map<String, Long> squaresPerRecording =
                    overrides.stream()
                             .collect(Collectors.groupingBy(
                                     SquareOverride::getRecordingName,
                                     Collectors.counting()
                             ));

            PaintLogger.infof("Applied squares overrides:");
            for (String rec : squaresPerRecording.keySet()) {
                PaintLogger.infof("                    %s: %d squares", rec, squaresPerRecording.get(rec));
            }
        }
    }

    // ────────────────────────────────────────────────────────────
    // CSV LOADING
    // ────────────────────────────────────────────────────────────

    /**
     * Loads square overrides from {@code Square Override.csv}.
     *
     * @param csvFile full path to the override file
     * @return a list of {@link SquareOverride} objects parsed from the CSV
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

    /** Private constructor — utility class. */
    private ImportSquareOverride() {}
}