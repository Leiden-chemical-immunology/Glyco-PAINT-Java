/*=============================================================================
 *  Class:        TrackMateUI.java
 *  Package:      paint.fiji.trackmate
 *
 *  PURPOSE:
 *    Provides the main interactive entry point for running TrackMate within
 *    the PAINT environment. Integrates configuration handling, experiment
 *    selection, and optional sweep or post-processing operations.
 *
 *  DESCRIPTION:
 *    • Runs TrackMate interactively through the Fiji plugin menu.
 *    • Validates project root and configuration state.
 *    • Displays a user dialog for selecting and running experiments.
 *    • Supports sweep configurations when enabled.
 *    • Optionally executes GENERATE_SQUARES after successful completion.
 *    • Ensures only one processing instance runs at a time.
 *
 *  KEY FEATURES:
 *    • Headless and GUI-compatible operation through {@link ProjectDialog}.
 *    • Runtime configuration using {@link PaintConfig} and {@link PaintPrefs}.
 *    • Integrated console logging via {@link PaintConsoleWindow}.
 *    • Thread-safe execution with a static volatile lock flag.
 *
 *  USAGE EXAMPLE:
 *    Plugin menu: Plugins → Glyco-PAINT → Run
 *
 *  DEPENDENCIES:
 *    – paint.shared.utils.*, paint.shared.config.*
 *    – paint.generatesquares.GenerateSquaresHeadless
 *    – paint.fiji.trackmate.RunTrackMateOnProjectSweep
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
=============================================================================*/

package paint.fiji.trackmate;


import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import paint.generatesquares.app.GenerateSquaresHeadless;
import paint.shared.config.paintconfig.PaintConfig;
import paint.shared.dialogs.ProjectDialog;
import paint.shared.utils.PaintPrefs;
import paint.shared.utils.JarInfoLogger;
import paint.shared.utils.PaintConsoleWindow;
import paint.shared.utils.PaintLogger;
import paint.shared.utils.PaintRuntime;
import paint.shared.objects.Project;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static paint.shared.constants.PaintFileNames.PAINT_SWEEP_CONFIGURATION_JSON;
import static paint.shared.utils.ProjectPathResolver.getValidProjectPath;

/**
 * Main user interface class for running TrackMate interactively within the
 * PAINT environment. Handles initialization, configuration, dialog management,
 * and execution of TrackMate processes.
 */
@Plugin(type = Command.class, menuPath = "Plugins>Glyco-PAINT>Run")
public class TrackMateUI extends RunTrackMateOnProjectSweep implements Command {

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
        PaintLogger.debugf("TrackMateUI.run - TrackMate plugin started (Interactive).");

        PaintRuntime.initialiseFromPrefs();

        // Log version and timestamp
        JarInfoLogger.JarInfo info = JarInfoLogger.getJarInfo(TrackMateUI.class);
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
        ProjectDialog projDialog = new ProjectDialog(null, projectPath, ProjectDialog.DialogMode.TRACKMATE);
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
    private void showWarning(String message) {
        JOptionPane optionPane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE);
        JDialog warnDialog = optionPane.createDialog(null, "Warning");
        warnDialog.setAlwaysOnTop(true);
        warnDialog.setVisible(true);
    }

    private boolean runTrackMatePipeline(Project project, boolean sweepSelected, ProjectDialog projDialog) {
        PaintLogger.debugf("TrackMateUI.runTrackMatePipeline - experiments (1): %s", project.getExperimentNames());
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
                PaintLogger.debugf("TrackMateUI.runTrackMatePipeline - experiments (2): %s", project.getExperimentNames());
                success = RunTrackMateOnProject.runProject(
                        currentProjectRoot,
                        imagesPath,
                        project.getExperimentNames(),
                        projDialog,
                        null
                );

                if (success && PaintConfig.getBoolean("TrackMate", "Run Generate Squares After", true)) {
                    PaintLogger.infof("TrackMate finished successfully. Starting Generate Squares...");
                    GenerateSquaresHeadless.run(currentProjectRoot, project.getExperimentNames());
                    PaintLogger.infof("Generate Squares completed successfully.");
                }
                PaintLogger.debugf("TrackMateUI.runTrackMatePipeline - Success: %b", success);
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