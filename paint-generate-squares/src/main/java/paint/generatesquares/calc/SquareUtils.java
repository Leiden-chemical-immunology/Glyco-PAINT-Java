/*=============================================================================
 *  Class:        SquareUtils.java
 *  Package:      paint.generatesquares.calc
 *
 *  PURPOSE:
 *    Provides utility methods for analyzing square-based track data in the
 *    Paint experiment workflow. Supports background estimation, density
 *    calculations, and extraction of tracks from selected squares.
 *
 *  DESCRIPTION:
 *    Includes static methods for:
 *      • Computing density values
 *      • Estimating background densities using iterative filtering
 *      • Extracting tracks from squares flagged as selected
 *      • Counting selected squares
 *
 *    Also defines an inner class for returning background estimation results.
 *
 *  RESPONSIBILITIES:
 *    • calculateDensity — compute density from counts, area, time, concentration
 *    • calculateBackgroundDensity — iterative background estimation
 *    • getTracksFromSelectedSquares — extract tracks from selected squares
 *    • getNumberOfSelectedSquares — count selected squares in a recording
 *
 *  USAGE EXAMPLE:
 *    List<Square> squares = recording.getSquaresOfRecording();
 *    SquareUtils.BackgroundEstimationResult result =
 *        SquareUtils.calculateBackgroundDensity(squares);
 *
 *  DEPENDENCIES:
 *    – paint.shared.objects.{Recording, Square, Track}
 *    – java.util.{List, ArrayList, Collections, Comparator}
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-27
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *============================================================================*/

package paint.generatesquares.calc;

import paint.shared.objects.Square;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SquareUtils {

    /**
     * Private constructor to prevent instantiation.
     */
    private SquareUtils() {
    }

    /**
     * Estimates the background density from a list of squares. This is done
     * by iteratively excluding squares whose track counts exceed a dynamically
     * computed threshold:
     * <p>
     *      threshold = mean + 2 * standardDeviation
     * <p>
     * The mean is recalculated each iteration until:
     *   • It stabilizes (relative change < EPSILON), or
     *   • A maximum number of iterations is reached.
     *
     * @param squares List of squares containing track counts; must not be null or empty.
     * @return BackgroundEstimationResult containing the estimated mean and the squares
     *         considered part of the background.
     */
    public static BackgroundEstimationResult calculateBackgroundDensity(List<Square> squares) {
        if (squares == null || squares.isEmpty()) {
            return new BackgroundEstimationResult(Double.NaN, Collections.emptyList());
        }

        double mean = squares.stream()
                             .mapToDouble(Square::getNumberOfTracks)
                             .average()
                             .orElse(Double.NaN);

        if (Double.isNaN(mean) || mean == 0) {
            return new BackgroundEstimationResult(mean, Collections.emptyList());
        }

        final double EPSILON  = 0.01;
        final int    MAX_ITER = 10;

        double prevMean;
        List<Square> current = new ArrayList<>(squares);

        for (int iter = 0; iter < MAX_ITER; iter++) {
            prevMean = mean;
            final double meanForLambda = mean;

            double std = Math.sqrt(
                    current.stream()
                           .mapToDouble(square -> Math.pow(square.getNumberOfTracks() - meanForLambda, 2))
                           .average()
                           .orElse(0)
            );

            final double threshold = meanForLambda + 2 * std;

            List<Square> filtered = new ArrayList<>();
            for (Square square : current) {
                if (square.getNumberOfTracks() <= threshold) {
                    filtered.add(square);
                }
            }

            if (filtered.isEmpty()) {
                break;
            }

            mean = filtered.stream()
                           .mapToDouble(Square::getNumberOfTracks)
                           .average()
                           .orElse(mean);

            current = filtered;

            // Stop when convergence is reached
            if (Math.abs(mean - prevMean) / prevMean < EPSILON) {
                break;
            }
        }

        return new BackgroundEstimationResult(mean, current);
    }

    /**
     * Holds the results of a background estimation: the mean background track count
     * and the list of squares that were categorized as background.
     */
    public static class BackgroundEstimationResult {

        private final double backgroundMean;
        private final List<Square> backgroundSquares;

        /**
         * Constructs an immutable result object.
         *
         * @param backgroundMean    estimated background track mean
         * @param backgroundSquares squares identified as belonging to the background
         */
        public BackgroundEstimationResult(double backgroundMean, List<Square> backgroundSquares) {
            this.backgroundMean    = backgroundMean;
            this.backgroundSquares = backgroundSquares;
        }

        /** @return mean background track count */
        public double getBackgroundMean() {
            return backgroundMean;
        }

        /** @return list of background squares */
        public List<Square> getBackgroundSquares() {
            return backgroundSquares;
        }
    }

    /**
     * Computes the average number of tracks among the smallest non-zero track counts
     * in the recording. The number of squares to average over is configurable.
     *
     * @param squaresOfRecording      list of squares (must not be null)
     * @param nrOfAverageCountSquares number of smallest non-zero values to average
     * @return average of selected small non-zero track counts, or 0.0 if none found
     */
    public static double calcAverageTrackCountInBackgroundSquares(
            List<Square> squaresOfRecording,
            int nrOfAverageCountSquares
    ) {

        List<Integer> trackCounts =
                squaresOfRecording.stream()
                                  .map(Square::getNumberOfTracks)
                                  .sorted(Comparator.reverseOrder())
                                  .collect(Collectors.toList());

        double total = 0.0;
        int n = 0;

        // Walk backwards (smallest values first)
        for (int i = trackCounts.size() - 1; i >= 0; i--) {
            int value = trackCounts.get(i);
            if (value > 0) {
                total += value;
                n++;
                if (n >= nrOfAverageCountSquares) {
                    break;
                }
            }
        }

        return n == 0 ? 0.0 : total / n;
    }
}