package paint.viewer.dialogs;


import paint.viewer.shared.SquareControlParams;
import paint.viewer.panels.RecordingControlsPanel;
import paint.viewer.panels.SquareGridPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Represents a dialog for controlling parameters related to square grid settings and visualization.
 * This dialog allows the user to configure various parameters such as density ratio, variability,
 * R-squared, and neighbour mode. It also provides options to apply these changes to recording,
 * experiment, or project levels.
 *
 * The dialog includes:
 * - Sliders to adjust density ratio, variability, and R-squared values.
 * - Radio buttons to select neighbour mode: Free, Relaxed, or Strict.
 * - Buttons to apply changes to recording, experiment, or project levels, or cancel the changes.
 *
 * The class integrates with {@code SquareGridPanel} to update the grid settings in real time and
 * with {@code RecordingControlsPanel.Listener} to propagate changes externally when applied.
 */
public class SquareControlDialog extends JDialog {

    private final JSlider densityRatioSlider;
    private final JSlider variabilitySlider;
    private final JSlider rSquaredSlider;

    private final JLabel densityRatioValue;
    private final JLabel variabilityValue;
    private final JLabel rSquaredValue;

    private final JRadioButton neighbourFree;
    private final JRadioButton neighbourRelaxed;
    private final JRadioButton neighbourStrict;

    private final RecordingControlsPanel.Listener listener;
    private final SquareGridPanel gridPanel;

    private final double origDensityRatio;
    private final double origVariability;
    private final double origRSquared;
    private final String origNeighbourMode;

    private static final DecimalFormat ONE_DEC = new DecimalFormat("0.0");

