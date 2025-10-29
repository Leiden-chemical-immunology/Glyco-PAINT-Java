/******************************************************************************
 *  Class:        ValidationHandler.java
 *  Package:      paint.shared.validate
 *
 *  PURPOSE:
 *    Performs batch validation of core PAINT experiment CSV files across one
 *    or more experiments within a project directory. Each experiment’s
 *    files are validated for schema, type, and consistency integrity.
 *
 *  DESCRIPTION:
 *    • Iterates over a list of experiment names and validates selected CSV files.
 *    • Routes validation to specific validator classes depending on file type
 *      (Experiment Info, Recordings, Squares, or Tracks).
 *    • Aggregates all validation messages into a unified report.
 *    • Outputs concise problem lines in the format:
 *         [ExperimentName] - [FileName] - [Problem summary]
 *    • Provides verbose logging output when {@link PaintRuntime#isVerbose()} is enabled.
 *
 *  RESPONSIBILITIES:
 *    • Coordinate multi-file validation across experiments.
 *    • Integrate individual validators into a unified workflow.
 *    • Provide human-readable reporting and console logging.
 *
 *  USAGE EXAMPLE:
 *    List<String> experiments = Arrays.asList("221012", "221108");
 *    List<String> files = Arrays.asList(EXPERIMENT_INFO_CSV, RECORDINGS_CSV);
 *    ValidationHandler.validate(projectPath, experiments, files);
 *
 *  DEPENDENCIES:
 *    – paint.shared.validate.{ExperimentInfoValidator, RecordingsValidator,
 *      SquaresValidator, TracksValidator, ValidationResult}
 *    – paint.shared.utils.{PaintLogger, PaintRuntime}
 *    – paint.shared.constants.PaintConstants
 *    – java.nio.file, java.util
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.shared.validate;

import paint.shared.utils.PaintLogger;
import paint.shared.utils.PaintRuntime;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static paint.shared.constants.PaintConstants.*;

/**
 * Central coordinator for validating multiple experiment CSV files within
 * a PAINT project. Executes specific validators per file type and aggregates
 * results into a combined {@link ValidationResult}.
 */
public final class ValidationHandler {

    // ───────────────────────────────────────────────────────────────────────────────
    // MAIN VALIDATION ENTRY POINTS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Validates a single file type across multiple experiments.
     *
     * @param projectPath     the base path of the PAINT project
     * @param experimentNames list of experiment directories to validate
     * @param fileName        the file name to validate (e.g., "Experiment Info.csv")
     * @return accumulated {@link ValidationResult} across all experiments
     */
    public static ValidationResult validateExperiments(Path projectPath,
                                                       List<String> experimentNames,
                                                       String fileName) {
        return validateExperiments(projectPath, experimentNames, Collections.singletonList(fileName));
    }

