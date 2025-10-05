package viewer;

import paint.shared.io.SquareTableIO;
import paint.shared.objects.Square;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintConstants.SQUARES_CSV;

public final class SquareCsvLoader {

    private SquareCsvLoader() {}

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
}