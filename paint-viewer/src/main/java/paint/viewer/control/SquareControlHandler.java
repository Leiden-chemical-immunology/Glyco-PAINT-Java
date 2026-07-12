/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.viewer.control;

import paint.viewer.ui.panels.SquareGridPanel;
import paint.viewer.model.SquareControlParams;

/**
 * Handles configuration and control of a {@link SquareGridPanel}
 * by applying user-defined visibility and threshold parameters.
 */
public class SquareControlHandler {

    private SquareGridPanel grid;


    /**
     * Applies the specified square control parameters to a grid panel.
     * <p>
     * If a panel is provided, it becomes the active target. If no panel
     * is given, the method applies the parameters to the currently
     * attached grid.
     * </p>
     *
     * @param params the square control parameters including density ratio,
     *               variability, R², and neighbour mode
     * @param panel  optional {@link SquareGridPanel} target; if null, the
     *               currently attached grid is used
     */
    public void apply(SquareControlParams params, SquareGridPanel panel) {
        if (panel != null) {
            this.grid = panel;
        }
        if (grid == null) {
            return;
        }

        grid.setControlParameters(
                params.minRequiredDensityRatio,
                params.maxAllowableVariability,
                params.minRequiredRSquared,
                params.neighbourMode
        );
        grid.applyVisibilityFilter();
    }
}