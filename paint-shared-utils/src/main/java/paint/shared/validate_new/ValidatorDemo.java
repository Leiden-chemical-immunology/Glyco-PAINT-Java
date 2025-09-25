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

            ValidationResult result = validateFile(file);

            System.out.println("\n--- Validating " + file.getName() + " ---");
            if (result.isValid()) {
                System.out.println("✔ Validation passed.");
            } else {
                System.out.println("⚠ Validation failed:");
                result.getErrors().forEach(System.out::println);
            }
        }
    }

    private static ValidationResult validateFile(File file) {
        String name = file.getName().toLowerCase();

        if (name.contains("experiment")) {
            return new ExperimentInfoValidator()
                    .validateWithConsistency(file, getExperimentName(file));
        } else if (name.contains("recording")) {
            return new RecordingsValidator()
                    .validateWithConsistency(file, getExperimentName(file));
        } else if (name.contains("track")) {
            return new TracksValidator().validate(file);
        } else if (name.contains("square")) {
            return new SquaresValidator().validate(file);
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