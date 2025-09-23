package paint.fiji.run;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import paint.shared.utils.JarInfo;
import paint.shared.validate.ImageRootValidator;
import paint.shared.config.PaintConfig;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import paint.shared.validate.ProjectValidator;
import paint.shared.utils.AppLogger;
import paint.shared.dialogs.ProjectSpecificationDialog;
import paint.shared.dialogs.ProjectSelectionDialog;

import static paint.shared.config.PaintConfig.getBoolean;
import static paint.shared.config.PaintConfig.getString;
import static paint.shared.constants.PaintConstants.PAINT_CONFIGURATION_JSON;
import static paint.fiji.trackmate.RunTrackMateOnExperiment.runTrackMateOnExperiment;
import static paint.shared.utils.JarInfoLogger.getJarInfo;
import static paint.shared.validate.ProjectValidator.formatReport;
import static paint.shared.validate.ProjectValidator.validateProject;
import static paint.shared.validate.ProjectValidator.ValidateResult;

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

        // Initialize logging for the session
        AppLogger.init("TrackMateOnProject");
        AppLogger.debugf("TrackMate plugin started");

        // Log the JAR's manifest metadata if available
        JarInfo info = getJarInfo(RunTrackMateOnProject.class);
        if (info != null) {
            AppLogger.infof("Compilation date: %s",info.implementationDate);
            AppLogger.infof("Version: %s",info.implementationVersion);
        } else {
            AppLogger.errorf("No manifest information found.");
        }

        // Ask user to select a project directory
        ProjectSelectionDialog projDlg = new ProjectSelectionDialog(null);
        Path projectPath = projDlg.showDialog();

        // Handle cancellation
        if (projectPath == null) {
            AppLogger.infof("User cancelled project selection.");
            return;
        }

        // Load paint-config.json from the selected project
        PaintConfig.initialise(projectPath.resolve(PAINT_CONFIGURATION_JSON));

        // Display experiment selection dialog in TRACKMATE mode
        ProjectSpecificationDialog dialog = new ProjectSpecificationDialog(null, projectPath, ProjectSpecificationDialog.DialogMode.TRACKMATE);

        /*
         * Register a callback that runs after the user clicks OK.
         * Validates paths and runs TrackMate for all selected experiments.
         */
        dialog.setCalculationCallback(project -> {

            boolean status = true;
            boolean debug = getBoolean("Debug", "RunTrackMateOnProject", false);

            // Load the Images Root path from configuration
            String imagesRoot = getString("Paths", "Images Root", "Fatal");
            boolean error = false;

            // Check if image root was found
            if (imagesRoot.equals("Fatal")) {
                AppLogger.errorf("No Image Path retrieved from configuration file - Fatal.");
                error = true;
            }

            if (debug) AppLogger.debugf("The Image Path is specified as '%s'.", imagesRoot);

            Path imagesPath = Paths.get(imagesRoot);

            // Verify existence of Images Root directory
            if (!error && !imagesPath.toFile().exists()) {
                AppLogger.errorf("The Image Root '%s' does not exist - Fatal.", imagesPath);
                error = true;
            }

            if (debug) AppLogger.debugf("The Image Path '%s' exists.", imagesRoot);

            // Validate that the image root contains data for the selected experiments
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

            // Check that the project path also exists
            if (!error && !projectPath.toFile().exists()) {
                AppLogger.errorf("Project Root '%s' does not exist - Fatal.", projectPath);
                error = true;
            }

            if (debug) AppLogger.debugf("The project root '%s' exists.", projectPath);

            // Abort execution if any critical path is missing
            if (error) {
                try {
                    AppLogger.errorf("About to exit with error, refer to log file.", projectPath);
                    Thread.sleep(10000); // pause to let user read error
                } catch (InterruptedException e) {
                    AppLogger.errorf("Failed to sleep - %s", e.getMessage());
                }
                System.exit(-1);
            }

            // Validation passed, begin processing
            if (debug) AppLogger.debugf("TrackMate processing started.");
            if (debug) AppLogger.debugf("Experiments %s", project.experimentNames.toString());

            // Validate experiment directories
            ValidateResult validateResult = validateProject(projectPath, project.experimentNames, ProjectValidator.Mode.VALIDATE_TRACKMATE );
            if (validateResult.isOk()) {
                formatReport(validateResult.getErrors());
            }

            if (debug) AppLogger.debugf("Experiments appear valid.");

            // Run TrackMate on each selected experiment
            for (String experimentName : project.experimentNames) {
                Path experimentPath = projectPath.resolve(experimentName);
                if (!Files.isDirectory(experimentPath)) {
                    AppLogger.errorf("Experiment directory '%s' does not exist.", experimentPath);
                } else {
                    try {
                        status = runTrackMateOnExperiment(experimentPath, imagesPath.resolve(experimentName));
                    } catch (Exception e) {
                        AppLogger.errorf("Error during TrackMate run for '%s': %s", experimentName, e.getMessage());
                        StringWriter sw = new StringWriter();
                        e.printStackTrace(new PrintWriter(sw));
                        AppLogger.errorf("An exception occurred:\n" + sw.toString());
                    } catch (Throwable t) {
                        AppLogger.errorf("Severe error during TrackMate run for '%s': %s", experimentName, t.getMessage());
                        StringWriter sw = new StringWriter();
                        t.printStackTrace(new PrintWriter(sw));
                        AppLogger.errorf("An exception occurred:\n" + sw.toString());
                    }
                }
            }

            return status;
        });

        // Show the experiment selection dialog (non-blocking)
        dialog.showDialog();
    }
}