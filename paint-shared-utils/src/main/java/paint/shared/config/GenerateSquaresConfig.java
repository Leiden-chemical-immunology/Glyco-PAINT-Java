package paint.shared.config;

/**
 * Holds the data retrieved from the GenerateSquares section of the Paint configuration file.
 */
public class GenerateSquaresConfig {

    private final int nrSquaresInRow;
    private final int nrSquaresInColumn;
    private final int minTracksToCalculateTau;
    private final double minRequiredRSquared;
    private final double minRequiredDensityRatio;
    private final double maxAllowableVariability;
    private final String neighbourMode;

    /*
     * Full constructor with all values specified
     */
    private GenerateSquaresConfig(PaintConfig paintConfig) {
        this.nrSquaresInRow = paintConfig.getInt("Generate Squares", "Nr of Squares in Row", 5);
        this.nrSquaresInColumn = paintConfig.getInt("Generate Squares", "Nr of Squares in Column", 5);
        this.minTracksToCalculateTau = paintConfig.getInt("Generate Squares", "Min Tracks to Calculate Tau", 20);
        this.minRequiredRSquared = paintConfig.getDouble("Generate Squares", "Min Required R Squared", 0.1);
        this.minRequiredDensityRatio = paintConfig.getDouble("Generate Squares", "Min Required Density Ratio", 0.1);
        this.maxAllowableVariability = paintConfig.getDouble("Generate Squares", "Max Allowable Variability", 10.0);
        this.neighbourMode = paintConfig.getString("Generate Squares", "Neighbour Mode", "Free");
    }

    public static GenerateSquaresConfig from(PaintConfig paintConfig) {
        return new GenerateSquaresConfig(paintConfig);
    }

    // Getters are not really needed as attributes are public
    // Setter methods are not needed either, as the attributes are final

    public int getNrSquaresInRow() { return nrSquaresInRow; }
    public int getNrSquaresInColumn() { return nrSquaresInColumn; }
    public int getMinTracksToCalculateTau() { return minTracksToCalculateTau; }
    public double getMinRequiredRSquared() { return minRequiredRSquared; }
    public double getMinRequiredDensityRatio() { return minRequiredDensityRatio; }
    public double getMaxAllowableVariability() { return maxAllowableVariability; }
    public String getNeighbourMode() { return neighbourMode; }

    @Override
    public String toString() {
        return "GenerateSquaresConfig{" +
                "nrSquaresInRow=" + nrSquaresInRow +
                ", nrSquaresInColumn=" + nrSquaresInColumn +
                ", minTracksToCalculateTau=" + minTracksToCalculateTau +
                ", minRequiredRSquared=" + minRequiredRSquared +
                ", minRequiredDensityRatio=" + minRequiredDensityRatio +
                ", maxAllowableVariability=" + maxAllowableVariability +
                ", neighbourMode='" + neighbourMode + '\'' +
                '}';
    }
}