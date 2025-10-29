/******************************************************************************
 *  Class:        CellAssignmentManager.java
 *  Package:      paint.viewer.logic
 *
 *  PURPOSE:
 *    Manages cell assignment and undo functionality for square grids within
 *    the PAINT viewer. Enables assigning a cell ID to selected squares and
 *    supports reverting to the previous assignment state.
 *
 *  DESCRIPTION:
 *    This class maintains an internal mapping of square numbers to assigned
 *    cell IDs. It provides operations to:
 *      • Assign a given cell ID to all currently selected squares.
 *      • Store historical assignment states for undo operations.
 *      • Restore a previous state upon undo.
 *
 *    Integration occurs via {@link paint.viewer.panels.SquareGridPanel},
 *    which provides access to squares and their selection state.
 *
 *  KEY FEATURES:
 *    • Assigns cell IDs to selected squares in bulk.
 *    • Maintains an undo stack for reversing recent assignments.
 *    • Automatically repaints the grid after each update.
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

import paint.shared.objects.Square;
import paint.viewer.panels.SquareGridPanel;

import java.util.*;

/**
 * Handles cell assignment operations for a grid of {@link Square} objects.
 * <p>
 * Supports both assigning a cell ID to selected squares and undoing
 * the last assignment operation by restoring the previous state.
 */
public class CellAssignmentManager {

    private final Map<Integer, Integer> squareAssignments = new HashMap<>();
    private final Deque<Map<Integer, Integer>> undoStack   = new ArrayDeque<>();

    /**
     * Assigns the specified cell ID to all currently selected squares in the grid.
     * The current assignment state is saved before modification, allowing undo.
     *
     * @param cellId the identifier to assign to the selected squares
     * @param grid   the grid panel containing the squares to be modified
     */
    public void assignSelectedSquares(int cellId, SquareGridPanel grid) {
        Set<Integer> selected = grid.getSelectedSquares();
        if (selected.isEmpty()) {
            return;
        }

        // Save the current state for undo
        undoStack.push(new HashMap<>(squareAssignments));

        for (Square square : grid.getSquares()) {
            if (selected.contains(square.getSquareNumber())) {
                square.setCellId(cellId);
                squareAssignments.put(square.getSquareNumber(), cellId);
            }
        }

        grid.clearMouseSelection();
        grid.repaint();
    }

    /**
     * Undoes the most recent assignment operation by restoring the previous
     * state from the undo stack. Updates the grid to reflect the restored
     * assignments and triggers a repaint.
     *
     * @param grid the grid panel containing the squares to revert
     */
    public void undo(SquareGridPanel grid) {
        if (undoStack.isEmpty()) {
            return;
        }

        squareAssignments.clear();
        squareAssignments.putAll(undoStack.pop());

        for (Square sq : grid.getSquares()) {
            int cellId = squareAssignments.containsKey(sq.getSquareNumber())
                    ? squareAssignments.get(sq.getSquareNumber())
                    : 0;
            sq.setCellId(cellId);
        }

        grid.repaint();
    }
}