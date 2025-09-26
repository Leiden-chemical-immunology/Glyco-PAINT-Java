package paint.shared.validate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Batch validator that checks one or more specific CSV files across multiple experiments in a project.
 *
 * Output policy:
 *  - Only problems are collected, one per line:
 *      [ExperimentName] - [FileName] - [Problem summary]
 *  - If there are no problems at all: "✔ All validations passed"
 */
public class Validation {

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
                        String flattened = err.replace("\n", " ").replaceAll("\\s+", " ").trim();
                        String msg = "[" + expName + "] - " + fileName + " - " + flattened;
                        overall.addError(msg);
                        report.add(msg);
                    }
                }
            }
        }

        if (report.isEmpty()) {
            overall.setReport("✔ All validations passed");
        } else {
            overall.setReport(String.join("\n", report));
        }

        return overall;
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

    public static void main(String[] args) {
        Path projectPath = Paths.get("/Users/hans/JavaPaintProjects/paint-shared-utils/src/test/resources/Paint Test Experiment Error");

        List<String> experiments = Arrays.asList(
                "221012 Experiment Info Test 0",
                "221012 Experiment Info Test 1",
                "221012 Experiment Info Test 2",
                "221012 Experiment Info Test 3",
                "221012 Experiment Info Test 4",
                "221012 Experiment Info Test 5"
        );

        List<String> fileNames = Arrays.asList(
                "Experiment Info.csv",
                "All Recordings Java.csv"
        );

        ValidationResult out = validateExperiments(projectPath, experiments, fileNames);
        System.out.println(out.getReport());

        System.out.println();
        System.out.println();

        // A Set of experiments without errors
        experiments = Arrays.asList(
                "221012 Experiment Info Test 0",
                "221012 Experiment Info Test 0",
                "221012 Experiment Info Test 0"
        );

        out = validateExperiments(projectPath, experiments, fileNames);
        System.out.println(out.getReport());
    }
}