package paint.viewer.shared;

/**
 * The SquareControlParams class encapsulates parameters used to control the behavior
 * and properties of a square configuration system. It provides a structure for
 * managing density ratio, variability, correlation coefficient, and neighborhood mode.
 *
 * Each field represents a distinct parameter:
 * - densityRatio: Represents the density ratio value as a double.
 * - variability: Represents the variability value as a double.
 * - rSquared: Represents the R-squared correlation coefficient, constrained to the range 0.0–1.0.
 * - neighbourMode: Specifies the mode of neighborhood interaction as a String.
 *
 * Instances of this class are immutable once created.
 */
public class SquareControlParams {
    public final double minRequiredDensityRatio;   // now double
    public final double maxAllowableVariability;    // now double
    public final double minRequiredRSquared;       // double 0.0–1.0
    public final String neighbourMode;

    /**
     * Constructs a new instance of {@code SquareControlParams} with the specified parameters.
     *
     * @param minRequiredDensityRatio the density ratio value representing the density configuration of the square system
     * @param maxAllowableVariability the variability value indicating the degree of variation in the system
     * @param minRequiredRSquared the R-squared correlation coefficient, constrained between 0.0 and 1.0, representing the statistical measure of fit
     * @param neighbourMode the mode of neighborhood interaction specified as a string
     */
    public SquareControlParams(double minRequiredDensityRatio,
                               double maxAllowableVariability,
                               double minRequiredRSquared,
                               String neighbourMode) {
        this.minRequiredDensityRatio = minRequiredDensityRatio;
        this.maxAllowableVariability = maxAllowableVariability;
        this.minRequiredRSquared     = minRequiredRSquared;
        this.neighbourMode           = neighbourMode;
    }

    /**
     * Returns a string representation of the SquareControlParams object. The returned string
     * includes the values of the density ratio, variability, R-squared, and neighbor mode.
     *
     * @return a string representation of the SquareControlParams instance, including all its parameters
     */
    @Override
    public String toString() {
        return "SquareControlParams{" +
                "densityRatio="     + minRequiredDensityRatio +
                ", variability="    + maxAllowableVariability +
                ", rSquared="       + minRequiredRSquared +
                ", neighbourMode='" + neighbourMode + '\'' +
                '}';
    }
}