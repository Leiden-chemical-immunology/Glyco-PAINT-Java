package viewer;

public class SquareControlParams {
    public final int densityRatio;
    public final int variability;
    public final double rSquared;
    public final int minDuration;
    public final int maxDuration;
    public final String neighbourMode;

    public SquareControlParams(int densityRatio, int variability, double rSquared,
                               int minDuration, int maxDuration, String neighbourMode) {
        this.densityRatio = densityRatio;
        this.variability = variability;
        this.rSquared = rSquared;
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
        this.neighbourMode = neighbourMode;
    }

    @Override
    public String toString() {
        return "density=" + densityRatio +
                ", variability=" + variability +
                ", rSquared=" + rSquared +
                ", minDuration=" + minDuration +
                ", maxDuration=" + maxDuration +
                ", neighbour=" + neighbourMode;
    }
}