package paint.shared.validate;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import paint.shared.constants.PaintConstants;
import tech.tablesaw.api.ColumnType;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Validates the structure and contents of a Paint project directory.
 */
public class ProjectValidator {

    // Modes of validation for different application contexts
    public enum Mode {
        VALIDATE_TRACKMATE,
        VALIDATE_GENERATE_SQUARES,
        VALIDATE_VIEWER
    }

    // Container to hold validation result and error list
    public static class ValidateResult {
        private final boolean ok;
        private final List<String> errors;

        public ValidateResult(boolean ok, List<String> errors) {
            this.ok = ok;
            this.errors = errors;
        }

        public boolean isOk() {
            return ok;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    // Discover all experiment directories that contain Experiment Info CSV
    public static List<String> discoverExperiments(Path projectRoot) throws IOException {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(projectRoot)) return names;

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(projectRoot)) {
            for (Path p : ds) {
                if (Files.isDirectory(p) && Files.exists(p.resolve(PaintConstants.EXPERIMENT_INFO_CSV))) {
                    names.add(p.getFileName().toString());
                }
            }
        }
        return names;
    }

    // Validate a single experiment directory
    public static List<String> validateExperiment(Path experimentDir, Mode mode) {
        List<String> errors = new ArrayList<>();

        try {
            // Validate Experiment Info CSV
            Path expCsv = experimentDir.resolve(PaintConstants.EXPERIMENT_INFO_CSV);
            if (Files.exists(expCsv)) {
                errors.addAll(validateCsv(expCsv,
                        PaintConstants.EXPERIMENT_INFO_COLS,
                        PaintConstants.EXPERIMENT_INFO_TYPES));
            } else {
                errors.add("Missing required file: " + expCsv);
            }

            // Additional validations for recording and track files
            if (mode == Mode.VALIDATE_GENERATE_SQUARES || mode == Mode.VALIDATE_VIEWER) {
                Path recCsv = experimentDir.resolve(PaintConstants.RECORDINGS_CSV);
                if (Files.exists(recCsv)) {
                    errors.addAll(validateCsv(recCsv,
                            PaintConstants.RECORDING_COLS,
                            PaintConstants.RECORDING_TYPES));
                } else {
                    errors.add("Missing required file: " + recCsv);
                }

                Path trkCsv = experimentDir.resolve(PaintConstants.TRACKS_CSV);
                if (Files.exists(trkCsv)) {
                    errors.addAll(validateCsv(trkCsv,
                            PaintConstants.TRACK_COLS,
                            PaintConstants.TRACK_TYPES));
                } else {
                    errors.add("Missing required file: " + trkCsv);
                }

                // Required image directories check
                if (!Files.isDirectory(experimentDir.resolve(PaintConstants.DIR_BRIGHTFIELD_IMAGES))) {
                    errors.add("Missing directory: " + PaintConstants.DIR_BRIGHTFIELD_IMAGES);
                }
                if (!Files.isDirectory(experimentDir.resolve(PaintConstants.DIR_TRACKMATE_IMAGES))) {
                    errors.add("Missing directory: " + PaintConstants.DIR_TRACKMATE_IMAGES);
                }
            }

            // Full validation mode includes Squares CSV check
            if (mode == Mode.VALIDATE_VIEWER) {
                Path sqCsv = experimentDir.resolve(PaintConstants.SQUARES_CSV);
                if (Files.exists(sqCsv)) {
                    errors.addAll(validateCsv(sqCsv,
                            PaintConstants.SQUARE_COLS,
                            PaintConstants.SQUARE_TYPES));
                } else {
                    errors.add("Missing required file: " + sqCsv);
                }
            }

        } catch (Exception e) {
            errors.add("Unexpected error while validating " + experimentDir + ": " + e.getMessage());
        }

        return errors;
    }

