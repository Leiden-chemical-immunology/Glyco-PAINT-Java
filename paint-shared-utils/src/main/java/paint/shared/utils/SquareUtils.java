package paint.shared.utils;

import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.objects.Track;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.selection.Selection;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for operations related to square data analysis.
 */
public class SquareUtils {

    /** Private constructor to prevent instantiation. */
    private SquareUtils() {}

    /**
     * Calculates the density of tracks in a specified area over time and considering a concentration factor.
     *
     * The density is computed as the number of tracks divided by the area,
     * then divided by the time, and finally divided by the concentration.
     * If any of the area, time, or concentration values are non-positive, an IllegalArgumentException is thrown.
     *
     * @param nrTracks      The total number of tracks to calculate density for.
     * @param area          The area over which the density is calculated. Must be positive.
     * @param time          The time period over which the density is calculated. Must be positive.
     * @param concentration The concentration factor to apply to the density calculation. Must be positive.
     * @return The calculated density as a double.
     * @throws IllegalArgumentException if area, time, or concentration is less than or equal to zero.
     */
    public static double calculateDensity(int nrTracks, double area, double time, double concentration) {
        if (area <= 0 || time <= 0 || concentration <= 0) {
            throw new IllegalArgumentException("Area, time, and concentration must be positive");
        }

        double density = nrTracks / area;
        density /= time;
        density /= concentration;

        return density;
    }

    /**
     * Applies a visibility filter to the squares in the provided recording based on specified
     * density ratio, variability, R-squared value, and neighbor mode criteria.
     * This method first filters squares based on numeric criteria, then optionally applies
     * neighbor-based filtering if a neighbor mode is specified.
     *
     * @param recording       The recording containing the squares to be filtered.
     * @param minDensityRatio The minimum density ratio required for a square to pass the filter.
     * @param maxVariability  The maximum variability allowed for a square to pass the filter.
     * @param minRSquared     The minimum R-squared value a square must have to pass the filter.
     * @param neighbourMode   The neighbor mode defining how adjacency is considered.
     *                        Possible values are "Free", "Relaxed", and "Strict".
     */
    public static void applyVisibilityFilter(Recording recording,
                                             double minDensityRatio,
                                             double maxVariability,
                                             double minRSquared,
                                             String neighbourMode) {

        List<Square> squares = recording.getSquaresOfRecording();
        if (squares == null || squares.isEmpty()) {
            return;
        }

        int total = squares.size();
        int visibleBasic = 0;

        // --- Pass 1: Filter by numeric criteria ---
        for (Square square : squares) {
            boolean passes = square.getDensityRatio() >= minDensityRatio
                    && square.getVariability() <= maxVariability
                    && square.getRSquared() >= minRSquared
                    && !Double.isNaN(square.getRSquared());

            square.setSelected(passes);
            if (passes) {
                visibleBasic++;
            }
        }

        // --- Pass 2: Apply neighbour filtering if requested ---
        if ("Free".equalsIgnoreCase(neighbourMode)) {
            return; // No neighbor constraints
        }

        Set<Square> keep = new HashSet<>();
        int keptCount = 0;

        for (Square square : squares) {
            if (!square.isSelected()) {
                continue;
            }

            boolean hasNeighbour = false;
            int r = square.getRowNumber();
            int c = square.getColNumber();

            for (Square other : squares) {
                if (other == square || !other.isSelected()) {
                    continue;
                }
                int dr = Math.abs(other.getRowNumber() - r);
                int dc = Math.abs(other.getColNumber() - c);

                if ("Relaxed".equalsIgnoreCase(neighbourMode)) {
                    // Corner or edge adjacency allowed
                    if (dr <= 1 && dc <= 1 && (dr + dc) > 0) {
                        hasNeighbour = true;
                        break;
                    }
                } else if ("Strict".equalsIgnoreCase(neighbourMode)) {
                    // Only direct edge adjacency allowed
                    if ((dr == 1 && dc == 0) || (dr == 0 && dc == 1)) {
                        hasNeighbour = true;
                        break;
                    }
                }
            }

            if (hasNeighbour) {
                keep.add(square);
                keptCount++;
            }
        }

        // --- Apply neighbour filtering result ---
        for (Square sq : squares) {
            if (!keep.contains(sq)) {
                sq.setSelected(false);
            }
        }

        PaintLogger.debugf("NeighbourMode [%s] neighbour-filtered: %d / %d retained",
                           neighbourMode, keptCount, visibleBasic);
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
    public static BackgroundEstimationResult estimateBackgroundDensity(List<Square> squares) {
        if (squares == null || squares.isEmpty())
            return new BackgroundEstimationResult(Double.NaN, Collections.emptyList());

        double mean = squares.stream()
                .mapToDouble(Square::getNumberOfTracks)
                .average().orElse(Double.NaN);

        if (Double.isNaN(mean) || mean == 0)
            return new BackgroundEstimationResult(mean, Collections.emptyList());

        final double EPSILON = 0.01;
        final int MAX_ITER = 10;
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

            if (filtered.isEmpty())
                break;

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
     * Retrieves a list of tracks from all selected squares in the provided recording.
     * If a square is selected and contains tracks, those tracks are added to the result.
     *
     * @param recording The recording object containing the squares to be checked.
     * @return A list of tracks from all selected squares in the recording.
     */
    public static List<Track> getTracksFromSelectedSquares(Recording recording) {
        List<Track> selectedTracks = new ArrayList<>();
        for (Square square : recording.getSquaresOfRecording()) {
            if (square.isSelected() && square.getTracks() != null) {
                selectedTracks.addAll(square.getTracks());
            }
        }
        return selectedTracks;
    }

    /**
     * Counts the number of squares marked as selected in the provided recording.
     *
     * @param recording The recording object containing a collection of squares.
     * @return The count of squares that are marked as selected.
     */
    public static int getNumberOfSelectedSquares(Recording recording) {
        int count = 0;
        for (Square square : recording.getSquaresOfRecording()) {
            if (square.isSelected()) count++;
        }
        return count;
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
            this.backgroundMean = backgroundMean;
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

    /**
     * Filters tracks within a given square, restricting to a specific range of x and y coordinates.
     * This method processes tracks to retain only those within the coordinate bounds of the specified square.
     * Handles inclusive or exclusive boundaries depending on whether the square is in the last column or row.
     *
     * @param tracks The table containing track data, with at least "Track X Location" and "Track Y Location" columns.
     * @param square The square specifying the area of interest with defined corner coordinates.
     * @param lastRowCol The index of the last column or row, used to adjust boundary conditions.
     * @return A new table containing only the tracks located within the specified square's boundaries.
     */
    public static Table filterTracksInSquare(Table tracks, Square square, int lastRowCol) {
        double x0 = square.getX0(), y0 = square.getY0(), x1 = square.getX1(), y1 = square.getY1();

        boolean isLastCol = square.getColNumber() == lastRowCol;
        boolean isLastRow = square.getRowNumber() == lastRowCol;

        double left = Math.min(x0, x1), right = Math.max(x0, x1);
        double top = Math.min(y0, y1), bottom = Math.max(y0, y1);

        DoubleColumn x = tracks.doubleColumn("Track X Location");
        DoubleColumn y = tracks.doubleColumn("Track Y Location");

        Selection selX = isLastCol
                ? x.isBetweenInclusive(left, right)
                : x.isGreaterThanOrEqualTo(left).and(x.isLessThan(right));

        Selection selY = isLastRow
                ? y.isBetweenInclusive(top, bottom)
                : y.isGreaterThanOrEqualTo(top).and(y.isLessThan(bottom));

        return tracks.where(selX.and(selY));
    }
}