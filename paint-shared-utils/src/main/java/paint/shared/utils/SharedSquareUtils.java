/******************************************************************************
 *  Class:        SharedSquareUtils.java
 *  Package:      paint.shared.utils
 *
 *  PURPOSE:
 *    Provides common utilities for operations on square regions within
 *    recordings, including filtering track data to a given spatial square
 *    and applying visibility-based selection criteria across squares.
 *
 *  DESCRIPTION:
 *    • filterTracksInSquare: Restricts a table of track coordinates to those
 *      that fall within the bounds of a specified square (taking into account
 *      boundary inclusivity for last row/column).
 *    • applyVisibilityFilter: Filters a list of squares in a recording based
 *      on numeric thresholds (density ratio, variability, R²) and optionally
 *      applies neighbour-based retention logic (Free, Relaxed, Strict).
 *
 *  RESPONSIBILITIES:
 *    • Provide static methods for spatial filtering of track data.
 *    • Apply selection and neighbour-mode logic to visible squares.
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
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.shared.utils;

import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.objects.Track;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.selection.Selection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class providing methods for filtering and evaluating square regions
 * in PAINT recordings. Supports both spatial filtering of track coordinates
 * and multi-criteria visibility selection of squares.
 */
public final class SharedSquareUtils {

    /** Private constructor to prevent instantiation. */
    private SharedSquareUtils() {}

    // ───────────────────────────────────────────────────────────────────────────────
    // FILTER TRACKS BY SQUARE REGION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Filters track coordinates to include only those within the specified square.
     * <p>
     * This method handles inclusive or exclusive boundaries depending on whether
     * the square is in the last column or row. It operates on a Tablesaw table
     * containing at least "Track X Location" and "Track Y Location" columns.
     * </p>
     *
     * @param tracks     table of track data with X/Y coordinate columns
     * @param square     the square region defining coordinate boundaries
     * @param lastRowCol index of the last row/column in the grid (for boundary handling)
     * @return new {@link Table} containing only the tracks located within the specified square
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

    // ───────────────────────────────────────────────────────────────────────────────
    // VISIBILITY FILTERING ACROSS SQUARES
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Applies a visibility filter to all squares in a recording based on numeric
     * thresholds and optional neighbour-based retention logic.
     * <p>
     * <b>Pass 1:</b> Marks each square selected if it meets:
     * <ul>
     *   <li>Density ratio ≥ minDensityRatio</li>
     *   <li>Variability ≤ maxVariability</li>
     *   <li>R² ≥ minRSquared and not NaN</li>
     * </ul>
     * <b>Pass 2:</b> If {@code neighbourMode != "Free"}, keeps only squares having
     * at least one selected neighbour:
     * <ul>
     *   <li>“Relaxed” → corner or edge adjacency allowed (dr ≤ 1 & dc ≤ 1)</li>
     *   <li>“Strict” → edge adjacency only (dr = 1 & dc = 0 or vice versa)</li>
     * </ul>
     *
     * @param squares         the list of squares
     * @param minDensityRatio minimum density ratio for selection
     * @param maxVariability  maximum allowed variability
     * @param minRSquared     minimum R² value for selection
     * @param neighbourMode   neighbour logic: "Free", "Relaxed", or "Strict"
     */
    public static void applyVisibilityFilter(List<Square> squares,
                                             double minDensityRatio,
                                             double maxVariability,
                                             double minRSquared,
                                             String neighbourMode) {

        if (squares == null || squares.isEmpty()) {
            return;
        }

        int visibleBasic = 0;

        // Pass 1 — Numeric filter
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

        // Pass 2 — Neighbour-based refinement
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
                    // Only direct-edge adjacency allowed
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
     * Retrieves a list of tracks from all selected squares in the provided recording.
     * If a square is selected and contains tracks, those tracks are added to the result.
     *
     * @param squares The squares to be checked.
     * @return A list of tracks from all selected squares in the recording.
     */
    public static List<Track> getTracksFromSelectedSquares(List<Square> squares) {
        List<Track> selectedTracks = new ArrayList<>();
        for (Square square : squares) {
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
            if (square.isSelected()) {
                count++;
            }
        }
        return count;
    }

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

        double density  = nrTracks / area;
        density        /= time;
        density        /= concentration;

        return density;
    }
}