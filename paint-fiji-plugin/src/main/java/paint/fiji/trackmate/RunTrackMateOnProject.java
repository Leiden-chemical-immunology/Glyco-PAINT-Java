package paint.fiji.trackmate;

import paint.shared.dialogs.ProjectDialog;
import paint.shared.utils.PaintLogger;
import paint.shared.validate.ValidationResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static paint.fiji.trackmate.RunTrackMateOnExperiment.runTrackMateOnExperiment;
import static paint.shared.constants.PaintConstants.EXPERIMENT_INFO_CSV;
import static paint.shared.utils.Miscellaneous.formatDuration;
import static paint.shared.validate.ImageRootValidator.validateImageRoot;
import static paint.shared.validate.ValidationHandler.validateExperiments;

/**
 * Core TrackMate project runner, reusable by plugin and sweep runner.
 */
public class RunTrackMateOnProject {

    public static boolean runProject(Path projectPath,
                                     Path imagesPath,
                                     List<String> experimentNames,
                                     ProjectDialog dialog,
                                     Path sweepDir) {

        boolean status = true;
        LocalDateTime start = LocalDateTime.now();

        // Validate image root
        ValidationResult validateResult = validateImageRoot(projectPath, imagesPath, experimentNames);
        if (!validateResult.isValid()) {
            for (String err : validateResult.getErrors()) {
                PaintLogger.errorf(err);
            }
            return false;
        }

        // Validate experiment info files
        validateResult = validateExperiments(projectPath, experimentNames, EXPERIMENT_INFO_CSV);
        if (!validateResult.isValid()) {
            for (String err : validateResult.getErrors()) {
                PaintLogger.errorf(err);
            }
            return false;
        }

        // Process each experiment
        for (String experimentName : experimentNames) {
            if (dialog != null && dialog.isCancelled()) {
                PaintLogger.warnf("Processing aborted by user.");
                break;
            }

            Path experimentPath = (sweepDir != null)
                    ? sweepDir.resolve(experimentName)
                    : projectPath.resolve(experimentName);

            if (!Files.isDirectory(projectPath.resolve(experimentName))) {
                PaintLogger.errorf("Experiment directory '%s' does not exist.", experimentPath);
                continue;
            }

            try {
                boolean ok = runTrackMateOnExperiment(
                        experimentPath, imagesPath.resolve(experimentName), dialog);
                if (!ok) {
                    status = false;
                }
            } catch (Exception e) {
                PaintLogger.errorf("Error during TrackMate run for '%s': %s", experimentName, e.getMessage());
                status = false;
            }
        }

        // Report
        Duration totalDuration = Duration.between(start, LocalDateTime.now());
        PaintLogger.infof("Processed %d experiments in %s.",
                          experimentNames.size(), formatDuration((int) (totalDuration.toMillis() / 1000)));
        return status;
    }
}