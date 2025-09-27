package paint.fiji.trackmate;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectSelectionDialog;
import paint.shared.dialogs.ProjectSpecificationDialog;
import paint.shared.utils.JarInfo;
import paint.shared.utils.PaintLogger;
import paint.shared.utils.PaintConsoleWindow;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static paint.shared.config.PaintConfig.getBoolean;
import static paint.shared.config.PaintConfig.getString;
import static paint.shared.utils.JarInfoLogger.getJarInfo;

/**
 * Fiji plugin entry point for running TrackMate on a selected project.
 * <p>
 * This class handles user dialogs and delegates processing to
 * {@link RunTrackMateOnProjectCore} or {@link RunTrackMateSweepOnProject}
 * if a sweep configuration file is present.
 */
@Plugin(type = Command.class, menuPath = "Plugins>Glyco-PAINT>Run TrackMate on Project")
public class RunTrackMateOnProject implements Command {

    private static volatile boolean running = false;

    @Override
    public void run() {

        // Prevent noisy AWT exception messages
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            PaintLogger.warningf("AWT is complaining - ignore");
        });

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

        // Project selection dialog
        ProjectSelectionDialog projDlg = new ProjectSelectionDialog(null);
        Path projectPath = projDlg.showDialog();

        if (projectPath == null) {
            PaintLogger.infof("User cancelled project selection.");
            return;
        }

        // Initialise config + logger
        PaintConfig.initialise(projectPath);
        PaintLogger.initialise(projectPath, "TrackMateOnProject");
        PaintLogger.debugf("TrackMate plugin started");

        // Log JAR info
        JarInfo info = getJarInfo(RunTrackMateOnProject.class);
        if (info != null) {
            PaintLogger.infof("Compilation date: %s", info.implementationDate);
            PaintLogger.infof("Version: %s", info.implementationVersion);
        } else {
            PaintLogger.errorf("No manifest information found.");
            PaintLogger.blankline();
        }

        // Experiment selection dialog
        ProjectSpecificationDialog dialog = new ProjectSpecificationDialog(
                null, projectPath, ProjectSpecificationDialog.DialogMode.TRACKMATE);

        // Ensure console closes when dialog closes
        PaintConsoleWindow.closeOnDialogDispose(dialog.getDialog());

        dialog.setCalculationCallback(project -> {
            PaintLogger.infof(">>> Entered calculation callback");

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
            PaintLogger.infof("@@@ TrackMate plugin started 1");

            running = true;
            dialog.setOkEnabled(false);

            try {
                boolean debug = getBoolean("Debug", "RunTrackMateOnProject", false);
                String imagesRoot = getString("Paths", "Images Root", "validate");
                Path imagesPath = Paths.get(imagesRoot);

                if (debug) {
                    PaintLogger.debugf("TrackMate processing started.");
                    PaintLogger.debugf("Experiments %s", project.experimentNames.toString());
                }

                // Sweep detection
                Path sweepFile = projectPath.resolve("Sweep Config.json");
                PaintLogger.infof("Sweep Config file %s", sweepFile.toString());
                if (Files.exists(sweepFile)) {
                    PaintLogger.infof("Sweep configuration detected at %s", sweepFile);
                    try {
                        return RunTrackMateSweepOnProject.runWithSweep(
                                projectPath, imagesPath, project.experimentNames);
                    } catch (IOException e) {
                        PaintLogger.errorf("Sweep execution failed: %s", e.getMessage());
                        return false;
                    }
                }

                // Normal execution
                return RunTrackMateOnProjectCore.runProject(
                        projectPath, imagesPath, project.experimentNames, dialog, null);

            } finally {
                running = false;
                SwingUtilities.invokeLater(() -> dialog.setOkEnabled(true));
            }
        });

        dialog.showDialog();
    }
}