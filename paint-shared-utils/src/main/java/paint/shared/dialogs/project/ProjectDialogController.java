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
 *      - Clean method-reference based communication with ProjectDialog.
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
import paint.shared.utils.PaintConsoleWindow;
import paint.shared.utils.PaintLogger;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

    /**
     * Factory method for creating a WindowAdapter that triggers a run() call
     * when the user attempts to close the window.
     */
    public static WindowAdapter onWindowClosing(Runnable action) {
        return new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                action.run();
            }
        };
    }

    private final DialogMode         mode;
    private final JDialog            dialog;
    private final PaintConfig        paintConfig;

    // Project root getter/setter supplied by the dialog
    private final Supplier<Path>     getProjectPath;   // retrieves current project root
    private final Consumer<Path>     setProjectPath;   // updates project root

    // UI panels
    private final ProjectPathsPanel  paths;
    private final SquaresParamsPanel params;           // null in VIEWER mode
    private final ExperimentsPanel   experiments;
    private final BottomBarPanel     bottom;

    // Worker logic references provided by ProjectDialog
    private final QuadRunnable       startWorker;      // executes heavy work with 4 UI callbacks
    private final Supplier<Thread>   getWorker;        // retrieves active worker thread
    private final Runnable           setCancelled;     // marks cancellation
    private final Runnable           clearCancelled;   // resets cancellation flag

    /**
     * Main controller constructor. Receives all functional interfaces and UI component references
     * from ProjectDialog, ensuring the controller remains unaware of their concrete implementation.
     */
    public ProjectDialogController(
            DialogMode         mode,
            JDialog            dialog,
            PaintConfig        paintConfig,
            Supplier<Path>     getProjectPath,  // A supplier takes no arguments and returns a value.
            Consumer<Path>     setProjectPath,  // A consumer takes one argument and returns nothing.
            ProjectPathsPanel  paths,
            SquaresParamsPanel params,
            ExperimentsPanel   experiments,
            BottomBarPanel     bottom,
            QuadRunnable       startWorker,     // (runUiDisable, runUiEnable, onSuccess, onFailure) define UI behaviour
            Supplier<Thread>   getWorker,       // A supplier takes no arguments and returns a value.
            Runnable           setCancelled,    // A runnable takes no arguments and returns nothing.
            Runnable           clearCancelled
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
    }

    /**
     * Initializes all UI listeners: browse buttons, sweep toggle, OK/Cancel buttons,
     * parameter validation, and worker invocation.
     */
    public void init() {
        // browse handlers
        paths.onBrowseProject(this::handleBrowseProject);
        paths.onBrowseImages(this::handleBrowseImages);

        // text listeners
        paths.onRootsChanged(this::updateOk);

        // experiments selection listeners
        experiments.onSelectionChanged(this::updateOk);

        // TrackMate -> run squares toggle affects params enabled & OK state
        if (params != null) {
            params.onParamsChanged(this::updateOk);
        }

        // Sweep checkbox + verbose + OK/Cancel
        bottom.onVerboseToggle();
        bottom.onSweepToggle(selected -> {

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
        });

        bottom.onOk(() -> {
            clearCancelled.run();

            // validate images root
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
                String msg = "Operation finished with errors. Check the log.";
                JOptionPane.showMessageDialog(dialog, msg, "Warning", JOptionPane.WARNING_MESSAGE);
            };

            startWorker.run(uiDisable, uiEnable, onSuccess, onFailure);
        });

        bottom.onCancel(() -> {
            setCancelled.run();
            Thread t = getWorker.get();
            if (t != null && t.isAlive()) {
                PaintLogger.infof("Cancellation requested — attempting graceful shutdown...");
                t.interrupt();

                new Thread(() -> {
                    try {
                        t.join(2000);
                    } catch (InterruptedException ignored) {
                    }
                    SwingUtilities.invokeLater(() -> {
                        if (t.isAlive()) {
                            PaintLogger.errorf("Worker thread did not stop — forcing JVM halt.");
                            Runtime.getRuntime().halt(0);
                        } else {
                            PaintLogger.infof("Worker thread terminated cleanly.");
                            clearCancelled.run();
                            bottom.resetOk(true);
                            try {
                                PaintConsoleWindow.closeIfVisible();
                            } catch (Throwable ignored) {
                            }
                            dialog.dispose();
                        }
                    });
                }, "ForceShutdownWatcher").start();
            } else {
                PaintLogger.infof("No active worker thread — closing dialog and console.");
                SwingUtilities.invokeLater(() -> {
                    clearCancelled.run();
                    bottom.resetOk(true);
                    try {
                        PaintConsoleWindow.closeIfVisible();
                    } catch (Throwable ignored) {
                    }
                    dialog.dispose();
                });
            }
        });

        // initial OK state
        updateOk();
    }

    /**
     * Handles when the user selects a new project root directory.
     */
    private void updateProjectRoot(Path newRoot) {
        setProjectPath.accept(newRoot);
        bottom.updateOkEnabled(validToRun());
    }

    /**
     * Determines whether execution is allowed, depending on mode and user selections.
     */
    private boolean validToRun() {
        if (mode == DialogMode.VIEWER) {
            return paths.isProjectRootValid();
        }
        return paths.isProjectRootValid() && experiments.anySelected();
    }

    /**
     * Enables or disables all input panels.
     */
    private void setInputsEnabled(boolean enabled) {
        paths.setEnabled(enabled, mode);
        experiments.setEnabled(enabled);
        bottom.setEnabled(enabled);
        if (params != null) {
            params.setEnabled(enabled);
        }
    }

    /**
     * A functional interface representing a callback that receives four Runnables
     * (typically: runUiDisable, runUiEnable, onSuccess, onFailure). Used to abstract
     * background worker startup from the controller.
     */
    @FunctionalInterface
    public interface QuadRunnable {
        void run(Runnable a, Runnable b, Runnable c, Runnable d);
    }

    private void updateOk() {
        bottom.updateOkEnabled(validToRun());
    }

    private void handleBrowseProject(File dir) {
        updateProjectRoot(dir.toPath());
    }

    private void handleBrowseImages(File ignored) {
        updateOk();
    }
}