/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.fiji.trackmate;


import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import paint.generatesquares.app.GenerateSquaresHeadless;
import paint.shared.config.TrackMateConfig;
import paint.shared.config.paintconfig.PaintConfig;
import paint.ui.dialogs.ProjectDialog;
import paint.shared.utils.PaintPrefs;
import paint.shared.utils.JarInfoLogger;
import paint.ui.console.PaintConsoleWindow;
import paint.shared.utils.PaintLogger;
import paint.shared.utils.PaintRuntime;
import paint.shared.objects.Project;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static paint.shared.constants.PaintFileNames.PAINT_SWEEP_CONFIGURATION_JSON;
import static paint.ui.dialogs.ProjectPathResolver.getValidProjectPath;

/**
 * Main user interface class for running TrackMate interactively within the
 * PAINT environment. Handles initialization, configuration, dialog management,
 * and execution of TrackMate processes.
 */
@SuppressWarnings("unused")
@Plugin(type = Command.class, menuPath = "Plugins>Glyco-PAINT>Run Batch")
public class TrackMateUIBatch extends RunTrackMateOnProjectSweep implements Command {

    /**
     * Prevents concurrent execution of multiple TrackMate runs.
     */
    private static volatile boolean running = false;

    /**
     * Executes the TrackMate workflow through an interactive GUI dialog.
     * <p>
     * The method:
     * <ul>
     *   <li>Ensures single-instance execution.</li>
     *   <li>Initializes logging and configuration state.</li>
     *   <li>Displays a project dialog for experiment selection.</li>
     *   <li>Runs the appropriate TrackMate pipeline (sweep or standard).</li>
     *   <li>Optionally triggers GENERATE_SQUARES after completion.</li>
     * </ul>
     */
    @Override
    public void run() {

        // ---------------------------------------------------------------------
        // Step 1 – Setup exception handler and concurrency lock
        // ---------------------------------------------------------------------
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                                                          PaintLogger.debugf("AWT complained: %s", throwable.getMessage())
        );

        if (running) {
            showWarning("TrackMate processing is already running.\nPlease wait until it finishes.");
            return;
        }

        // ---------------------------------------------------------------------
        // Step 2 – Retrieve project root
        // ---------------------------------------------------------------------
        Path projectPath = getValidProjectPath();
        if (projectPath == null) {
            return;
        }

        // ---------------------------------------------------------------------
        // Step 3 – Initialize logging, configuration, and runtime settings
        // ---------------------------------------------------------------------
        PaintConsoleWindow.createConsoleFor("TrackMate");
        PaintConfig.initialise(projectPath);

        String debugLevel = PaintPrefs.getString("Runtime", "Log Level", "INFO");
        PaintLogger.setLevel(debugLevel);
        PaintLogger.initialise(projectPath, "TrackMateOnProject.log");
        PaintLogger.debugf("TrackMateUIBatch.run - TrackMate plugin started (Interactive).");

        PaintRuntime.initialiseFromPrefs();

        // Log version and timestamp
        JarInfoLogger.JarInfo info = JarInfoLogger.getJarInfo(TrackMateUIBatch.class);
        if (info != null) {
            PaintLogger.infof("Compilation date: %s", info.implementationDate);
            PaintLogger.infof("Version: %s", info.implementationVersion);
        }

        LocalDateTime  now           = LocalDateTime.now();
        String         formattedTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        PaintLogger.infof("Current time: %s", formattedTime);
        PaintLogger.blankline();

        // ---------------------------------------------------------------------
        // Step 4 – Show experiment dialog
        // ---------------------------------------------------------------------
        ProjectDialog projDialog = new ProjectDialog(null, projectPath, ProjectDialog.DialogMode.TRACKMATE_BATCH);
        PaintConsoleWindow.closeOnDialogDispose(projDialog.getDialog());

        // ---------------------------------------------------------------------
        // Step 5 – Handle OK action callback
        projDialog.setCalculationCallback(project -> runTrackMatePipeline(project, projDialog.isSweepSelected(), projDialog));
        projDialog.showDialog();
    }

    // -------------------------------------------------------------------------
    // Utility Methods
    // -------------------------------------------------------------------------

    /**
     * Displays a warning dialog with the specified message.
     *
     * @param message warning message text to display
     */
    @SuppressWarnings("SameParameterValue")
    private void showWarning(String message) {
        JOptionPane optionPane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE);
        JDialog     warnDialog = optionPane.createDialog(null, "Warning");
        warnDialog.setAlwaysOnTop(true);
        warnDialog.setVisible(true);
    }

    @SuppressWarnings("unused")
    private boolean runTrackMatePipeline(Project project, boolean sweepSelected, ProjectDialog projDialog) {
        PaintLogger.debugf("TrackMateUIBatch.runTrackMatePipeline - experiments (1): %s", project.getExperimentNames());
        if (running) {
            showWarning("TrackMate processing is already running.\nPlease wait until it finishes.");
            return false;
        }

        running = true;

        try {
            Path    imagesPath         = project.getImagesRootPath();
            Path    currentProjectRoot = project.getProjectRootPath();
            boolean success;

            if (sweepSelected) {
                Path sweepFile = currentProjectRoot.resolve(PAINT_SWEEP_CONFIGURATION_JSON);
                if (Files.exists(sweepFile)) {
                    success = RunTrackMateOnProjectSweep.runWithSweep(
                            currentProjectRoot,
                            imagesPath,
                            project.getExperimentNames()
                    );
                } else {
                    PaintLogger.infof("No Sweep configuration found at %s", sweepFile);
                    return false;
                }
            } else {
                PaintLogger.debugf("TrackMateUIBatch.runTrackMatePipeline - experiments (2): %s", project.getExperimentNames());
                success = RunTrackMateOnProject.runProject(
                        currentProjectRoot,
                        imagesPath,
                        project.getExperimentNames(),
                        projDialog,
                        null
                );

                if (success && new TrackMateConfig().isRunGenerateSquaresAfter()) {
                    PaintLogger.infof("TrackMate finished successfully. Starting Generate Squares...");
                    GenerateSquaresHeadless.run(currentProjectRoot, project.getExperimentNames());
                    PaintLogger.infof("Generate Squares completed successfully.");
                }
                PaintLogger.debugf("TrackMateUIBatch.runTrackMatePipeline - Success: %b", success);
            }

            return success;

        } catch (Exception e) {
            PaintLogger.errorf("Error during TrackMate execution: %s", e.getMessage());
            return false;
        } finally {
            running = false;
        }
    }
}