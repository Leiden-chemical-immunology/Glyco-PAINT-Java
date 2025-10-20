package paint.shared.config;

import static paint.shared.config.PaintConfig.*;

/**
 * Holds the data retrieved from the GenerateSquares section of the Paint configuration file.
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


    /*
     * Full constructor with all values retrieved from file
     */
    private GenerateSquaresConfig() {

        // @formatter:off
        this.numberOfSquaresInRecording  = getInt(   "Generate Squares", "Number of Squares in Recording", 400);
        this.minTracksToCalculateTau     = getInt(   "Generate Squares", "Min Tracks to Calculate Tau", 20);
        this.minRequiredRSquared         = getDouble("Generate Squares", "Min Required R Squared", 0.1);
        this.minRequiredDensityRatio     = getDouble("Generate Squares", "Min Required Density Ratio", 0.1);
        this.maxAllowableVariability     = getDouble("Generate Squares", "Max Allowable Variability", 10.0);
        this.neighbourMode               = getString("Generate Squares", "Neighbour Mode", "Free");
        // @formatter:on
    }

    public static GenerateSquaresConfig from(PaintConfig paintConfig) {
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
                "nrSquaresInRow="            + numberOfSquaresInRecording +
                ", minTracksToCalculateTau=" + minTracksToCalculateTau +
                ", minRequiredRSquared="     + minRequiredRSquared +
                ", minRequiredDensityRatio=" + minRequiredDensityRatio +
                ", maxAllowableVariability=" + maxAllowableVariability +
                ", neighbourMode='"          + neighbourMode + '\'' +
                '}';
        // @formatter:on
    }
}