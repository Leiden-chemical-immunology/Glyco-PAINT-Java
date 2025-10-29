/******************************************************************************
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
 ******************************************************************************/

package paint.viewer.dialogs;

import paint.viewer.panels.RecordingControlsPanel;
import paint.viewer.panels.SquareGridPanel;
import paint.viewer.shared.SquareControlParams;

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
    private static final DecimalFormat ONE_DEC = new DecimalFormat("0.0");
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
        densityRatioSlider = createSlider(0, 20000, (int) Math.round(initParams.minRequiredDensityRatio * 10));
        variabilitySlider  = createSlider(0, 200,   (int) Math.round(initParams.maxAllowableVariability * 10));
        rSquaredSlider     = createSlider(0, 100,   (int) Math.round(initParams.minRequiredRSquared * 100));

        densityRatioValue = new JLabel();
        variabilityValue  = new JLabel();
        rSquaredValue     = new JLabel();

        JPanel slidersPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        slidersPanel.setBorder(BorderFactory.createTitledBorder("Square Filters"));
        slidersPanel.add(wrapSlider(densityRatioSlider, "Min Density Ratio", densityRatioValue));
        slidersPanel.add(wrapSlider(variabilitySlider, "Max Variability", variabilityValue));
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
        neighbourPanel.setBorder(BorderFactory.createTitledBorder("Neighbour Mode"));
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

        neighbourFree.addActionListener(e -> propagatePreview());
        neighbourRelaxed.addActionListener(e -> propagatePreview());
        neighbourStrict.addActionListener(e -> propagatePreview());

        // Apply button actions (commit + write to file)
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
     * Creates a vertical slider with labeled ticks.
     */
    private JSlider createSlider(int min, int max, int value) {
        JSlider slider = new JSlider(JSlider.VERTICAL, min, max, Math.min(max, Math.max(min, value)));
        slider.setMajorTickSpacing(Math.max(1, (max - min) / 5));
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        boolean isRSquared = (max == 100);
        double divisor = isRSquared ? 100.0 : 10.0;

        java.util.Hashtable<Integer, JLabel> table = new java.util.Hashtable<>();
        int major = Math.max(1, (max - min) / 5);
        for (int v = min; v <= max; v += major) {
            String text = isRSquared
                    ? ONE_DEC.format(v / divisor)
                    : String.valueOf((int) Math.round(v / divisor));
            table.put(v, new JLabel(text));
        }
        slider.setLabelTable(table);
        return slider;
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
        densityRatioValue.setText(ONE_DEC.format(densityRatioSlider.getValue() / 10.0));
        variabilityValue.setText(ONE_DEC.format(variabilitySlider.getValue() / 10.0));
        rSquaredValue.setText(ONE_DEC.format(rSquaredSlider.getValue() / 100.0));
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
        densityRatioSlider.setValue((int) Math.round(origDensityRatio * 10));
        variabilitySlider.setValue((int) Math.round(origVariability * 10));
        rSquaredSlider.setValue((int) Math.round(origRSquared * 100));

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
                densityRatioSlider.getValue() / 10.0,
                variabilitySlider.getValue() / 10.0,
                rSquaredSlider.getValue() / 100.0,
                getNeighbourMode()
        );
    }
}