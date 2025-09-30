package viewer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.io.IOException;

public class SquareControlDialog extends JDialog {

    private final JCheckBox showBordersCheckBox;
    private final JRadioButton numberNoneRadio;
    private final JRadioButton numberLabelRadio;
    private final JRadioButton numberSquareRadio;

    private final JSlider densityRatioSlider;
    private final JSlider variabilitySlider;
    private final JSlider rSquaredSlider;
    private final JSlider minDurationSlider;
    private final JSlider maxDurationSlider;

    private final JRadioButton neighbourFree;
    private final JRadioButton neighbourRelaxed;
    private final JRadioButton neighbourStrict;

    private final RecordingViewerFrame viewerFrame;

    // Remember last selected number mode when borders are ON
    private SquareGridPanel.NumberMode lastNumberMode = SquareGridPanel.NumberMode.NONE;

    // Remember original values (for Cancel)
    private int origDensityRatio;
    private int origVariability;
    private int origRSquared;
    private int origMinDuration;
    private int origMaxDuration;
    private String origNeighbourMode;

    public SquareControlDialog(JFrame owner, SquareGridPanel gridPanel, RecordingViewerFrame viewerFrame) {
        super(owner, "Square Controls", false);
        this.viewerFrame = viewerFrame;

        setLayout(new BorderLayout(10, 10));
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // --- Radio buttons for numbers ---
        numberNoneRadio = new JRadioButton("Show no number", true);
        numberLabelRadio = new JRadioButton("Show label number");
        numberSquareRadio = new JRadioButton("Show square number");

        ButtonGroup group = new ButtonGroup();
        group.add(numberNoneRadio);
        group.add(numberLabelRadio);
        group.add(numberSquareRadio);

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

        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));
        radioPanel.setBorder(BorderFactory.createTitledBorder("Numbers"));
        radioPanel.add(numberNoneRadio);
        radioPanel.add(numberLabelRadio);
        radioPanel.add(numberSquareRadio);

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

        content.add(showBordersCheckBox);
        content.add(Box.createVerticalStrut(10));
        content.add(radioPanel);

        // --- Sliders ---
        densityRatioSlider = createSlider(0, 100, 50, "Min Required Density Ratio");
        variabilitySlider = createSlider(0, 100, 50, "Max Allowable Variability");
        rSquaredSlider = createSlider(0, 100, 50, "Min Required R²");
        minDurationSlider = createSlider(0, 1000, 100, "Min Longest Duration");
        maxDurationSlider = createSlider(0, 1000, 500, "Max Longest Duration");

        content.add(densityRatioSlider);
        content.add(variabilitySlider);
        content.add(rSquaredSlider);
        content.add(minDurationSlider);
        content.add(maxDurationSlider);

        // --- Neighbour mode radios ---
        JPanel neighbourPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        neighbourPanel.setBorder(BorderFactory.createTitledBorder("Neighbour Mode"));

        neighbourFree = new JRadioButton("Free", true);
        neighbourRelaxed = new JRadioButton("Relaxed");
        neighbourStrict = new JRadioButton("Strict");

        ButtonGroup neighbourGroup = new ButtonGroup();
        neighbourGroup.add(neighbourFree);
        neighbourGroup.add(neighbourRelaxed);
        neighbourGroup.add(neighbourStrict);

        neighbourPanel.add(neighbourFree);
        neighbourPanel.add(neighbourRelaxed);
        neighbourPanel.add(neighbourStrict);

        content.add(neighbourPanel);

        add(content, BorderLayout.CENTER);

        // --- Buttons ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyRecording = new JButton("Apply to Recording");
        JButton applyExperiment = new JButton("Apply to Experiment");
        JButton applyProject = new JButton("Apply to Project");
        JButton cancelButton = new JButton("Cancel");

        buttonPanel.add(applyRecording);
        buttonPanel.add(applyExperiment);
        buttonPanel.add(applyProject);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // --- Listeners ---
        ChangeListener sliderListener = (ChangeEvent e) -> propagateValues();
        densityRatioSlider.addChangeListener(sliderListener);
        variabilitySlider.addChangeListener(sliderListener);
        rSquaredSlider.addChangeListener(sliderListener);
        minDurationSlider.addChangeListener(sliderListener);
        maxDurationSlider.addChangeListener(sliderListener);

        Action neighbourListener = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                propagateValues();
            }
        };
        neighbourFree.addActionListener(neighbourListener);
        neighbourRelaxed.addActionListener(neighbourListener);
        neighbourStrict.addActionListener(neighbourListener);

        applyRecording.addActionListener(e -> saveParameters("recording_params.csv"));
        applyExperiment.addActionListener(e -> saveParameters("experiment_params.csv"));
        applyProject.addActionListener(e -> saveParameters("project_params.csv"));

        cancelButton.addActionListener(e -> {
            restoreOriginals();
            dispose();
        });

        // Save originals
        origDensityRatio = densityRatioSlider.getValue();
        origVariability = variabilitySlider.getValue();
        origRSquared = rSquaredSlider.getValue();
        origMinDuration = minDurationSlider.getValue();
        origMaxDuration = maxDurationSlider.getValue();
        origNeighbourMode = getNeighbourMode();

        pack();
        setLocationRelativeTo(owner);
    }

    private JSlider createSlider(int min, int max, int value, String title) {
        JSlider slider = new JSlider(min, max, value);
        slider.setMajorTickSpacing((max - min) / 5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setBorder(BorderFactory.createTitledBorder(title));
        return slider;
    }

    private void propagateValues() {
        viewerFrame.updateSquareControlParameters(
                densityRatioSlider.getValue(),
                variabilitySlider.getValue(),
                rSquaredSlider.getValue(),
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
        rSquaredSlider.setValue(origRSquared);
        minDurationSlider.setValue(origMinDuration);
        maxDurationSlider.setValue(origMaxDuration);

        if ("Free".equals(origNeighbourMode)) neighbourFree.setSelected(true);
        else if ("Relaxed".equals(origNeighbourMode)) neighbourRelaxed.setSelected(true);
        else neighbourStrict.setSelected(true);

        propagateValues();
    }

    private void saveParameters(String filename) {
        try (FileWriter fw = new FileWriter(filename)) {
            fw.write("DensityRatio," + densityRatioSlider.getValue() + "\n");
            fw.write("Variability," + variabilitySlider.getValue() + "\n");
            fw.write("RSquared," + rSquaredSlider.getValue() + "\n");
            fw.write("MinDuration," + minDurationSlider.getValue() + "\n");
            fw.write("MaxDuration," + maxDurationSlider.getValue() + "\n");
            fw.write("NeighbourMode," + getNeighbourMode() + "\n");
            System.out.println("Parameters saved to " + filename);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}