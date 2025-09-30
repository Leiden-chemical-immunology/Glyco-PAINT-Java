package viewer;

public class SquareControlParams {
    public final double densityRatio;   // now double
    public final double variability;    // now double
    public final double rSquared;       // double 0.0–1.0
    public final int minDuration;
    public final int maxDuration;
    public final String neighbourMode;

    public SquareControlParams(double densityRatio,
                               double variability,
                               double rSquared,
                               int minDuration,
                               int maxDuration,
                               String neighbourMode) {
        this.densityRatio = densityRatio;
        this.variability = variability;
        this.rSquared = rSquared;
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
        this.neighbourMode = neighbourMode;
    }

    @Override
    public String toString() {
        return "SquareControlParams{" +
                "densityRatio=" + densityRatio +
                ", variability=" + variability +
                ", rSquared=" + rSquared +
                ", minDuration=" + minDuration +
                ", maxDuration=" + maxDuration +
                ", neighbourMode='" + neighbourMode + '\'' +
                '}';
    }
}