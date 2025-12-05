// =================================================================================================
//  File: src/main/java/paint/shared/dialogs/ProjectDialog.java
// =================================================================================================

/* =================================================================================================
 *  PURPOSE
 *      Main dialog for configuring and executing PAINT project operations (TrackMate, Generate
 *      Squares, Viewer). Manages UI composition, background worker behavior, and persistence of
 *      user selections.
 *
 *  DESCRIPTION
 *      This dialog acts as the central UI entry point for project-level operations. It wires
 *      together all project-related panels (paths, experiments, parameters), connects to a
 *      ProjectDialogController via method references, manages the worker thread for expensive
 *      computations, and ensures correct EDT behavior for UI updates.
 *
 *  KEY FEATURES
 *      - Supports three modes: TRACKMATE, GENERATE_SQUARES, VIEWER.
 *      - Assembles panels and components dynamically based on selected mode.
 *      - Provides a consistent project-building pipeline (persist + construct).
 *      - Includes cancel-safe worker thread execution with interrupt support.
 *      - Uses clear method references for controller wiring.
 *
 *  AUTHOR
 *      PAINT Toolkit
 *
 *  MODULE
 *      paint.shared.dialogs
 *
 *  UPDATED
 *      2025-11-21
 *
 *  COPYRIGHT
 *      Copyright (c) 2020–2025.
 *      All rights reserved.
 * =================================================================================================
 */


package paint.shared.dialogs;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.config.paintconfig.PaintConfig;
import paint.shared.dialogs.project.BottomBarPanel;
import paint.shared.dialogs.project.ExperimentsPanel;
import paint.shared.dialogs.project.ProjectDialogController;
import paint.shared.dialogs.project.ProjectPathsPanel;
import paint.shared.dialogs.project.SquaresParamsPanel;
import paint.shared.objects.Project;
import paint.shared.utils.PaintLogger;
import paint.shared.utils.PaintPrefs;
import paint.shared.utils.PaintRuntime;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main dialog responsible for configuring a PAINT Project and running associated operations
 * (TrackMate, Generate Squares, Viewer). Builds the UI, wires the controller callbacks,
 * and coordinates worker thread execution and cancellation.
 */
public class ProjectDialog {

    // ----- public API kept stable -----

    /**
     * Operation mode for the dialog. Determines UI content and behavior.
     */
    public enum DialogMode {
        TRACKMATE,
        GENERATE_SQUARES,
        VIEWER
    }

    /**
     * Functional callback for executing expensive computations on a fully constructed Project.
     * Returning {@code true} indicates success; {@code false} indicates failure.
     */
    @FunctionalInterface
    public interface CalculationCallback {
        boolean run(Project project);
    }

    private final JDialog       dialog;
    private final DialogMode    mode;

    private Path                projectPath;
    private CalculationCallback calculationCallback;
    private volatile boolean    cancelled     = false;
    private volatile Thread     workerThread;
    private boolean             workerStarted = false;

    private final ProjectPathsPanel  projectPathsPanel;
    private final SquaresParamsPanel squaresParamsPanel;
    private final ExperimentsPanel   experimentsPanel;
    private final BottomBarPanel     bottomBarPanel;

