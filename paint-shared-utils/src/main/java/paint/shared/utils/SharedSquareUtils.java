package paint.shared.utils;

import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.selection.Selection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SharedSquareUtils {

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
        double x0 = square.getX0();
        double y0 = square.getY0();
        double x1 = square.getX1();
        double y1 = square.getY1();

        boolean isLastCol = square.getColNumber() == lastRowCol;
        boolean isLastRow = square.getRowNumber() == lastRowCol;

        double left = Math.min(x0, x1), right = Math.max(x0, x1);
        double top  = Math.min(y0, y1), bottom = Math.max(y0, y1);

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

}
