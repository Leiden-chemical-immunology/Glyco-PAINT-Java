package viewer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Square Control Dialog (3-slider version)
 * - Density Ratio, Variability, R² sliders
 * - Border + Number display controls
 * - Neighbour mode (Free / Relaxed / Strict)
 * Compatible with new SquareGridPanel that loads squares once.
 */
public class SquareControlDialog extends JDialog {

    private final JCheckBox showBordersCheckBox;
    private final JCheckBox showShadingCheckBox;

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
    private final SquareGridPanel gridPanel;

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
        this.gridPanel = gridPanel;

        setLayout(new BorderLayout(10, 10));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // --- Numbers first so they're initialized before borders listener ---
        numberNoneRadio = new JRadioButton("Show no number", true);
        numberLabelRadio = new JRadioButton("Show label number");
        numberSquareRadio = new JRadioButton("Show square number");

        ButtonGroup numbersGroup = new ButtonGroup();
        numbersGroup.add(numberNoneRadio);
        numbersGroup.add(numberLabelRadio);
        numbersGroup.add(numberSquareRadio);

        numberNoneRadio.addActionListener(e -> {
            gridPanel.setNumberMode(SquareGridPanel.NumberMode.NONE);
            viewerFrame.updateSquareNumberMode(SquareGridPanel.NumberMode.NONE);
        });

        numberLabelRadio.addActionListener(e -> {
            gridPanel.setNumberMode(SquareGridPanel.NumberMode.LABEL);
            viewerFrame.updateSquareNumberMode(SquareGridPanel.NumberMode.LABEL);
        });

        numberSquareRadio.addActionListener(e -> {
            gridPanel.setNumberMode(SquareGridPanel.NumberMode.SQUARE);
            viewerFrame.updateSquareNumberMode(SquareGridPanel.NumberMode.SQUARE);
        });

        JPanel numbersInner = new JPanel();
        numbersInner.setLayout(new BoxLayout(numbersInner, BoxLayout.Y_AXIS));
        numbersInner.add(numberNoneRadio);
        numbersInner.add(numberLabelRadio);
        numbersInner.add(numberSquareRadio);

        JPanel numbersPanel = new JPanel(new BorderLayout());
        numbersPanel.setBorder(BorderFactory.createTitledBorder("Numbers"));
        numbersPanel.add(numbersInner, BorderLayout.WEST);

        // --- Borders & Shading ---
        showBordersCheckBox = new JCheckBox("Show borders of selected squares", true);
        showBordersCheckBox.addActionListener(e -> {
            boolean show = showBordersCheckBox.isSelected();
            gridPanel.setShowBorders(show);

            if (!show) {
                if (numberLabelRadio.isSelected()) {
                    lastNumberMode = SquareGridPanel.NumberMode.LABEL;
                } else if (numberSquareRadio.isSelected()) {
                    lastNumberMode = SquareGridPanel.NumberMode.SQUARE;
                } else {
                    lastNumberMode = SquareGridPanel.NumberMode.NONE;
                }

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

        // 🔹 NEW checkbox for shading
        showShadingCheckBox = new JCheckBox("Show shading of selected squares", true);
        showShadingCheckBox.addActionListener(e -> {
            boolean show = showShadingCheckBox.isSelected();
            gridPanel.setShowShading(show);
        });

        // 🔹 Combine both into one titled panel
        JPanel bordersPanel = new JPanel();
        bordersPanel.setLayout(new BoxLayout(bordersPanel, BoxLayout.Y_AXIS));
        bordersPanel.setBorder(BorderFactory.createTitledBorder("Borders & Shading"));
        bordersPanel.add(showBordersCheckBox);
        bordersPanel.add(showShadingCheckBox);

        content.add(bordersPanel);
        content.add(Box.createVerticalStrut(10));
        content.add(numbersPanel);
        content.add(Box.createVerticalStrut(10));

        // --- Three sliders ---
        densityRatioSlider = createSlider(0, 200, (int) Math.round(initParams.densityRatio * 10));
        variabilitySlider = createSlider(0, 200, (int) Math.round(initParams.variability * 10));
        rSquaredSlider = createSlider(0, 100, (int) Math.round(initParams.rSquared * 100));

        densityRatioValue = new JLabel();
        variabilityValue = new JLabel();
        rSquaredValue = new JLabel();

        JPanel slidersPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        slidersPanel.add(wrapSlider(densityRatioSlider, "Min Required Density Ratio", densityRatioValue));
        slidersPanel.add(wrapSlider(variabilitySlider, "Max Allowable Variability", variabilityValue));
        slidersPanel.add(wrapSlider(rSquaredSlider, "Min Required R²", rSquaredValue));

        content.add(slidersPanel);
        content.add(Box.createVerticalStrut(10));

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
        content.add(neighbourPanel);
        content.add(Box.createVerticalStrut(10));

        add(content, BorderLayout.CENTER);

        // --- Apply / Cancel ---
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

        applyRecording.addActionListener(e -> viewerFrame.applySquareControlParameters("Recording", collectParams()));
        applyExperiment.addActionListener(e -> viewerFrame.applySquareControlParameters("Experiment", collectParams()));
        applyProject.addActionListener(e -> viewerFrame.applySquareControlParameters("Project", collectParams()));
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
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(true);

        // Scale labels: /10 for 0..200 sliders, /100 for 0..100 (R²)
        int major = Math.max(1, (max - min) / 5);
        boolean isRSquared = (max == 100);
        double divisor = isRSquared ? 100.0 : 10.0;

        java.util.Hashtable<Integer, JLabel> table = new java.util.Hashtable<>();
        for (int v = min; v <= max; v += major) {
            String text = isRSquared ? ONE_DEC.format(v / divisor)       // keep 1 decimal for R²
                    : String.valueOf((int) Math.round(v / divisor)); // integer for the others
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
        viewerFrame.updateSquareControlParameters(
                densityRatioSlider.getValue() / 10.0,
                variabilitySlider.getValue() / 10.0,
                rSquaredSlider.getValue() / 100.0,
                getNeighbourMode()
        );
    }

    private String getNeighbourMode() {
        if (neighbourFree.isSelected()) {
            return "Free";
        }
        if (neighbourRelaxed.isSelected()) {
            return "Relaxed";
        }
        return "Strict";
    }

    private void restoreOriginals() {
        densityRatioSlider.setValue((int) Math.round(origDensityRatio * 10));
        variabilitySlider.setValue((int) Math.round(origVariability * 10));
        rSquaredSlider.setValue((int) Math.round(origRSquared * 100));

        if ("Free".equals(origNeighbourMode)) {
            neighbourFree.setSelected(true);
        } else if ("Relaxed".equals(origNeighbourMode)) {
            neighbourRelaxed.setSelected(true);
        } else {
            neighbourStrict.setSelected(true);
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