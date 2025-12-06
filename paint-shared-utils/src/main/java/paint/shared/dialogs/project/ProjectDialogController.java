// =================================================================================================
//  File: src/main/java/paint/shared/dialogs/project/ProjectDialogController.java
// =================================================================================================

/* =================================================================================================
 *  PURPOSE
 *      Controller class coordinating UI behavior for the ProjectDialog. It responds to user input,
 *      updates panels, validates run conditions, and delegates background execution and cancellation
 *      to the ProjectDialog via functional interfaces.
 *
 *  DESCRIPTION
 *      This controller is the glue between the dialog panels (paths, parameters, experiments, bottom
 *      bar) and the dialog logic. It does not own any UI components; instead it receives references
 *      to getters, setters, and worker functions from ProjectDialog. It handles:
 *          - browse button actions
 *          - enabling/disabling OK depending on validity
 *          - sweep configuration creation
 *          - executing the worker (via QuadRunnable)
 *          - managing cancellation behavior
 *          - EDT-safe updates
 *
 *  KEY FEATURES
 *      - Clean method-reference-based communication with ProjectDialog.
 *      - Uses Supplier, Consumer, Runnable, and QuadRunnable to remain fully decoupled.
 *      - Handles validation logic for when the OK button may be enabled.
 *      - Performs sweep creation checks.
 *      - Coordinates UI disable/enable states during background work.
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

import paint.shared.config.paintconfig.PaintConfig;
import paint.shared.utils.PaintLogger;
import paint.shared.utils.PaintPrefs;

import javax.swing.*;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static paint.shared.dialogs.ProjectDialog.DialogMode;

/**
 * Controller for ProjectDialog. It reacts to UI events coming from the panels,
 * controls enabling/disabling of buttons, performs validations, and manages the
 * interaction with the background worker (through QuadRunnable and Supplier/Consumer callbacks).
 */
public class ProjectDialogController {

    private final DialogMode          mode;
    private final JDialog             dialog;
    private final PaintConfig         paintConfig;
    private       boolean             workerStarted = false;

    // Project root getter/setter supplied by the dialog
    private final Supplier<Path>      getProjectPath;
    private final Consumer<Path>      setProjectPath;

    // UI panels
    private final ProjectPathsPanel   paths;
    private final SquaresParamsPanel  params; // null in VIEWER mode
    private final ExperimentsPanel    experiments;
    private final BottomBarPanel      bottom;

    // Worker logic references provided by ProjectDialog
    private final QuadRunnable        startWorker;
    private final Supplier<Thread>    getWorker;
    private final Runnable            setCancelled;
    private final Runnable            clearCancelled;

    // A callback provided by ProjectDialog to re-enable ALL UI elements
    private final Runnable enableAllUiFromDialog;

    // ----------------------------------------------------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------------------------------------------------

    public ProjectDialogController(
            DialogMode         mode,
            JDialog            dialog,
            PaintConfig        paintConfig,
            Supplier<Path>     getProjectPath,
            Consumer<Path>     setProjectPath,
            ProjectPathsPanel  paths,
            SquaresParamsPanel params,
            ExperimentsPanel   experiments,
            BottomBarPanel     bottom,
            QuadRunnable       startWorker,
            Supplier<Thread>   getWorker,
            Runnable           setCancelled,
            Runnable           clearCancelled,
            Runnable           enableAllUiFromDialog
    ) {
        this.mode           = mode;
        this.dialog         = dialog;
        this.paintConfig    = paintConfig;
        this.getProjectPath = getProjectPath;
        this.setProjectPath = setProjectPath;
        this.paths          = paths;
        this.params         = params;
        this.experiments    = experiments;
        this.bottom         = bottom;
        this.startWorker    = startWorker;
        this.getWorker      = getWorker;
        this.setCancelled   = setCancelled;
        this.clearCancelled = clearCancelled;

        this.enableAllUiFromDialog = enableAllUiFromDialog;
    }

