package viewer.logic;

import paint.shared.objects.Square;
import viewer.panels.SquareGridPanel;

import java.util.*;

/**
 * Manages the assignment and undo functionality for cell assignments
 * in a grid of squares.
 *
 * This class is responsible for assigning cell IDs to selected squares in a grid
 * and providing the ability to undo the most recent assignment operation.
 */
public class CellAssignmentManager {
    private final Map<Integer, Integer> squareAssignments = new HashMap<Integer, Integer>();
    private final Deque<Map<Integer, Integer>> undoStack = new ArrayDeque<Map<Integer, Integer>>();

    /**
     * Assigns the specified cell ID to all currently selected squares in the grid.
     * Updates the internal state to allow for undoing the assignment.
     *
     * @param cellId the identifier to assign to the selected squares
     * @param grid the grid panel containing the squares to be modified
     */
    public void assignSelectedSquares(int cellId, SquareGridPanel grid) {
        Set<Integer> selected = grid.getSelectedSquares();
        if (selected.isEmpty()) return;

        // Save current state for undo
        undoStack.push(new HashMap<Integer, Integer>(squareAssignments));

        for (Square sq : grid.getSquares()) {
            if (selected.contains(sq.getSquareNumber())) {
                sq.setCellId(cellId);
                squareAssignments.put(sq.getSquareNumber(), cellId);
            }
        }
        grid.clearMouseSelection();
        grid.repaint();
    }

    /**
     * Undoes the most recent assignment operation by restoring the state of square assignments
     * to the previous state stored in the undo stack. Updates the squares in the provided grid
     * and repaints the grid to reflect the restored state.
     *
     * @param grid the grid panel containing the squares to be reverted to their previous state
     */
    public void undo(SquareGridPanel grid) {
        if (undoStack.isEmpty()) return;

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