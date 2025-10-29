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
 * Dialog for configuring square control parameters in the viewer.
 * <p>
 * This dialog provides interactive sliders and radio buttons that allow users
 * to adjust parameters such as:
 * <ul>
 *   <li>Minimum Density Ratio</li>
 *   <li>Maximum Variability</li>
 *   <li>Minimum R² value</li>
 *   <li>Neighbour mode (Free, Relaxed, Strict)</li>
 * </ul>
 * Changes are previewed live in the main viewer, while pressing an Apply button
 * commits the parameters and writes them to file.
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
     * Constructs the Square Control dialog that allows users to modify
     * square grid parameters and preview changes in real time.
     *
     * @param owner the parent frame that owns this dialog
     * @param gridPanel the square grid panel to update visually
     * @param listener the listener for apply/preview callbacks
     * @param initParams the initial square control parameters
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
        variabilitySlider  = createSlider(0, 200,   (int) Math.round(initParams.variability * 10));
        rSquaredSlider     = createSlider(0, 100,   (int) Math.round(initParams.rSquared * 100));

        densityRatioValue = new JLabel();
        variabilityValue  = new JLabel();
        rSquaredValue     = new JLabel();

        JPanel slidersPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        slidersPanel.setBorder(BorderFactory.createTitledBorder("Square Filters"));
        slidersPanel.add(wrapSlider(densityRatioSlider, "Min Density Ratio", densityRatioValue));
        slidersPanel.add(wrapSlider(variabilitySlider,  "Max Variability",   variabilityValue));
        slidersPanel.add(wrapSlider(rSquaredSlider,     "Min R²",            rSquaredValue));

        // --- Neighbour mode ---
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

        // --- Apply / Cancel ---
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

        // --- Layout assembly ---
        content.add(slidersPanel);
        content.add(Box.createVerticalStrut(10));
        content.add(neighbourPanel);
        content.add(Box.createVerticalStrut(10));
        content.add(applyPanel);
        add(content, BorderLayout.CENTER);

        // --- Listeners for live preview ---
        ChangeListener sliderListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateValueLabels();
                propagateValues();
                propagatePreview();
                SwingUtilities.invokeLater(() -> gridPanel.repaint());
            }
        };
        densityRatioSlider.addChangeListener(sliderListener);
        variabilitySlider.addChangeListener(sliderListener);
        rSquaredSlider.addChangeListener(sliderListener);

        neighbourFree.addActionListener(e -> propagatePreview());
        neighbourRelaxed.addActionListener(e -> propagatePreview());
        neighbourStrict.addActionListener(e -> propagatePreview());

        // --- Apply buttons (commit + write to file) ---
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

        // --- Save originals for Cancel ---
        origDensityRatio = initParams.densityRatio;
        origVariability  = initParams.variability;
        origRSquared     = initParams.rSquared;
        origNeighbourMode = initParams.neighbourMode;

        updateValueLabels();
        propagatePreview();

        pack();
        setLocationRelativeTo(owner);
    }

    /** Creates a vertical slider with labeled ticks. */
    private JSlider createSlider(int min, int max, int value) {
        JSlider slider = new JSlider(JSlider.VERTICAL, min, max, Math.min(max, Math.max(min, value)));
        slider.setMajorTickSpacing(Math.max(1, (max - min) / 5));
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        // slider.setSnapToTicks(true);

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

    /** Wraps a slider with a label inside a titled border panel. */
    private JPanel wrapSlider(JSlider slider, String title, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(slider, BorderLayout.CENTER);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(valueLabel, BorderLayout.SOUTH);
        return panel;
    }

    /** Updates numeric value labels beside each slider. */
    private void updateValueLabels() {
        densityRatioValue.setText(ONE_DEC.format(densityRatioSlider.getValue() / 10.0));
        variabilityValue.setText(ONE_DEC.format(variabilitySlider.getValue() / 10.0));
        rSquaredValue.setText(ONE_DEC.format(rSquaredSlider.getValue() / 100.0));
    }

    /**
     * Sends live parameter updates to the listener for preview
     * without writing anything to disk.
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

    /** Returns the selected neighbour mode string. */
    private String getNeighbourMode() {
        if (neighbourFree.isSelected()) return "Free";
        if (neighbourRelaxed.isSelected()) return "Relaxed";
        return "Strict";
    }

    /** Restores original slider and neighbour mode values, updating the preview. */
    private void restoreOriginals() {
        densityRatioSlider.setValue((int) Math.round(origDensityRatio * 10));
        variabilitySlider.setValue((int) Math.round(origVariability * 10));
        rSquaredSlider.setValue((int) Math.round(origRSquared * 100));

        switch (origNeighbourMode) {
            case "Relaxed": neighbourRelaxed.setSelected(true); break;
            case "Strict":  neighbourStrict.setSelected(true);  break;
            default:        neighbourFree.setSelected(true);
        }

        updateValueLabels();
        propagatePreview();
    }

    /** Collects the current dialog settings into a parameter object. */
    private SquareControlParams collectParams() {
        return new SquareControlParams(
                densityRatioSlider.getValue() / 10.0,
                variabilitySlider.getValue() / 10.0,
                rSquaredSlider.getValue() / 100.0,
                getNeighbourMode()
        );
    }
}