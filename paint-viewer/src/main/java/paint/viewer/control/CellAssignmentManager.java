/*==============================================================================
 *  Class:        CellAssignmentManager.java
 *  Package:      paint.viewer.control
 *
 *  PURPOSE:
 *    Manages cell assignment and undo functionality for square grids within
 *    the PAINT viewer. Enables assigning a cell ID to selected squares and
 *    supports reverting to the previous assignment state.
 *
 *  DESCRIPTION:
 *    This class maintains a mapping between square numbers and assigned cell IDs.
 *    Assignments are applied to the currently selected squares, and each operation
 *    records the previous state in an undo stack to allow reversal.
 *
 *    Collaboration occurs with {@link paint.viewer.ui.panels.SquareGridPanel},
 *    which exposes square lists, selection state, and repaint triggering.
 *
 *  KEY FEATURES:
 *    • Bulk assignment of a cell ID to selected squares.
 *    • Undo support by restoring previous assignment snapshots.
 *    • Automatic grid refresh after each modification.
 *    • Fully self-contained logic with no retained UI state.
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
==============================================================================*/

package paint.viewer.control;

import paint.shared.objects.Square;
import paint.viewer.ui.panels.SquareGridPanel;

import java.util.*;

/**
 * Handles cell assignment operations for a grid of {@link Square} objects.
 * <p>
 * Provides:
 * <ul>
 *   <li>Assigning a common cell ID to all currently selected squares.</li>
 *   <li>Undoing the latest assignment by restoring the previous state.</li>
 *   <li>Clearing state when switching recordings.</li>
 * </ul>
 * All repainting is delegated to the {@link SquareGridPanel}.
 */
public class CellAssignmentManager {

    /** Maps squareNumber → cellId for all assigned squares. */
    private final Map<Integer, Integer> squareAssignments = new HashMap<>();

    /** Stack of previous assignment snapshots, enabling undo. */
    private final Deque<Map<Integer, Integer>> undoStack = new ArrayDeque<>();

    /**
     * Assigns the specified cell ID to all user-selected squares.
     * <p>
     * The current assignment map is copied and pushed onto the undo stack
     * before any modification occurs, ensuring a reversible operation.
     *
     * @param cellId ID to assign to each selected square
     * @param grid   the grid panel containing square objects and selection info
     * @return an unmodifiable view of the updated squareAssignments map
     */
    public Map<Integer, Integer> assignUserSelectedSquares(int cellId, SquareGridPanel grid) {

        // All square numbers currently selected by the user
        Set<Integer> userSelectedSquaresNumbers = grid.getUserSelectedSquaresNumbers();

        if (userSelectedSquaresNumbers.isEmpty()) {
            return Collections.unmodifiableMap(squareAssignments);
        }

        // Save the current state for undo
        undoStack.push(new HashMap<>(squareAssignments));

        // Apply new assignments
        for (Square square : grid.getSquares()) {
            if (userSelectedSquaresNumbers.contains(square.getSquareNumber())) {
                square.setCellId(cellId);
                squareAssignments.put(square.getSquareNumber(), cellId);
            }
        }

        // Clear temporary selection markers and repaint the UI
        grid.clearMouseSelection();
        grid.repaint();

        return Collections.unmodifiableMap(squareAssignments);
    }

    /**
     * Reverts the most recent assignment operation.
     * <p>
     * Restores the assignment map to the previous snapshot and applies the
     * restored values to the grid's square objects, followed by a repaint.
     *
     * @param grid the grid panel whose displayed squares must be updated
     */
    public void undo(SquareGridPanel grid) {
        if (undoStack.isEmpty()) {
            return;
        }

        // Restore prior assignment snapshot
        squareAssignments.clear();
        squareAssignments.putAll(undoStack.pop());

        // Apply restored IDs to the actual square objects
        for (Square square : grid.getSquares()) {
            int cellId = squareAssignments.containsKey(square.getSquareNumber())
                    ? squareAssignments.get(square.getSquareNumber())
                    : 0;
            square.setCellId(cellId);
        }

        grid.repaint();
    }

    /**
     * Clears all assignment and undo state, used whenever the viewer
     * navigates to a different recording.
     */
    public void clear() {
        undoStack.clear();
        squareAssignments.clear();
    }
}