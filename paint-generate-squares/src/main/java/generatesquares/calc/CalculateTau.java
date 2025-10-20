package generatesquares.calc;

import org.apache.commons.math3.fitting.leastsquares.*;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.util.FastMath;
import paint.shared.objects.Track;
import javax.swing.*;
import java.awt.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.Double.NaN;

public class CalculateTau {

    private CalculateTau() {
    }

    /**
     * Calculates tau by fitting a mono-exponential decay to a frequency
     * distribution of track durations. Returns a {@link CalculateTauResult}
     * with status and fit information.
     *
     * <p><strong>Flow:</strong></p>
     * <pre>
     * tracks → durations[] → TreeMap&lt;Double, Integer&gt; freq → x[], y[] → fit() → TauCalcResult
     * </pre>
     *
     * @param tracks              the list of input tracks
     * @param minTracksForTau     the minimum number of tracks required to attempt a fit
     * @param minRequiredRSquared the minimum acceptable R² value for the fit to be considered valid
     * @return a {@code CalculateTauResult} containing the fit outcome, parameters, and status
     * @throws IllegalArgumentException if {@code tracks} is {@code null}
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
}


/**
 * Fits y = m * exp(-t * x) + b to (x, y) and returns tau (ms) and R^2.
 * Unified single-class version: no AbstractCurveFitter subclassing needed.
 */
class CalculateTauExpDecayFitter {

    private CalculateTauExpDecayFitter() {
    }

    /**
     * Immutable result.
     */
    public static final class FitResult {
        public final double tauMs;
        public final double rSquared;

        public FitResult(double tauMs, double rSquared) {
            this.tauMs = tauMs;
            this.rSquared = rSquared;
        }
    }

    /**
     * Main entrypoint.
     *
     * @param x domain values
     * @param y range values (e.g., counts)
     */
    public static FitResult fit(double[] x, double[] y) {
        if (x == null || y == null || x.length != y.length || x.length < 2) {
            return new FitResult(NaN, NaN);
        }

        // Build model + Jacobian: y = m * exp(-t * x) + b, params p = [m, t, b]
        final MultivariateJacobianFunction model = new MultivariateJacobianFunction() {
            @Override
            public org.apache.commons.math3.util.Pair<RealVector, RealMatrix> value(final RealVector p) {
                final double m = p.getEntry(0);
                final double t = p.getEntry(1);
                final double b = p.getEntry(2);

                final double[] values = new double[x.length];
                final double[][] jac = new double[x.length][3];

                for (int i = 0; i < x.length; i++) {
                    double e = FastMath.exp(-t * x[i]);
                    values[i] = m * e + b;
                    jac[i][0] = e;              // d/dm
                    jac[i][1] = -m * x[i] * e;  // d/dt
                    jac[i][2] = 1.0;            // d/db
                }

                return new org.apache.commons.math3.util.Pair<RealVector, RealMatrix>(
                        new ArrayRealVector(values, false),
                        new Array2DRowRealMatrix(jac, false)
                );
            }
        };

        // Initial guess [m, t, b]
        final double[] p0 = initialGuess(x, y);

        // Weights: identity (you can inject real weights here if desired)
        final double[] w = new double[y.length];
        Arrays.fill(w, 1.0);
        final RealMatrix weight = new DiagonalMatrix(w);

        // Build and solve least-squares
        LeastSquaresProblem problem = new LeastSquaresBuilder()
                .start(p0)
                .model(model)
                .target(y)           // observed y
                .weight(weight)      // diagonal weights
                .maxEvaluations(10_000)
                .maxIterations(1_000)
                .build();

        LeastSquaresOptimizer optimizer = new LevenbergMarquardtOptimizer();
        LeastSquaresOptimizer.Optimum optimum;
        try {
            optimum = optimizer.optimize(problem);
        } catch (Throwable t) {
            return new FitResult(NaN, NaN);
        }

        final double[] p = optimum.getPoint().toArray();
        final double m = p[0], t = p[1], b = p[2];

        // tau (ms) = 1000 / t  (guard t>0)
        final double tauMs = (t > 0.0) ? (1000.0 / t) : NaN;

        // R^2 on original data
        final double r2 = computeRSquared(x, y, m, t, b);

        return new FitResult(tauMs, r2);
    }

    /**
     * Heuristic initial guess for [m, t, b].
     */
    private static double[] initialGuess(double[] x, double[] y) {
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < y.length; i++) {
            if (y[i] < minY) {
                minY = y[i];
            }
            if (y[i] > maxY) {
                maxY = y[i];
            }
            if (x[i] > maxX) {
                maxX = x[i];
            }
        }

        // Baseline guess b = min(y)
        double b = Math.max(0.0, minY);

        // Amplitude guess m = max(y) - b
        double m = Math.max(1e-6, maxY - b);

        // Rough t from a linearized tail: ln(y - b) ≈ ln(m) - t x
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
            double slope = (denominator == 0.0) ? -1.0 : (n * sumXY - sumX * sumY) / denominator;
            t = Math.max(1e-9, -slope); // slope ≈ -t
        } else {
            t = 1.0 / Math.max(1e-3, maxX); // fallback
        }

        // Clamp to reasonable ranges
        m = clamp(m, 1e-9, 1e9);
        t = clamp(t, 1e-9, 1e3);
        b = clamp(b, 0.0, Math.max(1.0, maxY));

