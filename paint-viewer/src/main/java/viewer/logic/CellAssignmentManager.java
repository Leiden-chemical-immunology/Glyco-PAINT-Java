package viewer.logic;

import paint.shared.objects.Square;
import viewer.panels.SquareGridPanel;

import java.util.*;

public class CellAssignmentManager {
    private final Map<Integer, Integer> squareAssignments = new HashMap<Integer, Integer>();
    private final Deque<Map<Integer, Integer>> undoStack = new ArrayDeque<Map<Integer, Integer>>();

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