package paint.viewer.override;

import paint.shared.objects.Square;
import paint.viewer.model.RecordingEntry;
import tech.tablesaw.api.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintConstants.*;

public final class SquareOverrideApplierCsv {

    private SquareOverrideApplierCsv() {
    }

    // ──────────────────────────────────────────────
    // Apply overrides from Squares.csv into memory
    // ──────────────────────────────────────────────
    public static void applySquareCsvOverrides(List<RecordingEntry> entries,
            Path projectPath) throws Exception {

        Path csv = projectPath.resolve("Squares.csv");
        if (!Files.exists(csv)) {
            throw new Exception("Squares.csv not found in project");
        }

        Table table = Table.read().csv(csv.toString());

        List<SquareOverride> overrides = new ArrayList<>();
        for (int i = 0; i < table.rowCount(); i++) {
            SquareOverride override = new SquareOverride();
            override.setExperimentName(table.column(EXPERIMENT_NAME).get(i).toString());
            override.setRecordingName(table.column(RECORDING_NAME).get(i).toString());
            override.setSquareNumber(Integer.parseInt(table.column(SQUARE_NUMBER).get(i).toString()));
            override.setCellId(Integer.parseInt(table.column(CELL_ID).get(i).toString()));
            override.setTimestamp(table.column(TIME_STAMP).get(i).toString());
            overrides.add(override);
        }

        for (RecordingEntry re : entries) {
            List<Square> squares = re.getRecording().getSquaresOfRecording();

            for (SquareOverride override : overrides) {
                for (Square square : squares) {

                    if (!override.getExperimentName().equals(square.getExperimentName())) {
                        continue;
                    }
                    if (!override.getRecordingName().equals(square.getRecordingName())) {
                        continue;
                    }
                    if (override.getSquareNumber() != square.getSquareNumber()) {
                        continue;
                    }

                    square.setCellId(override.getCellId());
                }
            }
        }
    }

    // ──────────────────────────────────────────────
    // Write OUT: Squares-<suffix>.csv
    // ──────────────────────────────────────────────
    public static void writeSquares(Path projectPath,
            String suffix,
            List<RecordingEntry> entries) throws Exception {

        Path out = projectPath.resolve("Squares-" + suffix + ".csv");

        List<Square> allSquares = new ArrayList<>();
        for (RecordingEntry re : entries) {
            allSquares.addAll(re.getRecording().getSquaresOfRecording());
        }

        Table t = Table.create("Squares");

        int      n      = allSquares.size();
        String[] exp    = new String[n];
        String[] rec    = new String[n];
        int[]    sqNum  = new int[n];
        int[]    cellId = new int[n];
        String[] ts     = new String[n];

        for (int i = 0; i < n; i++) {
            Square s  = allSquares.get(i);
            exp[i]    = s.getExperimentName();
            rec[i]    = s.getRecordingName();
            sqNum[i]  = s.getSquareNumber();
            cellId[i] = s.getCellId();
        }

        t.addColumns(
                tech.tablesaw.api.StringColumn.create(EXPERIMENT_NAME, exp),
                tech.tablesaw.api.StringColumn.create(RECORDING_NAME, rec),
                tech.tablesaw.api.IntColumn.create(SQUARE_NUMBER, sqNum),
                tech.tablesaw.api.IntColumn.create(CELL_ID, cellId),
                tech.tablesaw.api.StringColumn.create(TIME_STAMP, ts)
        );

        t.write().csv(out.toString());
        System.out.println("Wrote: " + out.getFileName());
    }
}