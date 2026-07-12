/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.ui.dialogs.project;

import paint.shared.utils.PaintPrefs;
import paint.shared.utils.PaintRuntime;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import static paint.ui.dialogs.ProjectDialog.DialogMode;

/**
 * Bottom bar panel containing Save Experiments, Verbose toggle, optional Sweep toggle (TRACKMATE),
 * and OK/Cancel buttons. The controller registers callbacks using onOk(), onCancel(),
 * onVerboseToggle(), and onSweepToggle(). This panel handles wiring the Swing events into those
 * callbacks and updating UI state (running, completed, enabled, etc.).
 */
public class BottomBarPanel {

    private final             JPanel    panel = new JPanel(new BorderLayout());

    private final             JCheckBox saveExperiments;
    private final             JCheckBox verbose;
    private final             JCheckBox sweep; // TRACKMATE only
    private final             JButton   okBtn;
    private final             JButton   cancelBtn;

    /** Where the durable UI preferences live — the same store that remembers Verbose. */
    private static final      String   PREFS_SECTION         = "Runtime";
    private static final      String   PREF_SAVE_EXPERIMENTS = "Save Experiments";

    // Default no-op callbacks so invocation is always safe
    private static final      Runnable NO_OP = () -> {};
    private static final      Consumer<Boolean> NO_OP_BOOL = b -> {};

    private Runnable          onOk      = NO_OP;
    private Runnable          onCancel  = NO_OP;
    private Consumer<Boolean> onVerbose = NO_OP_BOOL;
    private Consumer<Boolean> onSweep   = NO_OP_BOOL;

    /**
     * Constructs the bottom bar for the given mode.
     * In TRACKMATE mode, the Sweep checkbox is shown; otherwise omitted.
     */
    public BottomBarPanel(DialogMode mode, boolean verboseDefault) {
        JPanel left  = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        verbose         = new JCheckBox("Verbose", verboseDefault);
        left.add(verbose);

        // Sweep checkbox only exists in TRACKMATE mode
        if (mode == DialogMode.TRACKMATE_BATCH) {
            sweep           = new JCheckBox("Sweep", false);
            left.add(sweep);
        } else {
            sweep           = null;
        }

        // SaveExperiments checkbox not in TRACKMATE_SINGLE mode.
        //
        // This is a durable preference, like Verbose: "remember which experiments I picked".
        // It used to be hardcoded to false on every open, so the user had to re-tick it every
        // single run — and if they forgot, ProjectDialogController silently skipped writing the
        // selection and it was lost. Seed it from the stored preference and write the
        // preference back the moment it is toggled.
        if (mode != DialogMode.TRACKMATE_SINGLE) {
            saveExperiments = new JCheckBox(
                    "Save Experiments",
                    PaintPrefs.getBoolean(PREFS_SECTION, PREF_SAVE_EXPERIMENTS, false));
            saveExperiments.setToolTipText(
                    "Remember the selected experiments and pre-select them next time, "
                            + "in this and the other Paint tools.");
            saveExperiments.addActionListener(e ->
                    PaintPrefs.putBoolean(PREFS_SECTION, PREF_SAVE_EXPERIMENTS,
                            saveExperiments.isSelected()));
            left.add(saveExperiments);
        } else {
            saveExperiments = null;
        }

        okBtn     = new JButton("OK");
        cancelBtn = new JButton("Cancel");
        right.add(okBtn);
        right.add(cancelBtn);

        panel.add(left,  BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);

        verbose.addActionListener(this::handleVerbose);
        if (sweep != null) {
            sweep.addActionListener(this::handleSweep);
        }
        okBtn.addActionListener(this::handleOk);
        cancelBtn.addActionListener(this::handleCancel);
    }

    /**
     * Returns the Swing component for embedding.
     */
    public JPanel component() {
        return panel;
    }

    /**
     * Registers the callback invoked when OK is pressed.
     * If null is passed, it resets to a no-op callback.
     */
    public void onOk(Runnable r) {
        this.onOk = (r != null ? r : NO_OP);
    }

