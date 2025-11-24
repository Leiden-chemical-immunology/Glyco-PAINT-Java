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
        int nrSquares   = PaintConfig.getInt(   "Generate Squares", "Number of Squares in Recording", 400);
        double minRSq   = PaintConfig.getDouble("Generate Squares", "Min Required R Squared",         0.1);
        double minDens  = PaintConfig.getDouble("Generate Squares", "Min Required Density Ratio",     2.0);
        double maxVar   = PaintConfig.getDouble("Generate Squares", "Max Allowable Variability",      10.0);

        int row = 0;

        // TRACKMATE mode: checkbox to run Generate Squares after TrackMate completes
        if (mode == DialogMode.TRACKMATE) {
            runAfterTrackMate = new JCheckBox(
                    "Run Generate Squares after TrackMate",
                    PaintConfig.getBoolean("TrackMate", "Run Generate Squares After", true)
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
        label(panel, "Number of Squares in Recording", labelSize, pg);
        pg.gridx = 1;
        gridSizeCombo = new JComboBox<>(new String[]{"5x5", "10x10", "15x15", "20x20", "25x25", "30x30", "35x35", "40x40"});
        int n = (int) Math.sqrt(nrSquares);
        gridSizeCombo.setSelectedItem(n + "x" + n);
        panel.add(gridSizeCombo, pg);
        row++;

        // Min R²
        pg.gridx = 0;
        pg.gridy = row;
        label(panel, "Min Required R²", labelSize, pg);
        pg.gridx = 1;
        minRSqField = text(String.valueOf(minRSq), fieldSize);
        panel.add(minRSqField, pg);
        row++;

        // Min Density Ratio
        pg.gridx = 0;
        pg.gridy = row;
        label(panel, "Min Required Density Ratio", labelSize, pg);
        pg.gridx = 1;
        minDensityField = text(String.valueOf(minDens), fieldSize);
        panel.add(minDensityField, pg);
        row++;

        // Max Variability
        pg.gridx = 0;
        pg.gridy = row;
        label(panel, "Max Allowable Variability", labelSize, pg);
        pg.gridx = 1;
        maxVariabilityField = text(String.valueOf(maxVar), fieldSize);
        panel.add(maxVariabilityField, pg);

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
    public void onParamsChanged(Runnable r) {
        this.onChange = (r != null ? r : () -> {
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
        gridSizeCombo.setEnabled(squaresEnabled);
        minRSqField.setEnabled(squaresEnabled);
        minDensityField.setEnabled(squaresEnabled);
        maxVariabilityField.setEnabled(squaresEnabled);
    }

    /**
     * Persists the current field values to PaintConfig and saves the configuration.
     */
    public void persistTo(DialogMode mode) {
        if (gridSizeCombo != null) {
            String sel = (String) gridSizeCombo.getSelectedItem();
            if (sel != null && sel.contains("x")) {
                int side = Integer.parseInt(sel.split("x")[0].trim());
                PaintConfig.setInt("Generate Squares", "Number of Squares in Recording", side * side);
            }
        }
        if (minRSqField != null) {
            PaintConfig.setDouble("Generate Squares", "Min Required R Squared",
                                  parseDouble(minRSqField.getText(), 0.1));
        }
        if (minDensityField != null) {
            PaintConfig.setDouble("Generate Squares", "Min Required Density Ratio",
                                  parseDouble(minDensityField.getText(), 2.0));
        }
        if (maxVariabilityField != null) {
            PaintConfig.setDouble("Generate Squares", "Max Allowable Variability",
                                  parseDouble(maxVariabilityField.getText(), 10.0));
        }
        if (mode == DialogMode.TRACKMATE && runAfterTrackMate != null) {
            PaintConfig.setBoolean("TrackMate", "Run Generate Squares After", runAfterTrackMate.isSelected());
        }
        PaintConfig.instance().save();
    }

    /**
     * Adds a fixed-size label into the GridBag container.
     */
    private static void label(JPanel p, String text, Dimension size, GridBagConstraints pg) {
        JLabel l = new JLabel(text);
        l.setPreferredSize(size);
        p.add(l, pg);
    }

    /**
     * Builds a fixed-size text field initialized to a given value.
     */
    private static JTextField text(String v, Dimension size) {
        JTextField t = new JTextField(v);
        t.setColumns(8);
        t.setPreferredSize(size);
        return t;
    }

    /**
     * Enables or disables all the Generate Squares fields (grid size, R², density, variability).
     */
    private void setSquaresEnabled(boolean enabled) {
        gridSizeCombo.setEnabled(enabled);
        minRSqField.setEnabled(enabled);
        minDensityField.setEnabled(enabled);
        maxVariabilityField.setEnabled(enabled);
    }

    /**
     * Parses a double from string with fallback to a default on error.
     */
    private static double parseDouble(String s, double def) {
        try {
            return Double.parseDouble(s.trim());
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
}