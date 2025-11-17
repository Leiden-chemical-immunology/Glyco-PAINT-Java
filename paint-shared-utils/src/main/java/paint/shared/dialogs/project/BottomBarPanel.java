package paint.shared.dialogs.project;

import paint.shared.utils.PaintRuntime;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

import static paint.shared.dialogs.ProjectDialog.DialogMode;

public class BottomBarPanel {

    private final JPanel panel = new JPanel(new BorderLayout());

    private final JCheckBox saveExperiments;
    private final JCheckBox verbose;
    private final JCheckBox sweep; // TRACKMATE only

    private final JButton okBtn;
    private final JButton cancelBtn;

    private Runnable onOk = () -> {
    };

    private Runnable onCancel = () -> {
    };

    private Consumer<Boolean> onVerbose = v -> {
    };

    private Consumer<Boolean> onSweep = s -> {
    };

    public BottomBarPanel(DialogMode mode, boolean verboseDefault) {
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        saveExperiments = new JCheckBox("Save Experiments", false);
        verbose = new JCheckBox("Verbose", verboseDefault);

        left.add(saveExperiments);
        left.add(verbose);

        if (mode == DialogMode.TRACKMATE) {
            sweep = new JCheckBox("Sweep", false);
            left.add(sweep);
        } else {
            sweep = null;
        }

        okBtn = new JButton("OK");
        cancelBtn = new JButton("Cancel");
        right.add(okBtn);
        right.add(cancelBtn);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);

        // wire
        verbose.addActionListener(e -> {
            PaintRuntime.setVerbose(verbose.isSelected());
            onVerbose.accept(verbose.isSelected());
        });

        if (sweep != null) {
            sweep.addActionListener(e -> onSweep.accept(sweep.isSelected()));
        }

        okBtn.addActionListener(e -> onOk.run());
        cancelBtn.addActionListener(e -> onCancel.run());
    }

    public JPanel component() {
        return panel;
    }

    public void onOk(Runnable r) {
        this.onOk = (r != null ? r : () -> {
        });
    }

    public void onCancel(Runnable r) {
        this.onCancel = (r != null ? r : () -> {
        });
    }

    public void onVerboseToggle() {
        this.onVerbose = v -> {
        };
    }

    public void onSweepToggle(Consumer<Boolean> c) {
        this.onSweep = (c != null ? c : s -> {
        });
    }

    public void setEnabled(boolean enabled) {
        saveExperiments.setEnabled(enabled);
        verbose.setEnabled(enabled);
        okBtn.setEnabled(enabled);
        cancelBtn.setEnabled(enabled);
    }

    public boolean isSaveExperiments() {
        return saveExperiments.isSelected();
    }

    public void updateOkEnabled(boolean enabled) {
        okBtn.setEnabled(enabled);
    }

    public void showRunning() {
        okBtn.setText("Running...");
        okBtn.setEnabled(false);
    }

    public void showCompleted(boolean keepEnabled) {
        okBtn.setText("Completed");
        okBtn.setEnabled(keepEnabled);
        new javax.swing.Timer(1500, evt -> okBtn.setText("OK")).start();
    }

    public void resetOk(boolean enabled) {
        okBtn.setText("OK");
        okBtn.setEnabled(enabled);
    }

    /**
     * Returns whether the “Sweep” mode checkbox is selected.
     * Only meaningful when mode is TRACKMATE; otherwise returns false.
     */
    public boolean isSweepSelected() {
        return sweep != null && sweep.isSelected();
    }

    public void setSweepSelected(boolean selected) {
        if (sweep != null) {
            sweep.setSelected(selected);
        }
    }
}
