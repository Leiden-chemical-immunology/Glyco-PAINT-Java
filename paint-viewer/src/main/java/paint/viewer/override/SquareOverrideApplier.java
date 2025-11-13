package paint.viewer.override;

import static paint.shared.constants.PaintConstants.*;

import paint.shared.objects.Square;
import paint.shared.objects.SquareOverride;
import paint.viewer.model.RecordingEntry;

import tech.tablesaw.api.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SquareOverrideApplier {

    // ────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT (RecordingEntries + projectPath)
    // ────────────────────────────────────────────────────────────
    public static void applySquareOverrides(List<RecordingEntry> recordingEntries,
            Path projectPath) {

        Path csvPath = projectPath.resolve("Viewer").resolve("Square Override.csv");
        if (!Files.exists(csvPath)) {
            System.out.println("Square Override.csv not found → no overrides applied.");
            return;
        }

        List<SquareOverride> overrides = load(csvPath);

        // Flatten all squares from all RecordingEntries
        List<Square> allSquares = new ArrayList<>();
        for (RecordingEntry recordingEntry : recordingEntries) {
            allSquares.addAll(recordingEntry.getRecording().getSquaresOfRecording());
        }

        applyInternal(allSquares, overrides);
    }

    // ────────────────────────────────────────────────────────────
    // INTERNAL APPLY
    // ────────────────────────────────────────────────────────────
    private static void applyInternal(List<Square> squares,
            List<SquareOverride> overrides) {

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

            if (newCellId != null && newCellId != square.getCellId()) {

                square.setCellId(newCellId);
                applied++;

                System.out.println(
                        "Updated square: " +
                                square.getExperimentName() + " / " +
                                square.getRecordingName() + " / #" +
                                square.getSquareNumber() +
                                " | cellId " + square.getCellId() +
                                " → " + newCellId
                );
            }
        }

        System.out.println("Square overrides applied: " + applied);
    }

    // ────────────────────────────────────────────────────────────
    // CSV LOADING
    // ────────────────────────────────────────────────────────────
    private static List<SquareOverride> load(Path csvFile) {

        List<SquareOverride> list = new ArrayList<>();

        try {
            Table table = Table.read().csv(csvFile.toString());

            for (int i = 0; i < table.rowCount(); i++) {

                SquareOverride o = new SquareOverride();

                o.setExperimentName(table.column(EXPERIMENT_NAME).get(i).toString());
                o.setRecordingName(table.column(RECORDING_NAME).get(i).toString());
                o.setSquareNumber(Integer.parseInt(table.column(SQUARE_NUMBER).get(i).toString()));
                o.setCellId(Integer.parseInt(table.column(CELL_ID).get(i).toString()));
                o.setTimestamp(table.column(TIME_STAMP).get(i).toString());

                list.add(o);
            }

        } catch (Exception ex) {
            System.err.println("Error reading Square Override.csv → " + ex.getMessage());
        }

        return list;
    }

    private static String key(String experimentName,
            String recordingName,
            int squareId) {
        return experimentName + "§" + recordingName + "§" + squareId;
    }

    private SquareOverrideApplier() {}
}