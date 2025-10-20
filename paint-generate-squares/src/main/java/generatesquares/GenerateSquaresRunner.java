package generatesquares;

import paint.shared.config.PaintConfig;
import paint.shared.config.GenerateSquaresConfig;
import paint.shared.objects.Experiment;
import paint.shared.objects.Project;
import paint.shared.utils.HistogramPdfExporter;
import paint.shared.utils.PaintLogger;
import paint.shared.validate.ValidationResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static generatesquares.calc.GenerateSquareCalcs.generateSquaresForExperiment;
import static paint.shared.constants.PaintConstants.*;
import static paint.shared.io.ProjectDataLoader.loadExperiment;
import static paint.shared.utils.CsvConcatenator.concatenateNamedCsvFiles;
import static paint.shared.utils.Miscellaneous.formatDuration;
import static paint.shared.validate.ValidationHandler.validateExperiments;

/**
 * Provides a programmatic, non-GUI entry point for the Generate Squares workflow.
 */
public class GenerateSquaresRunner {

    /**
     * Executes the full Generate Squares pipeline for the given project path and experiments.
     *
     * @param projectPath      root project directory
     * @param experimentNames  list of experiment subdirectories to process
     * @throws Exception if any part of the workflow fails
     */
    public static void run(Path projectPath, List<String> experimentNames) throws Exception {
        // --- Initialize configuration and logging ---
        PaintConfig.initialise(projectPath);
        PaintLogger.initialise(projectPath, "Generate Squares.log");

        String debugLevel = PaintConfig.getString("Paint", "Log Level", "INFO");
        PaintLogger.setLevel(debugLevel);

        PaintLogger.infof("Starting Generate Squares (headless mode)");
        PaintLogger.infof("Project: %s", projectPath);
        PaintLogger.infof("Experiments: %s", experimentNames);

        // --- Validate input data ---
        ValidationResult validateResult = validateExperiments(
                projectPath,
                experimentNames,
                java.util.Arrays.asList(EXPERIMENT_INFO_CSV, RECORDING_CSV, TRACK_CSV)
        );

        if (!validateResult.isValid()) {
            for (String line : validateResult.getReport().split("\n")) {
                PaintLogger.errorf(line);
            }
            throw new IllegalStateException("Experiment validation failed.");
        }

        LocalDateTime start = LocalDateTime.now();

        // --- Construct the Project object expected by GenerateSquareCalcs ---
        GenerateSquaresConfig generateSquaresConfig = GenerateSquaresConfig.from(PaintConfig.instance());

        // @formatter:off
        Project project               = new Project();
        project.projectRootPath       = projectPath;
        project.experimentNames       = experimentNames;
        project.generateSquaresConfig = generateSquaresConfig;
        // @formatter:on

        // --- Run squares calculation for each experiment ---
        for (String experimentName : experimentNames) {
            PaintLogger.infof("Running Generate Squares for experiment: %s", experimentName);

            generateSquaresForExperiment(project, experimentName);

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
        try {
            PaintLogger.infof("Creating project-level summary files...");

            concatenateNamedCsvFiles(projectPath, SQUARE_CSV, experimentNames);
            concatenateNamedCsvFiles(projectPath, RECORDING_CSV, experimentNames);
            concatenateNamedCsvFiles(projectPath, EXPERIMENT_INFO_CSV, experimentNames);
            concatenateNamedCsvFiles(projectPath, TRACK_CSV, experimentNames);

            Duration duration = Duration.between(start, LocalDateTime.now());
            PaintLogger.infof("Finished Generate Squares for all experiments in %s", formatDuration(duration));
        } catch (Exception e) {
            PaintLogger.errorf("Failed to concatenate CSVs: %s", e.getMessage());
        }

        PaintLogger.infof("Generate Squares finished successfully.");
    }
}