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
 * Utility class providing helper functions for operations on {@link Square} and {@link Recording} objects.
 * <p>
 * Includes calculations of densities, selection filters, background estimations,
 * and table-based filtering for spatial data.
 *
 * <p>This class is not instantiable.
 */
public class SquareUtils {

    /** Private constructor to prevent instantiation. */
    private SquareUtils() {}

    /**
     * Calculates the density of tracks in a square.
     *
     * @param nrTracks      Number of tracks detected within the square.
     * @param area          Area of the square (in µm²).
     * @param time          Duration of the recording (in seconds).
     * @param concentration Concentration factor used for normalization.
     * @return Density value (normalized by area, time, and concentration).
     * @throws IllegalArgumentException if any of {@code area}, {@code time}, or {@code concentration} is non-positive.
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
     * Applies visibility and selection filters to the squares of a recording.
     * <p>
     * A square is marked as visible (selected) if:
     * <ul>
     *   <li>Its density ratio ≥ {@code minDensityRatio}</li>
     *   <li>Its variability ≤ {@code maxVariability}</li>
     *   <li>Its R² ≥ {@code minRSquared}</li>
     *   <li>And its R² value is numeric (not NaN)</li>
     * </ul>
     * <p>
     * Optionally applies neighbor filtering:
     * <ul>
     *   <li><b>Free</b> — no neighbor constraint</li>
     *   <li><b>Relaxed</b> — must touch another visible square by edge or corner</li>
     *   <li><b>Strict</b> — must share a common edge with another visible square</li>
     * </ul>
     *
     * @param recording       The recording whose squares are filtered.
     * @param minDensityRatio Minimum allowed density ratio.
     * @param maxVariability  Maximum allowed variability.
     * @param minRSquared     Minimum allowed R² value.
     * @param neighbourMode   Neighbor mode: "Free", "Relaxed", or "Strict".
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
     * Performs iterative mean/standard-deviation-based background estimation.
     * <p>
     * Removes outliers iteratively until convergence or a maximum number of iterations is reached.
     *
     * @param squares List of squares whose track counts are used for estimation.
     * @return {@link BackgroundEstimationResult} containing the mean and list of background squares.
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
     * Collects all {@link Track} objects from squares that are currently selected.
     *
     * @param recording Source recording.
     * @return Combined list of tracks from all selected squares.
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
     * Counts the number of selected squares in a recording.
     *
     * @param recording The recording to check.
     * @return Number of selected squares.
     */
    public static int getNumberOfSelectedSquares(Recording recording) {
        int count = 0;
        for (Square square : recording.getSquaresOfRecording()) {
            if (square.isSelected()) count++;
        }
        return count;
    }

    /**
     * Container class for background estimation results.
     */
    public static class BackgroundEstimationResult {
        private final double backgroundMean;
        private final List<Square> backgroundSquares;

        /**
         * Constructs a new {@code BackgroundEstimationResult}.
         *
         * @param backgroundMean    Mean track count of the estimated background.
         * @param backgroundSquares List of squares included in the background.
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
     * Computes the average track count among the {@code nrOfAverageCountSquares} least populated squares.
     * Ignores squares with zero tracks.
     *
     * @param squaresOfRecording      List of all squares in the recording.
     * @param nrOfAverageCountSquares Number of lowest-count squares to average.
     * @return Average track count of background squares.
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
     * Filters tracks within a square’s coordinate boundaries.
     *
     * @param tracks     The full table of track coordinates.
     * @param square     The square defining the region of interest.
     * @param lastRowCol Index of the last row/column (used for inclusive edge behavior).
     * @return A subset {@link Table} containing only tracks within the square.
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