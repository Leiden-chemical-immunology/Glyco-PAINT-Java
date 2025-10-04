package viewer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Hashtable;

public class SquareControlDialog extends JDialog {

    private final JCheckBox showBordersCheckBox;
    private final JRadioButton numberNoneRadio;
    private final JRadioButton numberLabelRadio;
    private final JRadioButton numberSquareRadio;

    private final JSlider densityRatioSlider;
    private final JSlider variabilitySlider;
    private final JSlider rSquaredSlider;

    private final JLabel densityRatioValue;
    private final JLabel variabilityValue;
    private final JLabel rSquaredValue;

    private final JRadioButton neighbourFree;
    private final JRadioButton neighbourRelaxed;
    private final JRadioButton neighbourStrict;

    private final RecordingViewerFrame viewerFrame;

    private SquareGridPanel.NumberMode lastNumberMode = SquareGridPanel.NumberMode.NONE;

    private double origDensityRatio;
    private double origVariability;
    private double origRSquared;
    private String origNeighbourMode;

    private static final DecimalFormat ONE_DEC = new DecimalFormat("0.0");

    public SquareControlDialog(JFrame owner,
                               SquareGridPanel gridPanel,
                               RecordingViewerFrame viewerFrame,
                               SquareControlParams initParams) {
        super(owner, "Square Controls", false);
        this.viewerFrame = viewerFrame;

        setLayout(new BorderLayout(10, 10));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // --- Borders ---
        showBordersCheckBox = new JCheckBox("Show borders of selected squares", true);

        numberNoneRadio = new JRadioButton("Show no number", true);
        numberLabelRadio = new JRadioButton("Show label number");
        numberSquareRadio = new JRadioButton("Show square number");

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

        // --- Numbers panel ---
        ButtonGroup numbersGroup = new ButtonGroup();
        numbersGroup.add(numberNoneRadio);
        numbersGroup.add(numberLabelRadio);
        numbersGroup.add(numberSquareRadio);

        numberNoneRadio.addActionListener(e -> gridPanel.setNumberMode(SquareGridPanel.NumberMode.NONE));
        numberLabelRadio.addActionListener(e -> gridPanel.setNumberMode(SquareGridPanel.NumberMode.LABEL));
        numberSquareRadio.addActionListener(e -> gridPanel.setNumberMode(SquareGridPanel.NumberMode.SQUARE));

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

        // --- Sliders (only 3 left) ---
        densityRatioSlider = createSlider(0, 200, (int) Math.round(initParams.densityRatio * 10));
        variabilitySlider  = createSlider(0, 200, (int) Math.round(initParams.variability * 10));
        rSquaredSlider     = createSlider(0, 100, (int) Math.round(initParams.rSquared * 100));

        densityRatioValue = new JLabel();
        variabilityValue  = new JLabel();
        rSquaredValue     = new JLabel();

        JPanel slidersPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        slidersPanel.add(wrapSlider(densityRatioSlider, "Min Required Density Ratio", densityRatioValue, true, 10));
        slidersPanel.add(wrapSlider(variabilitySlider,  "Max Allowable Variability",  variabilityValue,  true, 10));
        slidersPanel.add(wrapSlider(rSquaredSlider,     "Min Required R²",            rSquaredValue,     true, 100));

        content.add(Box.createVerticalStrut(10));
        content.add(slidersPanel);

        // --- Neighbour mode ---
        neighbourFree    = new JRadioButton("Free");
        neighbourRelaxed = new JRadioButton("Relaxed");
        neighbourStrict  = new JRadioButton("Strict");

        ButtonGroup neighbourGroup = new ButtonGroup();
        neighbourGroup.add(neighbourFree);
        neighbourGroup.add(neighbourRelaxed);
        neighbourGroup.add(neighbourStrict);

        switch (initParams.neighbourMode) {
            case "Relaxed": neighbourRelaxed.setSelected(true); break;
            case "Strict":  neighbourStrict.setSelected(true);  break;
            default:        neighbourFree.setSelected(true);    break;
        }

        JPanel neighbourPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        neighbourPanel.setBorder(BorderFactory.createTitledBorder("Neighbour Mode"));
        neighbourPanel.add(neighbourFree);
        neighbourPanel.add(neighbourRelaxed);
        neighbourPanel.add(neighbourStrict);
        neighbourPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, neighbourPanel.getPreferredSize().height));

        content.add(Box.createVerticalStrut(10));
        content.add(neighbourPanel);

        add(content, BorderLayout.CENTER);

        // --- Apply / Cancel ---
        JPanel applyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        applyPanel.setBorder(BorderFactory.createTitledBorder("Apply Changes"));
        JButton applyRecording = new JButton("Apply to Recording");
        JButton applyExperiment = new JButton("Apply to Experiment");
        JButton applyProject   = new JButton("Apply to Project");
        JButton cancelButton   = new JButton("Cancel");
        applyPanel.add(applyRecording);
        applyPanel.add(applyExperiment);
        applyPanel.add(applyProject);
        applyPanel.add(cancelButton);
        add(applyPanel, BorderLayout.SOUTH);

        // Listeners
        ChangeListener sliderListener = (ChangeEvent e) -> {
            updateValueLabels();
            propagateValues();
        };
        densityRatioSlider.addChangeListener(sliderListener);
        variabilitySlider.addChangeListener(sliderListener);
        rSquaredSlider.addChangeListener(sliderListener);

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
        origVariability  = initParams.variability;
        origRSquared     = initParams.rSquared;
        origNeighbourMode = initParams.neighbourMode;

        updateValueLabels();
        propagateValues();

        pack();
        setLocationRelativeTo(owner);
    }

    private JSlider createSlider(int min, int max, int value) {
        int safe = Math.max(min, Math.min(max, value));
        JSlider slider = new JSlider(JSlider.VERTICAL, min, max, safe);
        slider.setMajorTickSpacing(Math.max(1, (max - min) / 5));
        slider.setPaintTicks(true);
        slider.setPaintLabels(false);
        return slider;
    }

    private JPanel wrapSlider(JSlider slider, String title, JLabel valueLabel, boolean isDouble, int scale) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(slider, BorderLayout.CENTER);

        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(valueLabel, BorderLayout.SOUTH);

        int major = slider.getMajorTickSpacing();
        if (major > 0) {
            Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
            int min = slider.getMinimum();
            int max = slider.getMaximum();
            for (int v = min; v <= max; v += major) {
                if (isDouble) {
                    if (scale == 100) {
                        double real = v / 100.0;
                        labelTable.put(v, new JLabel(ONE_DEC.format(real)));
                    } else {
                        int intLabel = v / scale;
                        labelTable.put(v, new JLabel(Integer.toString(intLabel)));
                    }
                } else {
                    labelTable.put(v, new JLabel(String.valueOf(v)));
                }
            }
            slider.setLabelTable(labelTable);
            slider.setPaintLabels(true);
        }

        return panel;
    }

    private void updateValueLabels() {
        densityRatioValue.setText(ONE_DEC.format(densityRatioSlider.getValue() / 10.0));
        variabilityValue.setText(ONE_DEC.format(variabilitySlider.getValue() / 10.0));
        rSquaredValue.setText(ONE_DEC.format(rSquaredSlider.getValue() / 100.0));
    }

    private void propagateValues() {
        viewerFrame.updateSquareControlParameters(
                densityRatioSlider.getValue() / 10.0,
                variabilitySlider.getValue() / 10.0,
                rSquaredSlider.getValue() / 100.0,
                0, 0, getNeighbourMode()
        );
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

        if ("Free".equals(origNeighbourMode)) neighbourFree.setSelected(true);
        else if ("Relaxed".equals(origNeighbourMode)) neighbourRelaxed.setSelected(true);
        else neighbourStrict.setSelected(true);

        updateValueLabels();
        propagateValues();
    }

    private SquareControlParams collectParams() {
        return new SquareControlParams(
                densityRatioSlider.getValue() / 10.0,
                variabilitySlider.getValue() / 10.0,
                rSquaredSlider.getValue() / 100.0,
                0, 0, getNeighbourMode()
        );
    }
}