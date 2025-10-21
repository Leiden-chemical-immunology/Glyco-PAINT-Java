package paint.fiji.trackmate;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectSpecificationDialog;
import paint.shared.prefs.PaintPrefs;
import paint.shared.utils.JarInfoLogger;
import paint.shared.utils.PaintConsoleWindow;
import paint.shared.utils.PaintLogger;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================================
 *  TrackMateUI.java
 *  Part of the Glyco-PAINT Fiji plugin.
 *
 *  <p><b>Purpose:</b><br>
 *  Interactive version of the TrackMate launcher.
 *  Uses the project root stored in preferences, allowing the user
 *  to select experiments interactively before running.
 *  </p>
 *
 *  <p><b>Menu:</b><br>
 *  Plugins ▸ Glyco-PAINT ▸ Run (Interactive)
 *  </p>
 * ============================================================================
 */
@Plugin(type = Command.class, menuPath = "Plugins>Glyco-PAINT>Run (Interactive)")
public class TrackMateUI implements Command {

    private static volatile boolean running = false;

    @Override
    public void run() {

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                                                          PaintLogger.debugf("AWT complained: %s", throwable.getMessage())
        );

        if (running) {
            showWarning("TrackMate processing is already running.\nPlease wait until it finishes.");
            return;
        }

        // --- Retrieve project root from preferences ---
        String projectRoot = PaintPrefs.getString("Project Root", null);
        if (projectRoot == null || projectRoot.trim().isEmpty()) {
            showError("No project root found in preferences.\n" +
                              "Please set 'Project Root' in Glyco-PAINT preferences first.");
            return;
        }

        Path projectPath = Paths.get(projectRoot);
        if (!Files.isDirectory(projectPath)) {
            showError("The configured project path does not exist:\n" + projectPath);
            return;
        }

        // --- Initialise logging and configuration ---
        PaintConsoleWindow.createConsoleFor("TrackMate");
        PaintConfig.initialise(projectPath);
        String debugLevel = PaintConfig.getString("Paint", "Log Level", "INFO");
        PaintLogger.setLevel(debugLevel);
        PaintLogger.initialise(projectPath, "TrackMateOnProject.log");
        PaintLogger.debugf("TrackMate plugin started (Interactive).");

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
        ProjectSpecificationDialog dialog = new ProjectSpecificationDialog(
                null, projectPath, ProjectSpecificationDialog.DialogMode.TRACKMATE);
        PaintConsoleWindow.closeOnDialogDispose(dialog.getDialog());

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
                Path imagesPath = project.getImagesRootPath(); // ✅ from dialog
                Path currentProjectRoot = project.getProjectRootPath(); // ✅ new local variable

                if (debug) {
                    PaintLogger.debugf("TrackMate processing started.");
                    PaintLogger.debugf("Experiments: %s", project.experimentNames.toString());
                }

                Path sweepFile = currentProjectRoot.resolve("Sweep Config.json");
                if (Files.exists(sweepFile)) {
                    PaintLogger.infof("Sweep configuration detected at %s", sweepFile);
                    return RunTrackMateSweepOnProject.runWithSweep(
                            currentProjectRoot, imagesPath, project.experimentNames);
                }

                return RunTrackMate.run(currentProjectRoot, imagesPath, project.experimentNames);

            } catch (Exception e) {
                PaintLogger.errorf("Error during TrackMate execution: %s", e.getMessage());
                return false;
            } finally {
                running = false;
                SwingUtilities.invokeLater(() -> dialog.setOkEnabled(true));
            }
        });

        dialog.showDialog();
    }

    // --- Utility methods ---
    private void showWarning(String message) {
        JOptionPane optionPane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE);
        JDialog warnDialog = optionPane.createDialog(null, "Warning");
        warnDialog.setAlwaysOnTop(true);
        warnDialog.setVisible(true);
    }

    private void showError(String message) {
        JOptionPane optionPane = new JOptionPane(message, JOptionPane.ERROR_MESSAGE);
        JDialog errorDialog = optionPane.createDialog(null, "Configuration Error");
        errorDialog.setAlwaysOnTop(true);
        errorDialog.setVisible(true);
    }
}