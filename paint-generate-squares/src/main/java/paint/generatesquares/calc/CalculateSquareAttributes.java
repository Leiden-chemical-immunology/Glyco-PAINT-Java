/*=============================================================================
 *  Class:        CalculateSquareAttributes.java
 *  Package:      generatesquares.calc
 *
 *  PURPOSE:
 *    Performs quantitative analysis and metric calculations for squares and
 *    recordings in the Paint experiment workflow.
 *
 *  DESCRIPTION:
 *    This class provides static computational methods used by the “Generate Squares”
 *    pipeline. It computes detailed per-square and per-recording attributes such as:
 *      • Tau fitting and R² evaluation
 *      • Variability metrics
 *      • Density and density ratio calculations
 *      • Background density estimation
 *    It also applies visibility filters and assigns label numbers to valid squares.
 *
 *  RESPONSIBILITIES:
 *    • Calculate Tau and R² values from track data
 *    • Compute variability, density, and density ratio metrics
 *    • Estimate background density and apply visibility filters
 *    • Aggregate per-square metrics into recording-level summaries
 *
 *  USAGE EXAMPLE:
 *    CalculateSquareAttributes.calculateSquareAttributes(expPath, recording, config);
 *    CalculateSquareAttributes.calculateRecordingAttributes(recording, config);
 *
 *  DEPENDENCIES:
 *    - paint.shared.config.GenerateSquaresConfig
 *    - paint.shared.objects.{Square, Recording, Track}
 *    - paint.shared.utils.{SquareUtils, PaintLogger, CalculateTau}
 *    - generatesquares.calc.PlotUtils
 *    - tech.tablesaw.api.Table
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
=============================================================================*/

package paint.generatesquares.calc;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.config.paintconfig.PaintConfig;
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

import static paint.shared.objects.Square.calculateSquareArea;
import static paint.shared.utils.CalculateTau.calculateTau;
import static paint.shared.utils.Miscellaneous.round;
import static paint.shared.utils.SharedSquareUtils.*;

import static paint.shared.constants.PaintGeometry.IMAGE_WIDTH;
import static paint.shared.constants.PaintTiming.RECORDING_DURATION;
import static paint.shared.constants.PaintStringConstants.*;

public class CalculateSquareAttributes {

    /**
     * Fraction of a recording's squares (those with the lowest track counts) used
     * to estimate the background track density for the "original" density ratio
     * ({@code Density Ratio Ori}). This is a fixed methodological constant of the
     * original Python method, not a user-tunable parameter; it is kept explicit
     * here rather than as a magic literal.
     */
    private static final double BACKGROUND_SQUARES_FRACTION = 0.1;

    /**
     * Calculates attributes for every square in a recording, including Tau,
     * density, variability, density ratio, and various kinematic statistics.
     * Visibility filtering is performed based on configuration parameters,
     * followed by label number assignment for visible squares.
     *
     * @param experimentPath        the experiment path (used for plot output)
     * @param recording             the recording containing track and square data
     * @param generateSquaresConfig configuration parameters for the calculation
     */
    public static void calculateSquareAttributes(Path experimentPath,
            Recording recording,
            GenerateSquaresConfig generateSquaresConfig) {
        double                     minRequiredRSquared              = generateSquaresConfig.getMinRequiredRSquared();
        int                        minNumberOfTracksToCalculate     = generateSquaresConfig.getMinTracksToCalculate();
        int                        minNumberOfTracksToCalculateTau  = generateSquaresConfig.getMinTracksToCalculateTau();
        double                     maxAllowableVariability          = generateSquaresConfig.getMaxAllowableVariability();
        double                     minRequiredDensityRatio          = generateSquaresConfig.getMinRequiredDensityRatio();
        String                     neighbourMode                    = generateSquaresConfig.getNeighbourMode();
        int                        numberOfSquaresInRecording       = generateSquaresConfig.getNumberOfSquaresInRecording();
        double                     squareArea                       = calculateSquareArea(numberOfSquaresInRecording);    // Here we look at the single square
        double                     concentration                    = recording.getConcentration();
        List<Square>               squaresOfRecording               = recording.getSquaresOfRecording();
        boolean                    showTauFittingPlots              = PaintConfig.getBoolean(GENERATE_SQUARES, TAU_FITTING_PLOTS, false);
        BackgroundEstimationResult bgResult                         = calculateBackgroundDensity(squaresOfRecording);
        CalcParams                 params                           = new CalcParams(recording, generateSquaresConfig, bgResult);

        PaintLogger.debugf("Estimated Background track count = %.2f, number of background squares = %d%n",
                           params.numberOfTracksInBackgroundSquare, bgResult.getNumberOfBackgroundSquares());

        for (Square square : squaresOfRecording) {
            calculateAttributesForSquare(square, experimentPath, recording, generateSquaresConfig, params);
        }

        // --- Apply visibility filters ---
        applyVisibilityFilter(squaresOfRecording,
                              params.minRequiredDensityRatio,
                              params.maxAllowableVariability,
                              params.minRequiredRSquared,
                              params.neighbourMode);

        // --- Assign label numbers to visible squares ---
        int labelNumber = 0;
        for (Square sq : squaresOfRecording) {
            if (sq.isVisible()) {
                sq.setLabelNumber(labelNumber++);
            }
        }
    }

