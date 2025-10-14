package viewer.dialogs;


import viewer.shared.SquareControlParams;
import viewer.panels.RecordingControlsPanel;
import viewer.panels.SquareGridPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Square Control Dialog (sliders + neighbour mode only)
 * - Density Ratio, Variability, R² sliders
 * - Neighbour mode (Free / Relaxed / Strict)
 * - Apply / Cancel controls
 * Compatible with RecordingViewerFrame and SquareGridPanel.
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

    private JPanel wrapSlider(JSlider slider, String title, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(slider, BorderLayout.CENTER);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(valueLabel, BorderLayout.SOUTH);
        return panel;
    }

    private void updateValueLabels() {
        densityRatioValue.setText(ONE_DEC.format(densityRatioSlider.getValue() / 10.0));
        variabilityValue.setText(ONE_DEC.format(variabilitySlider.getValue() / 10.0));
        rSquaredValue.setText(ONE_DEC.format(rSquaredSlider.getValue() / 100.0));
    }

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

    private String getNeighbourMode() {
        if (neighbourFree.isSelected()) return "Free";
        if (neighbourRelaxed.isSelected()) return "Relaxed";
        return "Strict";
    }

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

    private SquareControlParams collectParams() {
        return new SquareControlParams(
                densityRatioSlider.getValue() / 10.0,
                variabilitySlider.getValue() / 10.0,
                rSquaredSlider.getValue() / 100.0,
                getNeighbourMode()
        );
    }
}