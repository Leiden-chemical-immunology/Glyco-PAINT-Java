package paint.shared.debug;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for validating the format of Paint project CSV files.
 *
 * <p>This version is self-contained: it defines its own schema arrays
 * (columns + types) instead of depending on PaintConstants and Tablesaw.
 * </p>
 */
public class ValidateProject {

    // ---------------------------------------------------
    // Local enum for schema typing (avoids Tablesaw)
    // ---------------------------------------------------
    private enum CsvType { STRING, INTEGER, DOUBLE, BOOLEAN, LOCAL_DATE_TIME }

    // Filenames (duplicated from PaintConstants)
    private static final String RECORDINGS_CSV = "All Recordings Java.csv";
    private static final String TRACKS_CSV     = "All Tracks Java.csv";
    private static final String SQUARES_CSV    = "All Squares Java.csv";
    private static final String EXPERIMENT_INFO_CSV = "Experiment Info.csv";

    // ---------------------------------------------------
    // Experiment info schema
    // ---------------------------------------------------
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

    // ---------------------------------------------------
    // Recording schema
    // ---------------------------------------------------
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

    // ---------------------------------------------------
    // Square schema
    // ---------------------------------------------------
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

    // ---------------------------------------------------
    // Track schema
    // ---------------------------------------------------
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

    // ---------------------------------------------------
    // Public methods
    // ---------------------------------------------------

    /**
     * Auto-discover experiment directories as immediate subfolders containing
     * experiment info CSVs.
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
     * Validate all CSV files of a given experiment folder.
     */
    public static List<String> validateExperiment(Path experimentDir, boolean checkSquares) {
        List<String> errors = new ArrayList<>();

        try {
            Path dataCsv = experimentDir.resolve(RECORDINGS_CSV);
            if (Files.exists(dataCsv)) {
                errors.addAll(validateCsv(dataCsv, RECORDING_COLS, RECORDING_TYPES));
            } else {
                errors.add("Missing required file: " + dataCsv);
            }

            Path expCsv = experimentDir.resolve(EXPERIMENT_INFO_CSV);
            if (Files.exists(expCsv)) {
                errors.addAll(validateCsv(expCsv, EXPERIMENT_INFO_COLS, EXPERIMENT_INFO_TYPES));
            } else {
                errors.add("Missing required file: " + expCsv);
            }

            if (checkSquares) {
                Path sqCsv = experimentDir.resolve(SQUARES_CSV);
                if (Files.exists(sqCsv)) {
                    errors.addAll(validateCsv(sqCsv, SQUARE_COLS, SQUARE_TYPES));
                } else {
                    errors.add("Missing required file: " + sqCsv);
                }
            }

            Path trackCsv = experimentDir.resolve(TRACKS_CSV);
            if (Files.exists(trackCsv)) {
                errors.addAll(validateCsv(trackCsv, TRACK_COLS, TRACK_TYPES));
            }

        } catch (Exception e) {
            errors.add("Unexpected error while validating " + experimentDir + ": " + e.getMessage());
        }

        return errors;
    }

    /**
     * Validate multiple experiments under a project root.
     */
    public static List<String> validateProject(Path projectRoot, List<String> experimentNames, boolean checkSquares) {
        List<String> allErrors = new ArrayList<>();

        for (String exp : experimentNames) {
            Path expDir = projectRoot.resolve(exp);
            if (Files.exists(expDir) && Files.isDirectory(expDir)) {
                List<String> expErrors = validateExperiment(expDir, checkSquares);
                for (String err : expErrors) {
                    allErrors.add("[" + exp + "] " + err);
                }
            } else {
                allErrors.add("[" + exp + "] ✖ Experiment folder not found: " + expDir);
            }
        }

        if (allErrors.isEmpty()) {
            allErrors.add("✔ Validation OK. Checked " + experimentNames.size() + " experiment(s).");
        }

        return allErrors;
    }

    // ---------------------------------------------------
    // Minimal CSV validator (only header check)
    // ---------------------------------------------------
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

    // ---------------------------------------------------
    // Pretty-print report
    // ---------------------------------------------------
    public static void printReport(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            System.out.println("✔ Validation OK.");
            return;
        }

        System.out.println("✖ Validation FAILED. Found " + errors.size() + " issue(s):");
        for (String err : errors) {
            System.out.println("  - " + err);
        }
    }
}