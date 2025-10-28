/******************************************************************************
 *  Class:        RunTrackMateOnProject.java
 *  Package:      paint.fiji.trackmate
 *
 *  PURPOSE:
 *    Provides orchestration for running the TrackMate analysis pipeline
 *    across multiple experiments within a project directory structure.
 *    Handles input validation, error reporting, user cancellation, and
 *    duration logging. Extends the functionality provided by
 *    {@link RunTrackMateOnExperiment} to operate at a project scale.
 *
 *  DESCRIPTION:
 *    • Validates that all required image and experiment files exist.
 *    • Performs per-experiment execution of the TrackMate workflow.
 *    • Supports user interruption via {@link ProjectDialog}.
 *    • Reports overall runtime and summary status through {@link PaintLogger}.
 *
 *  RESPONSIBILITIES:
 *    • Coordinate experiment-level processing into a project-level workflow.
 *    • Perform validation of both images and CSV configuration files.
 *    • Manage progress logging, duration measurement, and error handling.
 *
 *  USAGE EXAMPLE:
 *    boolean ok = RunTrackMateOnProject.runProject(
 *                     projectPath,
 *                     imagesPath,
 *                     experimentNames,
 *                     dialog,
 *                     sweepDir);
 *
 *  DEPENDENCIES:
 *    – paint.fiji.trackmate.RunTrackMateOnExperiment
 *    – paint.shared.dialogs.ProjectDialog
 *    – paint.shared.utils.PaintLogger
 *    – paint.shared.validate.ImageRootValidator
 *    – paint.shared.validate.ValidationHandler
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.fiji.trackmate;

import paint.shared.dialogs.ProjectDialog;
import paint.shared.utils.PaintLogger;
import paint.shared.validate.ValidationResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static paint.shared.constants.PaintConstants.EXPERIMENT_INFO_CSV;
import static paint.shared.utils.Miscellaneous.formatDuration;
import static paint.shared.validate.ImageRootValidator.validateImageRoot;
import static paint.shared.validate.ValidationHandler.validateExperiments;

/**
 * The {@code RunTrackMateOnProject} class orchestrates the TrackMate analysis
 * across multiple experiments contained within a project directory.
 * <p>
 * It validates image directories and configuration files before processing,
 * handles per-experiment execution, supports user-initiated cancellation, and
 * logs all key events and outcomes.
 * </p>
 * <p>
 * Each experiment is validated and processed independently, ensuring that a
 * failure in one experiment does not halt others. At completion, a total
 * runtime summary is logged.
 * </p>
 */
public class RunTrackMateOnProject extends RunTrackMateOnExperiment {

    /**
     * Executes the TrackMate analysis pipeline for all specified experiments
     * within the given project directory. Performs validation on both image
     * roots and CSV metadata, then runs the core processing for each experiment.
     *
     * @param projectPath     the root directory of the Paint project containing experiment subfolders
     * @param imagesPath      the directory containing image files for the experiments
     * @param experimentNames list of experiment folder names to be processed
     * @param dialog          optional {@link ProjectDialog} used for user-driven cancellation
     * @param sweepDir        optional override directory for writing analysis results
     *                        (defaults to {@code projectPath} if {@code null})
     * @return {@code true} if all experiments processed successfully;
     *         {@code false} if any validation or runtime errors occurred
     */
    public static boolean runProject(Path projectPath,
                                     Path imagesPath,
                                     List<String> experimentNames,
                                     ProjectDialog dialog,
                                     Path sweepDir) {

        boolean status = true;                               // Overall success flag
        LocalDateTime start = LocalDateTime.now();           // Timestamp for runtime measurement

        // ---------------------------------------------------------------------
        // Phase 1 – Validate that required images exist
        // ---------------------------------------------------------------------
        ValidationResult validateResult = validateImageRoot(projectPath, imagesPath, experimentNames);
        if (!validateResult.isValid()) {
            for (String err : validateResult.getErrors()) {
                PaintLogger.errorf(err);
            }
            return false;                                     // Abort on image validation failure
        }

        // ---------------------------------------------------------------------
        // Phase 2 – Validate experiment configuration (Experiment Info.csv)
        // ---------------------------------------------------------------------
        validateResult = validateExperiments(projectPath, experimentNames, EXPERIMENT_INFO_CSV);
        if (!validateResult.isValid()) {
            for (String err : validateResult.getErrors()) {
                PaintLogger.errorf(err);
            }
            return false;                                   // Abort on configuration validation failure
        }

        // ---------------------------------------------------------------------
        // Phase 3 – Execute TrackMate per experiment
        // ---------------------------------------------------------------------
        for (String experimentName : experimentNames) {
            // Allow user to cancel processing mid-run
            if (dialog != null && dialog.isCancelled()) {
                PaintLogger.warnf("Processing aborted by user.");
                break;
            }

            // Determine target experiment path (sweepDir override if given)
            Path experimentPath = (sweepDir != null)
                    ? sweepDir.resolve(experimentName)
                    : projectPath.resolve(experimentName);

            // Verify the directory actually exists
            if (!Files.isDirectory(experimentPath)) {
                PaintLogger.errorf("Experiment directory '%s' does not exist.", experimentPath);
                continue;
            }

            // Run the TrackMate workflow for this experiment
            try {
                boolean ok = runTrackMateOnExperiment(
                        experimentPath,
                        imagesPath.resolve(experimentName),
                        dialog);

                if (!ok) {
                    status = false;                         // Mark failure if sub-run failed
                }

            } catch (Exception e) {
                PaintLogger.errorf("Error during TrackMate run for '%s': %s",
                                   experimentName, e.getMessage());
                status = false;
            }
        }

        // ---------------------------------------------------------------------
        // Phase 4 – Report summary and duration
        // ---------------------------------------------------------------------
        Duration totalDuration = Duration.between(start, LocalDateTime.now());
        PaintLogger.infof("Processed %d experiments in %s.",
                          experimentNames.size(),
                          formatDuration((int) (totalDuration.toMillis() / 1000)));

        return status;
    }
}