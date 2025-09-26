package paint.fiji.run;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectSelectionDialog;
import paint.shared.dialogs.ProjectSpecificationDialog;
import paint.shared.utils.JarInfo;
import paint.shared.utils.PaintLogger;

import paint.shared.validateOld.ProjectValidatorOld;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static paint.fiji.trackmate.RunTrackMateOnExperiment.runTrackMateOnExperiment;
import static paint.shared.config.PaintConfig.getBoolean;
import static paint.shared.config.PaintConfig.getString;
import static paint.shared.utils.JarInfoLogger.getJarInfo;
import static paint.shared.utils.Miscellaneous.formatDuration;
import static paint.shared.validate.ImageRootValidator.validateImageRoot;

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
 * <p>
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

        // Ask user to select a project directory
        ProjectSelectionDialog projDlg = new ProjectSelectionDialog(null);
        Path projectPath = projDlg.showDialog();

        // Handle cancellation
        if (projectPath == null) {
            PaintLogger.infof("User cancelled project selection.");
            return;
        }

        // The user did OK and we have a valid projectPath
        PaintConfig.initialise(projectPath);
        PaintLogger.initialise(projectPath, "TrackMateOnProject");
        PaintLogger.debugf("TrackMate plugin started");

        // Log the JAR's manifest metadata if available
        JarInfo info = getJarInfo(RunTrackMateOnProject.class);
        if (info != null) {
            PaintLogger.infof("Compilation date: %s", info.implementationDate);
            PaintLogger.infof("Version: %s", info.implementationVersion);
        } else {
            PaintLogger.errorf("No manifest information found.");
            PaintLogger.infof();
        }

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

            // Record the start time
            LocalDateTime start = LocalDateTime.now();

            // Check if image root was found
            if (imagesRoot.equals("Fatal")) {
                PaintLogger.errorf("No Image Path retrieved from configuration file - Fatal.");
                error = true;
            }

            if (debug) PaintLogger.debugf("The Image Path is specified as '%s'.", imagesRoot);

            Path imagesPath = Paths.get(imagesRoot);

            // Verify existence of Images Root directory
            if (!error && !imagesPath.toFile().exists()) {
                PaintLogger.errorf("The Image Root '%s' does not exist - Fatal.", imagesPath);
                error = true;
            }

            if (debug) PaintLogger.debugf("The Image Path '%s' exists.", imagesRoot);

            // Validate that the image root contains data for the selected experiments
            List<String> report = validateImageRoot(
                    projectPath,
                    imagesPath,
                    project.experimentNames);
            if (!report.isEmpty()) {
                report.forEach(msg -> PaintLogger.errorf("%s", msg));
                PaintLogger.errorf("The Image Root Validation Failed - Fatal.");
                error = true;
            }

            if (debug) PaintLogger.debugf("The Image Path '%s' passed the validation test.", imagesRoot);

            // Check that the project path also exists
            if (!error && !projectPath.toFile().exists()) {
                PaintLogger.errorf("Project Root '%s' does not exist - Fatal.", projectPath);
                error = true;
            }

            if (debug) PaintLogger.debugf("The project root '%s' exists.", projectPath);

            // Abort execution if any critical path is missing
            if (error) {
                try {
                    PaintLogger.errorf("About to exit with error, refer to log file.", projectPath);
                    Thread.sleep(10000); // pause to let user read error
                } catch (InterruptedException e) {
                    PaintLogger.errorf("Failed to sleep - %s", e.getMessage());
                }
                System.exit(-1);
            }

            // Validation passed, begin processing
            if (debug) PaintLogger.debugf("TrackMate processing started.");
            if (debug) PaintLogger.debugf("Experiments %s", project.experimentNames.toString());

            // Validate experiment directories
//            ProjectValidatorOld.ValidateResult validateResult = validateProject(projectPath, project.experimentNames, ProjectValidatorOld.Mode.VALIDATE_TRACKMATE);
//            if (validateResult.isOk()) {
//                formatReport(validateResult.getErrors());
//            }

            if (debug) PaintLogger.debugf("Experiments appear valid.");

            // Run TrackMate on each selected experiment
            for (String experimentName : project.experimentNames) {
                Path experimentPath = projectPath.resolve(experimentName);
                if (!Files.isDirectory(experimentPath)) {
                    PaintLogger.errorf("Experiment directory '%s' does not exist.", experimentPath);
                } else {
                    try {
                        status = runTrackMateOnExperiment(experimentPath, imagesPath.resolve(experimentName));
                    } catch (Exception e) {
                        PaintLogger.errorf("Error during TrackMate run for '%s': %s", experimentName, e.getMessage());
                        StringWriter sw = new StringWriter();
                        e.printStackTrace(new PrintWriter(sw));
                        PaintLogger.errorf("An exception occurred:\n" + sw);
                    } catch (Throwable t) {
                        PaintLogger.errorf("Severe error during TrackMate run for '%s': %s", experimentName, t.getMessage());
                        StringWriter sw = new StringWriter();
                        t.printStackTrace(new PrintWriter(sw));
                        PaintLogger.errorf("An exception occurred:\n" + sw);
                    }
                }
            }
            Duration totalDuration = Duration.between(start, LocalDateTime.now());
            int durationInSeconds = (int) (totalDuration.toMillis() / 1000);
            PaintLogger.infof();
            PaintLogger.infof("Processed %d experiments in %s.", project.experimentNames.size(), formatDuration(durationInSeconds));
            return status;
        });

        // Show the experiment selection dialog (non-blocking)
        dialog.showDialog();
    }
}