    /**
     * Validates one or more specified files across multiple experiments.
     *
     * @param projectPath     the base path of the PAINT project
     * @param experimentNames list of experiment directories to validate
     * @param fileNames       list of CSV file names to validate
     * @return {@link ValidationResult} with accumulated validation issues
     */
    public static ValidationResult validateExperiments(Path projectPath,
                                                       List<String> experimentNames,
                                                       List<String> fileNames) {

        List<String>     report    = new ArrayList<>();
        ValidationResult overall   = new ValidationResult();
        boolean          isVerbose = PaintRuntime.isVerbose();

        for (String expName : experimentNames) {
            Path expDir = projectPath.resolve(expName);

            if (!Files.isDirectory(expDir)) {
                String msg = "[" + expName + "] - Directory - Missing experiment directory: " + expDir;
                overall.addError(msg);
                report.add(msg);
                continue;
            }

            if (isVerbose) {
                PaintLogger.infof("   Validating experiment: %s", expName);
            }

            for (String fileName : fileNames) {
                Path filePath = expDir.resolve(fileName);

                if (!Files.exists(filePath)) {
                    String msg = "[" + expName + "] - " + fileName + " - Missing file";
                    overall.addError(msg);
                    report.add(msg);
                    continue;
                }

                ValidationResult res = runValidator(fileName, filePath.toFile(), expName);

                if (!res.isValid()) {
                    for (String err : res.getErrors()) {
                        String msg = formatMessage(expName, fileName, err);
                        overall.addError(msg);
                        report.add(msg);
                    }
                }
            }
        }

        if (isVerbose) {
            PaintLogger.blankline();
        }

        if (!report.isEmpty()) {
            overall.setReport(String.join("\n", report));
        }

        return overall;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // VALIDATOR ROUTING LOGIC
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Routes validation based on a file name pattern to the appropriate validator.
     *
     * @param fileName        the CSV file name
     * @param file            the {@link File} object to validate
     * @param experimentName  the experiment name for context
     * @return a {@link ValidationResult} containing file-specific validation output
     */
    private static ValidationResult runValidator(String fileName, File file, String experimentName) {
        String lower = fileName.toLowerCase();
        if (lower.contains("experiment")) {
            return new ExperimentInfoValidator().validateWithConsistency(file, experimentName);
        } else if (lower.contains("recording")) {
            return new RecordingsValidator().validateWithConsistency(file, experimentName);
        } else if (lower.contains("track")) {
            return new TracksValidator().validate(file);
        } else if (lower.contains("square")) {
            return new SquaresValidator().validate(file);
        } else {
            ValidationResult res = new ValidationResult();
            res.addError("Unknown file: " + fileName);
            return res;
        }
    }

    /**
     * Formats a validation message consistently for inclusion in the summary report.
     */
    private static String formatMessage(String expName, String fileName, String err) {
        String flattened = err.replace("\n", " ").replaceAll("\\s+", " ").trim();

        if (flattened.startsWith("[" + expName + "]")) {
            return "[" + expName + "] - " + fileName + " - "
                    + flattened.substring(flattened.indexOf("]") + 1).trim();
        }
        return "[" + expName + "] - " + fileName + " - " + flattened;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // CONSOLE WRAPPER
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Performs validation and logs results directly to the console.
     *
     * @param projectPath     project root directory
     * @param experimentNames experiment directories to validate
     * @param fileNames       CSV file names to validate
     */
    public static void validate(Path projectPath, List<String> experimentNames, List<String> fileNames) {

        PaintLogger.infof("Validating experiments: %s", experimentNames);
        PaintLogger.blankline();
        PaintLogger.infof("Validating files: %s", fileNames);
        PaintLogger.blankline();

        ValidationResult validateResult = validateExperiments(projectPath, experimentNames, fileNames);

        if (!validateResult.isValid()) {
            for (String line : validateResult.getReport().split("\n")) {
                PaintLogger.errorf(line);
            }
        } else {
            PaintLogger.infof("All validations passed");
        }
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // TEST CASES
    // ───────────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        Path projectPath = Paths.get("/Users/hans/Paint Test Project");
        testCase2(projectPath);
    }

    private static void testCase2(Path projectPath) {
        PaintLogger.initialise(projectPath, "Test");
        List<String> experimentNames = Arrays.asList(
                "221012",
                "221101",
                "221108"
        );

        List<String> fileNames = Arrays.asList(
                "Experiment Info.csv",
                RECORDINGS_CSV,
                SQUARES_CSV,
                TRACKS_CSV
        );

        validate(projectPath, experimentNames, fileNames);
    }

    private static void testCase1(Path projectPath) {
        PaintLogger.initialise(projectPath, "Test");

        List<String> experimentNames = Arrays.asList(
                "221012 Experiment Info Test 0",
                "221012 Experiment Info Test 1",
                "221012 Experiment Info Test 2",
                "221012 Experiment Info Test 3",
                "221012 Experiment Info Test 4",
                "221012 Experiment Info Test 5"
        );

        List<String> fileNames = Arrays.asList(
                EXPERIMENT_INFO_CSV,
                RECORDINGS_CSV,
                SQUARES_CSV,
                TRACKS_CSV
        );

        validate(projectPath, experimentNames, fileNames);
    }
}