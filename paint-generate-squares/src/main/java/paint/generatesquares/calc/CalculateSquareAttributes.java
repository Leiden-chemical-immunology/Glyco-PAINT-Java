/******************************************************************************
 *  Class:        CalculateAttributes.java
 *  Package:      generatesquares.calc
 *
 *  PURPOSE:
 *    Performs quantitative analysis and metric calculations for squares and
 *    recordings in the Paint experiment workflow.
 *
 *  DESCRIPTION:
 *    This class provides static computational methods used by the “Generate Squares”
 *    process. It computes detailed per-square and per-recording attributes such as:
 *    Tau fitting, variability, density ratios, and background estimation.
 *    It also applies visibility filtering and labeling logic to identify valid squares.
 *
 *  RESPONSIBILITIES:
 *    • Calculate Tau and R² values from track data
 *    • Compute variability, density, and density ratio metrics
 *    • Estimate background density and apply visibility filters
 *    • Aggregate per-square metrics into recording-level summaries
 *
 *  USAGE EXAMPLE:
 *    CalculateAttributes.calculateSquareAttributes(experimentPath, recording, config);
 *    CalculateAttributes.calculateRecordingAttributes(recording, config);
 *
 *  DEPENDENCIES:
 *    - paint.shared.config.GenerateSquaresConfig
 *    - paint.shared.objects.{Square, Recording, Track}
 *    - paint.shared.utils.{SquareUtils, PaintLogger}
 *    - generatesquares.calc.CalculateTau
 *    - tech.tablesaw.api.Table
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-23
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.generatesquares.calc;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.objects.Track;
import paint.shared.utils.CalculateTau;
import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.util.List;

import static paint.generatesquares.calc.PlotUtils.saveTauFitPlot;
import static paint.generatesquares.calc.SquareUtils.*;
import static paint.shared.constants.PaintConstants.IMAGE_WIDTH;
import static paint.shared.constants.PaintConstants.RECORDING_DURATION;
import static paint.shared.objects.Square.calculateSquareArea;
import static paint.shared.utils.CalculateTau.calculateTau;
import static paint.shared.utils.Miscellaneous.round;
import static paint.shared.utils.SharedSquareUtils.*;

public class CalculateSquareAttributes {

    /**
     * Calculates attributes for each square in a recording, such as Tau, density, variability, and density ratio.
     * It also applies visibility filtering based on given parameters and assigns label numbers to selected squares.
     *
     * @param experimentPath        the file path of the current experiment for saving generated files
     * @param recording             the recording containing squares and associated track data
     * @param generateSquaresConfig the configuration parameters used for generating square attributes and analysis
     */
    public static void calculateSquareAttributes(Path experimentPath,
                                                 Recording recording,
                                                 GenerateSquaresConfig generateSquaresConfig) {
        double       minRequiredRSquared        = generateSquaresConfig.getMinRequiredRSquared();
        int          minTracksForTau            = generateSquaresConfig.getMinTracksToCalculateTau();
        double       maxAllowableVariability    = generateSquaresConfig.getMaxAllowableVariability();
        double       minRequiredDensityRatio    = generateSquaresConfig.getMinRequiredDensityRatio();
        String       neighbourMode              = generateSquaresConfig.getNeighbourMode();
        int          numberOfSquaresInRecording = generateSquaresConfig.getNumberOfSquaresInRecording();
        double       squareArea                 = calculateSquareArea(numberOfSquaresInRecording);    // Here we look at the single square
        double       concentration              = recording.getConcentration();
        List<Square> squaresOfRecording         = recording.getSquaresOfRecording();

        SquareUtils.BackgroundEstimationResult result = calculateBackgroundDensity(squaresOfRecording);
        double meanBackgroundTracks                   = result.getBackgroundMean();
        double backgroundTracksOri                    = calcAverageTrackCountInBackgroundSquares(recording.getSquaresOfRecording(), (int) (0.1 * numberOfSquaresInRecording));

        PaintLogger.debugf("Estimated Background track count = %.2f, n = %d%n",
                           meanBackgroundTracks, result.getBackgroundSquares().size());

        for (Square square : squaresOfRecording) {
            List<Track> tracksInSquare  = square.getTracks();
            Table       table           = square.getTracksTable();
            int         squareNumber    = square.getSquareNumber();
            Table tracksInSquareTable   = square.getTracksTable();

            if (tracksInSquare == null || tracksInSquare.isEmpty()) {
                continue;
            }

            if (tracksInSquare.size() >= minTracksForTau) {
                CalculateTau.CalculateTauResult results = calculateTau(tracksInSquare, minRequiredRSquared);

                if (paint.shared.config.PaintConfig.getBoolean("Generate Squares", "Plot Curve Fitting", false) &&
                        tracksInSquare.size() >= minTracksForTau) {
                    saveTauFitPlot(tracksInSquare, results, experimentPath, recording.getRecordingName(), squareNumber);
                }

                if (results.getStatus() == CalculateTau.CalculateTauResult.Status.TAU_SUCCESS) {
                    square.setTau(round(results.getTau(), 0));
                    square.setRSquared(round(results.getRSquared(), 3));
                } else {
                    square.setTau(Double.NaN);
                    square.setRSquared(Double.NaN);
                }
            } else {
                square.setTau(Double.NaN);
                square.setRSquared(Double.NaN);
            }

            if (table.rowCount() == 0) {
                continue;
            }

            // @format:off
            square.setVariability(                   round(calcVariability(table, squareNumber, numberOfSquaresInRecording, 10),         2));
            square.setDensity(                       round(calculateDensity(tracksInSquare.size(), squareArea, RECORDING_DURATION, concentration), 3));
            square.setDensityRatio(                  round(calculateDensityRatio(tracksInSquare.size(), meanBackgroundTracks),                     2));
            square.setDensityRatioOri(               round(calculateDensityRatio(tracksInSquare.size(), backgroundTracksOri),                      2));

            square.setMedianDiffusionCoefficient(    round(tracksInSquareTable.doubleColumn("Diffusion Coefficient").median(),         2));
            square.setMedianDiffusionCoefficientExt( round(tracksInSquareTable.doubleColumn("Diffusion Coefficient Ext").median(),     2));

            square.setMedianDisplacement(            round(tracksInSquareTable.doubleColumn("Track Displacement").median(),            1));
            square.setMaxDisplacement(               round(tracksInSquareTable.doubleColumn("Track Displacement").max(),               1));
            square.setTotalDisplacement(             round(tracksInSquareTable.doubleColumn("Track Displacement").sum(),               1));

            square.setMedianMaxSpeed(                round(tracksInSquareTable.doubleColumn("Track Max Speed").median(),               1));
            square.setMaxMaxSpeed(                   round(tracksInSquareTable.doubleColumn("Track Max Speed").max(),                  1));

            square.setMedianMedianSpeed(             round(tracksInSquareTable.doubleColumn("Track Median Speed").median(),            1));
            square.setMaxMedianSpeed(                round(tracksInSquareTable.doubleColumn("Track Median Speed").max(),               1));

            square.setMaxTrackDuration(              round(tracksInSquareTable.doubleColumn("Track Duration").max(),                   1));
            square.setTotalTrackDuration(            round(tracksInSquareTable.doubleColumn("Track Duration").sum(),                   1));
            square.setMedianTrackDuration(           round(tracksInSquareTable.doubleColumn("Track Duration").median(),                1));
            // @format:on

        }

        applyVisibilityFilter(squaresOfRecording,
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
     * Calculates various attributes for a recording, including Tau, density,
     * background statistics, and R-squared values. This method processes detailed
     * information about the recording and applies configuration settings to compute
     * the necessary metrics.
     *
     * @param recording             the recording object containing square and track data to process
     * @param generateSquaresConfig the configuration parameters for generating square attributes
     */
    public static void calculateRecordingAttributes(Recording recording,
                                                    GenerateSquaresConfig generateSquaresConfig) {

        double minRequiredRSquared = generateSquaresConfig.getMinRequiredRSquared();

        BackgroundEstimationResult result = SquareUtils.calculateBackgroundDensity(recording.getSquaresOfRecording());
        double       meanBackgroundTracks = result.getBackgroundMean();
        int          backgroundTracks     = result.getBackgroundSquares().stream().mapToInt(Square::getNumberOfTracks).sum();

        PaintLogger.debugf("Estimated Background track count = %.2f, n = %d%n", meanBackgroundTracks, result.getBackgroundSquares().size());

        recording.setNumberOfSquaresInBackground(result.getBackgroundSquares().size());
        recording.setNumberOfTracksInBackground(backgroundTracks);
        recording.setAverageTracksInBackGround(round(meanBackgroundTracks, 3));

        List<Track> tracksFromSelectedSquares = getTracksFromSelectedSquares(recording.getSquaresOfRecording());
        CalculateTau.CalculateTauResult results = calculateTau(tracksFromSelectedSquares, minRequiredRSquared);
        if (results.getStatus() == CalculateTau.CalculateTauResult.Status.TAU_SUCCESS) {
            recording.setTau(round(results.getTau(), 0));
            recording.setRSquared(round(results.getRSquared(), 3));
        } else {
            recording.setTau(Double.NaN);
            recording.setRSquared(Double.NaN);
        }

        double density = calculateDensity(
                tracksFromSelectedSquares.size(),
                calculateSquareArea(getNumberOfSelectedSquares(recording)),
                RECORDING_DURATION,
                recording.getConcentration()
        );
        recording.setDensity(round(density, 2));
    }

    public static double calculateDensityRatio(int numberOfTracksInSquare, double numberOfTracksInBackgroundSquare) {
        if (numberOfTracksInBackgroundSquare == 0) {
            return 0;
        } else {
            return numberOfTracksInSquare / numberOfTracksInBackgroundSquare;
        }
    }

    /**
     * Computes the spatial variability of track positions within a selected square of a recording.
     * The square is subdivided into a grid of size `granularity × granularity`, and the count
     * of tracks falling into each grid cell is determined. Variability is then quantified
     * as the coefficient of variation (standard deviation divided by mean) of the cell counts.
     *
     * @param tracks                     A table of track data, including x- and y-coordinate columns.
     * @param squareNumber               The index of the square region within the recording which is being analysed.
     * @param numberOfSquaresInRecording The total number of spatial squares defined for the recording.
     * @param granularity                The number of subdivisions (cells) along each dimension of the square’s grid.
     * @return                           The coefficient of variation (σ / μ) of the grid-cell counts;
     *                                   returns 0.0 if the mean cell count is zero (no tracks).
     */
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

    /**
     * Computes the mean of the given array of double values.
     *
     * @param values an array of double values for which the mean is to be calculated.
     *               The array must not be empty and must contain at least one value.
     * @return the mean (average) of the given array of double values.
     */
    // Utility: compute mean
    private static double mean(double[] values) {
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    /**
     * Calculates the population standard deviation of the given array of double values.
     *
     * @param values an array of double values for which the population standard deviation is to be calculated.
     *               The array must not be empty and must contain at least one value.
     * @param mean   the mean (average) of the values in the array, precomputed to optimize calculation.
     * @return the population standard deviation of the given array of double values.
     */
    // Utility: compute std (population standard deviation)
    private static double std(double[] values, double mean) {
        double sumSq = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / values.length);
    }

    /**
     * Calculates the grid indices for a point within a specific square of a recording.
     * The method determines the location of the point in a finer grid inside the square,
     * based on the specified granularity.
     *
     * @param x1               the x-coordinate of the point in the global coordinate system
     * @param y1               the y-coordinate of the point in the global coordinate system
     * @param width            the width of each square in the grid
     * @param height           the height of each square in the grid
     * @param squareSeqNr      the sequence number of the square in the grid
     * @param nrOfSquaresInRow the total number of squares in a single row of the grid
     * @param granularity      the number of subdivisions along one axis within a square
     * @return an array of two integers, where the first value is the x-index (column index)
     * and the second value is the y-index (row index) of the point in the finer grid
     */
    private static int[] getIndices(double x1,
                                    double y1,
                                    double width,
                                    double height,
                                    int squareSeqNr,
                                    int nrOfSquaresInRow,
                                    int granularity) {
        // Calculate the top-left corner (x0, y0) of the square
        double x0 = (squareSeqNr % nrOfSquaresInRow) * width;
        double y0 = (squareSeqNr / nrOfSquaresInRow) * height;     // Integer division is intended

        // Calculate the grid indices (xi, yi) for the track
        int xi = (int) (((x1 - x0) / width) * granularity);
        int yi = (int) (((y1 - y0) / height) * granularity);

        return new int[]{xi, yi};
    }
}