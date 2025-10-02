package viewer;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class CellAssignmentDialog extends JDialog {

    public interface Listener {
        void onAssign(int cellId);
        void onUndo();
        void onCancelSelection();
    }

    private final ButtonGroup group = new ButtonGroup();
    private final Map<JRadioButton, JPanel> squareByRadio = new LinkedHashMap<>();
    private final Listener listener;

    public CellAssignmentDialog(Frame owner, Listener listener) {
        super(owner, "Assign Cells", false);
        this.listener = listener;
        setLayout(new BorderLayout());

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        // --- ID 0: Do not assign ---
        list.add(createCellRow(0, "Do not assign to cell (ID 0)", Color.GRAY, true));

        // --- IDs 1..N vertically ---
        int cellCount = SquareGridPanel.getSupportedCellCount();
        for (int id = 1; id <= cellCount; id++) {
            Color c = SquareGridPanel.getColorForCell(id);
            list.add(createCellRow(id, "Assign to cell " + id, c, false));
        }

        JScrollPane scroll = new JScrollPane(list,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);

        // --- Controls ---
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton assignBtn = new JButton("Assign");
        JButton undoBtn = new JButton("Undo");
        JButton cancelBtn = new JButton("Cancel");
        JButton closeBtn = new JButton("Close");

        assignBtn.addActionListener(e -> {
            int selected = getSelectedCellId();
            listener.onAssign(selected);
        });
        undoBtn.addActionListener(e -> listener.onUndo());
        cancelBtn.addActionListener(e -> listener.onCancelSelection());
        closeBtn.addActionListener(e -> dispose());

        buttons.add(assignBtn);
        buttons.add(undoBtn);
        buttons.add(cancelBtn);
        buttons.add(closeBtn);
        add(buttons, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);

        // Ensure initial visual state applied
        updateVisualSelection();
    }

    private JPanel createCellRow(int cellId, String label, Color baseColor, boolean selected) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));

        // Black text only
        JRadioButton radio = new JRadioButton(label);
        radio.setForeground(Color.BLACK);
        radio.putClientProperty("cellId", Integer.valueOf(cellId));
        if (selected) radio.setSelected(true);
        group.add(radio);

        // Small square showing the color; clickable
        JPanel square = new JPanel() {
            @Override public Dimension getPreferredSize() { return new Dimension(20, 20); }
        };
        square.setBackground(baseColor);
        square.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        square.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        squareByRadio.put(radio, square);

        // Sync clicks
        radio.addActionListener(e -> updateVisualSelection());
        square.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                radio.setSelected(true);
                updateVisualSelection();
            }
        });

        row.add(radio);
        row.add(square);
        return row;
    }

    private int getSelectedCellId() {
        for (JRadioButton rb : squareByRadio.keySet()) {
            if (rb.isSelected()) {
                Object v = rb.getClientProperty("cellId");
                if (v instanceof Integer) return ((Integer) v).intValue();
            }
        }
        // Fallback: none
        return 0;
    }

    private void updateVisualSelection() {
        for (Map.Entry<JRadioButton, JPanel> e : squareByRadio.entrySet()) {
            JRadioButton rb = e.getKey();
            JPanel sq = e.getValue();

            int id = ((Integer) rb.getClientProperty("cellId")).intValue();
            Color base = (id == 0) ? Color.GRAY : SquareGridPanel.getColorForCell(id);

            if (rb.isSelected()) {
                sq.setBackground(lightenTowardWhite(base, 0.35));
                sq.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2)); // thicker border when selected
            } else {
                sq.setBackground(base);
                sq.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
            }
        }
        repaint();
    }

    private static Color lightenTowardWhite(Color c, double factor) {
        // factor in [0,1]; 0 => original, 1 => white
        int r = (int) Math.round(c.getRed()   + (255 - c.getRed())   * factor);
        int g = (int) Math.round(c.getGreen() + (255 - c.getGreen()) * factor);
        int b = (int) Math.round(c.getBlue()  + (255 - c.getBlue())  * factor);
        return new Color(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }
}