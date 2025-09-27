package paint.fiji.run;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectSelectionDialog;
import paint.shared.dialogs.ProjectSpecificationDialog;
import paint.shared.utils.JarInfo;
import paint.shared.utils.PaintLogger;
import paint.shared.utils.PaintConsoleWindow;
import paint.shared.validate.ValidationResult;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;

import static paint.fiji.trackmate.RunTrackMateOnExperiment.runTrackMateOnExperiment;
import static paint.shared.config.PaintConfig.getBoolean;
import static paint.shared.config.PaintConfig.getString;
import static paint.shared.constants.PaintConstants.EXPERIMENT_INFO_CSV;
import static paint.shared.utils.JarInfoLogger.getJarInfo;
import static paint.shared.utils.Miscellaneous.formatDuration;
import static paint.shared.validate.ImageRootValidator.validateImageRoot;
import static paint.shared.validate.Validation.validateExperiments;

/**
 * Fiji/TrackMate plugin entry point for running TrackMate on a complete project.
 * <p>
 * This plugin lets the user:
 * <ul>
 *   <li>Select a project</li>
 *   <li>Validate image paths and experiment info files</li>
 *   <li>Run TrackMate analysis on all experiments within the project</li>
 * </ul>
 */
@Plugin(type = Command.class, menuPath = "Plugins>Glyco-PAINT>Run TrackMate on Project")
public class RunTrackMateOnProject implements Command {

    /** Flag to prevent multiple concurrent runs of TrackMate on a project. */
    private static volatile boolean running = false;

