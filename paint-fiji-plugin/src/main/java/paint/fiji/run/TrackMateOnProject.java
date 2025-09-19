package paint.fiji.run;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import paint.shared.config.PaintConfig;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import paint.shared.utils.AppLogger;
import paint.shared.dialogs.ExperimentDialog;
import paint.shared.dialogs.ProjectDialog;

import static paint.shared.constants.PaintConstants.PAINT_CONFIGURATION_JSON;
import static paint.fiji.trackmate.RunTrackMateOnExperiment.cycleThroughRecordings;

@Plugin(type = Command.class, menuPath = "Plugins>Glyco-PAINT>Run TrackMate on Project")
public class TrackMateOnProject implements Command {

    @Override
    public void run() {

        // Set up logging
        AppLogger.init("Fiji");
        AppLogger.debugf("TrackMate plugin started - v6.");

        // Display the project directory selection dialog
        ProjectDialog projDlg = new ProjectDialog(null);
        Path projectPath = projDlg.showDialog();

        // If the user selected Cancel, then return.
        if (projectPath == null) {
            AppLogger.infof("User cancelled project selection.");
            return;
        }

        // Show ExperimentDialog in TRACKMATE mode
        ExperimentDialog dialog = new ExperimentDialog(null, projectPath, ExperimentDialog.DialogMode.TRACKMATE);

        // ✅ Register calculation callback – runs only after OK is pressed
        dialog.setCalculationCallback(project -> {
            PaintConfig paintConfig = PaintConfig.from(projectPath.resolve(PAINT_CONFIGURATION_JSON));
            String imagesDir = paintConfig.getString("Paths", "Image Directory", "Fatal");
            boolean error = false;
            if (imagesDir.equals("Fatal")) {
                AppLogger.errorf("No Image Path found - Fatal.");
                error = true;
            }
            Path imagesPath = Paths.get(imagesDir);
            if (!error && !imagesPath.toFile().exists()) {
                AppLogger.errorf("Image Path %s does not exist - Fatal.", imagesPath);
                error = true;
            }
            if (!error && !projectPath.toFile().exists()) {
                AppLogger.errorf("Project Path %s does not exist - Fatal.", projectPath);
                error = true;
            }
            if (error) {
                try {
                    Thread.sleep(5000); // pause 2 seconds
                } catch (InterruptedException e) {
                    AppLogger.errorf("Failed to sleep - %s", e.getMessage());
                }
                System.exit(-1);
            }

            AppLogger.debugf("TrackMate processing started.");
            AppLogger.debugf("Project path: %s", projectPath.toString());
            AppLogger.debugf("Images root : %s", imagesPath.toString());
            AppLogger.debugf("Experiments : %s", project.experimentNames.toString());

            // Cycle through the experiments and run TrackMate on each one
            for (String experimentName : project.experimentNames) {
                Path experimentPath = projectPath.resolve(experimentName);
                if (!Files.isDirectory(experimentPath)) {
                    AppLogger.errorf("Experiment directory '%s' does not exist.", experimentPath);
                } else {
                    try {
                        cycleThroughRecordings(experimentPath, imagesPath.resolve(experimentName));
                    } catch (Throwable t) {
                        AppLogger.errorf("Error during TrackMate run for '%s': %s",
                                experimentName, t.getMessage());
                        // t.printStackTrace();
                    }
                }
            }
        });

        // Non-blocking dialog – user must press OK to trigger processing
        dialog.showDialog();
    }
}