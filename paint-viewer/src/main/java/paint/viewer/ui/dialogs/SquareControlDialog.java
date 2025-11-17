/*==============================================================================
 *  Class:        SquareControlDialog.java
 *  Package:      paint.viewer.dialogs
 *
 *  PURPOSE:
 *    Provides a modeless dialog for interactively adjusting square-level
 *    filtering and neighbor control parameters within the PAINT viewer.
 *
 *  DESCRIPTION:
 *    The dialog allows users to modify and preview key filtering parameters
 *    for grid squares:
 *      • Minimum Density Ratio
 *      • Maximum Variability
 *      • Minimum R²
 *      • Neighbour Mode (Free, Relaxed, or Strict)
 *
 *    Parameter adjustments are immediately previewed in the main viewer
 *    without permanent changes. Pressing one of the Apply buttons commits
 *    the settings to the relevant scope (Recording, Experiment, or Project)
 *    and triggers file updates through the associated listener.
 *
 *  KEY FEATURES:
 *    • Live preview of slider and neighbour mode changes.
 *    • Modeless operation allowing real-time visual feedback.
 *    • Apply buttons for different persistence scopes.
 *    • Revert functionality restoring original values on Cancel.
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
 ==============================================================================*/

package paint.viewer.ui.dialogs;
import static paint.shared.constants.PaintConstants.*;


import paint.viewer.ui.panels.RecordingControlsPanel;
import paint.viewer.ui.panels.SquareGridPanel;
import paint.viewer.model.SquareControlParams;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * A dialog for configuring square control parameters in the viewer.
 * <p>
 * Provides interactive sliders and radio buttons that allow users to adjust
 * key square filtering parameters. Changes are previewed live in the main
 * viewer; pressing an Apply button commits and saves the configuration.
 */
