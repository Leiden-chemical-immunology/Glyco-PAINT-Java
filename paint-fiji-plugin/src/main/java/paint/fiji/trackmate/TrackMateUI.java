/******************************************************************************
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
 *    • Optionally executes "Generate Squares" after successful completion.
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
 ******************************************************************************/

package paint.fiji.trackmate;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import paint.generatesquares.GenerateSquaresHeadless;
import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectDialog;
import paint.shared.utils.PaintPrefs;
import paint.shared.utils.JarInfoLogger;
import paint.shared.utils.PaintConsoleWindow;
import paint.shared.utils.PaintLogger;
import paint.generatesquares.GenerateSquaresHeadless;
import paint.shared.utils.PaintRuntime;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static paint.shared.constants.PaintConstants.PAINT_SWEEP_CONFIGURATION_JSON;
import static paint.shared.utils.ValidProjectPath.getValidProjectPath;

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
     *   <li>Optionally triggers "Generate Squares" after completion.</li>
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
        PaintLogger.debugf("TrackMate plugin started (Interactive).");

        PaintRuntime.initialiseFromPrefs();

        // Log version and timestamp
        JarInfoLogger.JarInfo info = JarInfoLogger.getJarInfo(TrackMateUI.class);
        if (info != null) {
            PaintLogger.infof("Compilation date: %s", info.implementationDate);
            PaintLogger.infof("Version: %s", info.implementationVersion);
        }

        LocalDateTime now = LocalDateTime.now();
        String formattedTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        PaintLogger.infof("Current time: %s", formattedTime);
        PaintLogger.blankline();

        // ---------------------------------------------------------------------
        // Step 4 – Show experiment dialog
        // ---------------------------------------------------------------------
        ProjectDialog dialog = new ProjectDialog(null, projectPath, ProjectDialog.DialogMode.TRACKMATE);
        PaintConsoleWindow.closeOnDialogDispose(dialog.getDialog());

        // ---------------------------------------------------------------------
        // Step 5 – Handle OK action callback
        // ---------------------------------------------------------------------
        dialog.setCalculationCallback(project -> {

            if (running) {
                showWarning("TrackMate processing is already running.\nPlease wait until it finishes.");
                return false;
            }

            running = true;
            dialog.setOkEnabled(false);

            try {
                boolean debug = PaintConfig.getBoolean("Debug", "Debug RunTrackMateOnProject", false);
                Path imagesPath = project.getImagesRootPath();
                Path currentProjectRoot = project.getProjectRootPath();

                if (debug) {
                    PaintLogger.debugf("TrackMate processing started.");
                    PaintLogger.debugf("Experiments: %s", project.getExperimentNames());
                }

                boolean success;
                boolean sweepEnabled = dialog.isSweepSelected();
                Path sweepFile = currentProjectRoot.resolve(PAINT_SWEEP_CONFIGURATION_JSON);

                if (sweepEnabled) {
                    if (Files.exists(sweepFile)) {
                        success = RunTrackMateOnProjectSweep.runWithSweep(
                                currentProjectRoot, imagesPath, project.getExperimentNames());
                    } else {
                        PaintLogger.infof("No Sweep configuration detected at %s", sweepFile);
                        return false;
                    }
                } else {
                    success = RunTrackMateOnProject.runProject(
                            projectPath, imagesPath, project.getExperimentNames(), dialog, null);

                    if (success && PaintConfig.getBoolean("TrackMate", "Run Generate Squares After", true)) {
                        PaintLogger.infof("TrackMate finished successfully. Starting Generate Squares...");
                        PaintLogger.blankline();
                        GenerateSquaresHeadless.run(currentProjectRoot, project.getExperimentNames());
                        PaintLogger.infof("Generate Squares completed successfully.");
                    }
                }

                return success;

            } catch (Exception e) {
                PaintLogger.errorf("Error during TrackMate execution: %s", e.getMessage());
                return false;
            } finally {
                running = false;
            }
        });

        dialog.showDialog();
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
}