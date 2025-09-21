package paint.fiji.run;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import paint.fiji.utils.ImageRootValidator;
import paint.shared.config.PaintConfig;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import paint.shared.debug.ValidateProject;
import paint.shared.utils.AppLogger;
import paint.shared.dialogs.ProjectSpecificationDialog;
import paint.shared.dialogs.ProjectSelectionDialog;

import static paint.shared.config.PaintConfig.getBoolean;
import static paint.shared.config.PaintConfig.getString;
import static paint.shared.constants.PaintConstants.PAINT_CONFIGURATION_JSON;
import static paint.fiji.trackmate.RunTrackMateOnExperiment.runTrackMateOnExperiment;
import static paint.shared.debug.ValidateProject.formatReport;
import static paint.shared.debug.ValidateProject.validateProject;
import static paint.shared.debug.ValidateProject.ValidateResult;

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
        AppLogger.init("TrackMateOnProject");
        AppLogger.debugf("TrackMate plugin started");

        // Display the project directory selection dialog
        ProjectSelectionDialog projDlg = new ProjectSelectionDialog(null);
        Path projectPath = projDlg.showDialog();

        // If the user selected Cancel, then return.
        if (projectPath == null) {
            AppLogger.infof("User cancelled project selection.");
            return;
        }

        // Set up the json config
        PaintConfig.initialise(projectPath.resolve(PAINT_CONFIGURATION_JSON));

        // Show ExperimentDialog in TRACKMATE mode
        ProjectSpecificationDialog dialog = new ProjectSpecificationDialog(null, projectPath, ProjectSpecificationDialog.DialogMode.TRACKMATE);

        /*
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

            boolean status = true;
            boolean debug = getBoolean("Debug", "RunTrackMateOnProject", false);

            String imagesRoot = getString("Paths", "Images Root", "Fatal");
            boolean error = false;

            if (imagesRoot.equals("Fatal")) {
                AppLogger.errorf("No Image Path retrieved from configuration file- Fatal.");
                error = true;
            }
            if (debug) AppLogger.debugf("The Image Path is specified as '%s'.", imagesRoot);

            // The image root and project root need to exist
            Path imagesPath = Paths.get(imagesRoot);
            if (!error && !imagesPath.toFile().exists()) {
                AppLogger.errorf("The Image Root '%s' does not exist - Fatal.", imagesPath);
                error = true;
            }
            if (debug) AppLogger.debugf("The Image Path '%s' exists.", imagesRoot);

            // The images root exists but is it a valid?
            List<String> report = ImageRootValidator.validateImageRoot(
                    projectPath,
                    imagesPath,
                    project.experimentNames);
            if (!report.isEmpty()) {
                report.forEach(msg -> AppLogger.errorf("%s", msg));
                AppLogger.errorf("The Image Root Validation Failed - Fatal.");
                error = true;
            }
            if (debug) AppLogger.debugf("The Image Path '%s' passed the validation test.", imagesRoot);

            if (!error && !projectPath.toFile().exists()) {
                AppLogger.errorf("Project Root '%s' does not exist - Fatal.", projectPath);
                error = true;
            }
            if (debug) AppLogger.debugf("The project root '%s' exists.", projectPath);

            // If there is an error, display a warning message for 5 seconds and then exit
            if (error) {
                try {
                    AppLogger.errorf("About to exit with error, refer to log file.", projectPath);
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    AppLogger.errorf("Failed to sleep - %s", e.getMessage());
                }
                System.exit(-1);
            }

            // Basic conditions have been met
            if (debug) AppLogger.debugf("TrackMate processing started.");
            if (debug) AppLogger.debugf("Experiments %s", project.experimentNames.toString());

            // Verify if the experiments appear valid
            ValidateResult validateResult = validateProject(projectPath, project.experimentNames, ValidateProject.Mode.VALIDATE_TRACKMATE );
            if (validateResult.isOk()) {
                formatReport(validateResult.getErrors());
            }
            if (debug) AppLogger.debugf("Experiments appear valid.");

            // Cycle through the experiments and run TrackMate on each one
            for (String experimentName : project.experimentNames) {
                Path experimentPath = projectPath.resolve(experimentName);
                if (!Files.isDirectory(experimentPath)) {
                    AppLogger.errorf("Experiment directory '%s' does not exist.", experimentPath);
                } else {
                    try {
                        status = runTrackMateOnExperiment(experimentPath, imagesPath.resolve(experimentName));
                    } catch (Exception e) {
                        AppLogger.errorf("Error during TrackMate run for '%s': %s", experimentName, e.getMessage());
                        e.printStackTrace();
                    } catch (Throwable t) {
                        AppLogger.errorf("Severe error during TrackMate run for '%s': %s", experimentName, t.getMessage());
                        t.printStackTrace();
                    }
                }
            }
            return status;
        });

        // Non-blocking dialog – user must press OK to trigger processing
        dialog.showDialog();

    }
}