        return new double[]{m, t, b};
    }

    private static double computeRSquared(double[] x, double[] y, double m, double t, double b) {
        double meanY = 0.0;
        for (double v : y) {
            meanY += v;
        }
        meanY /= y.length;

        double ssRes = 0.0, ssTot = 0.0;
        for (int i = 0; i < y.length; i++) {
            double predicted = m * FastMath.exp(-t * x[i]) + b;
            double diff = y[i] - predicted;
            ssRes += diff * diff;
            double dy = y[i] - meanY;
            ssTot += dy * dy;
        }
        return (ssTot == 0.0) ? NaN : 1.0 - (ssRes / ssTot);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }


    /**
     * Simple wrapper: plots the raw data and, if valid, overlays the fitted exponential curve.
     */
    public static void plotFitting(double[] x, double[] y, FitResult result) {
        double[] fitX = null;
        double[] fitY = null;

        boolean validFit = (result != null &&
                Double.isFinite(result.tauMs) &&
                Double.isFinite(result.rSquared) &&
                result.rSquared > 0.0);

        if (validFit) {
            double t = 1000.0 / result.tauMs;
            double m = Arrays.stream(y).max().orElse(1);
            double b = Arrays.stream(y).min().orElse(0);

            int n = 100;
            fitX = new double[n];
            fitY = new double[n];
            double maxX = Arrays.stream(x).max().orElse(1);

            for (int i = 0; i < n; i++) {
                fitX[i] = i * maxX / (n - 1);
                fitY[i] = m * Math.exp(-t * fitX[i]) + b;
            }

            System.out.printf("✅ Fit succeeded: τ = %.3f ms, R² = %.6f%n", result.tauMs, result.rSquared);
        } else {
            System.out.println("⚠️ Fit failed — showing data only.");
            if (result != null) {
                System.out.printf("Reason: tau=%s, R²=%s%n", result.tauMs, result.rSquared);
            }
        }

        plotFit(x, y, fitX, fitY);
    }

    /**
     * Plots raw data and optional fitted curve.
     */
    private static void plotFit(double[] x, double[] y, double[] fitX, double[] fitY) {
        JFrame frame = new JFrame("Tau Fit Visualization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int marginLeft = 70, marginRight = 40, marginTop = 40, marginBottom = 60;

                double minX = Arrays.stream(x).min().orElse(0);
                double maxX = Arrays.stream(x).max().orElse(1);
                double minY = Arrays.stream(y).min().orElse(0);
                double maxY = Arrays.stream(y).max().orElse(1);

                double xScale = (w - marginLeft - marginRight) / (maxX - minX);
                double yScale = (h - marginTop - marginBottom) / (maxY - minY);

                int x0 = marginLeft;
                int y0 = h - marginBottom;
                g2.setColor(Color.GRAY);
                g2.drawLine(x0, y0, w - marginRight, y0);
                g2.drawLine(x0, y0, x0, marginTop);

                // Raw data points
                g2.setColor(new Color(30, 100, 200));
                for (int i = 0; i < x.length; i++) {
                    int px = (int) (marginLeft + (x[i] - minX) * xScale);
                    int py = (int) (y0 - (y[i] - minY) * yScale);
                    g2.fillOval(px - 4, py - 4, 8, 8);
                }

                // Fitted curve (if any)
                if (fitX != null && fitY != null) {
                    g2.setColor(Color.RED);
                    for (int i = 1; i < fitX.length; i++) {
                        int x1 = (int) (marginLeft + (fitX[i - 1] - minX) * xScale);
                        int y1 = (int) (y0 - (fitY[i - 1] - minY) * yScale);
                        int x2 = (int) (marginLeft + (fitX[i] - minX) * xScale);
                        int y2 = (int) (y0 - (fitY[i] - minY) * yScale);
                        g2.drawLine(x1, y1, x2, y2);
                    }
                } else {
                    // Optional message for failed fit
                    g2.setColor(Color.RED);
                    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
                    g2.drawString("Fit failed — showing data only", w / 2 - 130, marginTop + 20);
                }

                // Labels
                g2.setColor(Color.BLACK);
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
                g2.drawString("Duration", w / 2 - 30, h - 20);
                g2.rotate(-Math.PI / 2);
                g2.drawString("Frequency", -h / 2 - 30, 25);
                g2.rotate(Math.PI / 2);
            }
        };

        frame.add(panel);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        // Python-like test data
        double[] durations = {0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5};
        double[] freq = {2000, 1200, 750, 500, 300, 200, 150, 100, 70, 50};

        // Fit exponential decay directly
        CalculateTauExpDecayFitter.FitResult result = CalculateTauExpDecayFitter.fit(durations, freq);

        System.out.printf("Tau (ms): %.3f%n", result.tauMs);
        System.out.printf("R²: %.6f%n", result.rSquared);
        System.out.printf("Comparison with Python: Tau diff = %f and R² diff = %f%n",
                          result.tauMs - 997.0878843268896,
                          result.rSquared - 0.9995441821230724);
        plotFitting(durations, freq, result);

        durations = new double[] {0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.65, 0.8, 1.1, 1.951, 2.251, 3.101, 5.702};
        freq = new double[] {2.0, 2.0, 1.0, 4.0, 2.0, 1.0, 4.0, 1.0, 1.0, 2.0, 3.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
        result = CalculateTauExpDecayFitter.fit(durations, freq);

        plotFitting(durations, freq, result);
    }
}