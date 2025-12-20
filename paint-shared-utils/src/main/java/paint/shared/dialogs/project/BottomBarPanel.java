// =================================================================================================
//  File: src/main/java/paint/shared/dialogs/project/BottomBarPanel.java
// =================================================================================================

/* =================================================================================================
 *  PURPOSE
 *      Bottom bar component used by ProjectDialog. Provides the OK/Cancel buttons, the Verbose
 *      toggle, and (in TRACKMATE mode) the Sweep toggle. It offers callback hooks so the dialog
 *      controller can respond to user actions in a decoupled way.
 *
 *  DESCRIPTION
 *      This panel builds and wires the bottom toolbar used in PAINT project dialogs. It exposes
 *      callback registration methods (onOk, onCancel, onVerboseToggle, onSweepToggle) and internally
 *      manages UI state such as enabling, disabled states, and transient feedback such as "Running…"
 *      and "Completed". Verbose and Sweep (when available) act as behavioral modifiers for the run.
 *
 *  KEY FEATURES
 *      - Decoupled event callbacks using Runnable and Consumer<Boolean>.
 *      - Uses method references for clean event wiring.
 *      - Provides consistent UI state updates for Running, Completed, OK/Cancel, etc.
 *      - Gracefully handles null callbacks via NO_OP and NO_OP_BOOL fallbacks.
 *
 *  AUTHOR
 *      PAINT Toolkit
 *
 *  MODULE
 *      paint.shared.dialogs.project
 *
 *  UPDATED
 *      2025-11-21
 *
 *  COPYRIGHT
 *      Copyright (c) 2020–2025.
 *      All rights reserved.
 * =================================================================================================
 */

package paint.shared.dialogs.project;

import paint.shared.utils.PaintRuntime;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import static paint.shared.dialogs.ProjectDialog.DialogMode;

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
            saveExperiments = new JCheckBox("Save Experiments", false);
            left.add(sweep);
            left.add(saveExperiments);
        } else {
            sweep           = null;
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
}