/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.shared.config;

import paint.shared.config.paintconfig.PaintConfig;
import paint.shared.utils.PaintLogger;

import static paint.shared.constants.PaintStringConstants.*;

/**
 * Holds configuration parameters used during the “generate squares” phase of the
 * system. Values are loaded from a configuration file or preferences store, and
 * if missing, sensible defaults are applied.
 */
public class GenerateSquaresConfig {

    private final int     numberOfSquaresInRecording;
    private final int     gridSize;
    private final int     minTracksToCalculate;
    private final int     minTracksToCalculateTau;
    private final double  minRequiredRSquared;
    private final double  minRequiredDensityRatio;
    private final double  maxAllowableVariability;
    private final String  neighbourMode;
    private final boolean tauFittingPlots;
    private final boolean backgroundPlots;


    /**
     * Constructs a GenerateSquaresConfig by reading appropriate keys from the
     * {@link PaintConfig} under section GENERATE_SQUARES. If a
     * value is missing, a default is applied:
     * <ul>
     *   <li>numberOfSquaresInRecording: 400</li>
     *   <li>minTracksToCalculateTau: 20</li>
     *   <li>minRequiredRSquared: 0.1</li>
     *   <li>minRequiredDensityRatio: 0.1</li>
     *   <li>maxAllowableVariability: 10.0</li>
     *   <li>neighbourMode: "Free"</li>
     * </ul>
     */
    public GenerateSquaresConfig() {
        
        this.numberOfSquaresInRecording  = PaintConfig.getInt(    GENERATE_SQUARES, NUMBER_OF_SQUARES_IN_RECORDING,  400);
        this.minTracksToCalculate        = PaintConfig.getInt(    GENERATE_SQUARES, MIN_TRACKS_TO_CALCULATE,         5);
        this.minTracksToCalculateTau     = PaintConfig.getInt(    GENERATE_SQUARES, MIN_TRACKS_TO_CALCULATE_TAU,     20);
        this.minRequiredRSquared         = PaintConfig.getDouble( GENERATE_SQUARES, MIN_REQUIRED_R_SQUARED,          0.1);
        this.tauFittingPlots             = PaintConfig.getBoolean(GENERATE_SQUARES, TAU_FITTING_PLOTS,               false);
        this.backgroundPlots             = PaintConfig.getBoolean(GENERATE_SQUARES, BACKGROUND_PLOTS,                false);

        this.gridSize = gridSizeFor(this.numberOfSquaresInRecording);

        if (isWholeScreen()) {
            // One square covering the whole image. The selection criteria that compare a square
            // with the rest of the image cannot apply, so they are fixed rather than read.
            this.minRequiredDensityRatio = WHOLE_SCREEN_MIN_REQUIRED_DENSITY_RATIO;
            this.maxAllowableVariability = WHOLE_SCREEN_MAX_ALLOWABLE_VARIABILITY;
            this.neighbourMode           = WHOLE_SCREEN_NEIGHBOUR_MODE;
        } else {
            this.minRequiredDensityRatio = PaintConfig.getDouble( GENERATE_SQUARES, MIN_REQUIRED_DENSITY_RATIO,      2.0);
            this.maxAllowableVariability = PaintConfig.getDouble( GENERATE_SQUARES, MAX_ALLOWABLE_VARIABILITY,       10.0);
            this.neighbourMode           = PaintConfig.getString( GENERATE_SQUARES, NEIGHBOUR_MODE,                  "Free");
        }
    }

    // -------------------------------------------------------------------------
    // Whole screen (1x1 grid)
    // -------------------------------------------------------------------------

    /**
     * Density ratio threshold for a 1x1 grid. With one square, the background is that square
     * itself, so its density ratio is always 1 and would never pass the normal threshold.
     */
    public static final double WHOLE_SCREEN_MIN_REQUIRED_DENSITY_RATIO = 0.0;

    /**
     * Variability limit for a 1x1 grid: effectively none. Variability is then measured over the
     * whole image, empty areas included, and is not comparable with that of a normal square.
     * <p>
     * A large finite value rather than infinity: the value is stamped into Recordings.csv, and
     * the CSV writer formats infinity as "∞", which the reader cannot parse back.
     */
    public static final double WHOLE_SCREEN_MAX_ALLOWABLE_VARIABILITY = 1.0E9;

    /**
     * Neighbour mode for a 1x1 grid. Strict and Relaxed require a selected neighbouring square,
     * which a single square does not have.
     */
    public static final String WHOLE_SCREEN_NEIGHBOUR_MODE = "Free";