    // Validate a list of experiments by name
    public static ValidateResult validateProject(Path projectRoot, List<String> experimentNames, Mode mode) {
        List<String> allErrors = new ArrayList<>();

        for (String exp : experimentNames) {
            Path expDir = projectRoot.resolve(exp);
            if (Files.exists(expDir) && Files.isDirectory(expDir)) {
                List<String> expErrors = validateExperiment(expDir, mode);
                for (String err : expErrors) {
                    allErrors.add("[" + exp + "] " + err);
                }
            } else {
                allErrors.add("[" + exp + "] ✖ Experiment folder not found: " + expDir);
            }
        }

        return new ValidateResult(allErrors.isEmpty(), allErrors);
    }

    // Discover and validate all experiments in a project
    public static ValidateResult validateProject(Path projectRoot, Mode mode) throws IOException {
        List<String> experimentNames = discoverExperiments(projectRoot);
        if (experimentNames.isEmpty()) {
            List<String> errs = new ArrayList<>();
            errs.add("No experiments found under " + projectRoot);
            return new ValidateResult(false, errs);
        }
        return validateProject(projectRoot, experimentNames, mode);
    }

    // Validates CSV file headers using Apache Commons CSV
    private static List<String> validateCsv(Path filePath, String[] expectedCols, ColumnType[] expectedTypes) {
        List<String> report = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(filePath);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader().build())) {

            // Extract actual headers from CSV
            List<String> actualHeaders = new ArrayList<>(parser.getHeaderMap().keySet());

            // Check header count matches expected
            if (actualHeaders.size() != expectedCols.length) {
                report.add("Header length mismatch in " + filePath.getFileName() +
                        ": expected " + expectedCols.length + " but found " + actualHeaders.size());
            } else {
                // Check each header name matches expected
                for (int i = 0; i < expectedCols.length; i++) {
                    if (!actualHeaders.get(i).trim().equals(expectedCols[i])) {
                        report.add("Header mismatch in " + filePath.getFileName() +
                                " at column " + i + ": expected '" + expectedCols[i] +
                                "' but found '" + actualHeaders.get(i) + "'");
                    }
                }
            }
        } catch (IOException e) {
            report.add("I/O error reading " + filePath + ": " + e.getMessage());
        }
        return report;
    }

    // Format validation errors into a readable string
    public static String formatReport(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Validation FAILED. Found ").append(errors.size()).append(" issue(s):\n");
        for (String err : errors) {
            sb.append("  - ").append(err).append("\n");
        }
        return sb.toString();
    }

    // Command-line interface for project validation
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java paint.shared.validate.ProjectValidator <projectRoot> <mode> [<experiment1> <experiment2> ...]");
            System.err.println("Modes: TRACKMATE | GENERATE_SQUARES | VIEWER");
            System.exit(1);
        }

        Path projectRoot = Paths.get(args[0]);
        Mode mode;

        // Parse mode from argument
        String modeArg = args[1].toUpperCase();
        switch (modeArg) {
            case "TRACKMATE": mode = Mode.VALIDATE_TRACKMATE; break;
            case "GENERATE_SQUARES": mode = Mode.VALIDATE_GENERATE_SQUARES; break;
            case "VIEWER": mode = Mode.VALIDATE_VIEWER; break;
            default:
                System.err.println("Invalid mode: " + args[1]);
                System.err.println("Valid modes: TRACKMATE | GENERATE_SQUARES | VIEWER");
                System.exit(2);
                return;
        }

        List<String> experimentNames = null;
        if (args.length > 2) {
            experimentNames = Arrays.asList(Arrays.copyOfRange(args, 2, args.length));
        }

        try {
            ValidateResult validateResult;
            if (experimentNames == null || experimentNames.isEmpty()) {
                validateResult = validateProject(projectRoot, mode);
            } else {
                validateResult = validateProject(projectRoot, experimentNames, mode);
            }

            if (validateResult.isOk()) {
                System.out.println("Validation passed with no errors.");
                System.exit(0);
            } else {
                System.out.println(formatReport(validateResult.getErrors()));
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("Failed to run validation: " + e.getMessage());
            System.exit(2);
        }
    }
}
