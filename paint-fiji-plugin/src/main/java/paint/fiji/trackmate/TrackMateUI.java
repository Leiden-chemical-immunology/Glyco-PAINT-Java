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
 *  Provides the graphical entry point for running TrackMate on a project.
 *  Handles user interaction (dialogs), logging initialization, and delegates
 *  the actual processing to {@link RunTrackMate} for headless execution.
 *  </p>
 *
 *  <p><b>Notes:</b><br>
 *  This class replaces {@code RunTrackMateOnProject} as the plugin entry.
 *  It remains responsible only for the UI and user-triggered flow.
 *  </p>
 *
 *  <p><b>Author:</b> Herr Doctor<br>
 *  <b>Version:</b> 2.0<br>
 *  <b>Module:</b> paint-fiji-plugin
 *  </p>
 * ============================================================================
 */
@Plugin(type = Command.class, menuPath = "Plugins>Glyco-PAINT>Run")
public class TrackMateUI implements Command {

    private static volatile boolean running = false;

    @Override
    public void run() {

        // Prevent duplicate executions
        if (running) {
            JOptionPane.showMessageDialog(
                    null,
                    "TrackMate processing is already running.\nPlease wait until it finishes.",
                    "Already Running",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Select project root directory
        RootSelectionDialog selectionDialog = new RootSelectionDialog(null, RootSelectionDialog.Mode.PROJECT);
        Path projectPath = selectionDialog.showDialog();
        if (projectPath == null) {
            PaintLogger.infof("User cancelled project selection.");
            return;
        }

        // Initialise console + configuration
        PaintConsoleWindow.createConsoleFor("TrackMate");
        PaintConfig.initialise(projectPath);
        PaintLogger.setLevel(PaintConfig.getString("Paint", "Log Level", "INFO"));
        PaintLogger.initialise(projectPath, "TrackMateOnProject");
        PaintLogger.debugf("TrackMate plugin started.");

        // Log metadata
        JarInfoLogger.JarInfo info = JarInfoLogger.getJarInfo(TrackMateUI.class);
        if (info != null) {
            PaintLogger.infof("Compilation date: %s", info.implementationDate);
            PaintLogger.infof("Version: %s", info.implementationVersion);
        } else {
            PaintLogger.errorf("No manifest information found.");
        }

        String formattedTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        PaintLogger.infof("Current time: %s", formattedTime);
        PaintLogger.blankline();

        // Experiment selection dialog
        ProjectSpecificationDialog dialog = new ProjectSpecificationDialog(
                null, projectPath, ProjectSpecificationDialog.DialogMode.TRACKMATE);
        PaintConsoleWindow.closeOnDialogDispose(dialog.getDialog());

        dialog.setCalculationCallback(project -> {
            if (running) {
                JOptionPane.showMessageDialog(
                        null,
                        "TrackMate processing is already running.\nPlease wait until it finishes.",
                        "Already Running",
                        JOptionPane.WARNING_MESSAGE
                );
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
            } finally {
                running = false;
                SwingUtilities.invokeLater(() -> dialog.setOkEnabled(true));
            }
        });

        dialog.showDialog();
    }
}