    /**
     * Main entry point for the Fiji/TrackMate plugin.
     * <p>
     * Handles user interaction, validation, and launches TrackMate
     * processing across all experiments in the chosen project.
     */
    @Override
    public void run() {

        // Prevent AWT from showing annoying uncaught exception messages
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            PaintLogger.warningf("AWT is complaining - ignore");
        });

        // Prevent multiple runs in parallel
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

        // Step 1: Let the user select the project folder
        ProjectSelectionDialog projDlg = new ProjectSelectionDialog(null);
        Path projectPath = projDlg.showDialog();

        if (projectPath == null) {
            PaintLogger.infof("User cancelled project selection.");
            return;
        }

        // Step 2: Initialise configuration and logging
        PaintConfig.initialise(projectPath);
        PaintLogger.initialise(projectPath, "TrackMateOnProject");
        PaintLogger.debugf("TrackMate plugin started");

        // Step 3: Log build/version information from JAR manifest
        JarInfo info = getJarInfo(RunTrackMateOnProject.class);
        if (info != null) {
            PaintLogger.infof("Compilation date: %s", info.implementationDate);
            PaintLogger.infof("Version: %s", info.implementationVersion);
        } else {
            PaintLogger.errorf("No manifest information found.");
            PaintLogger.blankline();
        }

        // Step 4: Open the Project Specification dialog for experiment selection
        ProjectSpecificationDialog dialog = new ProjectSpecificationDialog(
                null, projectPath, ProjectSpecificationDialog.DialogMode.TRACKMATE);

        // Ensure the console closes when this dialog closes
        PaintConsoleWindow.closeOnDialogDispose(dialog.getDialog());

        // Step 5: Define the callback when user clicks "OK" in the dialog
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
                boolean status = true;
                boolean debug = getBoolean("Debug", "RunTrackMateOnProject", false);

                // Get image root path from configuration
                String imagesRoot = getString("Paths", "Images Root", "validate");
                boolean error = false;
                LocalDateTime start = LocalDateTime.now();

                // Validate image root setting
                if (imagesRoot.equals("Fatal")) {
                    PaintLogger.errorf("No Image Path retrieved from configuration file - Fatal.");
                    error = true;
                }

                if (debug) PaintLogger.debugf("The Image Path is specified as '%s'.", imagesRoot);

                Path imagesPath = Paths.get(imagesRoot);
                if (!error && !imagesPath.toFile().exists()) {
                    PaintLogger.errorf("The Image Root '%s' does not exist - Fatal.", imagesPath);
                    error = true;
                }

                if (debug) PaintLogger.debugf("The Image Path '%s' exists.", imagesRoot);

                // Validate image root against project and experiment list
                ValidationResult validateResult = validateImageRoot(
                        projectPath, imagesPath, project.experimentNames);
                if (!validateResult.isValid()) {
                    for (String validateError : validateResult.getErrors()) {
                        PaintLogger.errorf(validateError);
                    }
                    error = true;
                }

                if (debug) PaintLogger.debugf("The Image Path '%s' passed the validation test.", imagesRoot);

                // Validate project root existence
                if (!error && !projectPath.toFile().exists()) {
                    PaintLogger.errorf("Project Root '%s' does not exist - Fatal.", projectPath);
                    error = true;
                }

                if (debug) PaintLogger.debugf("The project root '%s' exists.", projectPath);

                // Abort if critical errors were found
                if (error) {
                    JOptionPane optionPane = new JOptionPane(
                            "TrackMate will not run when not all image files are present. Deselect the offending experiment.",
                            JOptionPane.ERROR_MESSAGE
                    );
                    JDialog errorDialog = optionPane.createDialog(null, "Aborting operation");
                    errorDialog.setAlwaysOnTop(true);
                    errorDialog.setVisible(true);
                    return false;
                }

                // Validate Experiment Info files for all selected experiments
                validateResult = validateExperiments(projectPath, project.experimentNames, EXPERIMENT_INFO_CSV);
                if (!validateResult.isValid()) {
                    for (String validateError : validateResult.getErrors()) {
                        PaintLogger.errorf(validateError);
                    }
                    JOptionPane optionPane = new JOptionPane(
                            "There are problems with Experiment Info files.",
                            JOptionPane.ERROR_MESSAGE
                    );
                    JDialog errorDialog = optionPane.createDialog(null, "Aborting operation");
                    errorDialog.setAlwaysOnTop(true);
                    errorDialog.setVisible(true);
                    return false;
                }

                if (debug) {
                    PaintLogger.debugf("TrackMate processing started.");
                    PaintLogger.debugf("Experiments %s", project.experimentNames.toString());
                }

                // Step 6: Run TrackMate for each experiment
                for (String experimentName : project.experimentNames) {
                    if (dialog.isCancelled()) {
                        PaintLogger.warningf("Processing aborted by user.");
                        break;
                    }

                    Path experimentPath = projectPath.resolve(experimentName);
                    if (!Files.isDirectory(experimentPath)) {
                        PaintLogger.errorf("Experiment directory '%s' does not exist.", experimentPath);
                    } else {
                        try {
                            status = runTrackMateOnExperiment(
                                    experimentPath, imagesPath.resolve(experimentName), dialog);
                        } catch (Exception e) {
                            // Log recoverable exceptions
                            PaintLogger.errorf("Error during TrackMate run for '%s': %s", experimentName, e.getMessage());
                            StringWriter sw = new StringWriter();
                            e.printStackTrace(new PrintWriter(sw));
                            PaintLogger.errorf("An exception occurred:\n" + sw);
                        } catch (Throwable t) {
                            // Log severe/unexpected errors
                            PaintLogger.errorf("Severe error during TrackMate run for '%s': %s", experimentName, t.getMessage());
                            StringWriter sw = new StringWriter();
                            t.printStackTrace(new PrintWriter(sw));
                            PaintLogger.errorf("An exception occurred:\n" + sw);
                        }
                    }
                }

                // Step 7: Handle user cancellation gracefully
                if (dialog.isCancelled()) {
                    PaintLogger.infof("Run was cancelled by the user.");
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(dialog.getDialog(),
                                "Processing was cancelled.\nThe run stopped after the current recording finished.",
                                "Processing Cancelled",
                                JOptionPane.INFORMATION_MESSAGE);
                    });
                    return false;
                }

                // Step 8: Log overall runtime statistics
                Duration totalDuration = Duration.between(start, LocalDateTime.now());
                int durationInSeconds = (int) (totalDuration.toMillis() / 1000);
                PaintLogger.blankline();
                PaintLogger.infof("Processed %d experiments in %s.",
                        project.experimentNames.size(),
                        formatDuration(durationInSeconds));

                return status;
            } finally {
                // Reset running flag and re-enable dialog
                running = false;
                SwingUtilities.invokeLater(() -> dialog.setOkEnabled(true));
            }
        });

        // Step 9: Show the dialog (blocking call)
        dialog.showDialog();
    }
}