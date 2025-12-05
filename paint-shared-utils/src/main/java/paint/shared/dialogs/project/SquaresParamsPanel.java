// =================================================================================================
//  File: src/main/java/paint/shared/dialogs/project/SquaresParamsPanel.java
// =================================================================================================

/* =================================================================================================
 *  PURPOSE
 *      UI panel for displaying and editing parameters related to the "Generate Squares" step.
 *      Includes number of squares, minimum R², density ratio, and variability. In TRACKMATE mode,
 *      also offers an option to run Generate Squares automatically after TrackMate finishes.
 *
 *  DESCRIPTION
 *      This panel is constructed using GridBagLayout and exposes a callback (onParamsChanged)
 *      that the controller can register to be notified whenever any parameter changes.
 *
 *      GridBagLayout as a spreadsheet with rows and columns
 *
 *      | col0 | col1 |
 *      |------|------|
 *      | row0 | row0 |
 *      | row1 | row1 |
 *      | row2 | row2 |
 *
 *      Parameters are initialized from PaintConfig and can be persisted back using persistTo().
 *      For TrackMate mode, enabling/disabling of squares parameters is tied to a checkbox that
 *      controls whether Generate Squares should run after TrackMate.
 *
 *  KEY FEATURES
 *      - Supports two modes: TRACKMATE (shows checkbox) and VIEWER (no checkbox).
 *      - Automatic persistence to PaintConfig.
 *      - Document listener and combo box listener fire a single onChange callback.
 *      - Clear separation of UI creation, enable/disable logic, and persistence logic.
 *
 *  AUTHOR
 *      PAINT Toolkit
 *
 *  MODULE
 *      paint.shared.dialogs.project
 *
 *  UPDATED
 *      2025-11-21
 *
 *  COPYRIGHT
 *      Copyright (c) 2020–2025.
 *      All rights reserved.
 * =================================================================================================
 */

package paint.shared.dialogs.project;

import paint.shared.config.paintconfig.PaintConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

import static paint.shared.constants.PaintStringConstants.NUMBER_OF_SQUARES_IN_RECORDING;
import static paint.shared.constants.PaintStringConstants.MIN_REQUIRED_R_SQUARED;
import static paint.shared.constants.PaintStringConstants.MIN_REQUIRED_DENSITY_RATIO;
import static paint.shared.constants.PaintStringConstants.MAX_ALLOWABLE_VARIABILITY;
import static paint.shared.constants.PaintStringConstants.GENERATE_SQUARES;
import static paint.shared.constants.PaintStringConstants.TRACKMATE;
import static paint.shared.constants.PaintStringConstants.RUN_GENERATE_SQUARES_AFTER;

import static paint.shared.dialogs.ProjectDialog.DialogMode;

/**
 * Panel that displays all the configuration parameters required for the "Generate Squares" step.
 * In TRACKMATE mode, a checkbox allows configuring whether to run Generate Squares automatically
 * after TrackMate processing. The panel allows changes, exposes a callback for any parameter
 * modification, and supports persistence of these settings back to PaintConfig.
 */
public class SquaresParamsPanel {

    private final JPanel            panel = new JPanel(new GridBagLayout());
    private       JCheckBox         runAfterTrackMate;
    private final JComboBox<String> gridSizeCombo;
    private final JTextField        minRSqField;
    private final JTextField        minDensityField;
    private final JTextField        maxVariabilityField;

    private       JLabel            gridSizeLabel;
    private       JLabel            minRSqLabel;
    private       JLabel            minDensityLabel;
    private       JLabel            maxVariabilityLabel;

    private       Color             normalLabelColor;
    private final Color             disabledLabelColor = Color.GRAY;

    // Callback triggered whenever any user-editable parameter changes.
    private Runnable onChange = () -> {
    };

