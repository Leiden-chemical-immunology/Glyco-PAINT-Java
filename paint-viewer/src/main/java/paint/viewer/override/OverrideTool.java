package paint.viewer.override;

import paint.shared.io.RecordingsTableIO;
import paint.shared.io.SquaresTableIO;
import paint.viewer.model.RecordingEntry;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static paint.shared.constants.PaintConstants.*;
import static paint.viewer.override.RecordingOverrideApplier.loadRecordingOverride;
import static paint.viewer.override.SquareOverrideApplier.loadSquareOverride;

public class OverrideTool {

    public static void main(String[] args) {

        String extension;


        if (args.length != 1 && args.length != 2) {
            System.err.println("Usage: java -cp paint-viewer.jar paint.viewer.cli.OverrideTool <Project-Path> <Extension>");
            System.exit(1);
        }

        Path projectPath = Paths.get(args[0]);

        if (args.length == 2) {
            extension = "-" + args[1];
        } else {
            extension = "-override";
        }
        processOverride(projectPath, extension);
    }

    public static void processOverride(Path projectPath, String extension) {
        // Does the project root exist?
        if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            System.err.println("Error: Project path does not exist or is not a directory: " + projectPath);
            System.exit(2);
        }

        ////////////////////////////////////////
        // Process recordings
        ////////////////////////////////////////

        // Read the Recordings if it exists
        Table recordingsTable;
        Path  recordingsCsvPath = projectPath.resolve(RECORDINGS_CSV);

        if (!Files.exists(recordingsCsvPath)) {
            System.err.println("Info: Recordings file does not exist: " + recordingsCsvPath);
            System.exit(2);
        }

        RecordingsTableIO recordingsTableIO = new RecordingsTableIO();
        try {
            recordingsTable = recordingsTableIO.readCsv(recordingsCsvPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Read the Recordings Override if it exists
        Path recordingOverridePath = projectPath.resolve("Viewer").resolve("Recording Override.csv");
        List<RecordingOverride> recordingOverrides = null;
        if (Files.exists(recordingOverridePath)) {
            recordingOverrides = loadRecordingOverride(recordingOverridePath);
        }

        // Apply the overrides and save the recordings
        if (recordingOverrides != null) {
            applyRecordingOverride(recordingsTable, recordingOverrides);

            String name                    = RECORDINGS_CSV.replaceFirst("(?i)\\.csv$", "");   // remove .csv (any case)
            name                           = name + extension + ".csv";
            Path overrideRecordingsCsvPath = projectPath.resolve(name);
            try {
                recordingsTableIO.writeCsv(recordingsTable, overrideRecordingsCsvPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        ////////////////////////////////////////
        // Process squares
        ////////////////////////////////////////

        // Read the Squares if it exists
        Table squaresTable;
        Path  squaresCsvPath = projectPath.resolve("Squares.csv");

        if (!Files.exists(squaresCsvPath)) {
            System.err.println("Info: Squares file does not exist: " + squaresCsvPath);
            System.exit(2);
        }

        SquaresTableIO squaresTableIO = new SquaresTableIO();
        try {
            squaresTable   = squaresTableIO.readCsv(squaresCsvPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Read the Squares Override if it exists
        Path squareOverridePath = projectPath.resolve("Viewer").resolve("Square Override.csv");
        List<SquareOverride> squareOverrides = null;
        if (Files.exists(squareOverridePath)) {
            squareOverrides = loadSquareOverride(squareOverridePath);
        }

        // Apply the overrides and save the recordings
        if (squareOverrides != null) {
            applySquareOverride(squaresTable, squareOverrides);

            String name                 = SQUARES_CSV.replaceFirst("(?i)\\.csv$", "");   // remove .csv (any case)
            name                        = name + extension + ".csv";
            Path overrideSquaresCsvPath = projectPath.resolve(name);
            try {
                squaresTableIO.writeCsv(squaresTable, overrideSquaresCsvPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void applyRecordingOverride(Table recordingsTable, List<RecordingOverride> overrides) {

        // Build a fast lookup by (experimentName, recordingName)
        // Key format: expName + "§" + recName
        java.util.Map<String, RecordingOverride> map = new java.util.HashMap<>();

        for (RecordingOverride override : overrides) {
            String key = key(override.getExperimentName(), override.getRecordingName());
            map.put(key, override);
        }

        int applied = 0;

        for (int row = 0; row < recordingsTable.rowCount(); row++) {

            String experimentName = recordingsTable.stringColumn(EXPERIMENT_NAME).get(row);
            String recordingName  = recordingsTable.stringColumn(RECORDING_NAME).get(row);

            String key = key(experimentName, recordingName);
            RecordingOverride override = map.get(key);

            if (override != null) {
                // Update columns directly in the table
                recordingsTable.doubleColumn(MIN_REQUIRED_DENSITY_RATIO).set( row, override.getMinRequiredDensityRatio());
                recordingsTable.doubleColumn(MIN_REQUIRED_R_SQUARED).set(     row, override.getMinRequiredRSquared());
                recordingsTable.doubleColumn(MAX_ALLOWABLE_VARIABILITY).set(  row, override.getMaxAllowableVariability());
                recordingsTable.stringColumn(NEIGHBOUR_MODE).set(             row, override.getNeighbourMode());
                applied++;
            }
        }

        System.out.println("Recording overrides applied: " + applied);
    }

    public static void applySquareOverride(Table squaresTable, List<SquareOverride> overrides) {

        // Build lookup map: "exp§rec§squareNumber" → SquareOverride
        java.util.Map<String, SquareOverride> map = new java.util.HashMap<>();

        for (SquareOverride override : overrides) {
            String key = key(
                    override.getExperimentName(),
                    override.getRecordingName(),
                    override.getSquareNumber()
            );
            map.put(key, override);
        }

        int applied = 0;

        for (int row = 0; row < squaresTable.rowCount(); row++) {

            String experimentName = squaresTable.stringColumn(EXPERIMENT_NAME).get(row);
            String recordingName  = squaresTable.stringColumn(RECORDING_NAME).get(row);
            int    square         = squaresTable.intColumn(SQUARE_NUMBER).get(row);

            String key = key(experimentName, recordingName, square);
            SquareOverride override = map.get(key);

            if (override != null) {
                squaresTable.intColumn(CELL_ID).set(row, override.getCellId());
                applied++;
            }
        }

        System.out.println("Square overrides applied: " + applied);
    }

    private static String key(String exp, String rec, int sq) {
        return exp + "§" + rec + "§" + sq;
    }

    private static String key(String exp, String rec) {
        return exp + "§" + rec;
    }

}