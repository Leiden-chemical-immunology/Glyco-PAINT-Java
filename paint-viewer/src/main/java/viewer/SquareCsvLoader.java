package viewer;

import paint.shared.io.SquareTableIO;
import paint.shared.objects.Square;
import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintConstants.SQUARES_CSV;

public final class SquareCsvLoader {

    private SquareCsvLoader() {}

    // ── Existing method: load squares for one recording ───────────────────────
    public static List<Square> loadSquaresForRecording(
            Path projectPath,
            String experimentName,
            String recordingName,
            int expectedNumberOfSquares) throws IOException {

        int numberOfSquaresRead = 0;
        Path squaresCsv = projectPath.resolve(experimentName).resolve(SQUARES_CSV);
        SquareTableIO io = new SquareTableIO();
        Table table = io.readCsv(squaresCsv);

        List<Square> out = new ArrayList<>();
        for (Row row : table) {
            if (!recordingName.equals(row.getString("Recording Name"))) {
                continue;
            }
            numberOfSquaresRead++;

            Square s = new Square();
            s.setRecordingName(recordingName);
            s.setSquareNumber(row.getInt("Square Number"));
            s.setRowNumber(row.getInt("Row Number"));
            s.setColNumber(row.getInt("Column Number"));
            s.setLabelNumber(row.getInt("Label Number"));
            s.setCellId(row.getInt("Cell ID"));
            s.setSelected(row.getBoolean("Selected"));

            // Optional attributes
            if (table.columnNames().contains("Density Ratio"))
                s.setDensityRatio(row.getDouble("Density Ratio"));
            if (table.columnNames().contains("Variability"))
                s.setVariability(row.getDouble("Variability"));
            if (table.columnNames().contains("R Squared"))
                s.setRSquared(row.getDouble("R Squared"));

            // Duration-related columns
            if (table.columnNames().contains("Median Track Duration"))
                s.setMedianTrackDuration(row.getDouble("Median Track Duration"));
            if (table.columnNames().contains("Max Track Duration"))
                s.setMaxTrackDuration(row.getDouble("Max Track Duration"));
            if (table.columnNames().contains("Total Track Duration"))
                s.setTotalTrackDuration(row.getDouble("Total Track Duration"));
            if (table.columnNames().contains("Median Long Track Duration"))
                s.setMedianLongTrackDuration(row.getDouble("Median Long Track Duration"));
            if (table.columnNames().contains("Median Short Track Duration"))
                s.setMedianShortTrackDuration(row.getDouble("Median Short Track Duration"));

            // Neighbour Mode column exists in some CSVs, but we ignore it
            if (table.columnNames().contains("Neighbour Mode")) {
                // just ignore; no field in Square
            }

            out.add(s);
            if (expectedNumberOfSquares != 0 && numberOfSquaresRead >= expectedNumberOfSquares) {
                break;
            }
        }

        if (expectedNumberOfSquares != 0 && numberOfSquaresRead != expectedNumberOfSquares) {
            throw new IllegalStateException(
                    "Expected " + expectedNumberOfSquares +
                            " squares, but found " + numberOfSquaresRead +
                            " in recording: " + recordingName);
        }
        return out;
    }

    // ── NEW method: load all squares for an experiment ────────────────────────
    public static List<Square> loadAllSquaresForExperiment(Path projectPath, String experimentName)
            throws IOException {

        Path squaresCsv = projectPath.resolve(experimentName).resolve(SQUARES_CSV);
        SquareTableIO io = new SquareTableIO();
        Table table = io.readCsv(squaresCsv);

        List<Square> out = new ArrayList<>();
        for (Row row : table) {
            Square s = new Square();
            s.setRecordingName(row.getString("Recording Name"));
            s.setSquareNumber(row.getInt("Square Number"));
            s.setRowNumber(row.getInt("Row Number"));
            s.setColNumber(row.getInt("Column Number"));
            s.setLabelNumber(row.getInt("Label Number"));
            s.setCellId(row.getInt("Cell ID"));
            s.setSelected(row.getBoolean("Selected"));
            s.setDensityRatio(row.getDouble("Density Ratio"));
            s.setVariability(row.getDouble("Variability"));
            s.setRSquared(row.getDouble("R Squared"));
            s.setMedianTrackDuration(row.getDouble("Median Track Duration"));
            s.setMaxTrackDuration(row.getDouble("Max Track Duration"));
            s.setTotalTrackDuration(row.getDouble("Total Track Duration"));
            s.setMedianLongTrackDuration(row.getDouble("Median Long Track Duration"));
            s.setMedianShortTrackDuration(row.getDouble("Median Short Track Duration"));
            s.setSelected(row.getBoolean("Selected"));
            out.add(s);

//            if (s.isSelected()) {
//                PaintLogger.infof("Square %s is selected", s.getSquareNumber());
//            }
        }

        return out;
    }
}