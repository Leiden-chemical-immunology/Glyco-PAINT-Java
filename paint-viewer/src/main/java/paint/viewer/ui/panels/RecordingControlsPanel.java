/*==============================================================================
 *  Class:        RecordingControlsPanel.java
 *  Package:      paint.viewer.ui.panels
 *
 *  PURPOSE:
 *    Provides a control panel for managing recording-related actions and
 *    grid display options in the PAINT viewer. Supports filtering, square
 *    selection, cell assignment, playback, data export, and visualization
 *    toggling.
 *
 *  DESCRIPTION:
 *    The panel organizes a structured set of user controls into three groups:
 *      1) Recording and grid action buttons (Filter, Select, Assign, Play,
 *         Export, Show Squares)
 *      2) Visualization toggles for borders and shading
 *      3) Number display mode options (None, Label, Square)
 *
 *    A {@link RecordingsControlListener} interface defines callbacks for all interactive
 *    controls, allowing the parent viewer frame to handle the actions.
 *    Layout is vertical for readability, with consistent sizing and spacing
 *    across buttons and toggle groups.
 *
 *  KEY FEATURES:
 *    • Centralized control panel for recording actions and display settings.
 *    • Live toggles for borders, shading, and number-display modes.
 *    • Listener interface for flexible integration with viewer logic.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
==============================================================================*/

package paint.viewer.ui.panels;

import javax.swing.*;
import java.awt.*;

/**
 * A user interface panel containing interactive controls for managing recordings
 * and grid visualization settings within the PAINT viewer.
 */
public class RecordingControlsPanel {

    /**
     * Defines callback methods for handling user actions triggered
     * by buttons or toggle components on this panel.
     */
    public interface RecordingsControlListener {
        void onFilterRequested();                                             // Called when the user requests to filter the recording list
        void onSelectSquaresRequested();                                      // Called when the user opens the square-selection dialog
        void onAssignCellsRequested();                                        // Called when the user opens the cell-assignment dialog
        void onPlayRecordingRequested();                                      // Called when the user requests to play the current recording
        void onExportLeftImageRequested();                                    // Called when the user requests to export the left grid image
        void onShowSquaresRequested();                                        // Called when the user requests to view the squares CSV
        void onBordersToggled(boolean showBorders);                           // Triggered when borders are toggled on or off
        void onShadingToggled(boolean showShading);                           // Triggered when shading is toggled on or off
        void onNumberModeChanged(SquareGridPanel.NumberMode mode);            // Called when the numeric display mode (None, Label, Square) is changed
        void onExcludeToggleRequested();                                      // Called when the Exclude/Include button is pressed
    }

    private final JPanel  root;
    private final JButton filterRecordingsButton;
    private final JButton selectSquaresButton;
    private final JButton assignCellsButton;
    private final JButton playRecordingButton;
    private final JButton exportImageButton;
    private final JButton showSquaresButton;
    private final JButton excludeButton;

    /**
     * Constructs a {@code RecordingControlsPanel} containing action buttons,
     * visualization toggles, and numbering mode options. All actions are
     * delegated to the provided {@link RecordingsControlListener}.
     *
     * @param recordingsControlListener the recordingsControlListener that receives callbacks when user actions occur
     */
    public RecordingControlsPanel(final RecordingsControlListener recordingsControlListener) {

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
        showSquaresButton      = new JButton("Show Squares Data");
        excludeButton          = new JButton("Exclude Recording");

        for (JButton button : new JButton[]{
                filterRecordingsButton,
                selectSquaresButton,
                assignCellsButton,
                playRecordingButton,
                exportImageButton,
                showSquaresButton,
                excludeButton
        }) {
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
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
        controls.add(Box.createVerticalStrut(10));
        controls.add(excludeButton);

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

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(none);
        buttonGroup.add(label);
        buttonGroup.add(square);

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
        filterRecordingsButton.addActionListener(e -> recordingsControlListener.onFilterRequested());
        selectSquaresButton.addActionListener(   e -> recordingsControlListener.onSelectSquaresRequested());
        assignCellsButton.addActionListener(     e -> recordingsControlListener.onAssignCellsRequested());
        playRecordingButton.addActionListener(   e -> recordingsControlListener.onPlayRecordingRequested());
        exportImageButton.addActionListener(     e -> recordingsControlListener.onExportLeftImageRequested());
        showSquaresButton.addActionListener(     e -> recordingsControlListener.onShowSquaresRequested());
        excludeButton.addActionListener(         e -> recordingsControlListener.onExcludeToggleRequested());

        showBorders.addActionListener(           e -> recordingsControlListener.onBordersToggled(showBorders.isSelected()));
        showShading.addActionListener(           e -> recordingsControlListener.onShadingToggled(showShading.isSelected()));
        none.addActionListener(                  e -> recordingsControlListener.onNumberModeChanged(SquareGridPanel.NumberMode.NONE));
        label.addActionListener(                 e -> recordingsControlListener.onNumberModeChanged(SquareGridPanel.NumberMode.LABEL));
        square.addActionListener(                e -> recordingsControlListener.onNumberModeChanged(SquareGridPanel.NumberMode.SQUARE));

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

    /**
     * Enables or disables all action buttons on the panel.
     * <p>
     * This is used to disable user interaction while modal dialogs
     * are open or while long-running operations are in progress.
     *
     * @param enabled {@code true} to enable all action buttons,
     *                {@code false} to disable them
     */
    public void setButtonsEnabled(boolean enabled) {
        filterRecordingsButton.setEnabled(enabled);
        selectSquaresButton.setEnabled(enabled);
        assignCellsButton.setEnabled(enabled);
        playRecordingButton.setEnabled(enabled);
        exportImageButton.setEnabled(enabled);
        showSquaresButton.setEnabled(enabled);
        excludeButton.setEnabled(enabled);
    }

    /**
     * Updates the text and appearance of the exclusion toggle button
     * based on the current recording's exclusion status.
     *
     * @param excluded {@code true} if the recording is currently excluded,
     *                 {@code false} otherwise
     */
    public void setExcludeButtonText(boolean excluded) {
        if (excludeButton == null) {
            return;
        }

        excludeButton.setText(excluded ? "Include Recording" : "Exclude Recording");
        Color defaultFg = UIManager.getColor("Button.foreground");
        if (defaultFg == null) {
            defaultFg = Color.BLACK;
        }

        excludeButton.setForeground(excluded ? Color.RED : defaultFg);
    }
}