    /**
     * Container for calculation parameters to reduce argument counts.
     */
    private static class CalcParams {
        final double  minRequiredRSquared;
        final int     minNumberOfTracksToCalculate;
        final int     minNumberOfTracksToCalculateTau;
        final double  maxAllowableVariability;
        final double  minRequiredDensityRatio;
        final String  neighbourMode;
        final int     numberOfSquaresInRecording;
        final double  squareArea;
        final double  concentration;
        final boolean showTauFittingPlots;
        final double  numberOfTracksInBackgroundSquare;
        final double  backgroundTracksOri;

        CalcParams(Recording recording, GenerateSquaresConfig config, BackgroundEstimationResult bgResult) {
            this.minRequiredRSquared              = config.getMinRequiredRSquared();
            this.minNumberOfTracksToCalculate     = config.getMinTracksToCalculate();
            this.minNumberOfTracksToCalculateTau  = config.getMinTracksToCalculateTau();
            this.maxAllowableVariability          = config.getMaxAllowableVariability();
            this.minRequiredDensityRatio          = config.getMinRequiredDensityRatio();
            this.neighbourMode                    = config.getNeighbourMode();
            this.numberOfSquaresInRecording       = config.getNumberOfSquaresInRecording();
            this.squareArea                       = calculateSquareArea(numberOfSquaresInRecording);
            this.concentration                    = recording.getConcentration();
            this.showTauFittingPlots              = PaintConfig.getBoolean(GENERATE_SQUARES, TAU_FITTING_PLOTS, false);
            this.numberOfTracksInBackgroundSquare = bgResult.getNumberOfTracksInBackgroundSquare();
            this.backgroundTracksOri              = calcAverageTrackCountInBackgroundSquares(recording.getSquaresOfRecording(),
                                                                                            (int) (BACKGROUND_SQUARES_FRACTION * numberOfSquaresInRecording));
        }
    }

