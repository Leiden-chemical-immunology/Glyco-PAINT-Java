package paint.shared.validate;

import paint.shared.constants.PaintConstants;
import tech.tablesaw.api.ColumnType;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Validates the structure and contents of a Paint project directory.
 *
 * <p>This class performs schema and file checks for Paint project experiments.
 * Validation depends on the mode:
 * <ul>
 *   <li>{@code VALIDATE_TRACKMATE} – checks Experiment Info only</li>
 *   <li>{@code VALIDATE_GENERATE_SQUARES} – checks Experiment Info, Recordings, Tracks, and required directories</li>
 *   <li>{@code VALIDATE_VIEWER} – checks everything including Squares</li>
 * </ul>
 *
 * <p>Schema definitions are based on {@link PaintConstants}.</p>
 */
public class ProjectValidator {

    /**
     * Validation modes to control which checks are performed.
     */
    public enum Mode {
        /** Only validate Experiment Info files. */
        VALIDATE_TRACKMATE,

        /** Validate experiment-level files and directories for square generation. */
        VALIDATE_GENERATE_SQUARES,

        /** Full validation including square-level data for viewer use. */
        VALIDATE_VIEWER
    }

    /**
     * Result container for project validation.
     */
    public static class ValidateResult {
        private final boolean ok;
        private final List<String> errors;

        /**
         * Constructs a validation result.
         *
         * @param ok     true if validation passed
         * @param errors list of validation error messages
         */
        public ValidateResult(boolean ok, List<String> errors) {
            this.ok = ok;
            this.errors = errors;
        }

        /** @return true if validation passed */
        public boolean isOk() {
            return ok;
        }

        /** @return list of validation errors */
        public List<String> getErrors() {
            return errors;
        }
    }

    /**
     * Scans the project root directory for valid experiments (containing an Experiment Info CSV).
     *
     * @param projectRoot path to the project root
     * @return list of discovered experiment names
     * @throws IOException if directory traversal fails
     */
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

    /**
     * Validates a single experiment directory for required files and schema consistency.
     *
     * @param experimentDir the path to the experiment folder
     * @param mode          validation mode
     * @return list of errors found
     */
    public static List<String> validateExperiment(Path experimentDir, Mode mode) {
        List<String> errors = new ArrayList<>();

        try {
            // Experiment Info
            Path expCsv = experimentDir.resolve(PaintConstants.EXPERIMENT_INFO_CSV);
            if (Files.exists(expCsv)) {
                errors.addAll(validateCsv(expCsv,
                        PaintConstants.EXPERIMENT_INFO_COLS,
                        PaintConstants.EXPERIMENT_INFO_TYPES));
            } else {
                errors.add("Missing required file: " + expCsv);
            }

            if (mode == Mode.VALIDATE_GENERATE_SQUARES || mode == Mode.VALIDATE_VIEWER) {
                // Recordings
                Path recCsv = experimentDir.resolve(PaintConstants.RECORDINGS_CSV);
                if (Files.exists(recCsv)) {
                    errors.addAll(validateCsv(recCsv,
                            PaintConstants.RECORDING_COLS,
                            PaintConstants.RECORDING_TYPES));
                } else {
                    errors.add("Missing required file: " + recCsv);
                }

                // Tracks
                Path trkCsv = experimentDir.resolve(PaintConstants.TRACKS_CSV);
                if (Files.exists(trkCsv)) {
                    errors.addAll(validateCsv(trkCsv,
                            PaintConstants.TRACK_COLS,
                            PaintConstants.TRACK_TYPES));
                } else {
                    errors.add("Missing required file: " + trkCsv);
                }

                // Required directories
                if (!Files.isDirectory(experimentDir.resolve(PaintConstants.DIR_BRIGHTFIELD_IMAGES))) {
                    errors.add("Missing directory: " + PaintConstants.DIR_BRIGHTFIELD_IMAGES);
                }
                if (!Files.isDirectory(experimentDir.resolve(PaintConstants.DIR_TRACKMATE_IMAGES))) {
                    errors.add("Missing directory: " + PaintConstants.DIR_TRACKMATE_IMAGES);
                }
            }

            if (mode == Mode.VALIDATE_VIEWER) {
                // Squares
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

    /**
     * Validates the given list of experiments.
     *
     * @param projectRoot     the root directory containing all experiments
     * @param experimentNames the names of experiments to validate
     * @param mode            validation mode
     * @return validation result with outcome and any issues
     */
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

    /**
     * Validates all experiments discovered in the project directory.
     *
     * @param projectRoot the root directory
     * @param mode        validation mode
     * @return result of validation
     * @throws IOException if discovery fails
     */
    public static ValidateResult validateProject(Path projectRoot, Mode mode) throws IOException {
        List<String> experimentNames = discoverExperiments(projectRoot);
        if (experimentNames.isEmpty()) {
            List<String> errs = new ArrayList<>();
            errs.add("No experiments found under " + projectRoot);
            return new ValidateResult(false, errs);
        }
        return validateProject(projectRoot, experimentNames, mode);
    }

    /**
     * Validates that the given CSV file has the correct headers and types.
     *
     * @param filePath      CSV file to validate
     * @param expectedCols  expected column names in order
     * @param expectedTypes expected column types (ignored here, for future use)
     * @return list of errors
     */
    private static List<String> validateCsv(Path filePath, String[] expectedCols, ColumnType[] expectedTypes) {
        List<String> report = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(filePath);
            if (lines.isEmpty()) {
                report.add("Empty file: " + filePath);
                return report;
            }

            String[] headers = lines.get(0).split(",");
            if (headers.length != expectedCols.length) {
                report.add("Header length mismatch in " + filePath.getFileName() +
                        ": expected " + expectedCols.length + " but found " + headers.length);
            } else {
                for (int i = 0; i < headers.length; i++) {
                    if (!headers[i].trim().equals(expectedCols[i])) {
                        report.add("Header mismatch in " + filePath.getFileName() +
                                " at column " + i + ": expected '" + expectedCols[i] +
                                "' but found '" + headers[i] + "'");
                    }
                }
            }
        } catch (IOException e) {
            report.add("I/O error reading " + filePath + ": " + e.getMessage());
        }
        return report;
    }

    /**
     * Formats a list of error messages into a human-readable validation report.
     *
     * @param errors list of error messages
     * @return formatted report string
     */
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

    /**
     * Command-line interface to validate a Paint project.
     * <pre>
     * Usage:
     *   java paint.shared.validate.ProjectValidator &lt;projectRoot&gt; &lt;mode&gt; [&lt;experiment1&gt; ...]
     * Modes:
     *   TRACKMATE
     *   GENERATE_SQUARES
     *   VIEWER
     * </pre>
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java paint.shared.validate.ProjectValidator <projectRoot> <mode> [<experiment1> <experiment2> ...]");
            System.err.println("Modes: TRACKMATE | GENERATE_SQUARES | VIEWER");
            System.exit(1);
        }

        Path projectRoot = Paths.get(args[0]);
        Mode mode;
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