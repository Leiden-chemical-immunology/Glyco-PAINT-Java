/******************************************************************************
 *  Class:        GenerateSquaresConfig.java
 *  Package:      paint.shared.config
 *
 *  PURPOSE:
 *    Encapsulates configuration settings for the “generate squares” process
 *    within the Glyco-PAINT system. Loads values from the configuration file
 *    and provides typed access (int, double, String) to each parameter.
 *
 *  DESCRIPTION:
 *    • numberOfSquaresInRecording: total number of squares in a recording
 *    • minTracksToCalculateTau: minimum track count required to compute Tau
 *    • minRequiredRSquared: minimum R² threshold for accepting a square
 *    • minRequiredDensityRatio: minimum density ratio threshold
 *    • maxAllowableVariability: upper bound on coefficient of variation for selection
 *    • neighbourMode: mode for neighbor-based logic (e.g., "Free", "Strict", "Relaxed")
 *
 *  RESPONSIBILITIES:
 *    • Read configuration values from the section identified by
 *      {@link PaintConfig#SECTION_GENERATE_SQUARES}
 *    • Provide getters for each configuration parameter
 *    • Enforce default values if configuration keys are missing
 *
 *  USAGE EXAMPLE:
 *    GenerateSquaresConfig config = GenerateSquaresConfig.from();
 *    int squareCount = config.getNumberOfSquaresInRecording();
 *    String mode = config.getNeighbourMode();
 *
 *  DEPENDENCIES:
 *    – {@link PaintConfig} for reading configuration values
 *    – {@link paint.shared.constants.PaintConstants} for default keys/values
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.shared.config;

import static paint.shared.config.PaintConfig.SECTION_GENERATE_SQUARES;
import static paint.shared.constants.PaintConstants.*;

/**
 * Holds configuration parameters used during the “generate squares” phase of the
 * system. Values are loaded from a configuration file or preferences store, and
 * if missing, sensible defaults are applied.
 */
public class GenerateSquaresConfig {

    
    private final int    numberOfSquaresInRecording;
    private final int    minTracksToCalculateTau;
    private final double minRequiredRSquared;
    private final double minRequiredDensityRatio;
    private final double maxAllowableVariability;
    private final String neighbourMode;
    

    /**
     * Constructs a GenerateSquaresConfig by reading appropriate keys from the
     * {@link PaintConfig} under section SECTION_GENERATE_SQUARES. If a
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
        
        this.numberOfSquaresInRecording  = PaintConfig.getInt(   SECTION_GENERATE_SQUARES, NUMBER_OF_SQUARES_IN_RECORDING,  400);
        this.minTracksToCalculateTau     = PaintConfig.getInt(   SECTION_GENERATE_SQUARES, MIN_TRACKS_TO_CALCULATE_TAU,     20);
        this.minRequiredRSquared         = PaintConfig.getDouble(SECTION_GENERATE_SQUARES, MIN_REQUIRED_R_SQUARED,          0.1);
        this.minRequiredDensityRatio     = PaintConfig.getDouble(SECTION_GENERATE_SQUARES, MIN_REQUIRED_DENSITY_RATIO,      0.1);
        this.maxAllowableVariability     = PaintConfig.getDouble(SECTION_GENERATE_SQUARES, MAX_ALLOWABLE_VARIABILITY,       10.0);
        this.neighbourMode               = PaintConfig.getString(SECTION_GENERATE_SQUARES, NEIGHBOUR_MODE,                  "Free");
        
    }

    /**
     * Static factory method to obtain an instance of this config.
     *
     * @return a new instance of GenerateSquaresConfig loaded from configuration
     */
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
        
        return "GenerateSquaresConfig{" +
                "Number of Squares in Row      = " + numberOfSquaresInRecording +
                ", Min Tracks To Calculate Tau = " + minTracksToCalculateTau +
                ", Min Required RSquared       = " + minRequiredRSquared +
                ", Min Required Density Ratio  = " + minRequiredDensityRatio +
                ", Max Allowable Variability   = " + maxAllowableVariability +
                ", Neighbour Mode              = '"+ neighbourMode + '\'' +
                '}';
        
    }
}