/******************************************************************************
 *  Class:        RecordingControlsPanel.java
 *  Package:      paint.viewer.panels
 *
 *  PURPOSE:
 *    Provides a control panel for managing recordings and display options
 *    in the PAINT viewer. Enables actions such as filtering, square selection,
 *    cell assignment, playback, and visualization toggling.
 *
 *  DESCRIPTION:
 *    The panel offers grouped user interface controls for interacting with
 *    recording data and the square grid display. It supports both functional
 *    controls (filter, select, assign, play) and visual toggles (borders,
 *    shading, numbering modes).
 *
 *    A {@link Listener} interface defines callback methods for handling
 *    user-triggered events such as filtering or display state changes.
 *    The panel layout uses vertical grouping for clarity and accessibility.
 *
 *  KEY FEATURES:
 *    • Centralized control panel for recording-related actions.
 *    • Live toggles for borders, shading, and numeric label modes.
 *    • Integrated listener interface for external event handling.
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

package paint.viewer.panels;

import paint.viewer.shared.SquareControlParams;

import javax.swing.*;
import java.awt.*;

/**
 * A user interface panel containing interactive controls for managing recordings
 * and grid visualization options within the PAINT viewer.
 */
public class RecordingControlsPanel {

    /**
     * Defines callback methods for handling user actions triggered by this panel.
     */
    public interface Listener {
        void onFilterRequested();
        void onSelectSquaresRequested();
        void onAssignCellsRequested();
        void onPlayRecordingRequested();
        void onExportLeftImageRequested();
        void onShowSquaresRequested();

        void onBordersToggled(boolean showBorders);
        void onShadingToggled(boolean showShading);
        void onNumberModeChanged(SquareGridPanel.NumberMode mode);
        void onApplySquareControl(String scope, SquareControlParams params);
    }

    private final JPanel  root;
    private       JButton filterRecordingsButton;
    private       JButton selectSquaresButton;
    private       JButton assignCellsButton;
    private       JButton playRecordingButton;
    private       JButton exportImageButton;
    private       JButton showSquaresButton;

    /**
     * Constructs a {@code RecordingControlsPanel} providing buttons, checkboxes,
     * and radio buttons for managing recording and grid behavior.
     *
     * @param listener the {@link Listener} that handles user actions
     */
    public RecordingControlsPanel(final Listener listener) {

        root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        root.setPreferredSize(new Dimension(240, 0));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // === Action buttons ===
        filterRecordingsButton = new JButton("Filter Recordings");
        selectSquaresButton    = new JButton("Select Squares");
        assignCellsButton      = new JButton("Assign Cells");
        playRecordingButton    = new JButton("Play Recording");
        exportImageButton      = new JButton("Export Image");
        showSquaresButton      = new JButton("Show Squares");

        for (JButton b : new JButton[]{
                filterRecordingsButton,
                selectSquaresButton,
                assignCellsButton,
                playRecordingButton,
                exportImageButton,
                showSquaresButton
        }) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        }

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBorder(BorderFactory.createTitledBorder("Controls"));
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, controls.getPreferredSize().height));

        controls.add(filterRecordingsButton);
        controls.add(Box.createVerticalStrut(10));
        controls.add(selectSquaresButton);
        controls.add(Box.createVerticalStrut(10));
        controls.add(assignCellsButton);
        controls.add(Box.createVerticalStrut(10));
        controls.add(playRecordingButton);
        controls.add(Box.createVerticalStrut(10));
        controls.add(exportImageButton);
        controls.add(Box.createVerticalStrut(10));
        controls.add(showSquaresButton);

        // === Borders and shading toggles ===
        JCheckBox showBorders = new JCheckBox("Show borders", true);
        JCheckBox showShading = new JCheckBox("Show shading", true);

        JPanel borders = new JPanel();
        borders.setLayout(new BoxLayout(borders, BoxLayout.Y_AXIS));
        borders.setBorder(BorderFactory.createTitledBorder("Borders and Shading"));
        borders.setAlignmentX(Component.LEFT_ALIGNMENT);
        borders.setMaximumSize(new Dimension(Integer.MAX_VALUE, borders.getPreferredSize().height));
        borders.add(showBorders);
        borders.add(Box.createVerticalStrut(5));
        borders.add(showShading);

        // === Number display options ===
        JRadioButton none   = new JRadioButton("None", true);
        JRadioButton label  = new JRadioButton("Label");
        JRadioButton square = new JRadioButton("Square");

        ButtonGroup g = new ButtonGroup();
        g.add(none);
        g.add(label);
        g.add(square);

        JPanel numbers = new JPanel();
        numbers.setLayout(new BoxLayout(numbers, BoxLayout.Y_AXIS));
        numbers.setBorder(BorderFactory.createTitledBorder("Numbers"));
        numbers.setAlignmentX(Component.LEFT_ALIGNMENT);
        numbers.setMaximumSize(new Dimension(Integer.MAX_VALUE, numbers.getPreferredSize().height));
        numbers.add(none);
        numbers.add(Box.createVerticalStrut(5));
        numbers.add(label);
        numbers.add(Box.createVerticalStrut(5));
        numbers.add(square);

        // === Action listeners ===
        filterRecordingsButton.addActionListener(e -> listener.onFilterRequested());
        selectSquaresButton.addActionListener(   e -> listener.onSelectSquaresRequested());
        assignCellsButton.addActionListener(     e -> listener.onAssignCellsRequested());
        playRecordingButton.addActionListener(   e -> listener.onPlayRecordingRequested());
        exportImageButton.addActionListener(     e -> listener.onExportLeftImageRequested());
        showSquaresButton.addActionListener(     e -> listener.onShowSquaresRequested());

        showBorders.addActionListener(           e -> listener.onBordersToggled(showBorders.isSelected()));
        showShading.addActionListener(           e -> listener.onShadingToggled(showShading.isSelected()));
        none.addActionListener(                  e -> listener.onNumberModeChanged(SquareGridPanel.NumberMode.NONE));
        label.addActionListener(                 e -> listener.onNumberModeChanged(SquareGridPanel.NumberMode.LABEL));
        square.addActionListener(                e -> listener.onNumberModeChanged(SquareGridPanel.NumberMode.SQUARE));

        // === Layout order ===
        content.add(controls);
        content.add(Box.createVerticalStrut(10));
        content.add(borders);
        content.add(Box.createVerticalStrut(10));
        content.add(numbers);
        content.add(Box.createVerticalGlue());

        root.add(content, BorderLayout.NORTH);
    }

    /**
     * Returns the root Swing component representing this panel.
     *
     * @return the root {@link JComponent}
     */
    public JComponent getComponent() {
        return root;
    }

    public void setButtonsEnabled(boolean enabled) {
        filterRecordingsButton.setEnabled(enabled);
        selectSquaresButton.setEnabled(enabled);
        assignCellsButton.setEnabled(enabled);
        playRecordingButton.setEnabled(enabled);
        exportImageButton.setEnabled(enabled);
    }
}