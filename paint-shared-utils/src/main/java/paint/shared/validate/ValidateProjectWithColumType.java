package paint.shared.validate;

import paint.shared.constants.PaintConstants;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.DirectoryStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static paint.shared.constants.PaintConstants.*;

/**
 * Utility class for validating the format of Paint project CSV files.
 *
 * <p>CLI usage examples:</p>
 *
 * <pre>{@code
 *   java -cp "<deps...>" paint.validation.ValidateFileFormat --project /path/to/project --experiments 221012,221101 --squares
 *   java -cp "<deps...>" paint.validation.ValidateFileFormat /path/to/project 221012 221101 --squares
 *   java -cp "<deps...>" paint.validation.ValidateFileFormat --project /path/to/project   (auto-discovers experiments)
 * }</pre>
 */
public class ValidateProjectWithColumType {

    public static void main(String[] args) {
        CliArgs cli = parseArgs(args);
        if (cli.showHelp) {
            printHelp();
            System.exit(0);
        }
        if (cli.projectRoot == null) {
            System.err.println("ERROR: Project root is required.");
            printHelp();
            System.exit(2);
        }

        Path project = Paths.get(cli.projectRoot);

        List<String> experiments = new ArrayList<>(cli.experiments);
        if (experiments.isEmpty()) {
            try {
                experiments = discoverExperiments(project);
                if (experiments.isEmpty()) {
                    System.err.println("No experiments discovered under: " + project);
                    System.exit(3);
                }
                System.out.println("Discovered experiments: " + String.join(", ", experiments));
            } catch (IOException e) {
                System.err.println("Failed to discover experiments: " + e.getMessage());
                System.exit(4);
            }
        }

        List<String> errors = validateProject_CT(project, experiments, cli.checkSquares);

        if (errors.isEmpty()) {
            System.out.println("✔ Validation OK. Checked " + experiments.size() + " experiment(s).");
            System.exit(0);
        } else {
            System.err.println("✖ Validation FAILED. Found " + errors.size() + " issue(s):");
            for (String err : errors) {
                System.err.println("  - " + err);
            }
            System.exit(1);
        }
    }

    private static void printHelp() {
        System.out.println(
                "ValidateFileFormat - validate Paint CSV schemas\n" +
                        "\n" +
                        "Usage:\n" +
                        "  java paint.validation.ValidateFileFormat --project <path> [--experiments e1,e2,...] [--squares]\n" +
                        "  java paint.validation.ValidateFileFormat <projectPath> [exp1 exp2 ...] [--squares]\n" +
                        "\n" +
                        "Options:\n" +
                        "  --project <path>      Project root directory (required if not given as first arg)\n" +
                        "  --experiments <list>  Comma-separated experiment folder names under the project root\n" +
                        "  --squares             Also validate all_squares.csv in each experiment\n" +
                        "  --no-squares          Do not validate all_squares.csv (default)\n" +
                        "  --help                Show this help\n" +
                        "\n" +
                        "If no experiments are specified, subdirectories containing data.csv or experiment info.csv\n" +
                        "are auto-discovered and validated."
        );
    }

    private static class CliArgs {
        String projectRoot;
        List<String> experiments = new ArrayList<>();
        boolean checkSquares = false;
        boolean showHelp = false;
    }

    private static CliArgs parseArgs(String[] args) {
        CliArgs out = new CliArgs();
        int i = 0;

        while (i < args.length) {
            String a = args[i];

            switch (a) {
                case "--help":
                case "-h":
                    out.showHelp = true;
                    i++;
                    break;

                case "--project":
                    i++;
                    if (i >= args.length) {
                        System.err.println("Missing value for --project");
                    } else {
                        out.projectRoot = args[i++];
                    }
                    break;

                case "--experiments":
                    i++;
                    if (i >= args.length) {
                        System.err.println("Missing value for --experiments");
                    } else {
                        out.experiments.addAll(splitCommaList(args[i++]));
                    }
                    break;

                case "--squares":
                    out.checkSquares = true;
                    i++;
                    break;

                case "--no-squares":
                    out.checkSquares = false;
                    i++;
                    break;

                default:
                    // Positional parsing:
                    if (out.projectRoot == null) {
                        out.projectRoot = a;
                    } else {
                        out.experiments.add(a);
                    }
                    i++;
            }
        }

        return out;
    }

    private static List<String> splitCommaList(String s) {
        if (s == null || s.isEmpty()) return new ArrayList<>();
        return Arrays.asList(s.split("\\s*,\\s*"));
    }

