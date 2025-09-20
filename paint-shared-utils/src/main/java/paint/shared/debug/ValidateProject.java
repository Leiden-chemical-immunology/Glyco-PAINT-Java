package paint.shared.debug;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility for validating the structure of Paint project experiment folders and CSV files.
 *
 * <p>Validation modes control which files and directories are required:
 * <ul>
 *   <li>{@link Mode#VALIDATE_TRACKMATE} – requires only {@code Experiment Info.csv}.</li>
 *   <li>{@link Mode#VALIDATE_GENERATE_SQUARES} – requires experiment info, recordings,
 *       tracks, and the image directories.</li>
 *   <li>{@link Mode#VALIDATE_VIEWER} – full validation, including the squares CSV.</li>
 * </ul></p>
 *
 * <p>Validation can be invoked in two ways:
 * <ul>
 *   <li>From code, via {@link #validateProject(Path, List, Mode)} or
 *       {@link #validateProject(Path, Mode)}.</li>
 *   <li>From the command line, via {@link #main(String[])}.</li>
 * </ul>
 * Both return a {@link ValidateResult} describing overall success and errors.</p>
 */
public class ValidateProject {

    /** Supported CSV type categories (used only for schema definition). */
    private enum CsvType { STRING, INTEGER, DOUBLE, BOOLEAN, LOCAL_DATE_TIME }

    /**
     * Validation mode – defines which files and directories must exist.
     */
    public enum Mode {
        /** Check only {@code Experiment Info.csv}. */
        VALIDATE_TRACKMATE,

        /** Check experiment info, recordings, tracks, and image directories. */
        VALIDATE_GENERATE_SQUARES,

        /** Full validation: includes squares CSV in addition to all other checks. */
        VALIDATE_VIEWER
    }

    /**
     * Result object containing outcome and error list.
     */
    public static class ValidateResult {
        private final boolean ok;
        private final List<String> errors;

        /**
         * Create a new validation result.
         *
         * @param ok     true if validation passed with no errors
         * @param errors list of errors (possibly empty if {@code ok} is true)
         */
        public ValidateResult(boolean ok, List<String> errors) {
            this.ok = ok;
            this.errors = errors;
        }

        /**
         * @return true if no validation errors
         */
        public boolean isOk() {
            return ok;
        }

        /**
         * @return list of validation errors (empty if none)
         */
        public List<String> getErrors() {
            return errors;
        }
    }

    // --- Filenames and directories ---
    private static final String RECORDINGS_CSV = "All Recordings Java.csv";
    private static final String TRACKS_CSV     = "All Tracks Java.csv";
    private static final String SQUARES_CSV    = "All Squares Java.csv";
    private static final String EXPERIMENT_INFO_CSV = "Experiment Info.csv";

    private static final String DIR_BRIGHTFIELD = "Brightfield Images";
    private static final String DIR_TRACKMATE   = "TrackMate Images";

    // --- Schemas ---
    private static final String[] EXPERIMENT_INFO_COLS = {
            "Recording Name", "Condition Number", "Replicate Number",
            "Probe Name", "Probe Type", "Cell Type", "Adjuvant",
            "Concentration", "Process Flag", "Threshold"
    };
    private static final CsvType[] EXPERIMENT_INFO_TYPES = {
            CsvType.STRING, CsvType.INTEGER, CsvType.INTEGER,
            CsvType.STRING, CsvType.STRING, CsvType.STRING,
            CsvType.STRING, CsvType.DOUBLE, CsvType.BOOLEAN, CsvType.DOUBLE
    };

    private static final String[] RECORDING_COLS = {
            "Recording Name", "Condition Number", "Replicate Number",
            "Probe Name", "Probe Type", "Cell Type", "Adjuvant",
            "Concentration", "Process Flag", "Threshold",
            "Number of Spots", "Number of Tracks", "Number of Spots in All Tracks",
            "Number of Frames", "Run Time", "Time Stamp", "Exclude",
            "Tau", "R Squared", "Density"
    };
    private static final CsvType[] RECORDING_TYPES = {
            CsvType.STRING, CsvType.INTEGER, CsvType.INTEGER,
            CsvType.STRING, CsvType.STRING, CsvType.STRING, CsvType.STRING,
            CsvType.DOUBLE, CsvType.BOOLEAN, CsvType.DOUBLE,
            CsvType.INTEGER, CsvType.INTEGER, CsvType.INTEGER, CsvType.INTEGER,
            CsvType.DOUBLE, CsvType.LOCAL_DATE_TIME, CsvType.BOOLEAN,
            CsvType.DOUBLE, CsvType.DOUBLE, CsvType.DOUBLE
    };

    private static final String[] SQUARE_COLS = {
            "Unique Key", "Recording Name", "Square Number", "Row Number",
            "Column Number", "Label Number", "Cell ID", "Selected",
            "Square Manually Excluded", "Image Excluded",
            "X0", "Y0", "X1", "Y1",
            "Number of Tracks", "Variability", "Density", "Density Ratio",
            "Tau", "R Squared", "Median Diffusion Coefficient",
            "Median Diffusion Coefficient Ext", "Median Long Track Duration",
            "Median Short Track Duration", "Median Displacement",
            "Max Displacement", "Total Displacement", "Median Max Speed",
            "Max Max Speed", "Median Mean Speed", "Max Mean Speed",
            "Max Track Duration", "Total Track Duration", "Median Track Duration"
    };
    private static final CsvType[] SQUARE_TYPES = {
            CsvType.STRING, CsvType.STRING, CsvType.INTEGER, CsvType.INTEGER,
            CsvType.INTEGER, CsvType.INTEGER, CsvType.INTEGER,
            CsvType.BOOLEAN, CsvType.BOOLEAN, CsvType.BOOLEAN,
            CsvType.DOUBLE, CsvType.DOUBLE, CsvType.DOUBLE, CsvType.DOUBLE,
            CsvType.INTEGER, CsvType.DOUBLE, CsvType.DOUBLE, CsvType.DOUBLE,
            CsvType.DOUBLE, CsvType.DOUBLE, CsvType.DOUBLE,
            CsvType.DOUBLE, CsvType.DOUBLE, CsvType.DOUBLE,
            CsvType.DOUBLE, CsvType.DOUBLE, CsvType.DOUBLE,
            CsvType.DOUBLE, CsvType.DOUBLE, CsvType.DOUBLE,
            CsvType.DOUBLE, CsvType.DOUBLE, CsvType.DOUBLE
    };

    private static final String[] TRACK_COLS = {
            "Unique Key", "Recording Name", "Track Id", "Track Label",
            "Number of Spots", "Number of Gaps", "Longest Gap",
            "Track Duration", "Track X Location", "Track Y Location",
            "Track Displacement", "Track Max Speed", "Track Median Speed",
            "Diffusion Coefficient", "Diffusion Coefficient Ext",
            "Total Distance", "Confinement Ratio", "Square Number", "Label Number"
    };
    private static final CsvType[] TRACK_TYPES = {
            CsvType.STRING, CsvType.STRING, CsvType.INTEGER, CsvType.STRING,
            CsvType.INTEGER, CsvType.INTEGER, CsvType.INTEGER,
            CsvType.DOUBLE, CsvType.DOUBLE, CsvType.DOUBLE,
            CsvType.DOUBLE, CsvType.DOUBLE, CsvType.DOUBLE,
            CsvType.DOUBLE, CsvType.DOUBLE, CsvType.DOUBLE,
            CsvType.DOUBLE, CsvType.INTEGER, CsvType.INTEGER
    };

    /**
     * Discover experiment directories as immediate subfolders containing
     * {@code Experiment Info.csv}.
     *
     * @param projectRoot root of the Paint project
     * @return list of experiment folder names
     * @throws IOException if traversal fails
     */
    public static List<String> discoverExperiments(Path projectRoot) throws IOException {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(projectRoot)) return names;

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(projectRoot)) {
            for (Path p : ds) {
                if (Files.isDirectory(p) && Files.exists(p.resolve(EXPERIMENT_INFO_CSV))) {
                    names.add(p.getFileName().toString());
                }
            }
        }
        return names;
    }

    /**
     * Validate a single experiment folder by checking required files and directories.
     * Requirements depend on the selected {@link Mode}.
     *
     * @param experimentDir experiment directory
     * @param mode          validation mode
     * @return list of validation errors (empty if none)
     */
    public static List<String> validateExperiment(Path experimentDir, Mode mode) {
        List<String> errors = new ArrayList<>();

        try {
            Path expCsv = experimentDir.resolve(EXPERIMENT_INFO_CSV);
            if (Files.exists(expCsv)) {
                errors.addAll(validateCsv(expCsv, EXPERIMENT_INFO_COLS, EXPERIMENT_INFO_TYPES));
            } else {
                errors.add("Missing required file: " + expCsv);
            }

            if (mode == Mode.VALIDATE_GENERATE_SQUARES || mode == Mode.VALIDATE_VIEWER) {
                Path recCsv = experimentDir.resolve(RECORDINGS_CSV);
                if (Files.exists(recCsv)) {
                    errors.addAll(validateCsv(recCsv, RECORDING_COLS, RECORDING_TYPES));
                } else {
                    errors.add("Missing required file: " + recCsv);
                }

                Path trkCsv = experimentDir.resolve(TRACKS_CSV);
                if (Files.exists(trkCsv)) {
                    errors.addAll(validateCsv(trkCsv, TRACK_COLS, TRACK_TYPES));
                } else {
                    errors.add("Missing required file: " + trkCsv);
                }

                if (!Files.isDirectory(experimentDir.resolve(DIR_BRIGHTFIELD))) {
                    errors.add("Missing directory: " + DIR_BRIGHTFIELD);
                }
                if (!Files.isDirectory(experimentDir.resolve(DIR_TRACKMATE))) {
                    errors.add("Missing directory: " + DIR_TRACKMATE);
                }
            }

            if (mode == Mode.VALIDATE_VIEWER) {
                Path sqCsv = experimentDir.resolve(SQUARES_CSV);
                if (Files.exists(sqCsv)) {
                    errors.addAll(validateCsv(sqCsv, SQUARE_COLS, SQUARE_TYPES));
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
     * Validate multiple experiments. This overload requires a list of experiment names
     * and does not perform auto-discovery.
     *
     * @param projectRoot     project root directory
     * @param experimentNames list of experiment folder names
     * @param mode            validation mode
     * @return {@link ValidateResult} with status and aggregated errors
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
     * High-level validation entry point that performs auto-discovery of experiment folders.
     * Equivalent to calling {@link #validateProject(Path, List, Mode)} with a discovered list.
     *
     * @param projectRoot root directory of the Paint project
     * @param mode        validation mode
     * @return {@link ValidateResult} with status and error list
     * @throws IOException if directory traversal fails
     */
    public static ValidateResult validateProject(Path projectRoot, Mode mode) throws IOException {
        List<String> experimentNames  = discoverExperiments(projectRoot);
        if (experimentNames.isEmpty()) {
            List<String> errs = new ArrayList<>();
            errs.add("No experiments found under " + projectRoot);
            return new ValidateResult(false, errs);
        }
        return validateProject(projectRoot, experimentNames, mode);
    }

    /**
     * Validate a CSV file header against the expected schema.
     *
     * @param filePath      CSV file path
     * @param expectedCols  expected header names
     * @param expectedTypes expected column types (unused, for reference)
     * @return list of issues found (empty if valid)
     */
    private static List<String> validateCsv(Path filePath, String[] expectedCols, CsvType[] expectedTypes) {
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
     * Format validation errors into a report string.
     *
     * @param errors list of errors
     * @return formatted report string, or empty string if none
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
     * CLI entry point. Validates based on command-line arguments.
     * Exits with code 0 on success, 1 on validation failure, 2 on errors.
     *
     * <p>Usage:</p>
     * <pre>
     *   java paint.shared.debug.ValidateProject &lt;projectRoot&gt; &lt;mode&gt; [experiment1 experiment2 ...]
     * </pre>
     *
     * <p>Modes: VALIDATE_TRACKMATE | VALIDATE_GENERATE_SQUARES | VALIDATE_VIEWER</p>
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java paint.shared.debug.ValidateProject <projectRoot> <mode> [<experiment1> <experiment2> ...]");
            System.err.println("Modes: VALIDATE_TRACKMATE | VALIDATE_GENERATE_SQUARES | VALIDATE_VIEWER");
            System.exit(1);
        }

        Path projectRoot = Paths.get(args[0]);
        Mode mode;
        try {
            mode = Mode.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid mode: " + args[1]);
            System.err.println("Valid modes: VALIDATE_TRACKMATE | VALIDATE_GENERATE_SQUARES | VALIDATE_VIEWER");
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