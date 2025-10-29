/******************************************************************************
 *  Class:        CellAssignmentDialog.java
 *  Package:      paint.viewer.dialogs
 *
 *  PURPOSE:
 *    Provides a dialog window for assigning, undoing, or canceling cell
 *    assignments within the PAINT viewer’s square grid interface.
 *
 *  DESCRIPTION:
 *    The dialog presents a scrollable list of available cell IDs, each
 *    represented by a color-coded square and a radio button label.
 *    The user can:
 *      • Select a cell ID and assign it to the current selection.
 *      • Undo the most recent cell assignment.
 *      • Cancel the current selection.
 *      • Close the dialog window.
 *
 *    The dialog interacts with {@link CellAssignmentDialog.Listener}, allowing
 *    external components (e.g., the viewer frame) to handle user actions.
 *    It operates modelessly, remaining accessible while the viewer remains active.
 *
 *  KEY FEATURES:
 *    • Scrollable, color-coded list of cell options.
 *    • Modeless behavior for fluid interaction with the main viewer.
 *    • Integrated event callback through {@link Listener}.
 *    • Intuitive visual feedback for selected cell options.
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
 * <p>The selection is visually represented by a colored square alongside
 * radio buttons, where the colors correspond to specific cells. The dialog
 * interacts with a {@link CellAssignmentDialog.Listener} interface for
 * handling user actions.</p>
 *
 * <p>Features of this dialog include:</p>
 * <ul>
 *   <li>Modeless behavior to remain accessible alongside the parent frame.</li>
 *   <li>Dynamic creation of cell options based on the number of supported cells.</li>
 *   <li>Visual UI components with color-coded squares and radio buttons.</li>
 *   <li>Scrollable layout for handling a large number of cells.</li>
 * </ul>
 */
public class CellAssignmentDialog extends JDialog {

    /** Listener interface for handling cell assignment actions. */
    public interface Listener {
        void onAssign(int cellId);
        void onUndo();
        void onCancelSelection();
    }

    private final ButtonGroup group = new ButtonGroup();
    private final Map<JRadioButton, JPanel> squareByRadio = new LinkedHashMap<>();

    /**
     * Constructs a modeless dialog for assigning cells to selected regions
     * within the viewer.
     *
     * @param owner    the parent frame owning this dialog
     * @param listener a {@link Listener} implementation to handle user actions
     */
    public CellAssignmentDialog(Frame owner, Listener listener) {
        super(owner, "Assign Cells", false);
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
        updateVisualSelection();
    }

    /**
     * Creates a panel row with a colored square and an associated radio button
     * for selecting a particular cell.
     *
     * @param cellId   unique identifier for the cell
     * @param label    descriptive label for the radio button
     * @param baseColor the color to display for this cell
     * @param selected whether this option should be preselected
     * @return a configured JPanel containing the radio button and color square
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

        radio.addActionListener(e -> updateVisualSelection());
        square.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                radio.setSelected(true);
                updateVisualSelection();
            }
        });

        row.add(square);
        row.add(radio);
        return row;
    }

    /**
     * Retrieves the cell ID associated with the selected radio button.
     *
     * @return the selected cell ID, or 0 if no selection is active
     */
    private int getSelectedCellId() {
        for (JRadioButton rb : squareByRadio.keySet()) {
            if (rb.isSelected()) {
                Object v = rb.getClientProperty("cellId");
                if (v instanceof Integer) {
                    return (Integer) v;
                }
            }
        }
        return 0;
    }

    /**
     * Updates the visual appearance of the colored squares to reflect the
     * currently selected radio button.
     */
    private void updateVisualSelection() {
        for (Map.Entry<JRadioButton, JPanel> e : squareByRadio.entrySet()) {
            JRadioButton rb = e.getKey();
            JPanel square = e.getValue();
            int id = (Integer) rb.getClientProperty("cellId");
            Color base = (id == 0) ? Color.GRAY : SquareGridPanel.getColorForCell(id);

            if (rb.isSelected()) {
                square.setBackground(base);
                square.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            } else {
                square.setBackground(Color.WHITE);
                square.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
            }
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