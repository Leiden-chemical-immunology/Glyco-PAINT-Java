package generatesquares.calc;

import org.apache.commons.math3.fitting.leastsquares.*;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.util.FastMath;
import paint.shared.objects.Track;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.Double.NaN;

/**
 * ============================================================================
 *  CalculateTau.java
 *  Part of the "Generate Squares" module.
 *
 *  <p><b>Purpose:</b><br>
 *  Performs exponential decay fitting of track duration distributions to
 *  determine characteristic time constants (Tau) and fit quality (R²).
 *  </p>
 *
 *  <p>Fits the model: y = m * exp(-t * x) + b</p>
 *
 *  <p><b>Author:</b> Hans Bakker<br>
 *  <b>Version:</b> 1.0</p>
 * ============================================================================
 */
public class CalculateTau {

    private CalculateTau() {
    }

    /**
     * Calculates tau by fitting a mono-exponential decay to a frequency
     * distribution of track durations.
     *
     * @param tracks              list of input tracks
     * @param minTracksForTau     minimum number of tracks required to attempt a fit
     * @param minRequiredRSquared minimum acceptable R² value
     * @return {@code CalculateTauResult} containing the fit outcome
     */
    public static CalculateTauResult calcTau(List<Track> tracks,
                                             int minTracksForTau,
                                             double minRequiredRSquared) {

        if (tracks == null || tracks.size() < minTracksForTau) {
            return new CalculateTauResult(NaN, NaN, CalculateTauResult.Status.TAU_INSUFFICIENT_POINTS);
        }

        // 1) Extract durations
        final int n = tracks.size();
        double[] durations = new double[n];
        for (int i = 0; i < n; i++) {
            durations[i] = tracks.get(i).getTrackDuration();
        }

        // 2) Build frequency distribution (sorted by duration)
        Map<Double, Integer> freq = createFrequencyDistribution(durations);

        // Need at least 2 distinct x-values to fit
        if (freq.size() < 2) {
            return new CalculateTauResult(0.0, 0.0, CalculateTauResult.Status.TAU_NO_FIT);
        }

        // 3) Convert to arrays for fitting
        double[] x = new double[freq.size()];
        double[] y = new double[freq.size()];
        int k = 0;
        for (Map.Entry<Double, Integer> e : freq.entrySet()) {
            x[k] = e.getKey();
            y[k] = e.getValue();
            k++;
        }

        // 4) Fit and evaluate quality
        CalculateTauExpDecayFitter.FitResult fitResult = CalculateTauExpDecayFitter.fit(x, y);

        // 5) Reject non-finite results
        if (!Double.isFinite(fitResult.rSquared) || !Double.isFinite(fitResult.tauMs)) {
            return new CalculateTauResult(fitResult.tauMs, fitResult.rSquared, CalculateTauResult.Status.TAU_NO_FIT);
        }

        // 6) Apply threshold
        if (fitResult.rSquared < minRequiredRSquared) {
            return new CalculateTauResult(fitResult.tauMs, fitResult.rSquared, CalculateTauResult.Status.TAU_RSQUARED_TOO_LOW);
        }

        return new CalculateTauResult(fitResult.tauMs, fitResult.rSquared, CalculateTauResult.Status.TAU_SUCCESS);
    }

    // ------------------------------------------------------------------------
    // Tau result
    // ------------------------------------------------------------------------
    public static class CalculateTauResult {

        private final double tau;
        private final double rsquared;
        private final Status status;

        public CalculateTauResult(double tau, double rsquared, Status status) {

            // @formatter:off
            this.tau      = tau;
            this.rsquared = rsquared;
            this.status   = status;
            // @formatter:on
        }

        public enum Status {
            TAU_SUCCESS,
            TAU_INSUFFICIENT_POINTS,
            TAU_RSQUARED_TOO_LOW,
            TAU_NO_FIT
        }

        public double getTau() {
            return tau;
        }

        public double getRSquared() {
            return rsquared;
        }

        public Status getStatus() {
            return status;
        }
    }

    /**
     * Build frequency distribution: key = duration, value = count.
     */
    private static Map<Double, Integer> createFrequencyDistribution(double[] trackDurations) {
        Map<Double, Integer> frequencyDistribution = new TreeMap<>();
        if (trackDurations == null) {
            return frequencyDistribution;
        }

        for (double duration : trackDurations) {
            Integer prev = frequencyDistribution.get(duration);
            frequencyDistribution.put(duration, (prev == null ? 1 : prev + 1));
        }
        return frequencyDistribution;
    }

    // ------------------------------------------------------------------------
    // Inner static class for exponential fitting
    // ------------------------------------------------------------------------
    private static class CalculateTauExpDecayFitter {

