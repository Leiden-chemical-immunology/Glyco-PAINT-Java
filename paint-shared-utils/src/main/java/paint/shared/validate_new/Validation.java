package paint.shared.validate_new;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Batch wrapper to validate one or more specific CSV files across multiple experiments in a project.
 *
 * You now specify the exact filenames, e.g. "Experiment Info.csv", "recordings.csv".
 */
public class Validation {

    /**
     * Validate selected CSV files for a set of experiments under a project.
     *
     * @param projectPath     root folder containing experiment subfolders
     * @param experimentNames list of experiment folder names
     * @param fileNames       list of exact filenames to validate (e.g. "Experiment Info.csv", "recordings.csv")
     * @return a human-readable report; empty if everything is valid
     */
    public static String validateExperiments(Path projectPath,
                                             List<String> experimentNames,
                                             List<String> fileNames) {
        List<String> report = new ArrayList<>();
        int okCount = 0;
        int errorCount = 0;

        for (String expName : experimentNames) {
            Path expDir = projectPath.resolve(expName);

            List<String> section = new ArrayList<>();
            section.add("─── Experiment: " + expName + " ─────────────────────────────────");

            if (!Files.isDirectory(expDir)) {
                section.add("✗ Missing experiment directory: " + expDir);
                report.add(String.join("\n", section));
                errorCount++;
                continue;
            }

            boolean experimentOk = true; // track status per experiment

            for (String fileName : fileNames) {
                Path filePath = expDir.resolve(fileName);

                if (!Files.exists(filePath)) {
                    section.add("✗ Missing file: " + fileName);
                    experimentOk = false;
                    continue;
                }

                ValidationResult res = runValidator(fileName, filePath.toFile(), expName);

                if (!res.isValid()) {
                    section.add("✗ Problems in " + fileName + ":");
                    for (String err : res.getErrors()) {
                        section.add("  • " + err.replace("\n", "\n    "));
                    }
                    experimentOk = false;
                } else {
                    section.add("✓ " + fileName + " OK");
                }
            }

            if (experimentOk) {
                okCount++;
            } else {
                errorCount++;
            }

            report.add(String.join("\n", section));
        }

        // Summary
        StringBuilder summary = new StringBuilder();
        summary.append("=== Validation Summary ===\n");
        summary.append("Project: ").append(projectPath.toAbsolutePath()).append("\n");
        summary.append("Experiments checked: ").append(experimentNames.size()).append("\n");
        summary.append("Files checked: ").append(fileNames).append("\n");
        summary.append("OK: ").append(okCount).append("\n");
        summary.append("With issues: ").append(errorCount).append("\n");

        if (report.isEmpty()) {
            return summary.append("\nAll selected experiments passed.\n").toString();
        }

        return summary.append("\n")
                .append(String.join("\n\n", report))
                .append("\n")
                .toString();
    }

    // Dispatch to correct validator based on file name
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

    // Example usage
    public static void main(String[] args) {
        Path projectPath = Paths.get("/Users/hans/Paint Test Experiment Error");
        List<String> experiments = new ArrayList<>();
        experiments.add("221012 Experiment Info Test 1");
        experiments.add("221012 Experiment Info Test 2");
        experiments.add("221012 Experiment Info Test 3");
        experiments.add("221012 Experiment Info Test 4");
        experiments.add("221012 Experiment Info Test 5");

        // ✅ Explicit filenames, not abstract types
        List<String> fileNames = Arrays.asList(
                "Experiment Info.csv",
                "All Recordings Java.csv"
                // "tracks.csv",
                // "squares.csv"
        );

        String out = validateExperiments(projectPath, experiments, fileNames);
        System.out.println(out);
    }
}