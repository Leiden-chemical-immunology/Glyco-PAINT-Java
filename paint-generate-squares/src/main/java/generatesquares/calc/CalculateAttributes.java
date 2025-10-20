package generatesquares.calc;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.objects.*;
import paint.shared.utils.PaintLogger;
import paint.shared.utils.SquareUtils;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.util.List;

import static generatesquares.calc.CalculateTau.calcTau;
import static paint.shared.constants.PaintConstants.IMAGE_WIDTH;
import static paint.shared.constants.PaintConstants.RECORDING_DURATION;
import static paint.shared.objects.Square.calcSquareArea;
import static paint.shared.utils.Miscellaneous.round;
import static paint.shared.utils.SquareUtils.*;

/**
 * ============================================================================
 *  CalculateAttributes.java
 *  Part of the "Generate Squares" module.
 *
 *  <p><b>Purpose:</b><br>
 *  Centralized computation of both square-level and recording-level
 *  metrics for the "Generate Squares" workflow.
 *  Handles:
 *  <ul>
 *      <li>Per-square metrics (Tau, Variability, Density, Density Ratio)</li>
 *      <li>Recording-level aggregates (Tau, Background, Density)</li>
 *  </ul>
 *  </p>
 *
 *  <p><b>Notes:</b><br>
 *  This class unifies functionality previously split between
 *  {@code CalculateRecordingAttributes} and {@code CalculateSquareAttributes}.
 *  All methods are static utilities used by {@link GenerateSquareCalcs}.
 *  </p>
 *
 *  <p><b>Author:</b> Herr Doctor<br>
 *  <b>Version:</b> 1.0<br>
 *  <b>Module:</b> paint-generate-squares
 *  </p>
 * ============================================================================
 */
public class CalculateAttributes {

    /**
     * Calculates detailed metrics for each square (Tau, variability, density ratio, etc.).
     *
     * @param experimentName        the experiment name
     * @param recording             the recording containing the squares
     * @param generateSquaresConfig the {@link GenerateSquaresConfig} parameters
     */
    public static void calculateSquareAttributes(Path experimentPath,
                                                 String experimentName,
                                                 Recording recording,
                                                 GenerateSquaresConfig generateSquaresConfig) {

        double minRequiredRSquared = generateSquaresConfig.getMinRequiredRSquared();
        int minTracksForTau = generateSquaresConfig.getMinTracksToCalculateTau();
        double maxAllowableVariability = generateSquaresConfig.getMaxAllowableVariability();
        double minRequiredDensityRatio = generateSquaresConfig.getMinRequiredDensityRatio();
        String neighbourMode = generateSquaresConfig.getNeighbourMode();
        int numberOfSquaresInRecording = generateSquaresConfig.getNumberOfSquaresInRecording();
        double area = calcSquareArea(numberOfSquaresInRecording);
        double concentration = recording.getConcentration();

        SquareUtils.BackgroundEstimationResult result = estimateBackgroundDensity(recording.getSquaresOfRecording());
        double meanBackgroundTracks = result.getBackgroundMean();
        double backgroundTracksOri = calcAverageTrackCountInBackgroundSquares(
                recording.getSquaresOfRecording(), (int) (0.1 * numberOfSquaresInRecording)
        );

        PaintLogger.debugf("Estimated Background track count = %.2f, n = %d%n",
                           meanBackgroundTracks, result.getBackgroundSquares().size());

        for (Square square : recording.getSquaresOfRecording()) {
            List<Track> tracks = square.getTracks();
            Table table = square.getTracksTable();
            int squareNumber = square.getSquareNumber();

            if (tracks == null || tracks.isEmpty()) continue;

            CalculateTau.CalculateTauResult results =
                    calcTau(tracks, minTracksForTau, minRequiredRSquared);

            // Plot curve fitting if enabled
            if (paint.shared.config.PaintConfig.getBoolean("Generate Squares", "Plot Curve Fitting", false) &&
                    tracks.size() >= minTracksForTau) {
                 TauPlotCollector.saveFitPlot(
                        tracks, results, experimentPath,
                        recording.getRecordingName(), squareNumber
                );
            }

            if (results.getStatus() == CalculateTau.CalculateTauResult.Status.TAU_SUCCESS) {
                square.setTau(round(results.getTau(), 0));
                square.setRSquared(round(results.getRSquared(), 3));
            } else {
                square.setTau(Double.NaN);
                square.setRSquared(Double.NaN);
            }

            if (table.rowCount() == 0) continue;

            double variability = calcVariability(table, squareNumber, numberOfSquaresInRecording, 10);
            square.setVariability(round(variability, 3));

            double density = calculateDensity(tracks.size(), area, RECORDING_DURATION, concentration);
            square.setDensity(round(density, 4));

            double densityRatio = tracks.size() / meanBackgroundTracks;
            square.setDensityRatio(round(densityRatio, 3));

            double densityRatioOri = tracks.size() / backgroundTracksOri;
            square.setDensityRatioOri(round(densityRatioOri, 3));
        }

        applyVisibilityFilter(recording,
                              minRequiredDensityRatio,
                              maxAllowableVariability,
                              minRequiredRSquared,
                              neighbourMode);

        int labelNumber = 0;
        for (Square sq : recording.getSquaresOfRecording()) {
            if (sq.isSelected()) {
                sq.setLabelNumber(labelNumber++);
            }
        }
    }

