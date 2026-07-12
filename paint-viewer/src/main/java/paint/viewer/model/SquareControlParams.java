/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.viewer.model;

/**
 * The {@code SquareControlParams} class encapsulates parameters used to control the
 * behavior and filtering of square grid configurations within the PAINT viewer.
 * <p>
 * It defines numerical thresholds for density ratio, variability, and R², and
 * specifies the neighborhood mode as a string parameter. Instances of this class
 * are immutable.
 * </p>
 */
public class SquareControlParams {
    public final double minRequiredDensityRatio;
    public final double maxAllowableVariability;
    public final double minRequiredRSquared;
    public final String neighbourMode;

    /**
     * Constructs a new immutable {@code SquareControlParams} instance with the
     * specified control parameter values.
     *
     * @param minRequiredDensityRatio minimum required density ratio threshold
     * @param maxAllowableVariability maximum allowable variability threshold
     * @param minRequiredRSquared minimum R² value (0.0–1.0) representing statistical fit
     * @param neighbourMode the neighborhood interaction mode
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
     * Returns a formatted string representation of this parameter set,
     * including density ratio, variability, R², and neighbour mode values.
     *
     * @return string representation of this {@code SquareControlParams} instance
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