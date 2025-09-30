package viewer;

import paint.shared.io.SquareTableIO;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintConstants.SQUARES_CSV;

public final class SquareCsvLoader {

    private SquareCsvLoader() {}

    /**
     * Loads squares for a given recording from <project>/<experiment>/SQUARES_CSV.
     * There file contains squares for multiple recordings sonyiou need ti check the recording name.
     * In principle, it is know how many squares should be read, so if you have seen that number of
     * squares you can stop looping.
     * If expectedNumberOfSquares is not 0, do this test, otherwise ignore
     */
    public static List<SquareForDisplay> loadSquaresForRecording(
            Path projectPath,
            String experimentName,
            String recordingName,
            int expectedNumberOfSquares) throws IOException {

        int numberOfSquaresRead = 0;
        Path squaresCsv = projectPath.resolve(experimentName).resolve(SQUARES_CSV);

        SquareTableIO io = new SquareTableIO();
        Table table = io.readCsv(squaresCsv);

        List<SquareForDisplay> out = new ArrayList<>();
        for (Row row : table) {
            if (!recordingName.equals(row.getString("Recording Name"))) {
                continue;
            }
            numberOfSquaresRead +=1;

            SquareForDisplay s = new SquareForDisplay();
            s.squareNumber = row.getInt("Square Number");
            s.row          = row.getInt("Row Number");
            s.col          = row.getInt("Column Number");
            s.labelNumber  = row.getInt("Label Number");
            s.cellId       = row.getInt("Cell ID");
            s.selected     = row.getBoolean("Selected");
            out.add(s);

            if (expectedNumberOfSquares != 0 && numberOfSquaresRead >= expectedNumberOfSquares) {
                break;
            }
        }
        if (expectedNumberOfSquares !=0 && numberOfSquaresRead != expectedNumberOfSquares) {
            throw new IllegalStateException(
                    "Expected " + expectedNumberOfSquares +
                            " squares, but found " + numberOfSquaresRead +
                            " in recording: " + recordingName
            );
        }
        return out;
    }
}