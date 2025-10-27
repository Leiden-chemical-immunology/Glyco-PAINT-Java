package paint.viewer.dialogs;

import paint.viewer.panels.SquareGridPanel;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A dialog window for assigning actions related to a specific cell within a
 * grid-like interface. The dialog allows users to select a cell via radio
 * buttons and perform actions such as assigning, undoing, canceling, or
 * closing the dialog.
 *
 * The selection is visually represented by a colored square alongside
 * radio buttons, where the colors correspond to specific cells. The dialog
 * interacts with a {@link CellAssignmentDialog.Listener} interface for
 * handling user actions.
 *
 * Features of this dialog include:
 * - Modeless behavior to remain accessible alongside the parent frame.
 * - Dynamic creation of cell options based on the number of supported cells.
 * - Visual UI components that include a custom color-coded cell representation
 *   and a grouped selection mechanism with radio buttons.
 * - Scrollable layout for handling a large number of cells.
 *
 * Key Actions:
 * 1. "Assign": Notifies the listener to assign a selected cell ID.
 * 2. "Undo": Notifies the listener to undo the last selection or assignment action.
 * 3. "Cancel": Notifies the listener to cancel the current selection.
 * 4. "Close": Closes the dialog without making changes.
 *
 * Constructor Parameters:
 * - `owner`: The parent frame that owns this dialog.
 * - `listener`: An implementation of the {@code CellAssignmentDialog.Listener} interface
 *   used for event handling when users perform actions in the dialog.
 */
public class CellAssignmentDialog extends JDialog {

    public interface Listener {
        void onAssign(int cellId);

        void onUndo();

        void onCancelSelection();
    }

    private final ButtonGroup group = new ButtonGroup();
    private final Map<JRadioButton, JPanel> squareByRadio = new LinkedHashMap<>();
    // private final Listener listener;

    public CellAssignmentDialog(Frame owner, Listener listener) {
        super(owner, "Assign Cells", false); // 🔹 modeless now
        //this.listener = listener;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        // --- ID 0: No cell ---
        list.add(createCellRow(0, "No cell", Color.GRAY, true));

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

        // @formatter:off
        JPanel buttons    = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton assignBtn = new JButton("Assign");
        JButton undoBtn   = new JButton("Undo");
        JButton cancelBtn = new JButton("Cancel");
        JButton closeBtn  = new JButton("Close");
        // @formatter:on

        assignBtn.addActionListener(e -> listener.onAssign(getSelectedCellId()));
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

        updateVisualSelection(); // initial state
    }

    /**
     * Creates a JPanel that represents a row containing a square indicator and a radio button
     * for selecting a specific cell, along with associated visual and interaction logic.
     *
     * @param cellId the unique identifier for the cell associated with this row
     * @param label the text label to display next to the radio button
     * @param baseColor the base border color for the square indicator
     * @param selected whether the radio button should be pre-selected upon creation
     * @return a JPanel configured with the square indicator and a labeled radio button
     */
    private JPanel createCellRow(int cellId, String label, Color baseColor, boolean selected) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));

        JRadioButton radio = new JRadioButton(label);
        radio.setForeground(Color.BLACK);
        radio.putClientProperty("cellId", cellId);
        if (selected) {
            radio.setSelected(true);
        }
        group.add(radio);

        // Square with only border, not filled
        JPanel square = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(20, 20);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(baseColor);
                g.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
            }
        };
        square.setBackground(Color.WHITE);
        square.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        square.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        squareByRadio.put(radio, square);

        // Sync clicks
        radio.addActionListener(e -> updateVisualSelection());
        square.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                radio.setSelected(true);
                updateVisualSelection();
            }
        });

        // Square first, then radio for alignment
        row.add(square);
        row.add(radio);

        return row;
    }

    /**
     * Retrieves the unique identifier (cell ID) for the currently selected cell
     * based on the selected radio button within the `squareByRadio` group.
     * If no radio button is selected, the method returns 0.
     *
     * @return the cell ID associated with the selected radio button, or 0 if none is selected
     */
    private int getSelectedCellId() {
        for (JRadioButton rb : squareByRadio.keySet()) {
            if (rb.isSelected()) {
                Object v = rb.getClientProperty("cellId");
                if (v instanceof Integer) {
                    return ((Integer) v);
                }
            }
        }
        return 0;
    }

    /**
     * Updates the visual selection of radio buttons and their associated squares in the dialog.
     *
     * Iterates over the entries in the squareByRadio mapping, which associates radio buttons
     * with corresponding JPanel (square) components. For each radio button, it retrieves the
     * cell ID stored in its client property and determines the corresponding color to apply.
     * If the cell ID is 0, a default color is applied; otherwise, a specific color is retrieved
     * for the cell ID. Finally, triggers a repaint of the component to reflect the updates.
     */
    private void updateVisualSelection() {
        for (Map.Entry<JRadioButton, JPanel> e : squareByRadio.entrySet()) {
            JRadioButton rb = e.getKey();

            int id = ((Integer) rb.getClientProperty("cellId"));
            Color base = (id == 0) ? Color.GRAY : SquareGridPanel.getColorForCell(id);
        }
        repaint();
    }

//  Above is the working version, but it does not do anything. Maybe this was meant?
//    private void updateVisualSelection() {
//        for (Map.Entry<JRadioButton, JPanel> e : squareByRadio.entrySet()) {
//            JRadioButton rb = e.getKey();
//            JPanel square = e.getValue();
//
//            int id = (Integer) rb.getClientProperty("cellId");
//            Color base = (id == 0) ? Color.GRAY : SquareGridPanel.getColorForCell(id);
//
//            if (rb.isSelected()) {
//                square.setBackground(base);
//                square.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
//            } else {
//                square.setBackground(Color.WHITE);
//                square.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
//            }
//        }
//        repaint();
//    }
}