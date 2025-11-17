// =================================================================================================
//  File: src/main/java/paint/shared/dialogs/ProjectDialog.java
// =================================================================================================
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ProjectDialog {

    // ----- public API kept stable -----
    public enum DialogMode {
        TRACKMATE,
        GENERATE_SQUARES,
        VIEWER}

    @FunctionalInterface
    public interface CalculationCallback {
        boolean run(Project project);
    }

    // ----- state -----
    private final JDialog     dialog;
    private final DialogMode  mode;

    private          Path                projectPath;
    private          CalculationCallback callback;
    private volatile boolean             cancelled = false;
    private volatile Thread              workerThread;

    // sub-components
    private final ProjectPathsPanel  projectPathsPanel;
    private final SquaresParamsPanel squaresParamsPanel;         // null in VIEWER
    private final ExperimentsPanel   experimentsPanel;
    private final BottomBarPanel     bottomBarPanel;

    public ProjectDialog(Frame owner, Path initialProjectPath, DialogMode mode) {
        this.mode               = mode;
        this.projectPath        = initialProjectPath;
        PaintConfig paintConfig = PaintConfig.instance();

        final String projectName = (projectPath != null && projectPath.getFileName() != null)
                ? projectPath.getFileName().toString() : "(none)";

        final String title = (mode == DialogMode.TRACKMATE)
                ? "Run TrackMate on Project - '" + projectName + "'"
                : (mode == DialogMode.VIEWER)
                ? "View Recordings for Project - '" + projectName + "'"
                : "Generate Squares for Project - '" + projectName + "'";

        this.dialog = new JDialog(owner, title, false);
        this.dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.dialog.addWindowListener(ProjectDialogController.onWindowClosing(() -> {
            cancelled = true;
            if (workerThread != null && workerThread.isAlive()) {
                workerThread.interrupt();
                PaintLogger.infof("Cancellation requested — interrupting background thread...");
            }
            dialog.dispose();
        }));

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

        // ---- wire controller ----
        final ProjectDialogController controller = new ProjectDialogController(
                mode,                                                   // mode
                dialog,                                                 // dialog
                paintConfig,                                            // paintConfig
                () -> projectPath,                                      // Supplier getProjectPath
                p -> {                                             // Consumer setProjectPAth
                    projectPath = p;
                    experimentsPanel.reload(projectPath);
                    projectPathsPanel.onProjectRootChanged(projectPath);
                },
                projectPathsPanel,                                      // projectPathsPanel
                squaresParamsPanel,                                     // squaresParamsPanel
                experimentsPanel,                                       // experimentsPanel
                bottomBarPanel,                                         // bottomBarPanel
                this::startWorker,                                      // startWorker
                () -> workerThread,                                     // getWorker
                () -> cancelled = true,                                 // setCancelled
                () -> cancelled = false                                 // clearCancelled
        );
        controller.init();

        // size and show defaults
        final int width  = 820;
        final int height = 600;
        dialog.setMinimumSize(new Dimension(width, height));
        dialog.setPreferredSize(new Dimension(width, height));
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
    }

    // ---------- public API ----------
    public void setCalculationCallback(CalculationCallback callback) {
        this.callback = callback;
    }

    public void showDialog() {
        dialog.setVisible(true);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public JDialog getDialog() {
        return dialog;
    }

    // ---------- internals ----------
    private Project buildProject() {
        final List<String> experimentNames = experimentsPanel.selectedExperimentNames();
        final Path imagesPath = projectPathsPanel.imagesRootText().isEmpty()
                ? null : Paths.get(projectPathsPanel.imagesRootText());

        // persist roots
        PaintPrefs.putString("Path", "Project Root", projectPathsPanel.projectRootText());
        PaintPrefs.putString("Path", "Images Root", projectPathsPanel.imagesRootText());

        // persist params
        if (squaresParamsPanel != null) {
            squaresParamsPanel.persistTo(mode);
        }

        final GenerateSquaresConfig gs  = new GenerateSquaresConfig();

        return new paint.shared.objects.Project(projectPath,
                imagesPath,
                experimentNames,
                gs,
                null
        );
    }

    private void startWorker(Runnable runUiDisable, Runnable runUiEnable, Runnable onSuccess, Runnable onFailure) {
        if (callback == null) {
            onFailure.run();
            return;
        }

        runUiDisable.run();
        cancelled = false;

        workerThread = new Thread(() -> {
            boolean ok = false;
            try {
                if (!cancelled && !Thread.currentThread().isInterrupted()) {
                    ok = callback.run(buildProject());
                }
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    cancelled = true;
                    PaintLogger.infof("Operation interrupted.");
                } else {
                    PaintLogger.errorf("Error in callback: %s", ex.getMessage());
                }
            }

            final boolean success = ok && !cancelled && !Thread.currentThread().isInterrupted();
            SwingUtilities.invokeLater(() -> {
                runUiEnable.run();
                if (success)
                    onSuccess.run();
                else
                    onFailure.run();
                workerThread = null;
            });
        }, "ProjectDialog-Worker");
        workerThread.start();
    }

    public boolean isSweepSelected() {
        return bottomBarPanel.isSweepSelected();
    }
}