package paint.shared.utils;

import paint.shared.objects.Square;
import java.util.List;

public class SquareUtils {

    /**
     * Apply visibility/selection rules to a list of squares.
     * Marks each square as selected if all filter criteria are met and numeric values are valid.
     */
    public static void applyVisibilityFilter(List<Square> squares,
                                             double minDensityRatio,
                                             double maxVariability,
                                             double minRSquared) {
        if (squares == null || squares.isEmpty()) return;

        for (Square sq : squares) {
            boolean selected =
                    Double.isFinite(sq.getDensityRatio()) &&
                            Double.isFinite(sq.getVariability()) &&
                            Double.isFinite(sq.getRSquared()) &&
                            sq.getDensityRatio() >= minDensityRatio &&
                            sq.getVariability() <= maxVariability &&
                            sq.getRSquared() >= minRSquared;

            sq.setSelected(selected);
        }
    }
}