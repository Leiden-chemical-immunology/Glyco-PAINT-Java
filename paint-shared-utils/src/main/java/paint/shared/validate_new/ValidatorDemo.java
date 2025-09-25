package paint.shared.validate_new;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ValidatorDemo {

    public static void main(String[] args) {
        // 🔧 Specify your files here
        List<String> filesToCheck = Arrays.asList(
                "/Users/hans/Paint Test Project/221012 Test 2/All Recordings Java.csv",
                "/Users/hans/Paint Test Project/221012 Test 2/All Tracks Java.csv",
                "/Users/hans/Paint Test Project/221012 Test 2/All Squares Java.csv",
                "/Users/hans/Paint Test Project/221012 Test 2/Experiment Info.csv");

        for (String path : filesToCheck) {
            File file = new File(path);
            if (!file.exists()) {
                System.err.println("❌ File not found: " + path);
                continue;
            }

            System.out.println("\n--- Validating " + file.getName() + " ---");

            // Header-only check
            ValidationResult headerOnly = validateFile(file, false);
            if (headerOnly.isValid()) {
                System.out.println("✔ Header validation passed.");
            } else {
                System.out.println("⚠ Header validation failed:");
                headerOnly.getErrors().forEach(System.out::println);
            }

            // Full validation (headers + values + consistency if applicable)
            ValidationResult full = validateFile(file, true);
            if (full.isValid()) {
                System.out.println("✔ Full validation passed.");
            } else {
                System.out.println("⚠ Full validation failed:");
                full.getErrors().forEach(System.out::println);
            }
        }
    }

    private static ValidationResult validateFile(File file, boolean checkValues) {
        String name = file.getName().toLowerCase();

        if (name.contains("experiment")) {
            ExperimentInfoValidator validator = new ExperimentInfoValidator();
            return checkValues
                    ? validator.validateWithConsistency(file, getExperimentName(file))
                    : validator.validateHeadersOnly(file);
        } else if (name.contains("recording")) {
            RecordingsValidator validator = new RecordingsValidator();
            return checkValues
                    ? validator.validateWithConsistency(file, getExperimentName(file))
                    : validator.validateHeadersOnly(file);
        } else if (name.contains("track")) {
            TracksValidator validator = new TracksValidator();
            return checkValues
                    ? validator.validate(file)   // full check
                    : validator.validateHeadersOnly(file);
        } else if (name.contains("square")) {
            SquaresValidator validator = new SquaresValidator();
            return checkValues
                    ? validator.validate(file)   // full check
                    : validator.validateHeadersOnly(file);
        } else {
            ValidationResult result = new ValidationResult();
            result.addError("Unknown file type: " + name);
            return result;
        }
    }

    private static String getExperimentName(File file) {
        File parent = file.getParentFile();
        return parent != null ? parent.getName() : "Experiment";
    }
}