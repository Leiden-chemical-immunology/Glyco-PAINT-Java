package paint.fiji.trackmate;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectDialog;
import paint.shared.prefs.PaintPrefs;
import paint.shared.utils.JarInfoLogger;
import paint.shared.utils.PaintConsoleWindow;
import paint.shared.utils.PaintLogger;
import generatesquares.GenerateSquaresHeadless;
import paint.shared.utils.PaintRuntime;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static paint.shared.constants.PaintConstants.PAINT_SWEEP_CONFIGURATION_JSON;
import static paint.shared.utils.ValidProjectPath.getValidProjectPath;

/**
 * TrackMateUI is a command-line plugin implementation that integrates with the TrackMate
 * processing workflow. It provides a graphical user interface and user interaction
 * capabilities to manage the TrackMate operation. This class is responsible for
 * initializing configurations, logging, and triggering the processing of experimental
 * data within a specified project.
 *
 * Features include:
 *
 * - Ensuring only one instance of the process runs at a time.
 * - Retrieving and validating project root paths.
 * - Initializing logging capabilities and configurations.
 * - Displaying project dialogs for user interaction.
 * - Handling the execution of TrackMate processing with optional sweep support.
 * - Triggering secondary operations such as "Generate Squares" after the main process.
 *
 * Thread safety is maintained by managing a volatile static flag that ensures no
 * concurrent execution of the main processing logic.
 */
@Plugin(type = Command.class, menuPath = "Plugins>Glyco-PAINT>Run")
public class TrackMateUI implements Command {

    private static volatile boolean running = false;

    /**
     * Executes the primary workflow of the TrackMate plugin, setting up the environment,
     * initializing logging, configuration, and displaying a dialog for user interaction.
     *
     * The method performs the following steps:
     *
     * 1. Configures a default uncaught exception handler to capture unhandled exceptions in threads.
     * 2. Verifies if the TrackMate processing is already running. If so, notifies the user to wait
     *    until the current process finishes.
     * 3. Retrieves the valid project root path. If no valid path is provided, the method exits.
     * 4. Initializes logging and configuration settings for TrackMate, including the console window,
     *    log level, and runtime preferences. Logs version and timestamp details for diagnostics.
     * 5. Launches a project selection dialog where users can configure and start the desired experiment.
     * 6. Handles the logic when the user confirms the dialog:
     *    - Verifies if processing is currently running and prevents initiating a parallel run.
     *    - Retrieves necessary project details such as root directory, experiment names, and
     *      configuration files.
     *    - Supports optional sweep configurations. If selected, checks for the sweep configuration file
     *      and runs the appropriate sweep workflow. Otherwise, executes the regular project workflow.
     *    - Optionally triggers the "Generate Squares" process after a successful TrackMate run.
     * 7. Ensures that exceptions during execution are logged, and resets the running state of the
     *    application for subsequent runs.
     *
     * This method is the entry point for initiating TrackMate execution within the application.
     */
    @Override
    public void run() {

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                                                          PaintLogger.debugf("AWT complained: %s", throwable.getMessage())
        );

        if (running) {
            showWarning("TrackMate processing is already running.\nPlease wait until it finishes.");
            return;
        }

        // --- Retrieve project root ---
        Path projectPath = getValidProjectPath();
        if (projectPath == null) {
            return;
        }

        // --- Initialise logging and configuration ---
        PaintConsoleWindow.createConsoleFor("TrackMate");
        PaintConfig.initialise(projectPath);
        String debugLevel = PaintPrefs.getString("Log Level", "INFO");
        PaintLogger.setLevel(debugLevel);
        PaintLogger.initialise(projectPath, "TrackMateOnProject.log");
        PaintLogger.debugf("TrackMate plugin started (Interactive).");
        PaintRuntime.initialiseFromPrefs();

        // --- Log version info ---
        JarInfoLogger.JarInfo info = JarInfoLogger.getJarInfo(TrackMateUI.class);
        if (info != null) {
            PaintLogger.infof("Compilation date: %s", info.implementationDate);
            PaintLogger.infof("Version: %s", info.implementationVersion);
        }

        LocalDateTime now = LocalDateTime.now();
        String formattedTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        PaintLogger.infof("Current time: %s", formattedTime);
        PaintLogger.blankline();

        // --- Experiment dialog ---
        ProjectDialog dialog = new ProjectDialog(null, projectPath, ProjectDialog.DialogMode.TRACKMATE);
        PaintConsoleWindow.closeOnDialogDispose(dialog.getDialog());

        // --- What happens when user presses OK ---
        dialog.setCalculationCallback(project -> {

            if (running) {
                JOptionPane optionPane = new JOptionPane(
                        "TrackMate processing is already running.\nPlease wait until it finishes.",
                        JOptionPane.WARNING_MESSAGE
                );
                JDialog warnDialog = optionPane.createDialog(null, "Already Running");
                warnDialog.setAlwaysOnTop(true);
                warnDialog.setVisible(true);
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
                    PaintLogger.debugf("Experiments: %s", project.getExperimentNames().toString());
                }

                boolean success;

                boolean sweepEnabled = dialog.isSweepSelected();
                Path sweepFile = currentProjectRoot.resolve(PAINT_SWEEP_CONFIGURATION_JSON);

                if (sweepEnabled) {
                    if (Files.exists(sweepFile)) {
                         success = RunTrackMateOnProjectSweep.runWithSweep(currentProjectRoot, imagesPath, project.getExperimentNames());
                    }
                    else {
                        PaintLogger.infof("No Sweep configuration detected at %s", sweepFile);
                        return false;
                    }
                } else {
                    success = RunTrackMateOnProject.runProject(projectPath, imagesPath, project.getExperimentNames(), dialog, null);

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
                //SwingUtilities.invokeLater(() -> dialog.setOkEnabled(true));
            }
        });

        dialog.showDialog();
    }

    /**
     * Displays a warning dialog with the specified message.
     *
     * @param message the warning message to be displayed in the dialog
     */
    // --- Utility methods ---
    private void showWarning(String message) {
        JOptionPane optionPane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE);
        JDialog warnDialog = optionPane.createDialog(null, "Warning");
        warnDialog.setAlwaysOnTop(true);
        warnDialog.setVisible(true);
    }

}