    // ----------------------------------------------------------------------------------------------------
    //  Initialization
    // ----------------------------------------------------------------------------------------------------

    public void init() {

        // Browsing
        paths.onBrowseProject(this::handleBrowseProject);
        paths.onBrowseImages(this::handleBrowseImages);

        // Text changes → OK may update
        paths.onRootsChanged(this::updateOk);
        experiments.onSelectionChanged(this::updateOk);

        // Parameter changes (TrackMate only)
        if (params != null) {
            params.onParamsChanged(this::updateOk);
        }

        // Sweep toggle
        bottom.onVerboseToggle();
        bottom.onSweepToggle(this::onSweepToggle);

        // OK/Cancel
        bottom.onOk(this::handleOk);
        bottom.onCancel(this::handleCancel);

        // Initial state
        updateOk();
    }

    // ----------------------------------------------------------------------------------------------------
    //  OK
    // ----------------------------------------------------------------------------------------------------

    private void handleOk() {

        if (mode == DialogMode.TRACKMATE) {
            final String img = paths.imagesRootText().trim();
            if (!new File(img).isDirectory()) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "The Images Root directory does not exist. Please select a valid directory.",
                        "Invalid Images Root",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
        }

        PaintLogger.debugf("ProjectDialogController.handleOK - project path: %s", paths.imagesRootText());
        PaintLogger.debugf("ProjectDialogController.handleOK - images  path: %s", paths.projectRootText());
        PaintLogger.debugf("ProjectDialogController.handleOK - experiments : %s", experiments.selectedExperimentNames());

        PaintPrefs.putString("Path", "Project Root", paths.projectRootText());
        PaintPrefs.putString("Path", "Images Root", paths.imagesRootText());

        clearCancelled.run();
        workerStarted = true;

        Runnable uiDisable = () -> {
            setInputsEnabled(false);
            bottom.showRunning();
        };

        Runnable uiEnable = () -> {
            setInputsEnabled(true);
            bottom.resetOk(mode == DialogMode.VIEWER);
        };

        Runnable onSuccess = () -> {
            PaintLogger.blankline();
            PaintLogger.infof("Operation completed successfully.");
            bottom.showCompleted(mode == DialogMode.VIEWER);
        };

        Runnable onFailure = () -> {
            JOptionPane.showMessageDialog(
                    dialog,
                    "Operation finished with errors. Check the log.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE
            );
        };

        startWorker.run(uiDisable, uiEnable, onSuccess, onFailure);
    }

    // ----------------------------------------------------------------------------------------------------
    //  CANCEL
    // ----------------------------------------------------------------------------------------------------

    private void handleCancel() {

        setCancelled.run();
        Thread t = getWorker.get();

        bottom.showStopping();

        // -----------------------------------------------------------------------------------------------
        // CASE 1 — Worker still running → perform real cancellation
        // -----------------------------------------------------------------------------------------------
        if (t != null && t.isAlive()) {
            PaintLogger.infof("Cancellation requested — waiting for worker to finish...");
            new Thread(() -> handleWorkerShutdown(t), "ForceShutdownWatcher").start();
            return;
        }

        // -----------------------------------------------------------------------------------------------
        // CASE 2 — Cancel BEFORE worker started (invalid dialog state)
        // Re-enable full UI so user can fix input
        // -----------------------------------------------------------------------------------------------
        if (!workerStarted) {
            PaintLogger.infof("Cancellation before start — closing dialog.");
            clearCancelled.run();
            SwingUtilities.invokeLater(dialog::dispose);
            return;
        }

        // -----------------------------------------------------------------------------------------------
        // CASE 3 — Worker started and is DONE → treat select Cancel as dialog close
        // -----------------------------------------------------------------------------------------------
        PaintLogger.infof("Cancel after completion — closing dialog.");
        paint.shared.utils.PaintConsoleWindow.closeIfVisible();
        SwingUtilities.invokeLater(dialog::dispose);
        clearCancelled.run();
    }

