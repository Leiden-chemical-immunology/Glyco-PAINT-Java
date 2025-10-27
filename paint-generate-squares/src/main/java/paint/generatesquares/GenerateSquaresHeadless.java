/******************************************************************************
 *  Class:        GenerateSquaresHeadless.java
 *  Package:      generatesquares
 *
 *  PURPOSE:
 *    Provides a headless (non-GUI) execution mode for the “Generate Squares”
 *    pipeline. Performs experiment validation, per-experiment computation,
 *    histogram export, and project-level CSV consolidation.
 *
 *  DESCRIPTION:
 *    This class is responsible for orchestrating the core “Generate Squares”
 *    logic without any user interface. It loads configuration parameters,
 *    validates experiments, delegates computation to
 *    {@link paint.generatesquares.calc.GenerateSquaresProcessor}, and exports results.
 *
 *  RESPONSIBILITIES:
 *    • Validate experiment input files before computation
 *    • Execute square-based calculations for each experiment
 *    • Export per-experiment histogram PDFs
 *    • Concatenate experiment-level CSVs into project summaries
 *
 *  USAGE EXAMPLE:
 *    GenerateSquaresHeadless.run(projectPath, Arrays.asList("Exp01", "Exp02"));
 *
 *  DEPENDENCIES:
 *    - paint.shared.config.{PaintConfig, GenerateSquaresConfig}
 *    - paint.shared.objects.{Project, Experiment}
 *    - paint.shared.utils.{PaintLogger, HistogramPdfExporter}
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
 ******************************************************************************/

package paint.generatesquares;

import paint.shared.config.PaintConfig;
import paint.shared.config.GenerateSquaresConfig;
import paint.shared.objects.Experiment;
import paint.shared.objects.Project;
import paint.generatesquares.calc.HistogramPdfExporter;
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
import static paint.shared.constants.PaintConstants.*;
import static paint.shared.io.ExperimentDataLoader.loadExperiment;
import static paint.shared.utils.CsvUtils.concatenateNamedCsvFiles;
import static paint.shared.utils.Miscellaneous.formatDuration;
import static paint.shared.validate.ValidationHandler.validateExperiments;

/**
 * Runs the Generate Squares pipeline headlessly:
 * validates experiments, performs calculations, exports results,
 * and builds project-level summary CSVs.
 */
public class GenerateSquaresHeadless {

    public static void run(Path projectPath, List<String> experimentNames) throws Exception {

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

        logContextAndConfiguration(projectPath, experimentNames);
        LocalDateTime start = LocalDateTime.now();

        // --- Prepare project ---
        GenerateSquaresConfig generateSquaresConfig = new GenerateSquaresConfig();
        Project project = new Project();
        project.setProjectRootPath(projectPath);
        project.setExperimentNames(experimentNames);
        project.setGenerateSquaresConfig(generateSquaresConfig);

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

            if (Thread.currentThread().isInterrupted()) {
                PaintLogger.infof("Cancelled before exporting histograms for %s", experimentName);
                return;
            }

            try {
                Experiment experiment = loadExperiment(projectPath, experimentName, true);
                Path pdfOut = projectPath
                        .resolve(experimentName)
                        .resolve("Output")
                        .resolve("Background.pdf");

                Files.createDirectories(pdfOut.getParent());
                HistogramPdfExporter.exportExperimentHistogramsToPdf(experiment, pdfOut);

            } catch (Exception e) {
                PaintLogger.errorf("Failed to export histograms for %s: %s", experimentName, e.getMessage());
            }
        }

        // --- Concatenate results ---
        if (Thread.currentThread().isInterrupted()) {
            PaintLogger.infof("Cancelled before concatenating project-level CSVs.");
            return;
        }

        try {
            PaintLogger.infof("Creating project-level summary files...");

            PaintLogger.infof("   Creating %s", projectPath.resolve(SQUARES_CSV));
            concatenateNamedCsvFiles(projectPath, SQUARES_CSV, experimentNames);
            if (Thread.currentThread().isInterrupted()) return;

            PaintLogger.infof("   Creating %s", projectPath.resolve(RECORDINGS_CSV));
            concatenateNamedCsvFiles(projectPath, RECORDINGS_CSV, experimentNames);
            if (Thread.currentThread().isInterrupted()) return;

            PaintLogger.infof("   Creating %s", projectPath.resolve(EXPERIMENT_INFO_CSV));
            concatenateNamedCsvFiles(projectPath, EXPERIMENT_INFO_CSV, experimentNames);
            if (Thread.currentThread().isInterrupted()) return;

            PaintLogger.infof("   Creating %s", projectPath.resolve(TRACKS_CSV));
            concatenateNamedCsvFiles(projectPath, TRACKS_CSV, experimentNames);

            PaintLogger.blankline();

            Duration duration = Duration.between(start, LocalDateTime.now());
            PaintLogger.infof("Finished Generate Squares for all experiments in %s", formatDuration(duration));

        } catch (Exception e) {
            PaintLogger.errorf("Failed to concatenate CSVs: %s", e.getMessage());
        }
    }

    private static void logContextAndConfiguration(Path projectPath, List<String> experimentNames) {

        int nSquares      = PaintConfig.getInt("Generate Squares", "Number of Squares in Recording", 400);
        int side          = (int) Math.round(Math.sqrt(nSquares));
        int minTracks     = PaintConfig.getInt("Generate Squares", "Min Tracks to Calculate Tau", 20);
        double minRSq     = PaintConfig.getDouble("Generate Squares", "Min Required R Squared", 0.1);
        double minDensity = PaintConfig.getDouble("Generate Squares", "Min Required Density Ratio", 2.0);
        double maxVar     = PaintConfig.getDouble("Generate Squares", "Max Allowable Variability", 10.0);

        // Neatly wrapped experiment list
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

        PaintLogger.doc("Generate Squares", Arrays.asList(
                "Starting Generate Squares analysis for project: " + projectPath.getFileName(),
                "",
                "Selected experiments:",
                formattedExperiments,
                "",
                "Using parameters:",
                String.format(Locale.getDefault(), "  • Grid size:                 %dx%d (%d squares)", side, side, nSquares),
                String.format(Locale.getDefault(), "  • Minimum tracks per square: %d", minTracks),
                String.format(Locale.getDefault(), "  • Minimum R²:                %.2f", minRSq),
                String.format(Locale.getDefault(), "  • Minimum density ratio:     %.1f", minDensity),
                String.format(Locale.getDefault(), "  • Maximum variability:       %.1f", maxVar),
                "",
                "Each recording will be divided into spatial squares, and per-square track statistics will be calculated.",
                "Results will be prepared per experiment and squares and tracks files updated",
                "",
                "The results will then be compiled into project level files:",
                String.format("  • %s", SQUARES_CSV),
                String.format("  • %s (with updated Square Number and Label Number fields)", TRACKS_CSV),
                String.format("  • %s", RECORDINGS_CSV),
                String.format("  • %s", EXPERIMENT_INFO_CSV)
        ));
    }
}