    private static void calculateAttributesForSquare(Square square, Path experimentPath, Recording recording,
                                                     GenerateSquaresConfig config, CalcParams params) {

        List<Track> tracksInSquare = square.getTracks();
        if (tracksInSquare == null || tracksInSquare.isEmpty()) {
            return;
        }

        Table table = square.getTracksTable();
        if (table.rowCount() == 0) {
            return;
        }

        // Do not calculate attributes if not enough tracks
        if (tracksInSquare.size() < params.minNumberOfTracksToCalculate) {
            square.resetCalculatedAttributes();
            return;
        }

        int squareNumber = square.getSquareNumber();

        // --- Tau fitting ---
        if (tracksInSquare.size() >= params.minNumberOfTracksToCalculateTau) {
            CalculateTau.CalculateTauResult results = calculateTau(tracksInSquare, params.minRequiredRSquared);

            if (params.showTauFittingPlots) {
                saveTauFitPlot(tracksInSquare, results, experimentPath,
                               recording.getRecordingName(), squareNumber);
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

        // --- Variability, density, kinematic metrics ---

        // Note that double values are written with 3 decimals in Squares

        final int PRECISION_L  = 2;
        final int PRECISION_M1 = 3;
        final int PRECISION_M2 = 4;
        final int PRECISION_H  = 5;

        square.setVariability(round(calculateVariability(table, squareNumber, params.numberOfSquaresInRecording, 10),       PRECISION_M1));
        square.setDensity(round(calculateDensity(tracksInSquare.size(), params.squareArea, RECORDING_DURATION, params.concentration), PRECISION_H));
        square.setDensityRatio(round(calculateDensityRatio(tracksInSquare.size(), params.numberOfTracksInBackgroundSquare),           PRECISION_M1));
        square.setDensityRatioOri(round(calculateDensityRatio(tracksInSquare.size(), params.backgroundTracksOri),                     PRECISION_M1));

        square.setMedianDiffusionCoefficient(round(table.doubleColumn(DIFFUSION_COEFFICIENT).median(),        PRECISION_M2));
        square.setMedianDiffusionCoefficientExt(round(table.doubleColumn(DIFFUSION_COEFFICIENT_EXT).median(), PRECISION_M1));

        square.setMedianDisplacement(round(table.doubleColumn(TRACK_DISPLACEMENT).median(), PRECISION_L));
        square.setMaxDisplacement(round(table.doubleColumn(TRACK_DISPLACEMENT).max(),       PRECISION_L));
        square.setTotalDisplacement(round(table.doubleColumn(TRACK_DISPLACEMENT).sum(),     PRECISION_L));

        square.setMedianMaxSpeed(round(table.doubleColumn(TRACK_MAX_SPEED).median(),        PRECISION_L));
        square.setMaxMaxSpeed(round(table.doubleColumn(TRACK_MAX_SPEED).max(),              PRECISION_L));

        square.setMedianMedianSpeed(round(table.doubleColumn(TRACK_MEDIAN_SPEED).median(),  PRECISION_L));
        square.setMaxMedianSpeed(round(table.doubleColumn(TRACK_MEDIAN_SPEED).max(),        PRECISION_L));

        square.setMaxTrackDuration(round(table.doubleColumn(TRACK_DURATION).max(),          PRECISION_L));
        square.setTotalTrackDuration(round(table.doubleColumn(TRACK_DURATION).sum(),        PRECISION_L));
        square.setMedianTrackDuration(round(table.doubleColumn(TRACK_DURATION).median(),    PRECISION_L));
    }

    /**
     * Computes recording-level attributes, including Tau, R², background statistics,
     * and density estimates based on selected (visible) squares.
     *
     * @param recording             the recording being processed
     * @param generateSquaresConfig configuration parameters
     */
    public static void calculateRecordingAttributes(Recording recording,
            GenerateSquaresConfig generateSquaresConfig) {

        double minRequiredRSquared = generateSquaresConfig.getMinRequiredRSquared();

        List<Square>               squaresOfRecording   = recording.getSquaresOfRecording();
        BackgroundEstimationResult bgResult             = calculateBackgroundDensity(squaresOfRecording);
        double                     meanBackgroundTracks = bgResult.getNumberOfTracksInBackgroundSquare();
        int                        backgroundTracks     = bgResult.getBackgroundSquares()
                                                                  .stream()
                                                                  .mapToInt(Square::getNumberOfTracks)
                                                                  .sum();

        recording.setNumberOfSquaresInBackground(bgResult.getBackgroundSquares().size());
        recording.setNumberOfTracksInBackground(backgroundTracks);
        recording.setAverageTracksInBackGround(round(meanBackgroundTracks, 3));

        List<Track> selectedTracks = getTracksFromSelectedSquares(squaresOfRecording);

        if (selectedTracks.isEmpty()) {
            recording.setTau(Double.NaN);
            recording.setRSquared(Double.NaN);
            recording.setDensity(0.0);
            return;
        }

        // --- Tau for the whole recording ---
        CalculateTau.CalculateTauResult tauResult = calculateTau(selectedTracks, minRequiredRSquared);

        if (tauResult.getStatus() == CalculateTau.CalculateTauResult.Status.TAU_SUCCESS) {
            recording.setTau(round(tauResult.getTau(), 0));
            recording.setRSquared(round(tauResult.getRSquared(), 3));
        } else {
            recording.setTau(Double.NaN);
            recording.setRSquared(Double.NaN);
        }

        // --- Recording density metrics ---
        int totalTracks = selectedTracks.size();

        int    nSelectedSquares = getNumberOfSelectedSquares(recording);
        double area             = calculateSquareArea(generateSquaresConfig.getNumberOfSquaresInRecording()) * nSelectedSquares;
        double concentration    = recording.getConcentration();

        recording.setDensity(round(calculateDensity(totalTracks, area, RECORDING_DURATION, concentration), 3));
    }

    /**
     * Returns the density ratio between a square and the background track density.
     */
    public static double calculateDensityRatio(int numberOfTracksInSquare,
            double numberOfTracksInBackgroundSquare) {
        return (numberOfTracksInBackgroundSquare == 0)
                ? 0
                : numberOfTracksInSquare / numberOfTracksInBackgroundSquare;
    }

    /**
     * Computes spatial variability of track positions within a square.
     * Variability is defined as the coefficient of variation (σ / μ) over
     * grid-cell track counts within a `granularity × granularity` subdivision
     * of the square.
     */
    public static double calculateVariability(Table tracks,
            int squareNumber,
            int numberOfSquaresInRecording,
            int granularity) {

        int[][] matrix    = new int[granularity][granularity];
        int     dimension = (int) Math.sqrt(numberOfSquaresInRecording);
        double  width     = IMAGE_WIDTH / dimension;
        double  height    = IMAGE_WIDTH / dimension;

        // Access the columns once
        DoubleColumn xCol = tracks.doubleColumn(TRACK_X_LOCATION);
        DoubleColumn yCol = tracks.doubleColumn(TRACK_Y_LOCATION);

        for (int i = 0; i < tracks.rowCount(); i++) {
            double x = xCol.get(i);  // The x-coordinate of the track
            double y = yCol.get(i);  // The y-coordinate of the track

            int[] indices = getIndices(x, y, width, height, squareNumber, dimension, granularity);
            int   xi      = indices[0];
            int   yi      = indices[1];

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
        return std / mean;
    }

    /**
     * Computes the mean of an array of doubles.
     */
    private static double mean(double[] values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    /**
     * Computes the population standard deviation of an array of doubles.
     */
    private static double std(double[] values, double mean) {
        double sumSq = 0.0;
        for (double value : values) {
            double diff = value - mean;
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / values.length);
    }

    /**
     * Calculates the grid indices for a point within a specific square of a recording.
     * The method determines the location of the point in a finer grid inside the square,
     * based on the specified granularity.
     *
     * @param x1                   the x-coordinate of the point in the global coordinate system
     * @param y1                   the y-coordinate of the point in the global coordinate system
     * @param width                the width of each square in the grid
     * @param height               the height of each square in the grid
     * @param squareSequenceNumber the sequence number of the square in the grid
     * @param numberOfSquaresInRow the total number of squares in a single row of the grid
     * @param granularity          the number of subdivisions along one axis within a square
     * @return an array of two integers, where the first value is the x-index (column index)
     * and the second value is the y-index (row index) of the point in the finer grid
     */
    private static int[] getIndices(double x1,
            double y1,
            double width,
            double height,
            int    squareSequenceNumber,
            int    numberOfSquaresInRow,
            int    granularity) {

        double x0 = (squareSequenceNumber % numberOfSquaresInRow) * width;
        double y0 = (squareSequenceNumber / numberOfSquaresInRow) * height;     // Integer division is intended

        int xi = (int) (((x1 - x0) / width)  * granularity);
        int yi = (int) (((y1 - y0) / height) * granularity);

        return new int[]{xi, yi};
    }
}