    /**
     * Calculates aggregate metrics for an entire recording (Tau, background, density).
     *
     * @param recording             the recording to analyze
     * @param generateSquaresConfig the {@link GenerateSquaresConfig} parameters
     */
    public static void calculateRecordingAttributes(Recording recording,
                                                    GenerateSquaresConfig generateSquaresConfig) {

        double minRequiredRSquared = generateSquaresConfig.getMinRequiredRSquared();
        int minTracksForTau = generateSquaresConfig.getMinTracksToCalculateTau();

        SquareUtils.BackgroundEstimationResult result = SquareUtils.estimateBackgroundDensity(recording.getSquaresOfRecording());
        double meanBackgroundTracks = result.getBackgroundMean();
        int backgroundTracks = result.getBackgroundSquares().stream().mapToInt(Square::getNumberOfTracks).sum();

        PaintLogger.debugf("Estimated Background track count = %.2f, n = %d%n",
                           meanBackgroundTracks, result.getBackgroundSquares().size());

        recording.setNumberOfSquaresInBackground(result.getBackgroundSquares().size());
        recording.setNumberOfTracksInBackground(backgroundTracks);
        recording.setAverageTracksInBackGround(round(meanBackgroundTracks, 3));

        List<Track> tracksFromSelectedSquares = SquareUtils.getTracksFromSelectedSquares(recording);
        int numberOfSelectedSquares = SquareUtils.getNumberOfSelectedSquares(recording);

        CalculateTau.CalculateTauResult results =
                calcTau(tracksFromSelectedSquares, minTracksForTau, minRequiredRSquared);

        if (results.getStatus() == CalculateTau.CalculateTauResult.Status.TAU_SUCCESS) {
            recording.setTau(round(results.getTau(), 0));
            recording.setRSquared(round(results.getRSquared(), 3));
        } else {
            recording.setTau(Double.NaN);
            recording.setRSquared(Double.NaN);
        }

        double density = calculateDensity(
                tracksFromSelectedSquares.size(),
                calcSquareArea(numberOfSelectedSquares),
                RECORDING_DURATION,
                recording.getConcentration()
        );
        recording.setDensity(round(density, 2));
    }

    // Main variability calculation
    public static double calcVariability(Table tracks,
                                         int squareNumber,
                                         int numberOfSquaresInRecording,
                                         int granularity) {

        // Matrix for variability analysis
        int[][] matrix = new int[granularity][granularity];

        // Width and height of a square
        int dimension = (int) Math.sqrt(numberOfSquaresInRecording);
        double width  = IMAGE_WIDTH / dimension;
        double height = IMAGE_WIDTH / dimension;

        // Access the columns once
        DoubleColumn xCol = tracks.doubleColumn("Track X Location");
        DoubleColumn yCol = tracks.doubleColumn("Track Y Location");

        // Loop over the tracks and fill the matrix
        for (int i = 0; i < tracks.rowCount(); i++) {
            double x = xCol.get(i);  // The x-coordinate of the track
            double y = yCol.get(i);  // The y-coordinate of the track

            // Get grid indices
            int[] indices = getIndices(x, y, width, height, squareNumber, dimension, granularity);
            int xi = indices[0];
            int yi = indices[1];

            if (xi >= 0 && xi < granularity && yi >= 0 && yi < granularity) {
                matrix[yi][xi]++;
            }
        }

        // Flatten matrix into an 1D array for stats
        int totalCells = granularity * granularity;
        double[] values = new double[totalCells];
        int idx = 0;
        for (int r = 0; r < granularity; r++) {
            for (int c = 0; c < granularity; c++) {
                values[idx++] = matrix[r][c];
            }
        }

        double mean = mean(values);
        if (mean == 0) {
            return 0.0;
        }

        double std = std(values, mean);
        return std / mean; // coefficient of variation
    }

    // Utility: compute mean
    private static double mean(double[] values) {
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    // Utility: compute std (population standard deviation)
    private static double std(double[] values, double mean) {
        double sumSq = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / values.length);
    }

    private static int[] getIndices(double x1,
                                    double y1,
                                    double width,
                                    double height,
                                    int squareSeqNr,
                                    int nrOfSquaresInRow,
                                    int granularity) {
        // Calculate the top-left corner (x0, y0) of the square
        double x0 = (squareSeqNr % nrOfSquaresInRow) * width;
        double y0 = (squareSeqNr / nrOfSquaresInRow) * height;

        // Calculate the grid indices (xi, yi) for the track
        int xi = (int) (((x1 - x0) / width) * granularity);
        int yi = (int) (((y1 - y0) / height) * granularity);

        return new int[]{xi, yi};
    }
}