public class SquareControlDialog extends JDialog {
//    private static final DecimalFormat ONE_DEC = new DecimalFormat("0.0");
    private final JSlider                         densityRatioSlider;
    private final JSlider                         variabilitySlider;
    private final JSlider                         rSquaredSlider;
    private final JLabel                          densityRatioValue;
    private final JLabel                          variabilityValue;
    private final JLabel                          rSquaredValue;
    private final JRadioButton                    neighbourFree;
    private final JRadioButton                    neighbourRelaxed;
    private final JRadioButton                    neighbourStrict;
    private final RecordingControlsPanel.Listener listener;
    private final double                          origDensityRatio;
    private final double                          origVariability;
    private final double                          origRSquared;
    private final String                          origNeighbourMode;
    /**
     * Constructs a dialog that enables interactive adjustment of square grid
     * parameters such as Density Ratio, Variability, and R² thresholds.
     *
     * @param owner      the parent frame that owns this dialog
     * @param gridPanel  the grid panel to update visually during preview
     * @param listener   listener receiving apply and preview callbacks
     * @param initParams the initial square control parameters
     */
    public SquareControlDialog(JFrame owner,
                               SquareGridPanel gridPanel,
                               RecordingControlsPanel.Listener listener,
                               SquareControlParams initParams) {
        super(owner, "Square Controls", false);
        this.listener = listener;

        setLayout(new BorderLayout(10, 10));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // ─────────────────────────────────────────────────────────────────────
        // Sliders
        // ─────────────────────────────────────────────────────────────────────

        densityRatioSlider = createSteppedSlider(
                0,               // min
                2000,            // max
                2,               // step
                initParams.minRequiredDensityRatio
        );
        variabilitySlider = createSteppedSlider(
                0,
                20,
                1,
                initParams.maxAllowableVariability
        );

        rSquaredSlider = createSteppedSlider(
                0.0,
                1.0,
                0.05,
                initParams.minRequiredRSquared
        );
        densityRatioValue = new JLabel();
        variabilityValue  = new JLabel();
        rSquaredValue     = new JLabel();

        JPanel slidersPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        slidersPanel.setBorder(BorderFactory.createTitledBorder("Square Filters"));
        slidersPanel.add(wrapSlider(densityRatioSlider, MIN_DENSITY_RATIO, densityRatioValue));
        slidersPanel.add(wrapSlider(variabilitySlider,  MAX_VARIABILITY,   variabilityValue));
        slidersPanel.add(wrapSlider(rSquaredSlider, "Min R²", rSquaredValue));

        // ─────────────────────────────────────────────────────────────────────
        // Neighbour mode radio buttons
        // ─────────────────────────────────────────────────────────────────────

        neighbourFree    = new JRadioButton("Free");
        neighbourRelaxed = new JRadioButton("Relaxed");
        neighbourStrict  = new JRadioButton("Strict");

        ButtonGroup neighbourGroup = new ButtonGroup();
        neighbourGroup.add(neighbourFree);
        neighbourGroup.add(neighbourRelaxed);
        neighbourGroup.add(neighbourStrict);

        switch (initParams.neighbourMode) {
            case "Relaxed":
                neighbourRelaxed.setSelected(true);
                break;
            case "Strict":
                neighbourStrict.setSelected(true);
                break;
            default:
                neighbourFree.setSelected(true);
        }

        JPanel neighbourPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        neighbourPanel.setBorder(BorderFactory.createTitledBorder(NEIGHBOUR_MODE));
        neighbourPanel.add(neighbourFree);
        neighbourPanel.add(neighbourRelaxed);
        neighbourPanel.add(neighbourStrict);
        neighbourPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ─────────────────────────────────────────────────────────────────────
        // Apply / Cancel controls
        // ─────────────────────────────────────────────────────────────────────

        JPanel applyPanel       = new JPanel(new FlowLayout(FlowLayout.LEFT));
        applyPanel.setBorder(BorderFactory.createTitledBorder("Apply Changes"));
        JButton applyRecording  = new JButton("Apply to Recording");
        JButton applyExperiment = new JButton("Apply to Experiment");
        JButton applyProject    = new JButton("Apply to Project");
        JButton cancelButton    = new JButton("Cancel");

        for (JButton b : new JButton[]{applyRecording, applyExperiment, applyProject, cancelButton}) {
            b.setPreferredSize(new Dimension(180, 28));
        }

        applyPanel.add(applyRecording);
        applyPanel.add(applyExperiment);
        applyPanel.add(applyProject);
        applyPanel.add(cancelButton);
        applyPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ─────────────────────────────────────────────────────────────────────
        // Layout assembly
        // ─────────────────────────────────────────────────────────────────────
        content.add(slidersPanel);
        content.add(Box.createVerticalStrut(10));
        content.add(neighbourPanel);
        content.add(Box.createVerticalStrut(10));
        content.add(applyPanel);
        add(content, BorderLayout.CENTER);

        // ─────────────────────────────────────────────────────────────────────
        // Listeners for live preview
        // ─────────────────────────────────────────────────────────────────────
        ChangeListener sliderListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateValueLabels();
                propagatePreview();
                SwingUtilities.invokeLater(gridPanel::repaint);
            }
        };
        densityRatioSlider.addChangeListener(sliderListener);
        variabilitySlider.addChangeListener(sliderListener);
        rSquaredSlider.addChangeListener(sliderListener);

        neighbourFree.addActionListener(   e -> propagatePreview());
        neighbourRelaxed.addActionListener(e -> propagatePreview());
        neighbourStrict.addActionListener( e -> propagatePreview());

        // Apply button actions (commit and write to the file)
        applyRecording.addActionListener(e -> {
            listener.onApplySquareControl("Recording", collectParams());
            dispose();
        });
        applyExperiment.addActionListener(e -> {
            listener.onApplySquareControl("Experiment", collectParams());
            dispose();
        });
        applyProject.addActionListener(e -> {
            listener.onApplySquareControl("Project", collectParams());
            dispose();
        });
        cancelButton.addActionListener(e -> {
            restoreOriginals();
            dispose();
        });

        // ─────────────────────────────────────────────────────────────────────
        // Preserve original values for cancel/restore
        // ─────────────────────────────────────────────────────────────────────

        origDensityRatio  = initParams.minRequiredDensityRatio;
        origVariability   = initParams.maxAllowableVariability;
        origRSquared      = initParams.minRequiredRSquared;
        origNeighbourMode = initParams.neighbourMode;

        updateValueLabels();
        propagatePreview();

        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Wraps a slider and value label inside a titled panel.
     */
    private JPanel wrapSlider(JSlider slider, String title, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(slider, BorderLayout.CENTER);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(valueLabel, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Updates the numeric labels to reflect current slider values.
     */
    private void updateValueLabels() {
        densityRatioValue.setText(
                formatSliderValue(getSliderDouble(densityRatioSlider, 0, 2), 2)
        );
        variabilityValue.setText(
                formatSliderValue(getSliderDouble(variabilitySlider, 0, 1), 1)
        );
        rSquaredValue.setText(
                formatSliderValue(getSliderDouble(rSquaredSlider, 0.0, 0.05), 0.05)
        );
    }

    /**
     * Sends live parameter updates for real-time preview without persistence.
     */
    private void propagatePreview() {
        SquareControlParams params = collectParams();
        listener.onApplySquareControl("Preview", params);
    }

    /**
     * Returns the currently selected neighbour mode string.
     */
    private String getNeighbourMode() {
        if (neighbourFree.isSelected()) {
            return "Free";
        }
        if (neighbourRelaxed.isSelected()) {
            return "Relaxed";
        }
        return "Strict";
    }

    /**
     * Restores original slider and neighbour mode values, updating the preview.
     */
    private void restoreOriginals() {
        densityRatioSlider.setValue((int) Math.round(origDensityRatio / 2.0));
        variabilitySlider.setValue((int) Math.round(origVariability / 1.0));
        rSquaredSlider.setValue((int) Math.round(origRSquared / 0.1));

        switch (origNeighbourMode) {
            case "Relaxed":
                neighbourRelaxed.setSelected(true);
                break;
            case "Strict":
                neighbourStrict.setSelected(true);
                break;
            default:
                neighbourFree.setSelected(true);
        }

        updateValueLabels();
        propagatePreview();
    }

    /**
     * Collects the current slider and neighbour mode state into a parameter object.
     */
    private SquareControlParams collectParams() {
        return new SquareControlParams(
                getSliderDouble(densityRatioSlider, 0, 2),
                getSliderDouble(variabilitySlider, 0, 1),
                getSliderDouble(rSquaredSlider, 0.0, 0.05),
                getNeighbourMode()
        );
    }

    private JSlider createSteppedSlider(double min, double max, double step, double initialValue) {

        int steps = (int) Math.round((max - min) / step);
        int initial = (int) Math.round((initialValue - min) / step);

        JSlider slider = new JSlider(JSlider.VERTICAL, 0, steps,
                                     Math.max(0, Math.min(steps, initial)));

        // We want major ticks ONLY
        slider.setPaintTicks(true);
        slider.setSnapToTicks(false);  // IMPORTANT: manual snapping below
        slider.setMinorTickSpacing(0); // no minor ticks

        // Major tick spacing (labelled)
        int major = Math.max(1, steps / 5);
        slider.setMajorTickSpacing(major);

        // Label table for major ticks only
        java.util.Hashtable<Integer, JLabel> table = new java.util.Hashtable<>();
        for (int i = 0; i <= steps; i += major) {
            double v = min + (i * step);
            table.put(i, new JLabel(formatSliderValue(v, step)));
        }
        slider.setLabelTable(table);
        slider.setPaintLabels(true);

        // Manual snapping to step values (NOT to major ticks!)
        slider.addChangeListener(e -> {
            int raw = slider.getValue();
            int snapped = Math.round(raw); // raw represents the step index
            if (raw != snapped) slider.setValue(snapped);
        });

        return slider;
    }

    private String formatSliderValue(double v, double step) {

        // Integer step → format as integer
        if (step >= 1.0) {
            return Integer.toString((int) Math.round(v));
        }

        // Step of 0.1 → one decimal
        if (Math.abs(step - 0.1) < 1e-9) {
            return String.format("%.1f", v);
        }

        // Step of 0.05 → two decimals
        if (Math.abs(step - 0.05) < 1e-9) {
            return String.format("%.2f", v);
        }

        // Default fallback
        return String.valueOf(v);
    }

    private double getSliderDouble(JSlider slider, double min, double step) {
        return min + slider.getValue() * step;
    }
}