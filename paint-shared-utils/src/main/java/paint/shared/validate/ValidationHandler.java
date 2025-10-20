package paint.shared.validate;

import paint.shared.utils.PaintLogger;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static paint.shared.constants.PaintConstants.*;

/**
 * Batch validator that checks one or more specific CSV files across multiple experiments in a project.
 * <p>
 * Output policy:
 * - Only problems are collected, one per line:
 * [ExperimentName] - [FileName] - [Problem summary]
 * - If there are no problems at all: "✔ All validations passed"
 */
public class ValidationHandler {

    public static ValidationResult validateExperiments(Path projectPath,
                                                       List<String> experimentNames,
                                                       String fileName) {
        return validateExperiments(projectPath, experimentNames, Collections.singletonList(fileName));
    }

    public static ValidationResult validateExperiments(Path projectPath,
                                                       List<String> experimentNames,
                                                       List<String> fileNames) {
        List<String> report = new ArrayList<>();
        ValidationResult overall = new ValidationResult();

        for (String expName : experimentNames) {
            Path expDir = projectPath.resolve(expName);

            if (!Files.isDirectory(expDir)) {
                String msg = "[" + expName + "] - Directory - Missing experiment directory: " + expDir;
                overall.addError(msg);
                report.add(msg);
                continue;
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
                        String msg = getString(expName, fileName, err);
                        overall.addError(msg);
                        report.add(msg);
                    }
                }
            }
        }

        if (!report.isEmpty()) {
            overall.setReport(String.join("\n", report));
        }

        return overall;
    }


    private static String getString(String expName, String fileName, String err) {
        String flattened = err.replace("\n", " ").replaceAll("\\s+", " ").trim();

        String msg;
        if (flattened.startsWith("[" + expName + "]")) {
            // Inner validator already added experiment name
            msg = "[" + expName + "] - " + fileName + " - " +
                    flattened.substring(flattened.indexOf("]") + 1).trim();
        } else {
            msg = "[" + expName + "] - " + fileName + " - " + flattened;
        }
        return msg;
    }


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


    public static void validate(Path projectPath, List<String> experimentNames, List<String> fileNames) {

        PaintLogger.infof("Validating experiments: %s", experimentNames);
        PaintLogger.infof();
        PaintLogger.infof("Validating files: %s", fileNames);
        PaintLogger.infof();
        ValidationResult validateResult = validateExperiments(projectPath, experimentNames, fileNames);
        if (!validateResult.isValid()) {
            for (String line : validateResult.getReport().split("\n")) {
                PaintLogger.errorf(line);
            }
        } else {
            PaintLogger.infof("No errors");
        }

    }


    public static void main(String[] args) {

        Path projectPath = Paths.get("/Users/hans/Paint Test Project");
        //validateAll(projectPath);

        testCase2(projectPath);
    }


    public static void testCase2(Path projectPath) {

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


    public static void testCase1(Path projectPath) {

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