        private CalculateTauExpDecayFitter() {}

        /** Immutable fit result. */
        private static final class FitResult {
            final double tauMs;
            final double rSquared;

            FitResult(double tauMs, double rSquared) {
                this.tauMs = tauMs;
                this.rSquared = rSquared;
            }
        }

        /** Performs least-squares exponential fitting. */
        private static FitResult fit(double[] x, double[] y) {
            if (x == null || y == null || x.length != y.length || x.length < 2) {
                return new FitResult(NaN, NaN);
            }

            final MultivariateJacobianFunction model = p -> {
                double m = p.getEntry(0);
                double t = p.getEntry(1);
                double b = p.getEntry(2);

                double[] values = new double[x.length];
                double[][] jac = new double[x.length][3];

                for (int i = 0; i < x.length; i++) {
                    double e = FastMath.exp(-t * x[i]);
                    values[i] = m * e + b;
                    jac[i][0] = e;              // d/dm
                    jac[i][1] = -m * x[i] * e;  // d/dt
                    jac[i][2] = 1.0;            // d/db
                }
                return new org.apache.commons.math3.util.Pair<>(
                        new ArrayRealVector(values, false),
                        new Array2DRowRealMatrix(jac, false)
                );
            };

            double[] p0 = initialGuess(x, y);
            double[] w = new double[y.length];
            Arrays.fill(w, 1.0);

            LeastSquaresProblem problem = new LeastSquaresBuilder()
                    .start(p0)
                    .model(model)
                    .target(y)
                    .weight(new DiagonalMatrix(w))
                    .maxEvaluations(10_000)
                    .maxIterations(1_000)
                    .build();

            try {
                LeastSquaresOptimizer.Optimum opt =
                        new LevenbergMarquardtOptimizer().optimize(problem);

                double[] p = opt.getPoint().toArray();
                double m = p[0], t = p[1], b = p[2];
                double tauMs = (t > 0.0) ? (1000.0 / t) : NaN;
                double r2 = computeRSquared(x, y, m, t, b);

                return new FitResult(tauMs, r2);
            } catch (Throwable ignored) {
                return new FitResult(NaN, NaN);
            }
        }

        private static double[] initialGuess(double[] x, double[] y) {
            double minY = Arrays.stream(y).min().orElse(0);
            double maxY = Arrays.stream(y).max().orElse(1);
            double maxX = Arrays.stream(x).max().orElse(1);

            double b = Math.max(0.0, minY);
            double m = Math.max(1e-6, maxY - b);

            double eps = Math.max(1e-6, 0.01 * m);
            double sumX = 0, sumXX = 0, sumY = 0, sumXY = 0;
            int n = 0;

            for (int i = 0; i < x.length; i++) {
                double yiAdj = y[i] - b;
                if (yiAdj > eps) {
                    double lx = x[i];
                    double ly = FastMath.log(yiAdj);
                    sumX += lx;
                    sumXX += lx * lx;
                    sumY += ly;
                    sumXY += lx * ly;
                    n++;
                }
            }

            double t;
            if (n >= 2) {
                double denominator = (n * sumXX - sumX * sumX);
                double slope = (denominator == 0.0)
                        ? -1.0
                        : (n * sumXY - sumX * sumY) / denominator;
                t = Math.max(1e-9, -slope);
            } else {
                t = 1.0 / Math.max(1e-3, maxX);
            }

            // Clamp to reasonable ranges
            m = clamp(m, 1e-9, 1e9);
            t = clamp(t, 1e-9, 1e3);
            b = clamp(b, 0.0, Math.max(1.0, maxY));

            return new double[]{m, t, b};
        }

        private static double computeRSquared(double[] x, double[] y, double m, double t, double b) {
            double meanY = Arrays.stream(y).average().orElse(0);
            double ssRes = 0.0, ssTot = 0.0;
            for (int i = 0; i < y.length; i++) {
                double predicted = m * FastMath.exp(-t * x[i]) + b;
                ssRes += Math.pow(y[i] - predicted, 2);
                ssTot += Math.pow(y[i] - meanY, 2);
            }
            return (ssTot == 0.0) ? NaN : 1.0 - (ssRes / ssTot);
        }

        private static double clamp(double v, double lo, double hi) {
            return Math.max(lo, Math.min(hi, v));
        }
    }

    public static double[] debugFit(double[] x, double[] y) {
        CalculateTauExpDecayFitter.FitResult result = CalculateTauExpDecayFitter.fit(x, y);
        return new double[]{ result.tauMs, result.rSquared };
    }
}