    /**
     * Auto-discover experiment directories as immediate subfolders containing
     * either data.csv or experiment_info.csv.
     */
    private static List<String> discoverExperiments(Path projectRoot) throws IOException {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(projectRoot)) return names;

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(projectRoot)) {
            for (Path p : ds) {
                if (Files.isDirectory(p)) {
                    boolean hasInfo = Files.exists(p.resolve(EXPERIMENT_INFO_CSV));
                    if (hasInfo) {
                        names.add(p.getFileName().toString());
                    }
                }
            }
        }
        return names;
    }

    /**
     * Validate all CSV files of a given experiment folder.
     *
     * @param experimentDir The experiment root directory
     * @param checkSquares  Whether to also check all_squares.csv
     * @return list of error messages (empty if valid)
     */
    public static List<String> validateExperiment(Path experimentDir, boolean checkSquares) {
        List<String> errors = new ArrayList<>();

        try {
            // All Recordings.csv
            Path dataCsv = experimentDir.resolve(RECORDINGS_CSV);
            if (Files.exists(dataCsv)) {
                errors.addAll(validateCsv(
                        dataCsv,
                        PaintConstants.RECORDING_COLS,
                        PaintConstants.RECORDING_TYPES
                ));
            } else {
                errors.add("Missing required file: " + dataCsv);
            }

            // experiment_info.csv
            Path expCsv = experimentDir.resolve(EXPERIMENT_INFO_CSV);
            if (Files.exists(expCsv)) {
                errors.addAll(validateCsv(
                        expCsv,
                        PaintConstants.EXPERIMENT_INFO_COLS,
                        PaintConstants.EXPERIMENT_INFO_TYPES
                ));
            } else {
                errors.add("Missing required file: " + expCsv);
            }

            // all_squares.csv (optional)
            if (checkSquares) {
                Path sqCsv = experimentDir.resolve(SQUARES_CSV);
                if (Files.exists(sqCsv)) {
                    errors.addAll(validateCsv(
                            sqCsv,
                            PaintConstants.SQUARE_COLS,
                            PaintConstants.SQUARE_TYPES
                    ));
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
     * Validates a list of experiments located under a given project root directory.
     * <p>
     * For each experiment name provided, this method checks if the corresponding
     * directory exists. If it does, the experiment is validated via
     * {@code validateExperiment}, and any errors found are collected with the
     * experiment name prepended for context. If the directory does not exist,
     * an error entry is added.
     * </p>
     *
     * @param projectRoot     the root directory of the project containing experiment folders
     * @param experimentNames the list of experiment folder names to validate
     * @param checkSquares    whether to perform additional validation of square data
     * @return a list of error messages; empty if all experiments are valid
     */
    public static List<String> validateProject_CT(Path projectRoot, List<String> experimentNames, boolean checkSquares) {
        List<String> allErrors = new ArrayList<>();

        for (String exp : experimentNames) {
            Path expDir = projectRoot.resolve(exp);
            if (Files.exists(expDir) && Files.isDirectory(expDir)) {
                List<String> expErrors = validateExperiment(expDir, checkSquares);
                for (String err : expErrors) {
                    allErrors.add("[" + exp + "] " + err);
                }
            } else {
                allErrors.add("Experiment folder not found: " + expDir);
            }
        }

        return allErrors;
    }

    /**
     * Validates the structure and types of a CSV file against expected column names
     * and column types.
     * <p>
     * The validation includes:
     * <ul>
     *   <li>Checking that the number of columns in the header matches the expected count.</li>
     *   <li>Verifying that column names appear in the expected order.</li>
     *   <li>Comparing actual column types to expected types, with some flexibility:
     *       <ul>
     *         <li>Empty columns produce a warning instead of an error.</li>
     *         <li>{@code DOUBLE} is considered compatible with {@code INTEGER}.</li>
     *         <li>{@code STRING} and {@code LOCAL_DATE_TIME} are treated as loosely compatible.</li>
     *       </ul>
     *   </li>
     * </ul>
     * If mismatches are found, descriptive error messages are added to the report.
     * </p>
     *
     * @param filePath      path to the CSV file to validate
     * @param expectedCols  the expected column names in order
     * @param expectedTypes the expected column types aligned with {@code expectedCols}
     * @return a list of validation messages, including errors and warnings;
     *         the list is empty if the file matches expectations
     */
    private static List<String> validateCsv(Path filePath, String[] expectedCols, ColumnType[] expectedTypes) {
        List<String> report = new ArrayList<>();
        boolean[] badColumn = new boolean[expectedTypes.length];

        try {
            // Build options forcing schema
            CsvReadOptions opts = CsvReadOptions.builder(filePath.toFile())
                    .header(true)
                    .columnTypes(expectedTypes)
                    .build();

            Table table = Table.read().usingOptions(opts);

            // --- validate header names ---
            String[] headers = table.columnNames().toArray(new String[0]);
            if (headers.length != expectedCols.length) {
                report.add("Header length mismatch: expected " + expectedCols.length +
                        " but found " + headers.length);
            }
            for (int i = 0; i < Math.min(headers.length, expectedCols.length); i++) {
                if (!headers[i].equals(expectedCols[i])) {
                    report.add("Header mismatch at column " + i + ": expected '" +
                            expectedCols[i] + "', found '" + headers[i] + "'");
                }
            }

            // --- validate column types ---
            for (int i = 0; i < Math.min(table.columnCount(), expectedTypes.length); i++) {
                ColumnType expectedType = expectedTypes[i];
                ColumnType actualType = table.column(i).type();

                // 1. Allow empty column (skip hard error, but warn)
                if (table.column(i).isEmpty()) {
                    report.add("⚠️ Column '" + expectedCols[i] +
                            "' is empty (expected " + expectedType.name() + ")");
                    continue;
                }

                // 2. Allow DOUBLE to match INTEGER
                if (expectedType == ColumnType.DOUBLE && actualType == ColumnType.INTEGER) {
                    continue;
                }

                // 3. Allow STRING <-> LOCAL_DATE_TIME flexibility
                if (expectedType == ColumnType.STRING && actualType == ColumnType.LOCAL_DATE_TIME) {
                    continue;
                }
                if (expectedType == ColumnType.LOCAL_DATE_TIME && actualType == ColumnType.STRING) {
                    report.add("⚠️ Column '" + expectedCols[i] +
                            "' is STRING but should be LOCAL_DATE_TIME");
                    continue;
                }

                // 4. Strict mismatch
                if (!expectedType.equals(actualType)) {
                    report.add("Type mismatch in column '" + expectedCols[i] +
                            "': expected " + expectedType.name() +
                            " but found " + actualType.name());
                }
            }

        } catch (Exception e) {
            report.add("I/O error: " + e.getMessage());
        }

        return report;
    }
}