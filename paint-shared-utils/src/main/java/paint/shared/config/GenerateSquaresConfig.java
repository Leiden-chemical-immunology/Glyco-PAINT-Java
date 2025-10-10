package paint.shared.config;

import static paint.shared.config.PaintConfig.*;

/**
 * Holds the data retrieved from the GenerateSquares section of the Paint configuration file.
 */
public class GenerateSquaresConfig {

    // @formatter:off
    private final int    numberOfSquaresInRow;
    private final int    numberOfSquaresInColumn;
    private final int    minTracksToCalculateTau;
    private final double minRequiredRSquared;
    private final double minRequiredDensityRatio;
    private final double maxAllowableVariability;
    private final String neighbourMode;
    // @formatter:on


    /*
     * Full constructor with all values specified
     */


    private GenerateSquaresConfig(PaintConfig paintConfig) {

        // @formatter:off
        this.numberOfSquaresInRow    = getInt("Generate Squares",    "Number of Squares In Row", 5);
        this.numberOfSquaresInColumn = getInt("Generate Squares",    "Number of Squares In Column", 5);
        this.minTracksToCalculateTau = getInt("Generate Squares",    "Min Tracks to Calculate Tau", 20);
        this.minRequiredRSquared     = getDouble("Generate Squares", "Min Required R Squared", 0.1);
        this.minRequiredDensityRatio = getDouble("Generate Squares", "Min Required Density Ratio", 0.1);
        this.maxAllowableVariability = getDouble("Generate Squares", "Max Allowable Variability", 10.0);
        this.neighbourMode           = getString("Generate Squares", "Neighbour Mode", "Free");
        // @formatter:on
    }


    public static GenerateSquaresConfig from(PaintConfig paintConfig) {
        return new GenerateSquaresConfig(paintConfig);
    }

    // Getters are not really needed as attributes are public
    // Setter methods are not needed either, as the attributes are final

    public int getNumberOfSquaresInRow() {
        return numberOfSquaresInRow;
    }

    public int getNumberOfSquaresInColumn() {
        return numberOfSquaresInColumn;
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
                "nrSquaresInRow=" + numberOfSquaresInRow +
                ", nrSquaresInColumn=" + numberOfSquaresInColumn +
                ", minTracksToCalculateTau=" + minTracksToCalculateTau +
                ", minRequiredRSquared=" + minRequiredRSquared +
                ", minRequiredDensityRatio=" + minRequiredDensityRatio +
                ", maxAllowableVariability=" + maxAllowableVariability +
                ", neighbourMode='" + neighbourMode + '\'' +
                '}';
    }
}