    /**
     * Constructs the panel and initializes fields based on PaintConfig and dialog mode.
     */
    public SquaresParamsPanel(DialogMode mode) {

        panel.setBorder(new TitledBorder("Generate Squares Parameters"));
        final GridBagConstraints pg = new GridBagConstraints();
        pg.insets  = new Insets(5,5,5,5);
        pg.anchor  = GridBagConstraints.WEST;
        pg.fill    = GridBagConstraints.NONE;

        // Load defaults from configuration
        int nrSquares   = PaintConfig.getInt(   GENERATE_SQUARES, NUMBER_OF_SQUARES_IN_RECORDING, 400);
        double minRSq   = PaintConfig.getDouble(GENERATE_SQUARES, MIN_REQUIRED_R_SQUARED,         0.1);
        double minDens  = PaintConfig.getDouble(GENERATE_SQUARES, MIN_REQUIRED_DENSITY_RATIO,     2.0);
        double maxVar   = PaintConfig.getDouble(GENERATE_SQUARES, MAX_ALLOWABLE_VARIABILITY,      10.0);

        int row = 0;

        // TRACKMATE mode: checkbox to run Generate Squares after TrackMate completes
        if (mode == DialogMode.TRACKMATE) {
            runAfterTrackMate = new JCheckBox(
                    "Run Generate Squares after TrackMate",
                    PaintConfig.getBoolean(TRACKMATE, RUN_GENERATE_SQUARES_AFTER, true)
            );
            pg.gridx = 0; pg.gridy = row; pg.gridwidth = 2;
            panel.add(runAfterTrackMate, pg);
            row++;
            pg.gridwidth = 1;

            runAfterTrackMate.addActionListener(e -> {
                setSquaresEnabled(runAfterTrackMate.isSelected());
                onChange.run();
            });
        }

        final Dimension labelSize = new Dimension(220, 20);
        final Dimension fieldSize = new Dimension(80, 24);

        pg.gridx = 0;
        pg.gridy = row;
        gridSizeLabel = label(panel, NUMBER_OF_SQUARES_IN_RECORDING, labelSize, pg);
        pg.gridx = 1;
        gridSizeCombo = new JComboBox<>(new String[]{"5x5", "10x10", "15x15", "20x20", "25x25", "30x30", "35x35", "40x40"});
        int n = (int) Math.sqrt(nrSquares);
        gridSizeCombo.setSelectedItem(n + "x" + n);
        panel.add(gridSizeCombo, pg);
        row++;

        // Min R²
        pg.gridx = 0;
        pg.gridy = row;
        minRSqLabel = label(panel, "Min Required R²", labelSize, pg);
        pg.gridx = 1;
        minRSqField = text(String.valueOf(minRSq), fieldSize);
        panel.add(minRSqField, pg);
        row++;

        // Min Density Ratio
        pg.gridx = 0;
        pg.gridy = row;
        minDensityLabel = label(panel, MIN_REQUIRED_DENSITY_RATIO, labelSize, pg);
        pg.gridx = 1;
        minDensityField = text(String.valueOf(minDens), fieldSize);
        panel.add(minDensityField, pg);
        row++;

        // Max Variability
        pg.gridx = 0;
        pg.gridy = row;
        maxVariabilityLabel = label(panel, MAX_ALLOWABLE_VARIABILITY, labelSize, pg);
        pg.gridx = 1;
        maxVariabilityField = text(String.valueOf(maxVar), fieldSize);
        panel.add(maxVariabilityField, pg);

        normalLabelColor = gridSizeLabel.getForeground();

        // Change listeners for all controls (REPLACED WITH METHOD REFERENCES)
        gridSizeCombo.addActionListener(this::handleChange);
        minRSqField.getDocument().addDocumentListener((SimpleDocumentListener) this::handleChange);
        minDensityField.getDocument().addDocumentListener((SimpleDocumentListener) this::handleChange);
        maxVariabilityField.getDocument().addDocumentListener((SimpleDocumentListener) this::handleChange);

        // Initial enable state for controls in TRACKMATE mode
        if (mode == DialogMode.TRACKMATE) {
            setSquaresEnabled(runAfterTrackMate.isSelected());
        }
    }

    /**
     * Returns the Swing component associated with this panel.
     */
    public JPanel component() {
        return panel;
    }

    /**
     * Registers a callback that fires whenever any parameter changes.
     * If null is passed, it resets to a no-op callback.
     */
    public void onParamsChanged(Runnable runnable) {
        this.onChange = (runnable != null ? runnable : () -> {
        });
    }

