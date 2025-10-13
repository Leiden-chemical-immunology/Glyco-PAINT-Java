package viewer.logic;

import viewer.shared.SquareControlParams;
import viewer.panels.SquareGridPanel;

public class SquareControlHandler {
    private SquareGridPanel grid;

    public void attach(SquareGridPanel panel) {
        this.grid = panel;
    }

    public void apply(SquareControlParams params, SquareGridPanel panel) {
        if (panel != null) {
            this.grid = panel;
        }
        if (grid == null) {
            return;
        }

        grid.setControlParameters(
                params.densityRatio,
                params.variability,
                params.rSquared,
                params.neighbourMode
        );
        grid.applyVisibilityFilter();
    }
}