    public ProjectDialog(Frame owner, Path initialProjectPath, DialogMode mode) {
        this.mode = mode;
        this.projectPath = initialProjectPath;

        PaintConfig paintConfig = PaintConfig.instance();

        final String projectName =
                (projectPath != null && projectPath.getFileName() != null)
                        ? projectPath.getFileName().toString()
                        : "(none)";

        final String title =
                (mode == DialogMode.TRACKMATE)
                        ? "Run TrackMate on Project - '" + projectName + "'"
                        : (mode == DialogMode.VIEWER)
                        ? "View Recordings for Project - '" + projectName + "'"
                        : "Generate Squares for Project - '" + projectName + "'";

        this.dialog = new JDialog(owner, title, false);
        this.dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onWindowClose();
            }
        });

        // ---- build UI ----
        final JPanel root = new JPanel(new BorderLayout());
        final JPanel form = new JPanel(new BorderLayout());

        projectPathsPanel  = new ProjectPathsPanel(mode, projectPath);
        squaresParamsPanel = (mode == DialogMode.VIEWER) ? null : new SquaresParamsPanel(mode);

        if (squaresParamsPanel != null) {
            form.add(projectPathsPanel.component(), BorderLayout.NORTH);
            form.add(squaresParamsPanel.component(), BorderLayout.CENTER);
        } else {
            form.add(projectPathsPanel.component(), BorderLayout.NORTH);
        }

        experimentsPanel = new ExperimentsPanel(projectPath);

        final JPanel center = new JPanel(new BorderLayout());
        center.add(experimentsPanel.component(), BorderLayout.CENTER);

        bottomBarPanel = new BottomBarPanel(mode, PaintRuntime.isVerbose());

        root.add(form, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(bottomBarPanel.component(), BorderLayout.SOUTH);

        dialog.setContentPane(root);

        // ---------------------------------------------------------------------
        // Controller wiring (INCLUDING enableAllUi callback)
        // ---------------------------------------------------------------------
        final ProjectDialogController controller =
                new ProjectDialogController(
                        mode,
                        dialog,
                        paintConfig,
                        this::getProjectPath,
                        this::setProjectPath,
                        projectPathsPanel,
                        squaresParamsPanel,
                        experimentsPanel,
                        bottomBarPanel,
                        this::startWorker,
                        this::getWorkerThread,
                        this::setCancelled,
                        this::clearCancelled,
                        this::enableAllUi
                );

        controller.init();

        dialog.setMinimumSize(new Dimension(820, 600));
        dialog.setMaximumSize(new Dimension(820, 600));

        dialog.pack();
        dialog.setLocationRelativeTo(owner);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void setCalculationCallback(CalculationCallback calculationCallback) {
        this.calculationCallback = calculationCallback;
    }

    /**
     * Makes the dialog visible.
     */
    public void showDialog() {
        dialog.setVisible(true);
    }

    /**
     * @return {@code true} if the user cancelled the operation
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * @return the underlying Swing dialog
     */
    public JDialog getDialog() {
        return dialog;
    }

    // -------------------------------------------------------------------------
    // Build Project object from UI state
    // -------------------------------------------------------------------------

    /**
     * Builds and returns a {@link Project} instance based on current UI state,
     * including selected experiments, path values, and persisted parameters.
     */
    private Project buildProject() {
        final List<String> experimentNames = experimentsPanel.selectedExperimentNames();
        final Path imagesPath =
                projectPathsPanel.imagesRootText().isEmpty()
                        ? null
                        : Paths.get(projectPathsPanel.imagesRootText());

        PaintLogger.debugf("ProjectDialog.buildProject - project root: %s", projectPathsPanel.projectRootText());
        PaintLogger.debugf("ProjectDialog.buildProject - images root : %s", projectPathsPanel.imagesRootText());
        PaintLogger.debugf("ProjectDialog.buildProject - experiments : %s", experimentNames);

        // persist roots
        PaintPrefs.putString("Path", "Project Root", projectPathsPanel.projectRootText());
        PaintPrefs.putString("Path", "Images Root",  projectPathsPanel.imagesRootText());

        if (squaresParamsPanel != null) {
            squaresParamsPanel.persistTo(mode);
        }

        return new Project(
                projectPath,
                imagesPath,
                experimentNames,
                new GenerateSquaresConfig(),
                null
        );
    }

    /**
     * Starts a background worker thread, ensuring UI disable/enable and EDT-safe callbacks.
     * This method is responsible for:
     * <ul>
     *     <li>Running heavy work off the EDT.</li>
     *     <li>Handling cancellation and thread interruption.</li>
     *     <li>Invoking success/failure runnables on the EDT.</li>
     * </ul>
     */
    private void startWorker(Runnable runUiDisable,
            Runnable runUiEnable,
            Runnable onSuccess,
            Runnable onFailure
    ) {
        PaintLogger.debugf("ProjectDialog.startWorker - Starting worker thread.");
        if (calculationCallback == null) {
            onFailure.run();
            return;
        }

        runUiDisable.run();
        cancelled     = false;
        workerStarted = true;

        final Project project = buildProject();

        workerThread = new Thread(() -> {
            boolean ok = false;

            try {
                if (!cancelled && !Thread.currentThread().isInterrupted()) {
                    PaintLogger.debugf("ProjectDialog.startWorker - Starting calculationCallback.run");
                    ok = calculationCallback.run(project);
                }
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    cancelled = true;
                } else {
                    PaintLogger.errorf("Error in callback: %s", ex.getMessage());
                }
            }

            final boolean success =
                    ok && !cancelled && !Thread.currentThread().isInterrupted();

            SwingUtilities.invokeLater(() -> {
                if (cancelled) {
                    return;
                }

                runUiEnable.run();
                if (success) {
                    onSuccess.run();
                }
                else {
                    onFailure.run();
                }
            });

        }, "ProjectDialog-Worker");

        workerThread.start();
    }

    // -------------------------------------------------------------------------
    // UI restore function — used by controller
    // -------------------------------------------------------------------------

    public void enableAllUi() {
        projectPathsPanel.setEnabled(true, mode);
        if (squaresParamsPanel != null) {
            squaresParamsPanel.setEnabled(true);
            squaresParamsPanel.setSquaresEnabled(squaresParamsPanel.isRunAfterTrackMateSelected());
        }
        experimentsPanel.setEnabled(true);
        bottomBarPanel.setEnabled(true);
    }

    /**
     * Handles window close events:
     * - If already cancelling, ignores extra close requests.
     * - If a worker is running, treats window close as a Cancel request (sets cancelled + interrupts worker).
     * - If no worker is running, disposes the dialog normally.
     */
    private void onWindowClose() {

        if (cancelled) {
            return;
        }

        if (workerThread != null && workerThread.isAlive()) {
            PaintLogger.infof("Window close → treating as Cancel request.");
            cancelled = true;
            workerThread.interrupt();
            return;
        }

        dialog.dispose();
    }

    // -------------------------------------------------------------------------
    // Misc internal helpers
    // -------------------------------------------------------------------------

    private Thread getWorkerThread() {
        return workerThread;
    }

    /**
     * Marks computation as cancelled.
     */
    private void setCancelled() {
        cancelled = true;
    }

    /**
     * Clears the cancellation flag.
     */
    private void clearCancelled() {
        cancelled = false;
    }

    /**
     * @return current project root path
     */
    private Path getProjectPath() {
        return projectPath;
    }

    /**
     * Sets the project path and refreshes dependent panels.
     */
    private void setProjectPath(Path projectPath) {
        this.projectPath = projectPath;
        experimentsPanel.reload(projectPath);
        projectPathsPanel.onProjectRootChanged(projectPath);
    }

    public boolean isSweepSelected() {
        return bottomBarPanel.isSweepSelected();
    }
}