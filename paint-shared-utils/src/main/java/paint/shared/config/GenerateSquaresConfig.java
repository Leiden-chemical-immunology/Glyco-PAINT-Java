package paint.shared.config;

import static paint.shared.config.PaintConfig.SECTION_GENERATE_SQUARES;
import static paint.shared.constants.PaintConstants.*;

/**
 * This class represents the configuration for generating squares within a system.
 * It holds various parameters required for controlling and defining the behavior
 * of square generation, including thresholds, counts, and mode settings.
 *
 * Instances of this class are created with configuration values retrieved
 * from a file using helper methods for obtaining integer, double, and string
 * values. These values are used to configure aspects such as the number of squares
 * to generate, constraints for calculations, and mode parameters.
 *
 * The configuration parameters include:
 * - Number of Squares in Recording: Specifies the total number of squares
 *   involved in the system.
 * - Min Tracks to Calculate Tau: Defines the minimum number of tracks required
 *   to perform Tau calculations.
 * - Min Required R Squared: Sets the minimum threshold for the R Squared value
 *   to ensure statistical reliability.
 * - Min Required Density Ratio: Determines the minimum density ratio required
 *   for calculations.
 * - Max Allowable Variability: Establishes the upper bound for allowed variability
 *   within the system.
 * - Neighbour Mode: Specifies the mode for handling neighborhood interactions.
 *   Typical values include "Free".
 *
 * The default values for these parameters are defined in the class and can be
 * overridden if needed during the creation of the configuration instance.
 */
public class GenerateSquaresConfig {

    // @formatter:off
    private final int    numberOfSquaresInRecording;
    private final int    minTracksToCalculateTau;
    private final double minRequiredRSquared;
    private final double minRequiredDensityRatio;
    private final double maxAllowableVariability;
    private final String neighbourMode;
    // @formatter:on


    /**
     * Constructs an instance of GenerateSquaresConfig with all configuration values
     * retrieved from a file. The configuration values include integer, double, and
     * string parameters which control aspects such as the number of squares in a recording,
     * thresholds for calculations, and mode preferences.
     *
     * Retrieves configuration values using the following methods:
     * - getInt: Used to fetch integer configuration values from the specified section and key.
     * - getDouble: Used to fetch double configuration values from the specified section and key.
     * - getString: Used to fetch string configuration values from the specified section and key.
     *
     * Default values for the configuration parameters are as follows:
     * - Number of Squares in Recording: 400
     * - Min Tracks to Calculate Tau: 20
     * - Min Required R Squared: 0.1
     * - Min Required Density Ratio: 0.1
     * - Max Allowable Variability: 10.0
     * - Neighbour Mode: "Free"
     */ /*
     * Full constructor with all values retrieved from file
     */
    public GenerateSquaresConfig() {

        // @formatter:off
        this.numberOfSquaresInRecording  = PaintConfig.getInt(   SECTION_GENERATE_SQUARES, NUMBER_OF_SQUARES_IN_RECORDING,400);
        this.minTracksToCalculateTau     = PaintConfig.getInt(   SECTION_GENERATE_SQUARES, MIN_TRACKS_TO_CALCULATE_TAU,   20);
        this.minRequiredRSquared         = PaintConfig.getDouble(SECTION_GENERATE_SQUARES, MIN_REQUIRED_R_SQUARED,        0.1);
        this.minRequiredDensityRatio     = PaintConfig.getDouble(SECTION_GENERATE_SQUARES, MIN_REQUIRED_DENSITY_RATIO,    0.1);
        this.maxAllowableVariability     = PaintConfig.getDouble(SECTION_GENERATE_SQUARES, MAX_ALLOWABLE_VARIABILITY,     10.0);
        this.neighbourMode               = PaintConfig.getString(SECTION_GENERATE_SQUARES, NEIGHBOUR_MODE,                "Free");
        // @formatter:on
    }

    public static GenerateSquaresConfig from() {
        return new GenerateSquaresConfig();
    }

    public int getNumberOfSquaresInRecording() {
        return numberOfSquaresInRecording;
    }

    public int getMinTracksToCalculateTau() {
        return minTracksToCalculateTau;
    }

    public double getMinRequiredRSquared() {
        return minRequiredRSquared;
    }

    public double getMinRequiredDensityRatio() {
        return minRequiredDensityRatio;
    }

    public double getMaxAllowableVariability() {
        return maxAllowableVariability;
    }

    public String getNeighbourMode() {
        return neighbourMode;
    }

    @Override
    public String toString() {

        // @formatter:off
        return "GenerateSquaresConfig{" +
                "Number of Squares in Row      = " + numberOfSquaresInRecording +
                ", Min Tracks To Calculate Tau = " + minTracksToCalculateTau +
                ", Min Required RSquared       = " + minRequiredRSquared +
                ", Min Required Density Ratio  = " + minRequiredDensityRatio +
                ", Max Allowable Variability   = " + maxAllowableVariability +
                ", Neighbour Mode              = '"+ neighbourMode + '\'' +
                '}';
        // @formatter:on
    }
}