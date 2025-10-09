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

/**
 * Loads square information from the SQUARES_CSV file.
 * This version assumes the CSV is complete and always contains
 * all standard columns defined in {@link Square}.
 */
public final class SquareCsvLoader {

    private SquareCsvLoader() {
    }

    // ── Load squares for one recording ────────────────────────────────
    public static List<Square> loadSquaresForRecording(
            Path projectPath,
            String experimentName,
            String recordingName,
            int expectedNumberOfSquares) throws IOException {

        Path squaresCsv = projectPath.resolve(experimentName).resolve(SQUARES_CSV);
        Table table = new SquareTableIO().readCsv(squaresCsv);

        List<Square> out = new ArrayList<>();
        int numberOfSquaresRead = 0;

        for (Row row : table) {
            if (!recordingName.equals(row.getString("Recording Name"))) {
                continue;
            }

            numberOfSquaresRead++;
            Square s = createSquareFromRow(row);
            out.add(s);

            if (expectedNumberOfSquares != 0 && numberOfSquaresRead >= expectedNumberOfSquares) {
                break;
            }
        }

        if (expectedNumberOfSquares != 0 && numberOfSquaresRead != expectedNumberOfSquares) {
            throw new IllegalStateException(String.format(
                    "Expected %d squares, but found %d in recording: %s",
                    expectedNumberOfSquares, numberOfSquaresRead, recordingName));
        }

        return out;
    }

    // ── Load all squares for an experiment ────────────────────────────
    public static List<Square> loadAllSquaresForExperiment(Path projectPath, String experimentName)
            throws IOException {

        Path squaresCsv = projectPath.resolve(experimentName).resolve(SQUARES_CSV);
        Table table = new SquareTableIO().readCsv(squaresCsv);

        List<Square> out = new ArrayList<>();
        for (Row row : table) {
            out.add(createSquareFromRow(row));
        }

        return out;
    }

    // ── Helper to populate all Square fields ──────────────────────────
    private static Square createSquareFromRow(Row row) {
        Square s = new Square();

        s.setUniqueKey(row.getString("Unique Key"));
        s.setRecordingName(row.getString("Recording Name"));
        s.setSquareNumber(row.getInt("Square Number"));
        s.setRowNumber(row.getInt("Row Number"));
        s.setColNumber(row.getInt("Column Number"));
        s.setLabelNumber(row.getInt("Label Number"));
        s.setCellId(row.getInt("Cell ID"));
        s.setSelected(row.getBoolean("Selected"));
        s.setSquareManuallyExcluded(row.getBoolean("Square Manually Excluded"));
        s.setImageExcluded(row.getBoolean("Image Excluded"));

        s.setX0(row.getDouble("X0"));
        s.setY0(row.getDouble("Y0"));
        s.setX1(row.getDouble("X1"));
        s.setY1(row.getDouble("Y1"));

        s.setNumberOfTracks(row.getInt("Number of Tracks"));
        s.setVariability(row.getDouble("Variability"));
        s.setDensity(row.getDouble("Density"));
        s.setDensityRatio(row.getDouble("Density Ratio"));
        s.setTau(row.getDouble("Tau"));
        s.setRSquared(row.getDouble("R Squared"));

        s.setMedianDiffusionCoefficient(row.getDouble("Median Diffusion Coefficient"));
        s.setMedianDiffusionCoefficientExt(row.getDouble("Median Diffusion Coefficient Ext"));
        s.setMedianLongTrackDuration(row.getDouble("Median Long Track Duration"));
        s.setMedianShortTrackDuration(row.getDouble("Median Short Track Duration"));
        s.setMedianDisplacement(row.getDouble("Median Displacement"));
        s.setMaxDisplacement(row.getDouble("Max Displacement"));
        s.setTotalDisplacement(row.getDouble("Total Displacement"));

        s.setMedianMaxSpeed(row.getDouble("Median Max Speed"));
        s.setMaxMaxSpeed(row.getDouble("Max Max Speed"));
        s.setMedianMeanSpeed(row.getDouble("Median Mean Speed"));
        s.setMaxMeanSpeed(row.getDouble("Max Mean Speed"));

        s.setMaxTrackDuration(row.getDouble("Max Track Duration"));
        s.setTotalTrackDuration(row.getDouble("Total Track Duration"));
        s.setMedianTrackDuration(row.getDouble("Median Track Duration"));

        return s;
    }
}