    // ----------------------------------------------------------------------------------------------------
    //  Worker Shutdown
    // ----------------------------------------------------------------------------------------------------

    private void handleWorkerShutdown(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException ignored) {}
        SwingUtilities.invokeLater(() -> finishWorkerShutdown(thread));
    }

    private void finishWorkerShutdown(Thread thread) {

        if (thread.isAlive()) {
            PaintLogger.errorf("Worker thread did not stop — forcing JVM halt.");
            Runtime.getRuntime().halt(0);
            return;
        }

        PaintLogger.infof("Worker thread terminated cleanly.");

        workerStarted = false;
        bottom.resetOk(true);
        clearCancelled.run();

        enableFullUI();
    }

    // ----------------------------------------------------------------------------------------------------
    //  Sweep Toggle
    // ----------------------------------------------------------------------------------------------------

    private void onSweepToggle(boolean selected) {

        if (selected) {
            final Path root = getProjectPath.get();
            final Path sweepFile = root.resolve("Paint Sweep Configuration.json");

            if (!java.nio.file.Files.exists(sweepFile)) {

                int res = JOptionPane.showConfirmDialog(
                        dialog,
                        "The file \"Paint Sweep Configuration.json\" does not exist in the project root.\n\n" +
                                "Do you want to create it now with default sweep settings?",
                        "Sweep Configuration Missing",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (res == JOptionPane.YES_OPTION) {
                    try {
                        paintConfig.setSweepDefaults(root);

                        JOptionPane.showMessageDialog(
                                dialog,
                                "Sweep configuration file has been created:\n" +
                                        sweepFile.toAbsolutePath() +
                                        "\nYou should edit that file to enable the desired sweep options.",
                                "Sweep File Created",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(
                                dialog,
                                "Failed to create sweep configuration:\n" + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                } else {
                    bottom.setSweepSelected(false);
                }
            }
        }

        updateOk();
    }

    // ----------------------------------------------------------------------------------------------------
    //  Browse handlers
    // ----------------------------------------------------------------------------------------------------

    private void handleBrowseProject(File dir) {
        updateProjectRoot(dir.toPath());
    }

    private void handleBrowseImages(File ignored) {
        updateOk();
    }

    private void updateProjectRoot(Path newRoot) {
        setProjectPath.accept(newRoot);
        bottom.updateOkEnabled(validToRun());
    }

    // ----------------------------------------------------------------------------------------------------
    //  Utility
    // ----------------------------------------------------------------------------------------------------

    private void updateOk() {
        bottom.updateOkEnabled(validToRun());
    }

    private boolean validToRun() {
        if (mode == DialogMode.VIEWER) {
            return paths.isProjectRootValid();
        }
        return paths.isProjectRootValid() && experiments.anySelected();
    }

    /**
     * Enables/disables all interactive UI elements EXCEPT the Cancel button.
     */
    private void setInputsEnabled(boolean enabled) {

        // Disable Project + Images root fields
        paths.setEnabled(enabled, mode);

        // Disable TrackMate / Squares parameters
        if (params != null) {
            params.setEnabled(enabled);
        }

        experiments.setEnabled(enabled);

        bottom.setEnabled(enabled);

        if (!enabled) {
            bottom.keepCancelEnabled();
        }
    }

    /**
     * Restores full dialog UI after cancellation-before-start.
     */
    private void enableFullUI() {
        enableAllUiFromDialog.run();          // Re-enable all fields/buttons
        bottom.resetOk(validToRun());         // Restore OK button state
    }

    /**
     * Functional interface used to launch worker logic with four callbacks.
     */
    @FunctionalInterface
    public interface QuadRunnable {
        @SuppressWarnings("unused")
        void run(Runnable a, Runnable b, Runnable c, Runnable d);
    }
}