    /**
     * Whether the grid is 1x1: one square covering the whole image, giving a Tau over all tracks.
     * In that case {@code Min Required Density Ratio}, {@code Max Allowable Variability} and
     * {@code Neighbour Mode} are not read from the configuration but fixed to the
     * {@code WHOLE_SCREEN_*} values.
     *
     * @return {@code true} for a 1x1 grid
     */
    public boolean isWholeScreen() {
        return gridSize == 1;
    }

    /**
     * Converts a square <em>count</em> into a grid <em>side</em> length — the single definition
     * of that conversion.
     * <p>
     * It previously existed inline at four call sites in two different forms: some truncated with
     * {@code (int) Math.sqrt(n)}, others rounded with {@code (int) Math.round(Math.sqrt(n))}. The
     * count always comes from a fixed dropdown (5x5 … 40x40) and so is a perfect square, which is
     * why the two never disagreed in practice — but there is no reason to keep two formulas around
     * waiting to.
     * <p>
     * A non-perfect square can only come from a hand-edited configuration file. That still yields a
     * well-defined grid, just a smaller one than was asked for, so this warns rather than fails.
     *
     * @param numberOfSquaresInRecording the configured number of squares per recording
     * @return the number of squares along one side of the grid
     */
    public static int gridSizeFor(int numberOfSquaresInRecording) {
        int side = (int) Math.round(Math.sqrt(numberOfSquaresInRecording));
        if (side * side != numberOfSquaresInRecording) {
            PaintLogger.warnf(
                    "'%s' is %d, which is not a perfect square. Using a %dx%d grid (%d squares).",
                    NUMBER_OF_SQUARES_IN_RECORDING, numberOfSquaresInRecording, side, side, side * side);
        }
        return side;
    }

    /**
     * @return total number of squares in a recording grid.
     */
    public int getNumberOfSquaresInRecording() {
        return numberOfSquaresInRecording;
    }

    /**
     * The side length of the square grid — i.e. {@code sqrt(numberOfSquaresInRecording)}.
     * <p>
     * This is the single place that converts a square <em>count</em> into a grid
     * <em>side</em>. It used to be recomputed inline at four call sites, and not
     * consistently: some truncated with {@code (int) Math.sqrt(n)} while others rounded
     * with {@code (int) Math.round(Math.sqrt(n))}. The count always comes from a fixed
     * dropdown (5x5 … 40x40) so it is a perfect square and the two agree — but there is
     * no reason to leave two formulas lying around waiting to disagree.
     *
     * @return the number of squares along one side of the grid
     */
    public int getGridSize() {
        return gridSize;
    }

    /**
     * @return minimum number of tracks needed for basic calculations.
     */
    public int getMinTracksToCalculate() {
        return minTracksToCalculate;
    }

    /**
     * @return minimum track count required to compute a valid Tau value.
     */
    public int getMinTracksToCalculateTau() {
        return minTracksToCalculateTau;
    }

    /**
     * @return the threshold for minimum acceptable R² of binding kinetics fits.
     */
    public double getMinRequiredRSquared() {
        return minRequiredRSquared;
    }

    /**
     * @return the threshold for minimum density ratio (foreground vs background).
     */
    public double getMinRequiredDensityRatio() {
        return minRequiredDensityRatio;
    }

    /**
     * @return the maximum allowed coefficient of variation for selection.
     */
    public double getMaxAllowableVariability() {
        return maxAllowableVariability;
    }

    /**
     * @return the active neighbour mode (e.g., "Free", "Strict").
     */
    public String getNeighbourMode() {
        return neighbourMode;
    }

    /**
     * @return {@code true} if per-square Tau-fitting plots should be generated (default {@code false}).
     */
    public boolean isTauFittingPlots() {
        return tauFittingPlots;
    }

    /**
     * @return {@code true} if background histogram plots should be generated (default {@code false}).
     */
    public boolean isBackgroundPlots() {
        return backgroundPlots;
    }

    @Override
    public String toString() {

        return "GenerateSquaresConfig{" +
                "Number of Squares in Row      = " + numberOfSquaresInRecording +
                ", Min Tracks To Calculate     = " + minTracksToCalculate +
                ", Min Tracks To Calculate Tau = " + minTracksToCalculateTau +
                ", Min Required R Squared      = " + minRequiredRSquared +
                ", Min Required Density Ratio  = " + minRequiredDensityRatio +
                ", Max Allowable Variability   = " + maxAllowableVariability +
                ", Neighbour Mode              = '"+ neighbourMode + '\'' +
                ", Tau Fitting Plots           = " + tauFittingPlots +
                ", Background Plots            = " + backgroundPlots +
                '}';
    }
}