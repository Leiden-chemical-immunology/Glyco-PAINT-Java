package viewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;




/**
 * Dialog for adjusting square control parameters
 * (density ratio, variability, R², duration, neighbour mode)
 * and applying them to a Recording / Experiment / Project scope.
 */
public class SquareControlDialog extends JDialog {

    private final RecordingViewerFrame parentFrame;
    private final SquareGridPanel gridPanel;

    private final JSpinner densitySpinner;
    private final JSpinner variabilitySpinner;
    private final JSpinner r2Spinner;
    private final JSpinner minDurationSpinner;
    private final JSpinner maxDurationSpinner;
    private final JComboBox<String> neighbourBox;

    private boolean cancelled = true;

    public SquareControlDialog(
            RecordingViewerFrame parentFrame,
            SquareGridPanel gridPanel,
            Frame owner,
            SquareControlParams defaults
    ) {
        super(owner, "Square Control Parameters", true);
        this.parentFrame = parentFrame;
        this.gridPanel = gridPanel;

        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 6));

        densitySpinner = new JSpinner(new SpinnerNumberModel(defaults.densityRatio, 0.0, 10.0, 0.01));
        variabilitySpinner = new JSpinner(new SpinnerNumberModel(defaults.variability, 0.0, 10.0, 0.01));
        r2Spinner = new JSpinner(new SpinnerNumberModel(defaults.rSquared, 0.0, 1.0, 0.01));
        minDurationSpinner = new JSpinner(new SpinnerNumberModel(defaults.minDuration, 0, 10000, 10));
        maxDurationSpinner = new JSpinner(new SpinnerNumberModel(defaults.maxDuration, 0, 10000, 10));
        neighbourBox = new JComboBox<>(new String[]{"Free", "Fixed", "Neighbour"});
        neighbourBox.setSelectedItem(defaults.neighbourMode);

        form.add(new JLabel("Min Density Ratio:"));
        form.add(densitySpinner);
        form.add(new JLabel("Max Variability:"));
        form.add(variabilitySpinner);
        form.add(new JLabel("Min R²:"));
        form.add(r2Spinner);
        form.add(new JLabel("Min Duration:"));
        form.add(minDurationSpinner);
        form.add(new JLabel("Max Duration:"));
        form.add(maxDurationSpinner);
        form.add(new JLabel("Neighbour Mode:"));
        form.add(neighbourBox);

        add(form, BorderLayout.CENTER);

        // --- Bottom buttons ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton("Cancel");
        JButton applyBtn = new JButton("Apply");
        buttons.add(applyBtn);
        buttons.add(cancelBtn);

        JPanel scopePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel applyLabel = new JLabel("Apply scope:");
        JButton applyRecordingBtn = new JButton("Recording");
        JButton applyExperimentBtn = new JButton("Experiment");
        JButton applyProjectBtn = new JButton("Project");
        for (JButton b : Arrays.asList(applyRecordingBtn, applyExperimentBtn, applyProjectBtn)) {
            b.setFocusPainted(false);
        }

        scopePanel.add(applyLabel);
        scopePanel.add(applyRecordingBtn);
        scopePanel.add(applyExperimentBtn);
        scopePanel.add(applyProjectBtn);

        bottomPanel.add(scopePanel, BorderLayout.WEST);
        bottomPanel.add(buttons, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- Actions ---
        applyBtn.addActionListener(e -> {
            cancelled = false;
            applyLocally();
            dispose();
        });

        cancelBtn.addActionListener(e -> dispose());

        applyRecordingBtn.addActionListener(e -> {
            cancelled = false;
            applyLocally();
            parentFrame.applySquareControlParameters("Recording", buildParams());
            dispose();
        });

        applyExperimentBtn.addActionListener(e -> {
            cancelled = false;
            applyLocally();
            parentFrame.applySquareControlParameters("Experiment", buildParams());
            dispose();
        });

        applyProjectBtn.addActionListener(e -> {
            cancelled = false;
            applyLocally();
            parentFrame.applySquareControlParameters("Project", buildParams());
            dispose();
        });

        pack();
        setLocationRelativeTo(owner);
    }

    private void applyLocally() {
        parentFrame.updateSquareControlParameters(
                (double) densitySpinner.getValue(),
                (double) variabilitySpinner.getValue(),
                (double) r2Spinner.getValue(),
                (int) minDurationSpinner.getValue(),
                (int) maxDurationSpinner.getValue(),
                (String) neighbourBox.getSelectedItem()
        );
    }

    private SquareControlParams buildParams() {
        return new SquareControlParams(
                (double) densitySpinner.getValue(),
                (double) variabilitySpinner.getValue(),
                (double) r2Spinner.getValue(),
                (int) minDurationSpinner.getValue(),
                (int) maxDurationSpinner.getValue(),
                (String) neighbourBox.getSelectedItem()
        );
    }

    public boolean isCancelled() {
        return cancelled;
    }
}