    /**
     * Constructs a dialog for configuring square control parameters. The dialog allows
     * users to adjust sliders for density ratio, variability, and R² values, select neighbor
     * modes, and apply changes to recording, experiment, or project scopes.
     *
     * @param owner the parent JFrame that owns this dialog
     * @param gridPanel the panel displaying the square grid
     * @param listener a listener for handling actions triggered by the dialog, such as applying changes
     * @param initParams the initial square control parameters to populate the dialog
     */
    public SquareControlDialog(JFrame owner,
                               SquareGridPanel gridPanel,
                               RecordingControlsPanel.Listener listener,
                               SquareControlParams initParams) {
        super(owner, "Square Controls", false);
        this.listener = listener;
        this.gridPanel = gridPanel;

        setLayout(new BorderLayout(10, 10));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // --- Sliders ---
        densityRatioSlider = createSlider(0, 20000, (int) Math.round(initParams.densityRatio * 10));
        variabilitySlider = createSlider(0, 200, (int) Math.round(initParams.variability * 10));
        rSquaredSlider = createSlider(0, 100, (int) Math.round(initParams.rSquared * 100));

        densityRatioValue = new JLabel();
        variabilityValue = new JLabel();
        rSquaredValue = new JLabel();

        JPanel slidersPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        slidersPanel.setBorder(BorderFactory.createTitledBorder("Square Filters"));
        slidersPanel.add(wrapSlider(densityRatioSlider, "Min Density Ratio", densityRatioValue));
        slidersPanel.add(wrapSlider(variabilitySlider, "Max Variability", variabilityValue));
        slidersPanel.add(wrapSlider(rSquaredSlider, "Min R²", rSquaredValue));

        // --- Neighbour mode ---
        neighbourFree = new JRadioButton("Free");
        neighbourRelaxed = new JRadioButton("Relaxed");
        neighbourStrict = new JRadioButton("Strict");

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

        // --- Apply / Cancel ---
        JPanel applyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        applyPanel.setBorder(BorderFactory.createTitledBorder("Apply Changes"));
        JButton applyRecording = new JButton("Apply to Recording");
        JButton applyExperiment = new JButton("Apply to Experiment");
        JButton applyProject = new JButton("Apply to Project");
        JButton cancelButton = new JButton("Cancel");

        for (JButton b : new JButton[]{applyRecording, applyExperiment, applyProject, cancelButton}) {
            b.setPreferredSize(new Dimension(180, 28));
        }

        applyPanel.add(applyRecording);
        applyPanel.add(applyExperiment);
        applyPanel.add(applyProject);
        applyPanel.add(cancelButton);
        applyPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- Layout assembly ---
        content.add(slidersPanel);
        content.add(Box.createVerticalStrut(10));
        content.add(neighbourPanel);
        content.add(Box.createVerticalStrut(10));
        content.add(applyPanel);

        add(content, BorderLayout.CENTER);

        // --- Listeners ---
        ChangeListener sliderListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateValueLabels();
                propagateValues();
            }
        };
        densityRatioSlider.addChangeListener(sliderListener);
        variabilitySlider.addChangeListener(sliderListener);
        rSquaredSlider.addChangeListener(sliderListener);

        neighbourFree.addActionListener(e -> propagateValues());
        neighbourRelaxed.addActionListener(e -> propagateValues());
        neighbourStrict.addActionListener(e -> propagateValues());

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

        // --- Save originals ---
        origDensityRatio = initParams.densityRatio;
        origVariability = initParams.variability;
        origRSquared = initParams.rSquared;
        origNeighbourMode = initParams.neighbourMode;

        updateValueLabels();
        propagateValues();

        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Creates a vertical {@code JSlider} with the specified minimum, maximum, and initial values.
     * The slider is configured with labeled tick marks and snap-to-tick behavior. Labels are generated
     * based on the range, with special formatting applied if the maximum value equals 100.
     *
     * @param min the minimum value of the slider
     * @param max the maximum value of the slider
     * @param value the initial value of the slider
     * @return a configured {@code JSlider} instance
     */
    private JSlider createSlider(int min, int max, int value) {
        JSlider slider = new JSlider(JSlider.VERTICAL, min, max, Math.min(max, Math.max(min, value)));
        slider.setMajorTickSpacing(Math.max(1, (max - min) / 5));
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(true);

        boolean isRSquared = (max == 100);
        double divisor = isRSquared ? 100.0 : 10.0;

        java.util.Hashtable<Integer, JLabel> table = new java.util.Hashtable<Integer, JLabel>();
        int major = Math.max(1, (max - min) / 5);
        for (int v = min; v <= max; v += major) {
            String text = isRSquared ? ONE_DEC.format(v / divisor)
                    : String.valueOf((int) Math.round(v / divisor));
            table.put(v, new JLabel(text));
        }
        slider.setLabelTable(table);
        return slider;
    }

    /**
     * Wraps a JSlider and a JLabel into a titled JPanel. The JSlider is displayed in the center,
     * and the JLabel, which typically represents the slider's value, is aligned at the bottom.
     *
     * @param slider the JSlider to be added to the panel
     * @param title the title to be displayed on the border of the panel
     * @param valueLabel the JLabel to display the slider's value, shown beneath the slider
     * @return a JPanel containing the slider and the value label, with a titled border
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
     * Updates the labels displaying the current values for density ratio, variability, and R².
     * The slider values are formatted to one decimal place using the {@code ONE_DEC} formatter
     * and set to the corresponding labels. The density ratio and variability values are scaled
     * by dividing by 10, while the R² value is scaled by dividing by 100.
     */
    private void updateValueLabels() {
        densityRatioValue.setText(ONE_DEC.format(densityRatioSlider.getValue() / 10.0));
        variabilityValue.setText(ONE_DEC.format(variabilitySlider.getValue() / 10.0));
        rSquaredValue.setText(ONE_DEC.format(rSquaredSlider.getValue() / 100.0));
    }

    /**
     * Updates the grid panel's control parameters and re-applies the visibility filter.
     *
     * This method propagates the current values from the dialog's sliders to the grid panel.
     * The density ratio, variability, and R² values are obtained from their respective sliders
     * and converted to appropriate scales before being passed to the grid panel's control parameters.
     * The neighbor mode is determined via {@code getNeighbourMode()}.
     *
     * After updating the parameters, the method applies the visibility filter and repaints the grid panel.
     */
    private void propagateValues() {
        // directly update grid while sliding, no call to RecordingViewerFrame
        gridPanel.setControlParameters(
                densityRatioSlider.getValue() / 10.0,
                variabilitySlider.getValue() / 10.0,
                rSquaredSlider.getValue() / 100.0,
                getNeighbourMode()
        );
        gridPanel.applyVisibilityFilter();
        gridPanel.repaint();
    }

    /**
     * Determines the selected neighbor mode based on the state of specific UI components.
     *
     * If the "Free" option is selected, it returns "Free".
     * If the "Relaxed" option is selected, it returns "Relaxed".
     * Otherwise, it defaults to returning "Strict".
     *
     * @return a {@code String} representing the current neighbor mode, which can be "Free", "Relaxed", or "Strict"
     */
    private String getNeighbourMode() {
        if (neighbourFree.isSelected()) return "Free";
        if (neighbourRelaxed.isSelected()) return "Relaxed";
        return "Strict";
    }

    /**
     * Restores the original configuration values for the dialog's sliders and neighbor mode controls.
     *
     * The method sets the sliders (density ratio, variability, and R²) to their
     * respective original values, rescaled to match the slider's internal representation.
     * It also resets the neighbor mode radio buttons (Free, Relaxed, or Strict)
     * to the originally selected mode.
     *
     * After restoring these values, the method updates the value labels and propagates
     * the restored values to the grid panel.
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
        propagateValues();
    }

    /**
     * Collects the current values from the sliders and the neighbor mode selection,
     * and encapsulates them into a {@code SquareControlParams} object.
     *
     * The density ratio and variability values are derived by dividing the slider values by 10.0,
     * while the R² value is derived by dividing the slider value by 100.0. The neighbor mode
     * is obtained from the {@code getNeighbourMode} method.
     *
     * @return a {@code SquareControlParams} object containing the density ratio, variability,
     * R², and neighbor mode based on the current settings in the dialog.
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