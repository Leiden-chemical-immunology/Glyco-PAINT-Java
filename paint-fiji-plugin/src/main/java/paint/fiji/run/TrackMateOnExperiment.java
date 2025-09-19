package paint.fiji.run;

import org.scijava.command.Command;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ExperimentDialog;
import paint.fiji.trackmate.RunTrackMateExperimentDialog;
import paint.shared.utils.AppLogger;

import static paint.fiji.trackmate.RunTrackMateOnExperiment.cycleThroughRecordings;

/**
 * SciJava Command that opens ExperimentDialog and launches TrackMate work
 * on a background thread.
 */
// Make this one invisible
// @Plugin(type = Command.class, menuPath = "Plugins>Glyco-PAINT>Run TrackMate on Experiment")
public class TrackMateOnExperiment implements Command {

    private PaintConfig config;

    @Override
    public void run() {
        final Path[] imagesAndExperiment = new Path[2];
        final boolean[] ok = new boolean[1];

        // Set up logging
        AppLogger.init("TrackMateOnExperiment");
        AppLogger.debugf("RunTrackMateOnExperiment plugin started - v1.");

        // Ask for directories first (like before)
        try {
            SwingUtilities.invokeAndWait(() -> {
                RunTrackMateExperimentDialog dlg = new RunTrackMateExperimentDialog(null);
                dlg.setVisible(true);

                if (dlg.isConfirmed()) {
                    ok[0] = true;
                    imagesAndExperiment[0] = Paths.get(dlg.getImagesDir());
                    imagesAndExperiment[1] = Paths.get(dlg.getExperimentDir());
                }
            });
        } catch (Exception e) {
            AppLogger.errorf(e.getMessage());
            return;
        }

        if (!ok[0]) {
            AppLogger.infof("RunTrackMateOnExperiment: cancelled by user.");
            return;
        }

        // Unpack the paths
        Path imagesPath = imagesAndExperiment[0];
        Path experimentPath = imagesAndExperiment[1];

        // Now open the ExperimentDialog for TRACKMATE mode
        ExperimentDialog experimentDialog =
                new ExperimentDialog(null, experimentPath, ExperimentDialog.DialogMode.TRACKMATE);

        // Register the calculation callback
        experimentDialog.setCalculationCallback(project -> {
            cycleThroughRecordings(project.getProjectPath(), imagesPath);
        });

        // Show the dialog (non-blocking; it manages itself)
        SwingUtilities.invokeLater(experimentDialog::showDialog);
    }
}