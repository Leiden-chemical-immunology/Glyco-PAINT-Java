/******************************************************************************
 *  Class:        SquareControlHandler.java
 *  Package:      paint.viewer.logic
 *
 *  PURPOSE:
 *    Provides parameter management and visibility control for a
 *    {@link paint.viewer.panels.SquareGridPanel} within the PAINT viewer.
 *
 *  DESCRIPTION:
 *    This handler applies user-defined square control parameters such as
 *    minimum density ratio, maximum variability, minimum R², and neighbour
 *    mode to a target grid panel. The updated configuration determines
 *    which squares remain visible according to the applied filters.
 *
 *    The handler may attach to an existing {@code SquareGridPanel} and
 *    update it dynamically as parameters are changed or applied.
 *
 *  KEY FEATURES:
 *    • Manages and updates visibility thresholds for grid squares.
 *    • Applies density, variability, and R² filtering logic.
 *    • Supports both attached and externally provided grid panels.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-10-29
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.viewer.logic;

import paint.viewer.panels.SquareGridPanel;
import paint.viewer.shared.SquareControlParams;

/**
 * Handles configuration and control of a {@link SquareGridPanel}
 * by applying user-defined visibility and threshold parameters.
 */
public class SquareControlHandler {

    private SquareGridPanel grid;

    /**
     * Attaches the specified {@link SquareGridPanel} to this handler.
     * All subsequent parameter applications will target this grid.
     *
     * @param panel the grid panel to be managed
     */
    public void attach(SquareGridPanel panel) {
        this.grid = panel;
    }

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