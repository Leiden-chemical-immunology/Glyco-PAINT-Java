/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.generatesquares.calc;

import paint.shared.objects.Square;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides utility methods for analyzing square-based track data in the Paint experiment
 * workflow. Supports background estimation, density calculations, and extraction of tracks
 * from selected squares.
 * <p>
 * Includes static methods for:
 * </p>
 * <ul>
 *   <li>Computing density values</li>
 *   <li>Estimating background densities using iterative filtering</li>
 *   <li>Extracting tracks from squares flagged as selected</li>
 *   <li>Counting selected squares</li>
 * </ul>
 * <p>
 * Also defines an inner class for returning background estimation results.
 * </p>
 * <ul>
 *   <li>calculateDensity — compute density from counts, area, time, concentration</li>
 *   <li>calculateBackgroundDensity — iterative background estimation</li>
 *   <li>getTracksFromSelectedSquares — extract tracks from selected squares</li>
 *   <li>getNumberOfSelectedSquares — count selected squares in a recording</li>
 * </ul>
 */
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
     *   • It stabilizes (relative change &lt; EPSILON), or
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

        private final double       numberOfTracksInBackgroundSquare;
        private final List<Square> backgroundSquares;
        private final int          numberOfBackgroundSquares;

        /**
         * Constructs an immutable result object.
         *
         * @param numberOfTracksInBackgroundSquare    estimated background track mean
         * @param backgroundSquares squares identified as belonging to the background
         */
        public BackgroundEstimationResult(double numberOfTracksInBackgroundSquare, List<Square> backgroundSquares) {
            this.numberOfTracksInBackgroundSquare = numberOfTracksInBackgroundSquare;
            this.backgroundSquares                = backgroundSquares;
            this.numberOfBackgroundSquares        = backgroundSquares.size();
        }

        /** @return mean background track count */
        public double getNumberOfTracksInBackgroundSquare() {
            return numberOfTracksInBackgroundSquare;
        }

        /** @return list of background squares */
        public List<Square> getBackgroundSquares() {
            return backgroundSquares;
        }

        /** @return the number of background squares */
        public int getNumberOfBackgroundSquares() {
            return numberOfBackgroundSquares;
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
            int          nrOfAverageCountSquares
    ) {

        List<Integer> trackCounts =
                squaresOfRecording.stream()
                                  .map(Square::getNumberOfTracks)
                                  .sorted(Comparator.reverseOrder())
                                  .collect(Collectors.toList());

        double total = 0.0;
        int    n     = 0;

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