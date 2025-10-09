package paint.shared.utils;

import paint.shared.objects.Square;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class SquareUtils {

    /**
     * Apply visibility/selection rules to a list of squares.
     * Marks each square as selected if all filter criteria are met and numeric values are valid.
     */
    public static void applyVisibilityFilter(List<Square> squares,
                                             double minDensityRatio,
                                             double maxVariability,
                                             double minRSquared,
                                             String neighbourMode) {
        if (squares == null || squares.isEmpty()) return;

        int total = squares.size();
        int visibleBasic = 0;

        // --- First pass: basic visibility based on numerical criteria ---
        for (Square sq : squares) {
            boolean passes = sq.getDensityRatio() >= minDensityRatio
                    && sq.getVariability() <= maxVariability
                    && sq.getRSquared() >= minRSquared
                    && !Double.isNaN(sq.getRSquared());

            sq.setSelected(passes);
            if (passes) visibleBasic++;
        }

        PaintLogger.debugf("VisibilityFilter [%s] basic pass: %d / %d squares visible",
                neighbourMode, visibleBasic, total);

        // --- Second pass: neighbour filtering ---
        if ("Free".equalsIgnoreCase(neighbourMode)) {
            PaintLogger.debugf("NeighbourMode = Free → skipping neighbour constraints");
            return;
        }

        Set<Square> keep = new HashSet<>();
        int keptCount = 0;

        for (Square sq : squares) {
            if (!sq.isSelected()) continue;

            boolean hasNeighbour = false;
            int r = sq.getRowNumber();
            int c = sq.getColNumber();

            for (Square other : squares) {
                if (other == sq || !other.isSelected()) continue;
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
                keep.add(sq);
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