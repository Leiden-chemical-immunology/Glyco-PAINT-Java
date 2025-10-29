/******************************************************************************
 *  Class:        SquareUtils.java
 *  Package:      paint.generatesquares.calc
 *
 *  PURPOSE:
 *    Provides utility methods for analysis of square-based track data in the Paint
 *    experiment workflow, including background estimation, density calculations,
 *    and extraction of tracks from selected squares.
 *
 *  DESCRIPTION:
 *    Contains static methods that compute densities, filter and categorize squares
 *    based on track counts, extract tracks from selected squares within a recording,
 *    and count selected squares. Also includes an inner class to hold results of
 *    background estimation.
 *
 *  RESPONSIBILITIES:
 *    • calculateDensity: compute density given tracks, area, time, concentration
 *    • calculateBackgroundDensity: iterative filtering to estimate background mean
 *    • getTracksFromSelectedSquares: extract tracks from squares flagged as selected
 *    • getNumberOfSelectedSquares: count squares marked as selected in a recording
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
 ******************************************************************************/

package paint.generatesquares.calc;

import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.objects.Track;

import java.util.*;
import java.util.stream.Collectors;

public class SquareUtils {

    /** Private constructor to prevent instantiation. */
    private SquareUtils() {
    }




    /**
     * Estimates the background density of track counts from a list of squares.
     * The method iteratively filters squares with track counts exceeding a dynamically
     * calculated threshold (mean + 2 * standard deviation), recalculates the mean,
     * and repeats until the mean stabilizes or a maximum number of iterations is reached.
     *
     * @param squares List of Square objects containing track count information.
     *                Must not be null or empty.
     * @return A BackgroundEstimationResult object containing the estimated mean track count
     *         for the background and the list of squares identified as background.
     */
    public static BackgroundEstimationResult calculateBackgroundDensity(List<Square> squares) {
        if (squares == null || squares.isEmpty()) {
            return new BackgroundEstimationResult(Double.NaN, Collections.emptyList());
        }

        double mean = squares.stream()
                .mapToDouble(Square::getNumberOfTracks)
                .average().orElse(Double.NaN);

        if (Double.isNaN(mean) || mean == 0) {
            return new BackgroundEstimationResult(mean, Collections.emptyList());
        }

        final double EPSILON = 0.01;
        final int MAX_ITER   = 10;
        double prevMean;

        List<Square> current = new ArrayList<>(squares);

        for (int iter = 0; iter < MAX_ITER; iter++) {
            prevMean = mean;
            final double meanForLambda = mean;

            double std = Math.sqrt(current.stream()
                                           .mapToDouble(sq -> Math.pow(sq.getNumberOfTracks() - meanForLambda, 2))
                                           .average().orElse(0));

            final double threshold = meanForLambda + 2 * std;

            List<Square> filtered = new ArrayList<>();
            for (Square sq : current) {
                if (sq.getNumberOfTracks() <= threshold) {
                    filtered.add(sq);
                }
            }

            if (filtered.isEmpty()) {
                break;
            }

            mean = filtered.stream()
                    .mapToDouble(Square::getNumberOfTracks)
                    .average().orElse(mean);

            current = filtered;

            // Stop if the mean stabilizes
            if (Math.abs(mean - prevMean) / prevMean < EPSILON) {
                break;
            }
        }

        return new BackgroundEstimationResult(mean, current);
    }


    /**
     * Represents the result of a background estimation process for track counts.
     * Contains the mean track count of the estimated background and the list of
     * squares identified as background.
     */
    public static class BackgroundEstimationResult {
        private final double backgroundMean;
        private final List<Square> backgroundSquares;

        /**
         * Constructs a new BackgroundEstimationResult with the provided mean background value
         * and the list of squares classified as background.
         *
         * @param backgroundMean the mean value of tracks estimated as background
         * @param backgroundSquares the list of squares identified as background
         */
        public BackgroundEstimationResult(double backgroundMean, List<Square> backgroundSquares) {
            this.backgroundMean    = backgroundMean;
            this.backgroundSquares = backgroundSquares;
        }

        /** @return Estimated mean background track count. */
        public double getBackgroundMean() {
            return backgroundMean;
        }

        /** @return List of squares classified as background. */
        public List<Square> getBackgroundSquares() {
            return backgroundSquares;
        }
    }

    /**
     * Calculates the average track count in a specified number of background squares
     * by selecting a configurable number of the smallest non-zero track counts from
     * the provided list of squares.
     *
     * @param squaresOfRecording A list of Square objects, each containing track count data.
     *                           Must not be null.
     * @param nrOfAverageCountSquares The number of smallest non-zero track counts to include
     *                                 in the average calculation. Must be greater than zero.
     * @return The average track count as a double, calculated from the smallest non-zero
     *         track counts in the specified number of squares. Returns 0.0 if none are found
     *         or if the number of valid squares is zero.
     */
    public static double calcAverageTrackCountInBackgroundSquares(List<Square> squaresOfRecording,
                                                                  int nrOfAverageCountSquares) {

        List<Integer> trackCounts = squaresOfRecording.stream()
                .map(Square::getNumberOfTracks)
                .collect(Collectors.toList());

        // Sort descending
        trackCounts.sort(Comparator.reverseOrder());

        double total = 0.0;
        int n = 0;

        // Traverse backward (smallest to largest)
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