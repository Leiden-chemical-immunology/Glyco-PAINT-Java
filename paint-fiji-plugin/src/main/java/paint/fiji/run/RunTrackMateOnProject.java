package paint.fiji.run;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import debug.ImageRootValidator;
import paint.shared.config.PaintConfig;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import paint.shared.debug.ValidateProject;
import paint.shared.utils.AppLogger;
import paint.shared.dialogs.ProjectSpecificationDialog;
import paint.shared.dialogs.ProjectSelectionDialog;

import static paint.shared.constants.PaintConstants.PAINT_CONFIGURATION_JSON;
import static paint.fiji.trackmate.RunTrackMateOnExperiment.runTrackMateOnExperiment;
import static paint.shared.debug.ValidateProject.validateProject;

/**
 * SciJava/Fiji command plugin that runs the TrackMate pipeline on
 * selected experiments in a selected project.
 * <p>
 * The plugin guides the user through selecting a project directory and
 * experiments, validates configuration paths, and invokes
 * {@link paint.fiji.trackmate.RunTrackMateOnExperiment#runTrackMateOnExperiment(Path, Path)}
 * for each experiment.
 * </p>
 *
 * <h3>Workflow</h3>
 * <ol>
 *     <li>User selects a project directory using {@link ProjectSelectionDialog}.</li>
 *     <li>User selects experiments in {@link ProjectSpecificationDialog} (TRACKMATE mode).</li>
 *     <li>Configuration is read from <code>paint-config.json</code>.</li>
 *     <li>Checks are made for the presence of image root and project directories.</li>
 *     <li>TrackMate is executed for each experiment in the project.</li>
 * </ol>
 *
 * Registered in Fiji under the menu path:
 * <pre>
 * Plugins &gt; Glyco-PAINT &gt; Run TrackMate on Project
 * </pre>
 */
@Plugin(type = Command.class, menuPath = "Plugins>Glyco-PAINT>Run TrackMate on Project")
public class RunTrackMateOnProject implements Command {

    /**
     * Entry point for the SciJava Command.
     * <p>
     * Sets up logging, asks the user to select a project,
     * launches an {@link ProjectSpecificationDialog}, and triggers TrackMate processing
     * on all selected experiments when the user confirms.
     * </p>
     */
    @Override
    public void run() {

        // Set up logging
        AppLogger.init("Fiji");
        AppLogger.debugf("TrackMate plugin started - v6.");

        // Display the project directory selection dialog
        ProjectSelectionDialog projDlg = new ProjectSelectionDialog(null);
        Path projectPath = projDlg.showDialog();

        // If the user selected Cancel, then return.
        if (projectPath == null) {
            AppLogger.infof("User cancelled project selection.");
            return;
        }

        // Show ExperimentDialog in TRACKMATE mode
        ProjectSpecificationDialog dialog = new ProjectSpecificationDialog(null, projectPath, ProjectSpecificationDialog.DialogMode.TRACKMATE);

        /**
         * Registers the TrackMate execution logic as a callback with the experiment dialog.
         * <p>
         * This callback is invoked only after the user presses <b>OK</b> in the dialog.
         * It performs the following steps:
         * <ol>
         *     <li>Loads {@link PaintConfig} from the project root.</li>
         *     <li>Validates that both the <em>Image Root</em> and <em>Project Root</em> directories exist.</li>
         *     <li>Aborts with an error (after 5s pause) if required paths are missing.</li>
         *     <li>Logs debug information about the selected project and experiments.</li>
         *     <li>Iterates over all selected experiments and invokes
         *         {@link paint.fiji.trackmate.RunTrackMateOnExperiment#runTrackMateOnExperiment(Path, Path)}
         *         for each one.</li>
         *     <li>Handles and logs any {@link Throwable} thrown during execution.</li>
         * </ol>
         *
         * @param project the project configuration object from {@link ProjectSpecificationDialog},
         *                containing experiment names to process
         */
        dialog.setCalculationCallback(project -> {

            // There has to be an Image Root for TrackMate specified, otherwise nothing can work
            PaintConfig paintConfig = PaintConfig.from(projectPath.resolve(PAINT_CONFIGURATION_JSON));
            String imagesRoot = paintConfig.getString("Paths", "Images Root", "Fatal");
            boolean error = false;

            if (imagesRoot.equals("Fatal")) {
                AppLogger.errorf("No Image Path found - Fatal.");
                error = true;
            }

            // The images root exists but is it the  correct place?
            List<String> report = ImageRootValidator.validateImageRoot(
                    projectPath,
                    Paths.get(imagesRoot),
                    project.experimentNames);
            if (!report.isEmpty()) {
                report.forEach(System.out::println);
                AppLogger.errorf("Image Root Validation Failed - Fatal.");
                error = true;
            }

            // The image root and project root need to exist
            Path imagesPath = Paths.get(imagesRoot);
            if (!error && !imagesPath.toFile().exists()) {
                AppLogger.errorf("Image Root %s does not exist - Fatal.", imagesPath);
                error = true;
            }
            if (!error && !projectPath.toFile().exists()) {
                AppLogger.errorf("Project Root %s does not exist - Fatal.", projectPath);
                error = true;
            }

            // If there is an error, display a warning message for 5 seconds and then exit
            if (error) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    AppLogger.errorf("Failed to sleep - %s", e.getMessage());
                }
                System.exit(-1);
            }

            // Basic conditions have been met
            AppLogger.debugf("TrackMate processing started.");
            AppLogger.debugf("Project root : %s", projectPath.toString());
            AppLogger.debugf("Images root  : %s", imagesPath.toString());
            AppLogger.debugf("Experiments  : %s", project.experimentNames.toString());

            // Verify if the experiments appear valid
            List<String> errors = validateProject(projectPath, project.experimentNames, false);
            ValidateProject.printReport(errors);

            // Cycle through the experiments and run TrackMate on each one
            for (String experimentName : project.experimentNames) {
                Path experimentPath = projectPath.resolve(experimentName);
                if (!Files.isDirectory(experimentPath)) {
                    AppLogger.errorf("Experiment directory '%s' does not exist.", experimentPath);
                } else {
                    try {
                        runTrackMateOnExperiment(experimentPath, imagesPath.resolve(experimentName));
                    } catch (Throwable t) {
                        AppLogger.errorf("Error during TrackMate run for '%s': %s",
                                experimentName, t.getMessage());
                    }
                }
            }
        });

        // Non-blocking dialog – user must press OK to trigger processing
        dialog.showDialog();
    }
}