/*=============================================================================
 *  Class:        GenerateSquaresHeadless.java
 *  Package:      generatesquares
 *
 *  PURPOSE:
 *    Provides a headless (non-GUI) execution mode for the “Generate Squares”
 *    pipeline. Performs experiment validation, per-experiment computation,
 *    histogram export, and project-level CSV consolidation.
 *
 *  DESCRIPTION:
 *    This class orchestrates the core “Generate Squares” logic without any
 *    user interface. It loads configuration parameters, validates experiments,
 *    delegates computation to
 *    {@link paint.generatesquares.calc.GenerateSquaresProcessor}, and exports
 *    all resulting files.
 *
 *  RESPONSIBILITIES:
 *    • Validate experiment input files prior to computation
 *    • Execute square-based calculations for each experiment
 *    • Export per-experiment histogram PNGs
 *    • Concatenate experiment-level CSVs into project-level summaries
 *
 *  USAGE EXAMPLE:
 *    GenerateSquaresHeadless.run(projectPath, Arrays.asList("Exp01", "Exp02"));
 *
 *  DEPENDENCIES:
 *    - paint.shared.config.{PaintConfig, GenerateSquaresConfig}
 *    - paint.shared.objects.{Project, Experiment}
 *    - paint.shared.utils.{PaintLogger}
 *    - paint.shared.validate.ValidationHandler
 *    - generatesquares.calc.GenerateSquaresProcessor
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-23
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
=============================================================================*/

package paint.generatesquares.app;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.config.paintconfig.PaintConfig;
import paint.shared.objects.Experiment;
import paint.shared.objects.Project;
import paint.shared.utils.PaintLogger;
import paint.shared.validate.ValidationResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static paint.generatesquares.calc.GenerateSquaresProcessor.generateSquaresForExperiment;
import static paint.generatesquares.calc.PlotUtils.exportBackgroundHistogramsToPngs;

import static paint.shared.io.ExperimentDataLoader.loadExperiment;
import static paint.shared.utils.CsvUtils.concatenateNamedCsvFiles;
import static paint.shared.utils.Miscellaneous.formatDuration;
import static paint.shared.validate.ValidationHandler.validateExperiments;

import static paint.shared.constants.PaintFileNames.EXPERIMENT_INFO_CSV;
import static paint.shared.constants.PaintFileNames.RECORDINGS_CSV;
import static paint.shared.constants.PaintFileNames.TRACKS_CSV;
import static paint.shared.constants.PaintFileNames.SQUARES_CSV;

import static paint.shared.constants.PaintColumnNames.*;

/**
 * Executes the Generate Squares pipeline in headless mode. This includes
 * validating experiments, running per-experiment calculations, generating
 * histogram plots, and producing project-level summary CSVs.
 */
public class GenerateSquaresHeadless {

    /**
     * Runs the Generate Squares workflow for the specified project and list of
     * experiments. The process includes validation, computation, histogram
     * export, and final CSV aggregation.
     *
     * @param projectPath     the root path of the project
     * @param experimentNames the experiments to process
     */
    public static void run(Path projectPath, List<String> experimentNames)  {

        // --- Early abort check ---
        if (Thread.currentThread().isInterrupted()) {
            PaintLogger.infof("Generate Squares run aborted before start (user cancelled).");
            return;
        }

        // --- Validate input data ---
        PaintLogger.infof("Validating input data...");
        ValidationResult validateResult = validateExperiments(
                projectPath,
                experimentNames,
                Arrays.asList(EXPERIMENT_INFO_CSV, RECORDINGS_CSV, TRACKS_CSV)
        );
        if (!validateResult.isValid()) {
            for (String line : validateResult.getReport().split("\n")) {
                PaintLogger.errorf(line);
            }
            throw new IllegalStateException("Experiment validation failed.");
        }

        // --- Log context and configuration. show what we are going to do ---
        logContextAndConfiguration(projectPath, experimentNames);
        LocalDateTime start = LocalDateTime.now();

        // --- Prepare project container ---
        Project project = new Project();
        project.setProjectRootPath(projectPath);
        project.setExperimentNames(experimentNames);
        project.setGenerateSquaresConfig(new GenerateSquaresConfig());    // This reads the config data using PaintConfig

        // -- See if we should generate background plots ---
        boolean showPlots = PaintConfig.getBoolean(GENERATE_SQUARES, BACKGROUND_PLOTS, false);

        // --- Run each experiment ---
        for (String experimentName : experimentNames) {

            if (Thread.currentThread().isInterrupted()) {
                PaintLogger.infof("Generate Squares run stopped early (user cancelled).");
                return;
            }

            PaintLogger.infof("Running Generate Squares for experiment: %s", experimentName);

            try {
                generateSquaresForExperiment(project, experimentName);
            } catch (Exception e) {
                PaintLogger.errorf("Error processing experiment %s: %s", experimentName, e.getMessage());
                continue;
            }

            // --- Generate background histogram plots, if enabled ---
            if (showPlots) {
                try {
                    Experiment experiment = loadExperiment(
                            projectPath,
                            experimentName,
                            true,   // Load Squares
                            false   // Skip Tracks
                    );

                    Path experimentPath = projectPath.resolve(experimentName);
                    Path outPath        = experimentPath.resolve("Output");
                    Files.createDirectories(outPath);

                    exportBackgroundHistogramsToPngs(
                            experiment,
                            experimentPath);

                } catch (Exception e) {
                    PaintLogger.errorf("Failed to export histograms for %s: %s",
                                       experimentName, e.getMessage());
                }
            }
        }

        if (Thread.currentThread().isInterrupted()) {
            PaintLogger.infof("Cancelled before concatenating project-level CSVs.");
            return;
        }

        // --- Concatenate project-level CSVs ---
        try {
            PaintLogger.infof("Creating project-level summary files...");

            PaintLogger.infof("   Creating %s", projectPath.resolve(SQUARES_CSV));
            concatenateNamedCsvFiles(projectPath, SQUARES_CSV, experimentNames);

            PaintLogger.infof("   Creating %s", projectPath.resolve(RECORDINGS_CSV));
            concatenateNamedCsvFiles(projectPath, RECORDINGS_CSV, experimentNames);

            PaintLogger.infof("   Creating %s", projectPath.resolve(EXPERIMENT_INFO_CSV));
            concatenateNamedCsvFiles(projectPath, EXPERIMENT_INFO_CSV, experimentNames);

            PaintLogger.infof("   Creating %s", projectPath.resolve(TRACKS_CSV));
            concatenateNamedCsvFiles(projectPath, TRACKS_CSV, experimentNames);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to concatenate CSVs: %s", e.getMessage());
        }

        PaintLogger.blankline();

        Duration duration = Duration.between(start, LocalDateTime.now());
        PaintLogger.infof("Finished Generate Squares for all experiments in %s",
                          formatDuration(duration));
    }