    /**
     * Enables or disables the panel. When in TRACKMATE mode, the Generate Squares fields
     * respect both the main enabled flag and the state of the "Run after TrackMate" checkbox.
     */
    public void setEnabled(boolean enabled) {

        if (runAfterTrackMate != null) {
            runAfterTrackMate.setEnabled(enabled);
        }

        boolean squaresEnabled = enabled && (runAfterTrackMate == null || runAfterTrackMate.isSelected());

        setSquaresEnabled(squaresEnabled);

        gridSizeLabel.setForeground(squaresEnabled ? normalLabelColor : disabledLabelColor);
        minRSqLabel.setForeground(squaresEnabled ? normalLabelColor : disabledLabelColor);
        minDensityLabel.setForeground(squaresEnabled ? normalLabelColor : disabledLabelColor);
        maxVariabilityLabel.setForeground(squaresEnabled ? normalLabelColor : disabledLabelColor);
    }

    /**
     * Persists the current field values to PaintConfig and saves the configuration.
     */
    public void persistTo(DialogMode mode) {
        if (gridSizeCombo != null) {
            String selectedItem = (String) gridSizeCombo.getSelectedItem();
            if (selectedItem != null && selectedItem.contains("x")) {
                int side = Integer.parseInt(selectedItem.split("x")[0].trim());
                PaintConfig.setInt(GENERATE_SQUARES, NUMBER_OF_SQUARES_IN_RECORDING, side * side);
            }
        }
        if (minRSqField != null) {
            PaintConfig.setDouble(GENERATE_SQUARES, MIN_REQUIRED_R_SQUARED,
                                  parseDouble(minRSqField.getText(), 0.1));
        }
        if (minDensityField != null) {
            PaintConfig.setDouble(GENERATE_SQUARES, MIN_REQUIRED_DENSITY_RATIO,
                                  parseDouble(minDensityField.getText(), 2.0));
        }
        if (maxVariabilityField != null) {
            PaintConfig.setDouble(GENERATE_SQUARES, MAX_ALLOWABLE_VARIABILITY ,
                                  parseDouble(maxVariabilityField.getText(), 10.0));
        }
        if (mode == DialogMode.TRACKMATE && runAfterTrackMate != null) {
            PaintConfig.setBoolean(TRACKMATE, RUN_GENERATE_SQUARES_AFTER, runAfterTrackMate.isSelected());
        }
        PaintConfig.instance().save();
    }

    /**
     * Adds a fixed-size label into the GridBag container.
     */
    private static JLabel label(JPanel jPanel, String text, Dimension size, GridBagConstraints pg) {
        JLabel jLabel = new JLabel(text);
        jLabel.setPreferredSize(size);
        jPanel.add(jLabel, pg);
        return jLabel;
    }

    /**
     * Builds a fixed-size text field initialized to a given value.
     */
    private static JTextField text(String v, Dimension size) {
        JTextField jTextField = new JTextField(v);
        jTextField.setColumns(8);
        jTextField.setPreferredSize(size);
        return jTextField;
    }

    /**
     * Enables or disables all the Generate Squares fields (grid size, R², density, variability).
     */
    public void setSquaresEnabled(boolean enabled) {
        gridSizeCombo.setEnabled(enabled);
        minRSqField.setEnabled(enabled);
        minDensityField.setEnabled(enabled);
        maxVariabilityField.setEnabled(enabled);

        gridSizeLabel.setForeground(enabled ? normalLabelColor : disabledLabelColor);
        minRSqLabel.setForeground(enabled ? normalLabelColor : disabledLabelColor);
        minDensityLabel.setForeground(enabled ? normalLabelColor : disabledLabelColor);
        maxVariabilityLabel.setForeground(enabled ? normalLabelColor : disabledLabelColor);
    }

    /**
     * Parses a double from string with fallback to a default on error.
     */
    private static double parseDouble(String doubleString, double def) {
        try {
            return Double.parseDouble(doubleString.trim());
        } catch (Exception e) {
            return def;
        }
    }

    // ------------------------------------------------------------------------------------
    // Added overloaded event handlers for method references (NO formatting changed above)
    // ------------------------------------------------------------------------------------

    private void handleChange(java.awt.event.ActionEvent e) {
        onChange.run();
    }

    private void handleChange(javax.swing.event.DocumentEvent e) {
        onChange.run();
    }

    public boolean isRunAfterTrackMateSelected() {
        return runAfterTrackMate != null && runAfterTrackMate.isSelected();
    }
}