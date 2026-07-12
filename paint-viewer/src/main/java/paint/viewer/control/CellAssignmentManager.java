/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

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

    /** Tracks whether this is the first assignment action since switching recordings. */
    private boolean firstAssignmentForRecording = true;

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

    /**
     * Sets whether this is the first assignment for the current recording.
     *
     * @param firstAssignmentForRecording {@code true} if first assignment
     */
    public void setFirstAssignmentForRecording(boolean firstAssignmentForRecording) {
        this.firstAssignmentForRecording = firstAssignmentForRecording;
    }

    /**
     * @return {@code true} if this is the first assignment for the recording.
     */
    public boolean isFirstAssignmentForRecording() {
        return firstAssignmentForRecording;
    }
}