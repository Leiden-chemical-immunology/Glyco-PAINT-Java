package paint.shared.validate_new;

import java.io.File;

public class ValidatorDemo {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java paint.shared.validate_new.ValidatorDemo <csv-file>");
            System.exit(1);
        }

        File file = new File(args[0]);
        if (!file.exists()) {
            System.err.println("File not found: " + file.getAbsolutePath());
            System.exit(1);
        }

        ValidationResult result;

        // Choose validator based on filename
        String name = file.getName().toLowerCase();
        if (name.equals("experiment_info.csv")) {
            ExperimentInfoValidator validator = new ExperimentInfoValidator();
            result = validator.validateWithConsistency(file, getExperimentName(file));
        } else if (name.equals("recordings.csv")) {
            RecordingsValidator validator = new RecordingsValidator();
            result = validator.validateWithConsistency(file, getExperimentName(file));
        } else if (name.equals("tracks.csv")) {
            TracksValidator validator = new TracksValidator();
            result = validator.validate(file);
        } else if (name.equals("squares.csv")) {
            SquaresValidator validator = new SquaresValidator();
            result = validator.validate(file);
        } else {
            System.err.println("❌ Unknown file type: " + name);
            return;
        }

        // Print results
        if (result.isValid()) {
            System.out.println("✔ Validation passed for " + file.getName());
        } else {
            System.out.println("⚠ Validation failed for " + file.getName() + ":");
            result.getErrors().forEach(System.out::println);
        }
    }

    /**
     * Helper: guess experiment name from parent directory.
     */
    private static String getExperimentName(File file) {
        File parent = file.getParentFile();
        return (parent != null) ? parent.getName() : "Experiment";
    }
}