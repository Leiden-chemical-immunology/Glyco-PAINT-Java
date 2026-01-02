/*=============================================================================
 *  Class:        SharedSquareUtils.java
 *  Package:      paint.shared.utils
 *
 *  PURPOSE:
 *    Provides shared utility methods for square-level calculations and data
 *    transformations within the PAINT framework.
 *
 *  DESCRIPTION:
 *    The {@code SharedSquareUtils} class centralizes common operations
 *    on {@link Square} objects, such as unique key generation and visibility
 *    state management.
 *
 *  KEY FEATURES:
 *    • Static methods for spatial filtering of track data.
 *    • Selection and neighbour-mode logic for square visibility.
 *    • Standardized track-in-square overlap detection.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
=============================================================================*/

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

import static paint.shared.constants.PaintStringConstants.TRACK_X_LOCATION;
import static paint.shared.constants.PaintStringConstants.TRACK_Y_LOCATION;

/**
 * Utility class providing methods for filtering and evaluating square regions
 * in PAINT recordings. Supports both spatial filtering of track coordinates
 * and multi-criteria visibility selection of squares.
 */
public final class SharedSquareUtils {

    /**
     * Private constructor to prevent instantiation.
     */
    private SharedSquareUtils() {
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // FILTER TRACKS BY SQUARE REGION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Filters track coordinates to include only those within the specified square.
     * <p>
     * This method handles inclusive or exclusive boundaries depending on whether
     * the square is in the last column or row. It operates on a Tablesaw table
     * containing at least TRACK_X_LOCATION and TRACK_Y_LOCATION columns.
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

        // See if the square sits at the right or bottom border of the grid.
        boolean isLastCol = square.getColNumber() == lastRowCol;
        boolean isLastRow = square.getRowNumber() == lastRowCol;

        // Ensures that left < right and top < bottom even if coordinates were swapped.
        double left   = Math.min(x0, x1);
        double right  = Math.max(x0, x1);
        double top    = Math.min(y0, y1);
        double bottom = Math.max(y0, y1);

        // Get the x,y coordinates of the tracks
        DoubleColumn x = tracks.doubleColumn(TRACK_X_LOCATION);
        DoubleColumn y = tracks.doubleColumn(TRACK_Y_LOCATION);

        // For normal squares → take left ≤ x < right
	    // For rightmost column → take left ≤ x ≤ right (inclusive on both sides)
        Selection selX = isLastCol
                ? x.isGreaterThanOrEqualTo(left).and(x.isLessThanOrEqualTo(right))
                : x.isGreaterThanOrEqualTo(left).and(x.isLessThan(right));

        // For normal squares → top ≤ y < bottom
		// For bottom squares → top ≤ y ≤ bottom
        Selection selY = isLastRow
                ? y.isGreaterThanOrEqualTo(top).and(y.isLessThanOrEqualTo(bottom))
                : y.isGreaterThanOrEqualTo(top).and(y.isLessThan(bottom));

        return tracks.where(selX.and(selY));
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // VISIBILITY FILTERING ACROSS SQUARES
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Applies the full visibility filter to squares belonging to a specific recording.
     * <p>
     * The method performs a two-stage filtering process:
     *
     * <h3>1. Numeric filter</h3>
     * A square becomes visible if all of the following are true:
     * <ul>
     *     <li>its R² value is not NaN and ≥ {@code minRequiredRSquared}</li>
     *     <li>its density ratio ≥ {@code minRequiredDensityRatio}</li>
     *     <li>its variability ≤ {@code maxAllowableVariability}</li>
     * </ul>
     * If {@code recordingName} is non-null, only squares whose
     * {@code getRecordingName()} matches the given name are evaluated;
     * others are skipped.
     *
     * <h3>2. Neighbour filter</h3>
     * Applied only when {@code neighbourMode} is not {@code "Free"}.
     * A square that passed the numeric filter will remain visible only if
     * it has at least one other <em>visible</em> neighbouring square.
     * Neighbourhood rules:
     * <ul>
     *   <li><b>"Relaxed"</b>: any of the 8 surrounding squares (edge or corner) counts as neighbouring</li>
     *   <li><b>"Strict"</b>: only 4-connected neighbours (up, down, left, right) count</li>
     * </ul>
     * If {@code neighbourMode} is neither {@code "Free"}, {@code "Relaxed"},
     * nor {@code "Strict"}, an {@link IllegalArgumentException} is thrown.
     *
     * <h3>Final step</h3>
     * Any square not selected during the neighbour filter is set to invisible.
     *
     * @param squares                    the squares to filter
     * @param recordingName              if non-null, only squares from this recording are evaluated;
     *                                   if null, all squares are processed
     * @param minRequiredDensityRatio    minimum density ratio required to be visible
     * @param maxAllowableVariability    maximum variability allowed to be visible
     * @param minRequiredRSquared        minimum R² required to be visible
     * @param neighbourMode              neighbour rule: {@code "Free"}, {@code "Relaxed"}, or {@code "Strict"}
     */
    public static void applyVisibilityFilterOnRecording(
            List<Square> squares,
            String       recordingName,
            double       minRequiredDensityRatio,
            double       maxAllowableVariability,
            double       minRequiredRSquared,
            String       neighbourMode) {

        if (squares == null || squares.isEmpty()) {
            return;
        }

        // Build the subset we will actually process.
        List<Square> target = new ArrayList<>();
        if (recordingName == null) {
            target.addAll(squares);
        } else {
            for (Square s : squares) {
                if (recordingName.equals(s.getRecordingName())) {
                    target.add(s);
                }
            }
        }

        if (target.isEmpty()) {
            return;
        }

        int visibleBasic = 0;

        // Pass 1 — Numeric filter (ONLY on target)
        for (Square square : target) {
            boolean passes =
                    !Double.isNaN(square.getRSquared()) &&
                            square.getDensityRatio() >= minRequiredDensityRatio &&
                            square.getVariability()  <= maxAllowableVariability &&
                            square.getRSquared()     >= minRequiredRSquared;

            square.setVisible(passes);
            if (passes) {
                visibleBasic++;
            }
        }

        // Pass 2 — Neighbour-based refinement (ONLY on target)
        if ("Free".equalsIgnoreCase(neighbourMode)) {
            return;
        }

        boolean relaxed = "Relaxed".equalsIgnoreCase(neighbourMode);
        boolean strict  = "Strict".equalsIgnoreCase(neighbourMode);
        if (!relaxed && !strict) {
            throw new IllegalArgumentException("Invalid neighbourMode: " + neighbourMode);
        }

        Set<Square> keep      = new HashSet<>();
        int         keptCount = 0;

        for (Square square : target) {
            if (!square.isVisible()) {
                continue;
            }

            boolean hasNeighbour = false;
            int rowNumber = square.getRowNumber();
            int colNumber = square.getColNumber();

            for (Square other : target) {
                if (other == square || !other.isVisible()) {
                    continue;
                }

                int differenceInRow    = Math.abs(other.getRowNumber() - rowNumber);
                int differenceInColumn = Math.abs(other.getColNumber() - colNumber);

                if (relaxed) {
                    if (differenceInRow <= 1 && differenceInColumn <= 1) {
                        hasNeighbour = true;
                        break;
                    }
                } else { // strict
                    if ((differenceInRow == 1 && differenceInColumn == 0) || (differenceInRow == 0 && differenceInColumn == 1)) {
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

        // Apply neighbour filtering result (ONLY on target)
        for (Square square : target) {
            if (!keep.contains(square)) {
                square.setVisible(false);
            }
        }

        PaintLogger.debugf("NeighbourMode [%s] neighbour-filtered: %d / %d retained",
                           neighbourMode, keptCount, visibleBasic);
    }


    /**
     * Applies the visibility filter to all squares in a recording using the given
     * numeric thresholds and neighbour-mode rules.
     * <p>
     * This is a convenience method that delegates to
     * {@link #applyVisibilityFilterOnRecording(List, String, double, double, double, String)}
     * without restricting to a specific recording name (i.e., it applies the filter
     * to all squares in the list).
     *
     * <h3>Filtering steps</h3>
     * <ol>
     *   <li><b>Numeric filter:</b>
     *       A square becomes visible if:
     *       <ul>
     *         <li>its R² is not NaN and ≥ {@code minRSquared}</li>
     *         <li>its density ratio ≥ {@code minDensityRatio}</li>
     *         <li>its variability ≤ {@code maxVariability}</li>
     *       </ul>
     *   </li>
     *   <li><b>Neighbour filter:</b>
     *       Only applied when {@code neighbourMode} is not {@code "Free"}.
     *       <ul>
     *         <li>{@code "Relaxed"} → any touching neighbour (8-connected)</li>
     *         <li>{@code "Strict"} → edge-adjacent neighbour only (4-connected)</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * @param squares        the list of squares to filter
     * @param minDensityRatio minimum required density ratio
     * @param maxVariability   maximum allowable variability
     * @param minRSquared      minimum required R²
     * @param neighbourMode    neighbour filtering mode:
     *                         {@code "Free"}, {@code "Relaxed"}, or {@code "Strict"}
     */
    public static void applyVisibilityFilter(
            List<Square> squares,
            double       minDensityRatio,
            double       maxVariability,
            double       minRSquared,
            String       neighbourMode) {
        applyVisibilityFilterOnRecording(squares, null, minDensityRatio, maxVariability,  minRSquared, neighbourMode);
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
            if (square.isVisible() && square.getTracks() != null) {
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
            if (square.isVisible()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Calculates the density of tracks in a specified area over time and considering a concentration factor.
     * <p>
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