    /**
     * Logs project and configuration context before execution begins, including
     * selected experiments and all relevant parameter settings.
     */
    private static void logContextAndConfiguration(Path projectPath, List<String> experimentNames) {

        int nSquares      = PaintConfig.getInt(   GENERATE_SQUARES, NUMBER_OF_SQUARES_IN_RECORDING, 400);
        int minTracks     = PaintConfig.getInt(   GENERATE_SQUARES, MIN_TRACKS_TO_CALCULATE_TAU,    20);
        double minRSq     = PaintConfig.getDouble(GENERATE_SQUARES, MIN_REQUIRED_R_SQUARED,         0.1);
        double minDensity = PaintConfig.getDouble(GENERATE_SQUARES, MIN_REQUIRED_DENSITY_RATIO,     2.0);
        double maxVar     = PaintConfig.getDouble(GENERATE_SQUARES, MAX_ALLOWABLE_VARIABILITY,      10.0);
        int side          = (int) Math.round(Math.sqrt(nSquares));

        String formattedExperiments;
        if (experimentNames.isEmpty()) {
            formattedExperiments = "                   (none selected — please verify selection)";
        } else {
            final int MAX_WIDTH = 100;
            final String INDENT = "                   ";

            StringBuilder sb = new StringBuilder("  "); // the first line has only 2 leading spaces
            int currentLineLength = 2;
            int effectiveMaxWidthFirstLine = MAX_WIDTH - (INDENT.length() - 2);

            for (int i = 0; i < experimentNames.size(); i++) {
                String exp = experimentNames.get(i);
                int tokenLength = exp.length() + (i < experimentNames.size() - 1 ? 2 : 0);

                // use narrower limit for first line, normal for later lines
                int limit = (sb.indexOf("\n") == -1) ? effectiveMaxWidthFirstLine : MAX_WIDTH;

                if (currentLineLength + tokenLength > limit) {
                    sb.append("\n").append(INDENT);
                    currentLineLength = INDENT.length();
                }

                sb.append(exp);
                currentLineLength += exp.length();

                if (i < experimentNames.size() - 1) {
                    sb.append(", ");
                    currentLineLength += 2;
                }
            }

            formattedExperiments = sb.toString();
        }

        PaintLogger.doc(GENERATE_SQUARES, Arrays.asList(
                "Starting Generate Squares analysis for project: " + projectPath.getFileName(),
                "",
                "Selected experiments:",
                formattedExperiments,
                "",
                "Using parameters:",
                String.format(Locale.getDefault(), "  • Grid size:                 %dx%d (%d squares)", side, side, nSquares),
                String.format(Locale.getDefault(), "  • Minimum tracks per square: %d",                 minTracks),
                String.format(Locale.getDefault(), "  • Minimum R²:                %.2f",               minRSq),
                String.format(Locale.getDefault(), "  • Minimum density ratio:     %.1f",               minDensity),
                String.format(Locale.getDefault(), "  • Maximum variability:       %.1f",               maxVar),
                "",
                "Each recording will be divided into spatial squares, and per-square track statistics will be calculated.",
                "Results will be prepared per experiment, and squares and tracks files updated.",
                "",
                "The results will then be compiled into project-level files:",
                String.format("  • %s", SQUARES_CSV),
                String.format("  • %s (with updated Square Number and Label Number fields)", TRACKS_CSV),
                String.format("  • %s", RECORDINGS_CSV),
                String.format("  • %s", EXPERIMENT_INFO_CSV)
        ));
    }
}