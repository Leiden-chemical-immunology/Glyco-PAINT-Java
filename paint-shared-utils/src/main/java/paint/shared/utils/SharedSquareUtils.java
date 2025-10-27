/******************************************************************************
 *  Class:        SharedSquareUtils.java
 *  Package:      paint.shared.utils
 *
 *  PURPOSE:
 *    Provides common utilities for operations on square regions within
 *    recordings, including filtering track data to a given spatial square
 *    and applying visibility‐based selection criteria across squares.
 *
 *  DESCRIPTION:
 *    • filterTracksInSquare: Restricts a table of track coordinates to those
 *      that fall within the bounds of a specified square (taking into account
 *      boundary inclusivity for last row/column).  
 *    • applyVisibilityFilter: Filters a list of squares in a recording based
 *      on numeric thresholds (density ratio, variability, R²) and optionally
 *      applies neighbour‐based retention logic (Free, Relaxed, Strict).
 *
 *  RESPONSIBILITIES:
 *    • Provide static methods for spatial filtering of track data and
 *      square‐selection logic based on experiment criteria.  
 *
 *  USAGE EXAMPLE:
 *    Table filtered = SharedSquareUtils.filterTracksInSquare(tracksTable, square, lastRowCol);
 *    SharedSquareUtils.applyVisibilityFilter(recording, minDensityRatio, maxVariability, minRSq, neighbourMode);
 *
 *  DEPENDENCIES:
 *    – paint.shared.objects.Recording  
 *    – paint.shared.objects.Square  
 *    – tech.tablesaw.api.Table, DoubleColumn  
 *    – tech.tablesaw.selection.Selection  
 *    – java.util.List, java.util.Set, java.util.HashSet  
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
     * It handles inclusive or exclusive boundaries depending on whether the square lies in the last column or row.
     *
     * @param tracks     The table containing track data, with at least "Track X Location" and
     *                   "Track Y Location" columns.
     * @param square     The square specifying the area of interest, with defined corner coordinates
     *                   and row/column indices.
     * @param lastRowCol The index of the last column or row (maximum index) in the grid of squares,
     *                   used to decide whether boundary edges are inclusive.
     * @return A new {@link Table} containing only the tracks located within the boundaries of the specified square.
     */
    public static Table filterTracksInSquare(Table tracks, Square square, int lastRowCol) {
        double x0 = square.getX0();
        double y0 = square.getY0();
        double x1 = square.getX1();
        double y1 = square.getY1();

        boolean isLastCol = square.getColNumber() == lastRowCol;
        boolean isLastRow = square.getRowNumber() == lastRowCol;

        double left   = Math.min(x0, x1);
        double right  = Math.max(x0, x1);
        double top    = Math.min(y0, y1);
        double bottom = Math.max(y0, y1);

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
     * numeric criteria and neighbour‐mode criteria.  
     * <p>
     * First pass: each square is marked selected if it meets the minimum density ratio,
     * maximum variability, and minimum R² thresholds (and the R² is not NaN).  
     * <p>
     * Second pass (unless neighbourMode = "Free"): among the selected squares, further
     * retain only those that have a selected neighbour according to the neighbourMode:
     * – "Relaxed": corner or edge adjacency allowed (dr ≤1 & dc ≤1, dr+dc >0)  
     * – "Strict": direct edge adjacency only (either dr =1 & dc =0 OR dr =0 & dc =1)
     *
     * @param recording       The {@link Recording} object containing the list of {@link Square}
     *                        objects to be filtered.
     * @param minDensityRatio The minimum density ratio required for a square to pass basic filter.
     * @param maxVariability  The maximum variability allowed for a square to pass the basic filter.
     * @param minRSquared     The minimum R² value a square must have to pass the basic filter.
     * @param neighbourMode   The neighbour mode defining how adjacency is considered for the second filter
     *                        stage — expected values: "Free", "Relaxed", or "Strict".
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
            return; // No neighbour constraints
        }

        Set<Square> keep     = new HashSet<>();
        int keptCount        = 0;

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