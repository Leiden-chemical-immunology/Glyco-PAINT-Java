package paint.viewer.override;

import static paint.shared.constants.PaintConstants.*;

import paint.shared.objects.Square;
import paint.viewer.model.RecordingEntry;

import tech.tablesaw.api.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SquareOverrideApplierCsv {

    // ──────────────────────────────────────────────
    // Apply overrides from Squares.csv into memory
    // ──────────────────────────────────────────────
    public static void applySquareCsvOverrides(List<RecordingEntry> entries,
            Path projectPath) throws Exception {

        Path csv = projectPath.resolve("Squares.csv");
        if (!Files.exists(csv)) {
            throw new Exception("Squares.csv not found in project");
        }

        Table t = Table.read().csv(csv.toString());

        List<SquareOverride> overrides = new ArrayList<>();
        for (int i = 0; i < t.rowCount(); i++) {
            SquareOverride o = new SquareOverride();
            o.setExperimentName(t.column(EXPERIMENT_NAME).get(i).toString());
            o.setRecordingName(t.column(RECORDING_NAME).get(i).toString());
            o.setSquareNumber(Integer.parseInt(t.column(SQUARE_NUMBER).get(i).toString()));
            o.setCellId(Integer.parseInt(t.column(CELL_ID).get(i).toString()));
            o.setTimestamp(t.column(TIME_STAMP).get(i).toString());
            overrides.add(o);
        }

        for (RecordingEntry re : entries) {
            List<Square> squares = re.getRecording().getSquaresOfRecording();

            for (SquareOverride o : overrides) {
                for (Square s : squares) {

                    if (!o.getExperimentName().equals(s.getExperimentName())) continue;
                    if (!o.getRecordingName().equals(s.getRecordingName())) continue;
                    if (o.getSquareNumber() != s.getSquareNumber()) continue;

                    s.setCellId(o.getCellId());
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

        int n = allSquares.size();
        String[] exp = new String[n];
        String[] rec = new String[n];
        int[] sqNum = new int[n];
        int[] cellId = new int[n];
        String[] ts = new String[n];

        for (int i = 0; i < n; i++) {
            Square s = allSquares.get(i);
            exp[i] = s.getExperimentName();
            rec[i] = s.getRecordingName();
            sqNum[i] = s.getSquareNumber();
            cellId[i] = s.getCellId();
            // ts[i] = s.getTimestamp();
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

    private SquareOverrideApplierCsv() {}
}