package paint.fiji.trackmate;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectSpecificationDialog;
import paint.shared.dialogs.RootSelectionDialog;
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
 *  Allows the user to select a project and experiments before running.
 *  </p>
 *
 *  <p><b>Menu:</b><br>
 *  Plugins ▸ Glyco-PAINT ▸ Run TrackMate on Project
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
            JOptionPane optionPane = new JOptionPane(
                    "TrackMate processing is already running.\nPlease wait until it finishes.",
                    JOptionPane.WARNING_MESSAGE
            );
            JDialog warnDialog = optionPane.createDialog(null, "Already Running");
            warnDialog.setAlwaysOnTop(true);
            warnDialog.setVisible(true);
            return;
        }

        // Select project root directory
        RootSelectionDialog selectionDialog = new RootSelectionDialog(null, RootSelectionDialog.Mode.PROJECT);
        Path projectPath = selectionDialog.showDialog();
        if (projectPath == null) {
            PaintLogger.infof("User cancelled project selection.");
            return;
        }

        // --- Initialise logging and configuration ---
        PaintConsoleWindow.createConsoleFor("TrackMate");
        PaintConfig.initialise(projectPath);
        String debugLevel = PaintConfig.getString("Paint", "Log Level", "INFO");
        PaintLogger.setLevel(debugLevel);
        PaintLogger.initialise(projectPath, "TrackMateOnProject");
        PaintLogger.debugf("TrackMate plugin started.");

        // --- Log JAR information ---
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
                String imagesRoot = PaintPrefs.getString("Images Root", "");
                Path imagesPath = Paths.get(imagesRoot);

                if (debug) {
                    PaintLogger.debugf("TrackMate processing started.");
                    PaintLogger.debugf("Experiments: %s", project.experimentNames.toString());
                }

                // Detect sweep configuration
                Path sweepFile = projectPath.resolve("Sweep Config.json");
                if (Files.exists(sweepFile)) {
                    PaintLogger.infof("Sweep configuration detected at %s", sweepFile);
                    return RunTrackMateSweepOnProject.runWithSweep(
                            projectPath, imagesPath, project.experimentNames);
                }

                // Normal processing
                return RunTrackMate.run(projectPath, imagesPath, project.experimentNames);

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
}