    /**
     * Registers the callback invoked when Cancel is pressed.
     * If null is passed, it resets to a no-op callback.
     */
    public void onCancel(Runnable r) {
        this.onCancel = (r != null ? r : NO_OP);
    }

    /**
     * Registers a callback for when the Verbose checkbox toggles.
     * NOTE: Current implementation resets onVerbose to a no-op. This is called once by the controller.
     */
    public void onVerboseToggle(Consumer<Boolean> c) {
        this.onVerbose = (c != null ? c : NO_OP_BOOL);
    }

    /**
     * Registers a callback for when the Sweep checkbox toggles.
     * If null is passed, it resets to a no-op Boolean consumer.
     */
    public void onSweepToggle(Consumer<Boolean> c) {
        this.onSweep = (c != null ? c : NO_OP_BOOL);
    }

    /**
     * Enables or disables all interactive widgets.
     * Note: the controller should generally NOT use this during a run,
     * so that Cancel can remain enabled.
     */
    public void setEnabled(boolean enabled) {
        panel.setEnabled(enabled);

        if (saveExperiments != null) {
            saveExperiments.setEnabled(enabled);
        }
        verbose.setEnabled(enabled);
        if (sweep != null) {
            sweep.setEnabled(enabled);
        }
        okBtn.setEnabled(enabled);
        cancelBtn.setEnabled(enabled);
    }

    public void updateOkEnabled(boolean enabled) {
        okBtn.setEnabled(enabled);
    }

    /**
     * Displays the transient “Running…” state when a background task starts.
     */
    public void showRunning() {
        okBtn.setText("Running...");
        okBtn.setEnabled(false);
        cancelBtn.setEnabled(true);
    }

    /**
     * Displays the “Completed” state for a short time, then restores OK.
     */
    public void showCompleted(boolean keepEnabled) {
        okBtn.setText("Completed");
        okBtn.setEnabled(keepEnabled);
        new javax.swing.Timer(1500, evt -> okBtn.setText("OK")).start();
    }

    /**
     * Restores the OK button label and enabled state.
     */
    public void resetOk(boolean enabled) {
        okBtn.setText("OK");
        okBtn.setEnabled(enabled);
        cancelBtn.setEnabled(true);
    }

    /**
     * Returns whether the “Sweep” mode checkbox is selected.
     * Only meaningful when mode is TRACKMATE; otherwise returns false.
     */
    public boolean isSweepSelected() {
        return sweep != null && sweep.isSelected();
    }

    /**
     * Sets the Sweep checkbox selection state if it exists.
     */
    public void setSweepSelected(boolean selected) {
        if (sweep != null) {
            sweep.setSelected(selected);
        }
    }

    /**
     * Event handler for the Verbose checkbox.
     */
    @SuppressWarnings("unused")
    private void handleVerbose(ActionEvent e) {
        boolean value = verbose.isSelected();
        PaintRuntime.setVerbose(value);
        onVerbose.accept(value);
    }

    /**
     * Event handler for the optional Sweep checkbox.
     */
    @SuppressWarnings("unused")
    private void handleSweep(ActionEvent e) {
        onSweep.accept(sweep.isSelected());
    }

    /**
     * Event handler for the OK button.
     */
    @SuppressWarnings("unused")
    private void handleOk(ActionEvent e) {
        onOk.run();
    }

    /**
     * Event handler for the Cancel button.
     */
    @SuppressWarnings("unused")
    private void handleCancel(ActionEvent e) {
        onCancel.run();
    }

    /**
     * Show UI state while cancellation is in progress.
     * Cancel is disabled to avoid double clicks.
     */
    public void showStopping() {
        okBtn.setText("Stopping...");
        okBtn.setEnabled(false);
        cancelBtn.setEnabled(false);
    }

    public void keepCancelEnabled() {
        cancelBtn.setEnabled(true);
    }

    public boolean isSaveExperimentsSelected() {
        return saveExperiments != null && saveExperiments.isSelected();
    }
}