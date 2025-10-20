package paint.shared.utils;

import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.objects.Track;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.selection.Selection;

import java.util.*;
import java.util.stream.Collectors;

public class SquareUtils {

    private SquareUtils() {}


    /**
     * Calculate the density of tracks in a square.
     *
     * @param nrTracks      number of tracks
     * @param area          area of the square (in µm²)
     * @param time          time in seconds (normally 100 sec = 2000 frames)
     * @param concentration concentration factor for normalization
     * @return density value
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
     * Apply visibility/selection rules to a list of squares.
     * Marks each square as selected if all filter criteria are met and numeric values are valid.
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

        // --- First pass: basic visibility based on numerical criteria ---
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

        // --- Second pass: neighbour filtering ---
        if ("Free".equalsIgnoreCase(neighbourMode)) {
            // PaintLogger.debugf("NeighbourMode = Free → skipping neighbour constraints");
            return;
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
                    // Touches at corner or edge
                    if (dr <= 1 && dc <= 1 && (dr + dc) > 0) {
                        hasNeighbour = true;
                        break;
                    }
                } else if ("Strict".equalsIgnoreCase(neighbourMode)) {
                    // Must share an edge (no corner-only contact)
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
     * Iterative mean/std-based background estimation.
     * Returns both the mean and the final background squares.
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

            if (Math.abs(mean - prevMean) / prevMean < EPSILON)
                break;
        }

        return new BackgroundEstimationResult(mean, current);
    }


    public static List<Track> getTracksFromSelectedSquares(Recording recording) {
        List<Track> selectedTracks = new ArrayList<>();

        for (Square square : recording.getSquaresOfRecording()) {
            if (square.isSelected() && square.getTracks() != null) {
                selectedTracks.addAll(square.getTracks());
            }
        }

        return selectedTracks;
    }

    public static int getNumberOfSelectedSquares(Recording recording) {
        int count = 0;

        for (Square square : recording.getSquaresOfRecording()) {
            if (square.isSelected()) {
                count++;
            }
        }

        return count;
    }

    private static double getMedian(List<Double> values) {
        if (values.isEmpty()) return Double.NaN;
        int n = values.size();
        if (n % 2 == 1) return values.get(n / 2);
        return 0.5 * (values.get(n / 2 - 1) + values.get(n / 2));
    }

    /**
     * Common container for background estimation results.
     */
    public static class BackgroundEstimationResult {
        private final double backgroundMean;
        private final List<Square> backgroundSquares;

        public BackgroundEstimationResult(double backgroundMean, List<Square> backgroundSquares) {
            this.backgroundMean = backgroundMean;
            this.backgroundSquares = backgroundSquares;
        }

        public double getBackgroundMean() {
            return backgroundMean;
        }

        public List<Square> getBackgroundSquares() {
            return backgroundSquares;
        }
    }

    public static double calcAverageTrackCountInBackgroundSquares(List<Square> squaresOfRecording, int nrOfAverageCountSquares) {

        List<Integer> trackCounts = squaresOfRecording.stream()
                .map(Square::getNumberOfTracks)
                .collect(Collectors.toList());

        // Sort descending
        trackCounts.sort(Comparator.reverseOrder());

        double total = 0.0;
        int n = 0;

        // Traverse backward (from smallest to largest), ignoring zeros
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


    // Different version, can be deleted

    public static double calculateAverageTrackCountOfBackground(Recording recording, int nrOfAverageCountSquares) {

        List<Integer> trackCounts = new ArrayList<>();
        List<Square> squares = recording.getSquaresOfRecording();

        for (Square sq : squares) {
            trackCounts.add(sq.getTracks().size());
        }

        // Sort descending
        trackCounts.sort(Collections.reverseOrder());

        int total = 0;
        int n = 0;

        // Find the first non-zero value
        int m;
        for (m = trackCounts.size() - 1; m >= 0; m--) {
            if (trackCounts.get(m) != 0) {
                break;
            }
        }

        // Iterate from the smallest to the largest (like Python's reverse loop)
        for (int i = m; i >= 0; i--) {
            int v = trackCounts.get(i);

            total += v;
            n++;
            if (n >= nrOfAverageCountSquares) {
                break;
            }

        }

        if (n == 0) {
            return 0.0;
        } else {
            return (double) total / n;
        }
    }

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