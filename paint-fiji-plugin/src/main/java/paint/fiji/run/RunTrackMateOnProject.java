package paint.fiji.run;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectSelectionDialog;
import paint.shared.dialogs.ProjectSpecificationDialog;
import paint.shared.utils.JarInfo;
import paint.shared.utils.PaintLogger;
import paint.shared.validate.ValidationResult;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystemNotFoundException;
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
 * SciJava/Fiji command plugin that runs the TrackMate pipeline on
 * selected experiments in a selected project.
 */
@Plugin(type = Command.class, menuPath = "Plugins>Glyco-PAINT>Run TrackMate on Project")
public class RunTrackMateOnProject implements Command {

    private static volatile boolean running = false;

    @Override
    public void run() {
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

        // Ask user to select a project directory
        ProjectSelectionDialog projDlg = new ProjectSelectionDialog(null);
        Path projectPath = projDlg.showDialog();

        if (projectPath == null) {
            PaintLogger.infof("User cancelled project selection.");
            return;
        }

        // Initialise configuration + logging
        PaintConfig.initialise(projectPath);
        PaintLogger.initialise(projectPath, "TrackMateOnProject");
        PaintLogger.debugf("TrackMate plugin started");

        // Log JAR metadata
        JarInfo info = getJarInfo(RunTrackMateOnProject.class);
        if (info != null) {
            PaintLogger.infof("Compilation date: %s", info.implementationDate);
            PaintLogger.infof("Version: %s", info.implementationVersion);
        } else {
            PaintLogger.errorf("No manifest information found.");
            PaintLogger.infof();
        }

        ProjectSpecificationDialog dialog = new ProjectSpecificationDialog(
                null, projectPath, ProjectSpecificationDialog.DialogMode.TRACKMATE);

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

                String imagesRoot = getString("Paths", "Images Root", "validate");
                boolean error = false;
                LocalDateTime start = LocalDateTime.now();

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

                ValidationResult validateResult = validateImageRoot(
                        projectPath, imagesPath, project.experimentNames);
                if (!validateResult.isValid()) {
                    for (String validateError : validateResult.getErrors()) {
                        PaintLogger.errorf(validateError);
                    }
                    error = true;
                }

                if (debug) PaintLogger.debugf("The Image Path '%s' passed the validation test.", imagesRoot);

                if (!error && !projectPath.toFile().exists()) {
                    PaintLogger.errorf("Project Root '%s' does not exist - Fatal.", projectPath);
                    error = true;
                }

                if (debug) PaintLogger.debugf("The project root '%s' exists.", projectPath);

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

                for (String experimentName : project.experimentNames) {
                    System.out.println("Test if we should proceed");
                    System.out.println(dialog.isCancelled());
                    if (dialog.isCancelled()) {
                        PaintLogger.warningf("Processing aborted by user request.");
                        break; // stop before next experiment
                    }

                    Path experimentPath = projectPath.resolve(experimentName);
                    if (!Files.isDirectory(experimentPath)) {
                        PaintLogger.errorf("Experiment directory '%s' does not exist.", experimentPath);
                    } else {
                        try {
                            status = runTrackMateOnExperiment(
                                    experimentPath, imagesPath.resolve(experimentName));
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

                if (dialog.isCancelled()) {
                    PaintLogger.infof("Run was cancelled by the user.");
                    return false;
                }

                Duration totalDuration = Duration.between(start, LocalDateTime.now());
                int durationInSeconds = (int) (totalDuration.toMillis() / 1000);
                PaintLogger.infof();
                PaintLogger.infof("Processed %d experiments in %s.",
                        project.experimentNames.size(),
                        formatDuration(durationInSeconds));

                return status;
            } finally {
                running = false;
                SwingUtilities.invokeLater(() -> dialog.setOkEnabled(true));
            }
        });

        dialog.showDialog();
    }
}