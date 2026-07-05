package paint.shared.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization tests for the pure exponential-decay fitter exposed via
 * {@link CalculateTau#debugFit(double[], double[])}.
 *
 * <p>These tests pin the <em>current</em> behaviour of the fitter so that later
 * refactoring cannot silently change the numerical result. They call only the
 * public {@code debugFit} entry point and touch no production code.</p>
 *
 * <p>Two kinds of assertion are used:</p>
 * <ul>
 *   <li><b>Invariants</b> (null / too-few / mismatched input) are guaranteed by
 *       the fitter's guard clause and cannot spuriously fail.</li>
 *   <li>The <b>synthetic-fit</b> expectations reuse the reference values already
 *       recorded in the project's original {@code CalculateTauTest} harness. If
 *       the very first run reports a mismatch larger than the tolerance below,
 *       widen {@link #TAU_REL_TOL} / {@link #R2_ABS_TOL} rather than assuming a
 *       regression — small last-digit differences can occur across platforms.</li>
 * </ul>
 */
@DisplayName("CalculateTau.debugFit — exponential decay fitter")
class CalculateTauFitTest {

    /** Index into the {tauMs, rSquared} array returned by debugFit. */
    private static final int TAU = 0;
    private static final int R2 = 1;

    /** Relative tolerance on tau (~1 ms out of ~997 ms). */
    private static final double TAU_REL_TOL = 1e-3;
    /** Absolute tolerance on R². */
    private static final double R2_ABS_TOL = 1e-3;

    // Reference dataset and expected outputs taken from the project's original
    // CalculateTauTest harness ("Synthetic Exponential").
    private static final double[] SYNTH_X = {0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5};
    private static final double[] SYNTH_Y = {2000, 1200, 750, 500, 300, 200, 150, 100, 70, 50};
    private static final double EXPECTED_TAU = 997.0878843268896;
    private static final double EXPECTED_R2 = 0.9995441821230724;

    @Test
    @DisplayName("fits a clean synthetic exponential to the recorded reference values")
    void fitsSyntheticExponential() {
        double[] result = CalculateTau.debugFit(SYNTH_X, SYNTH_Y);

        assertEquals(EXPECTED_TAU, result[TAU], EXPECTED_TAU * TAU_REL_TOL,
                "tau (ms) drifted from the recorded reference value");
        assertEquals(EXPECTED_R2, result[R2], R2_ABS_TOL,
                "R² drifted from the recorded reference value");
    }

    @Test
    @DisplayName("a well-behaved decay yields a finite positive tau and high R²")
    void fitQualityIsSane() {
        // y = 1000 * exp(-x / 2) sampled cleanly -> a good exponential fit.
        int n = 12;
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = i * 0.5;
            y[i] = 1000.0 * Math.exp(-x[i] / 2.0);
        }

        double[] result = CalculateTau.debugFit(x, y);

        assertTrue(Double.isFinite(result[TAU]) && result[TAU] > 0.0,
                "expected a finite positive tau, got " + result[TAU]);
        assertTrue(result[R2] > 0.99 && result[R2] <= 1.0 + 1e-9,
                "expected R² in (0.99, 1.0], got " + result[R2]);
    }

    @Test
    @DisplayName("returns NaN when given fewer than two points")
    void tooFewPointsReturnsNaN() {
        double[] result = CalculateTau.debugFit(new double[]{1.0}, new double[]{5.0});
        assertTrue(Double.isNaN(result[TAU]), "tau should be NaN for a single point");
        assertTrue(Double.isNaN(result[R2]), "R² should be NaN for a single point");
    }

    @Test
    @DisplayName("returns NaN for null inputs")
    void nullInputReturnsNaN() {
        double[] result = CalculateTau.debugFit(null, null);
        assertTrue(Double.isNaN(result[TAU]), "tau should be NaN for null input");
        assertTrue(Double.isNaN(result[R2]), "R² should be NaN for null input");
    }

    @Test
    @DisplayName("returns NaN when x and y lengths differ")
    void mismatchedLengthsReturnNaN() {
        double[] result = CalculateTau.debugFit(new double[]{1, 2, 3}, new double[]{1, 2});
        assertTrue(Double.isNaN(result[TAU]), "tau should be NaN for mismatched lengths");
        assertTrue(Double.isNaN(result[R2]), "R² should be NaN for mismatched lengths");
    }
}
