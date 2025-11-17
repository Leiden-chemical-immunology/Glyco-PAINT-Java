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

public class ProjectDialogController {

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
    private final PaintConfig        cfg;

    private final Supplier<Path>     getProjectPath;
    private final Consumer<Path>     setProjectPath;

    private final ProjectPathsPanel  paths;
    private final SquaresParamsPanel params;       // null in VIEWER
    private final ExperimentsPanel   experiments;
    private final BottomBarPanel     bottom;

    private final Supplier<Object>   buildProject; // returns Project
    private final QuadRunnable       startWorker;  // (runUiDisable, runUiEnable, onSuccess, onFailure)
    private final Supplier<Thread>   getWorker;
    private final Runnable           setCancelled;
    private final Runnable           clearCancelled;

    public ProjectDialogController(
            DialogMode mode,
            JDialog dialog,
            PaintConfig cfg,
            Supplier<Path> getProjectPath,
            Consumer<Path> setProjectPath,
            ProjectPathsPanel paths,
            SquaresParamsPanel params,
            ExperimentsPanel experiments,
            BottomBarPanel bottom,
            Supplier<Object> buildProject,
            QuadRunnable startWorker,
            Supplier<Thread> getWorker,
            Runnable setCancelled,
            Runnable clearCancelled
    ) {
        this.mode           = mode;
        this.dialog         = dialog;
        this.cfg            = cfg;
        this.getProjectPath = getProjectPath;
        this.setProjectPath = setProjectPath;
        this.paths          = paths;
        this.params         = params;
        this.experiments    = experiments;
        this.bottom         = bottom;
        this.buildProject   = buildProject;
        this.startWorker    = startWorker;
        this.getWorker      = getWorker;
        this.setCancelled   = setCancelled;
        this.clearCancelled = clearCancelled;
    }

    public void init() {
        // browse handlers
        paths.onBrowseProject(dir -> updateProjectRoot(dir.toPath()));
        paths.onBrowseImages( dir -> bottom.updateOkEnabled(validToRun()));

        // text listeners
        paths.onRootsChanged(() -> bottom.updateOkEnabled(validToRun()));

        // experiments selection listeners
        experiments.onSelectionChanged(() -> bottom.updateOkEnabled(validToRun()));

        // TrackMate -> run squares toggle affects params enabled & OK state
        if (params != null) {
            params.onParamsChanged(() -> bottom.updateOkEnabled(validToRun()));
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
                            cfg.setSweepDefaults(root);
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
            bottom.updateOkEnabled(validToRun());
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
        bottom.updateOkEnabled(validToRun());
    }

    private void updateProjectRoot(Path newRoot) {
        setProjectPath.accept(newRoot);
        bottom.updateOkEnabled(validToRun());
    }

    private boolean validToRun() {
        if (mode == DialogMode.VIEWER) {
            return paths.isProjectRootValid();
        }
        return paths.isProjectRootValid() && experiments.anySelected();
    }

    private void setInputsEnabled(boolean enabled) {
        paths.setEnabled(enabled, mode);
        experiments.setEnabled(enabled);
        bottom.setEnabled(enabled);
        if (params != null) {
            params.setEnabled(enabled);
        }
    }

    @FunctionalInterface
    public interface QuadRunnable {
        void run(Runnable a, Runnable b, Runnable c, Runnable d);
    }
}