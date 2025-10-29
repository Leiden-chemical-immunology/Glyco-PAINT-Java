package paint.viewer.logic;

import paint.viewer.shared.SquareControlParams;
import paint.viewer.panels.SquareGridPanel;

/**
 * The SquareControlHandler class provides functionality to manage
 * and control the behavior of a SquareGridPanel by configuring its
 * parameters and applying visibility filters.
 */
public class SquareControlHandler {

    private SquareGridPanel grid;

    public void attach(SquareGridPanel panel) {
        this.grid = panel;
    }

    /**
     * Applies the given control parameters to a specified or existing SquareGridPanel.
     * If a new panel is provided, it is set as the current grid.
     * The method configures the grid with density ratio, variability,
     * R-squared value, and neighbour mode, then applies a visibility filter.
     *
     * @param params the control parameters to be applied, which include density ratio,
     *               variability, R-squared, and neighbour mode
     * @param panel  the SquareGridPanel to which the control parameters are applied;
     *               can be null if an existing panel is already attached
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