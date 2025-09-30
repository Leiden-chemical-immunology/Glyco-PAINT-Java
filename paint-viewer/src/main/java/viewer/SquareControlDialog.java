package viewer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class SquareControlDialog extends JDialog {

    private final JCheckBox showBordersCheckBox;
    private final JRadioButton numberNoneRadio;
    private final JRadioButton numberLabelRadio;
    private final JRadioButton numberSquareRadio;

    private final JSlider densityRatioSlider;
    private final JSlider variabilitySlider;
    private final JSlider rSquaredSlider; // scaled 0..100 → 0.0..1.0
    private final JSlider minDurationSlider;
    private final JSlider maxDurationSlider;

    private final JRadioButton neighbourFree;
    private final JRadioButton neighbourRelaxed;
    private final JRadioButton neighbourStrict;

    private final RecordingViewerFrame viewerFrame;

    private SquareGridPanel.NumberMode lastNumberMode = SquareGridPanel.NumberMode.NONE;

    private int origDensityRatio;
    private int origVariability;
    private double origRSquared; // now double
    private int origMinDuration;
    private int origMaxDuration;
    private String origNeighbourMode;

    public SquareControlDialog(JFrame owner,
                               SquareGridPanel gridPanel,
                               RecordingViewerFrame viewerFrame,
                               SquareControlParams initParams) {
        super(owner, "Square Controls", false);
        this.viewerFrame = viewerFrame;

        setLayout(new BorderLayout(10, 10));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // --- Number display radios ---
        numberNoneRadio = new JRadioButton("Show no number", true);
        numberLabelRadio = new JRadioButton("Show label number");
        numberSquareRadio = new JRadioButton("Show square number");

        ButtonGroup numbersGroup = new ButtonGroup();
        numbersGroup.add(numberNoneRadio);
        numbersGroup.add(numberLabelRadio);
        numbersGroup.add(numberSquareRadio);

        numberNoneRadio.addActionListener(e -> {
            gridPanel.setNumberMode(SquareGridPanel.NumberMode.NONE);
            lastNumberMode = SquareGridPanel.NumberMode.NONE;
        });
        numberLabelRadio.addActionListener(e -> {
            gridPanel.setNumberMode(SquareGridPanel.NumberMode.LABEL);
            lastNumberMode = SquareGridPanel.NumberMode.LABEL;
        });
        numberSquareRadio.addActionListener(e -> {
            gridPanel.setNumberMode(SquareGridPanel.NumberMode.SQUARE);
            lastNumberMode = SquareGridPanel.NumberMode.SQUARE;
        });

        // --- Show borders checkbox ---
        showBordersCheckBox = new JCheckBox("Show borders of selected squares", true);
        showBordersCheckBox.addActionListener(e -> {
            boolean show = showBordersCheckBox.isSelected();
            gridPanel.setShowBorders(show);

            if (!show) {
                if (numberLabelRadio.isSelected()) lastNumberMode = SquareGridPanel.NumberMode.LABEL;
                else if (numberSquareRadio.isSelected()) lastNumberMode = SquareGridPanel.NumberMode.SQUARE;
                else lastNumberMode = SquareGridPanel.NumberMode.NONE;

                numberNoneRadio.setSelected(true);
                gridPanel.setNumberMode(SquareGridPanel.NumberMode.NONE);

                numberNoneRadio.setEnabled(false);
                numberLabelRadio.setEnabled(false);
                numberSquareRadio.setEnabled(false);
            } else {
                numberNoneRadio.setEnabled(true);
                numberLabelRadio.setEnabled(true);
                numberSquareRadio.setEnabled(true);

                switch (lastNumberMode) {
                    case LABEL:
                        numberLabelRadio.setSelected(true);
                        gridPanel.setNumberMode(SquareGridPanel.NumberMode.LABEL);
                        break;
                    case SQUARE:
                        numberSquareRadio.setSelected(true);
                        gridPanel.setNumberMode(SquareGridPanel.NumberMode.SQUARE);
                        break;
                    default:
                        numberNoneRadio.setSelected(true);
                        gridPanel.setNumberMode(SquareGridPanel.NumberMode.NONE);
                        break;
                }
            }
        });

        JPanel bordersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bordersPanel.setBorder(BorderFactory.createTitledBorder("Borders"));
        bordersPanel.add(showBordersCheckBox);
        bordersPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, bordersPanel.getPreferredSize().height));

        content.add(bordersPanel);
        content.add(Box.createVerticalStrut(10));

        // --- Numbers frame ---
        JPanel numbersInner = new JPanel();
        numbersInner.setLayout(new BoxLayout(numbersInner, BoxLayout.Y_AXIS));
        numberNoneRadio.setAlignmentX(Component.LEFT_ALIGNMENT);
        numberLabelRadio.setAlignmentX(Component.LEFT_ALIGNMENT);
        numberSquareRadio.setAlignmentX(Component.LEFT_ALIGNMENT);
        numbersInner.add(numberNoneRadio);
        numbersInner.add(numberLabelRadio);
        numbersInner.add(numberSquareRadio);

        JPanel numbersPanel = new JPanel(new BorderLayout());
        numbersPanel.setBorder(BorderFactory.createTitledBorder("Numbers"));
        numbersPanel.add(numbersInner, BorderLayout.WEST);
        numbersPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, numbersPanel.getPreferredSize().height));

        content.add(numbersPanel);

        // --- Sliders ---
        densityRatioSlider = createSlider(1, 10, initParams.densityRatio);
        variabilitySlider = createSlider(1, 10, initParams.variability);

        // R² is special: 0–1 double → 0–100 int
        rSquaredSlider = createSlider(0, 100, (int) Math.round(initParams.rSquared * 100));

        minDurationSlider = createSlider(0, 1000, initParams.minDuration);
        maxDurationSlider = createSlider(0, 1000, initParams.maxDuration);

        JPanel slidersPanel = new JPanel(new GridLayout(1, 5, 10, 0));
        slidersPanel.add(wrapSlider(densityRatioSlider, "Min Required Density Ratio"));
        slidersPanel.add(wrapSlider(variabilitySlider, "Max Allowable Variability"));
        slidersPanel.add(wrapSlider(rSquaredSlider, "Min Required R²"));
        slidersPanel.add(wrapSlider(minDurationSlider, "Min Longest Duration"));
        slidersPanel.add(wrapSlider(maxDurationSlider, "Max Longest Duration"));

        content.add(Box.createVerticalStrut(10));
        content.add(slidersPanel);

        // --- Neighbour mode frame ---
        JPanel neighbourPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        neighbourPanel.setBorder(BorderFactory.createTitledBorder("Neighbour Mode"));
        neighbourFree = new JRadioButton("Free");
        neighbourRelaxed = new JRadioButton("Relaxed");
        neighbourStrict = new JRadioButton("Strict");

        ButtonGroup neighbourGroup = new ButtonGroup();
        neighbourGroup.add(neighbourFree);
        neighbourGroup.add(neighbourRelaxed);
        neighbourGroup.add(neighbourStrict);

        switch (initParams.neighbourMode) {
            case "Relaxed": neighbourRelaxed.setSelected(true); break;
            case "Strict":  neighbourStrict.setSelected(true); break;
            default:        neighbourFree.setSelected(true); break;
        }

        neighbourPanel.add(neighbourFree);
        neighbourPanel.add(neighbourRelaxed);
        neighbourPanel.add(neighbourStrict);
        neighbourPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, neighbourPanel.getPreferredSize().height));

        content.add(Box.createVerticalStrut(10));
        content.add(neighbourPanel);

        add(content, BorderLayout.CENTER);

        // --- Apply buttons frame ---
        JPanel applyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        applyPanel.setBorder(BorderFactory.createTitledBorder("Apply Changes"));

        JButton applyRecording = new JButton("Apply to Recording");
        JButton applyExperiment = new JButton("Apply to Experiment");
        JButton applyProject = new JButton("Apply to Project");
        JButton cancelButton = new JButton("Cancel");

        applyPanel.add(applyRecording);
        applyPanel.add(applyExperiment);
        applyPanel.add(applyProject);
        applyPanel.add(cancelButton);

        add(applyPanel, BorderLayout.SOUTH);

        // --- Listeners ---
        ChangeListener sliderListener = (ChangeEvent e) -> propagateValues();
        densityRatioSlider.addChangeListener(sliderListener);
        variabilitySlider.addChangeListener(sliderListener);
        rSquaredSlider.addChangeListener(sliderListener);
        minDurationSlider.addChangeListener(sliderListener);
        maxDurationSlider.addChangeListener(sliderListener);

        neighbourFree.addActionListener(e -> propagateValues());
        neighbourRelaxed.addActionListener(e -> propagateValues());
        neighbourStrict.addActionListener(e -> propagateValues());

        applyRecording.addActionListener(e ->
                viewerFrame.applySquareControlParameters("Recording", collectParams()));
        applyExperiment.addActionListener(e ->
                viewerFrame.applySquareControlParameters("Experiment", collectParams()));
        applyProject.addActionListener(e ->
                viewerFrame.applySquareControlParameters("Project", collectParams()));

        cancelButton.addActionListener(e -> {
            restoreOriginals();
            dispose();
        });

        // Save originals
        origDensityRatio = initParams.densityRatio;
        origVariability = initParams.variability;
        origRSquared = initParams.rSquared; // double
        origMinDuration = initParams.minDuration;
        origMaxDuration = initParams.maxDuration;
        origNeighbourMode = initParams.neighbourMode;

        pack();
        setLocationRelativeTo(owner);
    }

    private JSlider createSlider(int min, int max, int value) {
        JSlider slider = new JSlider(JSlider.VERTICAL, min, max, value);
        slider.setMajorTickSpacing((max - min) / 5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        return slider;
    }

    private JPanel wrapSlider(JSlider slider, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        ToolTipManager.sharedInstance().setEnabled(true);
        panel.setToolTipText(title);
        slider.setToolTipText(title);
        panel.add(slider, BorderLayout.CENTER);
        return panel;
    }

    private void propagateValues() {
        viewerFrame.updateSquareControlParameters(
                densityRatioSlider.getValue(),
                variabilitySlider.getValue(),
                rSquaredSlider.getValue() / 100.0, // convert back to 0.0–1.0
                minDurationSlider.getValue(),
                maxDurationSlider.getValue(),
                getNeighbourMode()
        );
    }

    private String getNeighbourMode() {
        if (neighbourFree.isSelected()) return "Free";
        if (neighbourRelaxed.isSelected()) return "Relaxed";
        return "Strict";
    }

    private void restoreOriginals() {
        densityRatioSlider.setValue(origDensityRatio);
        variabilitySlider.setValue(origVariability);
        rSquaredSlider.setValue((int) Math.round(origRSquared * 100));
        minDurationSlider.setValue(origMinDuration);
        maxDurationSlider.setValue(origMaxDuration);

        if ("Free".equals(origNeighbourMode)) neighbourFree.setSelected(true);
        else if ("Relaxed".equals(origNeighbourMode)) neighbourRelaxed.setSelected(true);
        else neighbourStrict.setSelected(true);

        propagateValues();
    }

    private SquareControlParams collectParams() {
        return new SquareControlParams(
                densityRatioSlider.getValue(),
                variabilitySlider.getValue(),
                rSquaredSlider.getValue() / 100.0, // double
                minDurationSlider.getValue(),
                maxDurationSlider.getValue(),
                getNeighbourMode()
        );
    }
}