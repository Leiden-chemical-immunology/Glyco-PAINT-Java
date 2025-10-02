package viewer;

import javax.swing.*;
import java.awt.*;

public class CellAssignmentDialog extends JDialog {
    private int selectedCell = 1;
    private final Color[] cellColors = {
            Color.RED, Color.GREEN, Color.BLUE,
            Color.MAGENTA, Color.ORANGE, Color.CYAN
    };

    public interface Listener {
        void onAssign(int cellId);
        void onUndo();
        void onCancelSelection();
    }

    public CellAssignmentDialog(Frame owner, Listener listener) {
        super(owner, "Assign Cells", false);
        setLayout(new BorderLayout());

        // --- Radio buttons for cell choice ---
        JPanel cellPanel = new JPanel(new GridLayout(1, 6));
        ButtonGroup group = new ButtonGroup();
        for (int i = 1; i <= 6; i++) {
            JRadioButton btn = new JRadioButton("Cell " + i);
            btn.setForeground(cellColors[i - 1]);
            if (i == 1) btn.setSelected(true);
            final int id = i;
            btn.addActionListener(e -> selectedCell = id);
            group.add(btn);
            cellPanel.add(btn);
        }
        add(cellPanel, BorderLayout.NORTH);

        // --- Control buttons ---
        JPanel buttonPanel = new JPanel();
        JButton assignBtn = new JButton("Assign");
        assignBtn.addActionListener(e -> listener.onAssign(selectedCell));

        JButton undoBtn = new JButton("Undo");
        undoBtn.addActionListener(e -> listener.onUndo());

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> listener.onCancelSelection());

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());

        buttonPanel.add(assignBtn);
        buttonPanel.add(undoBtn);
        buttonPanel.add(cancelBtn);
        buttonPanel.add(closeBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }
}