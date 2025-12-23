/*==============================================================================
 *  Class:        OverrideTool.java
 *  Package:      paint.viewer.override
 *
 *  PURPOSE:
 *    Standalone command-line utility for applying Recording and Square override
 *    CSV files to a PAINT project. This tool reads the project's Recordings.csv
 *    and Squares.csv files, loads applicable overrides from the Viewer folder,
 *    applies all modifications, and writes new CSV files suffixed with a
 *    user-defined extension (default: "-override").
 *
 *  DESCRIPTION:
 *    The OverrideTool performs four major operations:
 *
 *      1. Loads the Squares.csv and Recordings.csv tables for a project.
 *      2. Loads "Recording Override.csv" and "Square Override.csv" files when
 *         present in the project's Viewer directory.
 *      3. Applies the loaded overrides:
 *          • Recording overrides: update filtering thresholds (density ratio,
 *            R², variability, neighbour mode) and recompute square visibility.
 *          • Square overrides: update cell assignments for individual squares.
 *      4. Writes new CSV files containing the overridden data, preserving the
 *         original versions and preventing accidental overwrites.
 *
 *    The tool is typically executed automatically when the Viewer closes with
 *    "Import Overrides" enabled, but may be run manually for batch processing.
 *
 *  KEY FEATURES:
 *    • Fully CLI-driven, no GUI dependencies.
 *    • Reads/writes Tablesaw CSV structures.
 *    • Atomic override handling for both recordings and squares.
 *    • Recomputes square visibility after filtering thresholds are replaced.
 *    • Produces new CSV files with a safe extension suffix.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-11-17
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
==============================================================================*/

package paint.viewer.override;

import paint.shared.objects.Square;
import paint.viewer.override.recording_exclude.RecordingExclude;
import paint.viewer.override.recording_override.RecordingOverride;
import paint.viewer.override.square_override.SquareOverride;
import tech.tablesaw.api.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static paint.shared.io.MainIOInterface.*;
import static paint.shared.utils.SharedSquareUtils.applyVisibilityFilterOnRecording;
import static paint.viewer.override.recording_exclude.RecordingExcludeApplier.applyRecordingExclude;
import static paint.viewer.override.recording_exclude.RecordingExcludeApplier.loadRecordingExclude;
import static paint.viewer.override.recording_override.RecordingOverrideApplier.loadRecordingOverride;
import static paint.viewer.override.square_override.SquareOverrideApplier.loadSquareOverride;

import static paint.shared.constants.PaintStringConstants.*;
import static paint.shared.constants.PaintFileNames.RECORDINGS_CSV;
import static paint.shared.constants.PaintFileNames.SQUARES_CSV;

/**
 * Command-line utility for applying both Recording and Square override files
 * to a PAINT project. This tool processes:
 * <ul>
 *   <li>{@code Recording Override.csv}</li>
 *   <li>{@code Square Override.csv}</li>
 * </ul>
 * and generates updated CSV files containing the applied corrections.
 * <p>
 * Intended for automatic or manual batch override processing.
 */
public class OverrideTool {

    /**
     * Entry point for command-line execution.
     *
     * @param args 1 or 2 arguments:
     *             <ul>
     *               <li>args[0] → project path</li>
     *               <li>args[1] → optional extension suffix</li>
     *             </ul>
     */
    public static void main(String[] args) {

        String extension;

        if (args.length != 1 && args.length != 2) {
            System.err.println("Usage: java -cp paint-viewer.jar paint.viewer.cli.OverrideTool <Project-Path> <Extension>");
            System.exit(1);
        }

        Path projectPath = Paths.get(args[0]);

        // Optional suffix for newly written CSVs
        if (args.length == 2) {
            extension = "-" + args[1];
        } else {
            extension = "-override";
        }
        processOverride(projectPath, extension);
    }

    /**
     * Executes the full override procedure on the given project path.
     *
     * @param projectPath project root directory
     * @param extension   extension added to output CSV files (e.g. "-override")
     */
    public static void processOverride(Path projectPath, String extension) {
        // Does the project root exist?
        if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            System.err.println("Error: Project path does not exist or is not a directory: " + projectPath);
            System.exit(2);
        }

        ////////////////////////////////////////
        // Read Squares and Recordings
        ////////////////////////////////////////

        // Read the Squares if it exists
        Table squaresTable;
        Path  squaresCsvPath = projectPath.resolve("Squares.csv");

        if (!Files.exists(squaresCsvPath)) {
            System.err.println("Info: Squares file does not exist: " + squaresCsvPath);
            System.exit(2);
        }

        try {
            squaresTable = readSquaresTable(projectPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Read the Recordings if it exists
        Table recordingsTable;
        Path  recordingsCsvPath = projectPath.resolve(RECORDINGS_CSV);

        if (!Files.exists(recordingsCsvPath)) {
            System.err.println("Info: Recordings file does not exist: " + recordingsCsvPath);
            System.exit(2);
        }

        try {
            recordingsTable = readRecordingsTable(projectPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ////////////////////////////////////////
        // Process recordings
        ////////////////////////////////////////

        // Read the Recordings Override if it exists
        Path recordingOverridePath = projectPath.resolve("Viewer").resolve("Recording Override.csv");
        List<RecordingOverride> recordingOverrides = null;
        if (Files.exists(recordingOverridePath)) {
            recordingOverrides = loadRecordingOverride(recordingOverridePath);
        }

        // Apply the overrides and save the recordings
        if (recordingOverrides != null) {
            applyRecordingOverride(recordingsTable, squaresTable, recordingOverrides);

            String name                    = RECORDINGS_CSV.replaceFirst("(?i)\\.csv$", "");   // remove .csv (any case)
            name                           = name + extension + ".csv";
            Path overrideRecordingsCsvPath = projectPath.resolve(name);
            writeSpecificRecordingsFile(overrideRecordingsCsvPath, recordingsTable);
        }


        ////////////////////////////////////////
        // Process Exclude recordings
        ////////////////////////////////////////

        // Read the Recordings Exclude if it exists
        Path recordingExcludePath = projectPath.resolve("Viewer").resolve("Recording Exclude.csv");
        List<RecordingExclude> recordingExcludes = null;
        if (Files.exists(recordingExcludePath)) {
            recordingExcludes = loadRecordingExclude(recordingExcludePath);
        }

        // Apply the overrides and save the recordings
        if (recordingExcludes != null) {
            applyRecordingExclude(recordingsTable, projectPath);
        }


        ////////////////////////////////////////
        // Process squares
        ////////////////////////////////////////

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
            writeSpecificSquaresFile(overrideSquaresCsvPath, squaresTable);
        }
    }

    /**
     * Applies all RecordingOverride rows to the recordings table and recalculates
     * visibility of squares for the affected recordings.
     *
     * @param recordingsTable target recordings table
     * @param squaresTable    target squares table
     * @param overrides       list of RecordingOverride objects
     */
    public static void applyRecordingOverride(Table recordingsTable, Table squaresTable, List<RecordingOverride> overrides) {

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

            String            key      = key(experimentName, recordingName);
            RecordingOverride override = map.get(key);

            if (override != null) {
                // Update columns directly in the table
                recordingsTable.doubleColumn(MIN_REQUIRED_DENSITY_RATIO).set( row, override.getMinRequiredDensityRatio());
                recordingsTable.doubleColumn(MIN_REQUIRED_R_SQUARED).set(     row, override.getMinRequiredRSquared());
                recordingsTable.doubleColumn(MAX_ALLOWABLE_VARIABILITY).set(  row, override.getMaxAllowableVariability());
                recordingsTable.stringColumn(NEIGHBOUR_MODE).set(             row, override.getNeighbourMode());
                applied++;

                // Now apply the filter criteria to the Squares of the Recordings

                List<Square> squares = squareTableToList(squaresTable);
                applyVisibilityFilterOnRecording(
                        squares,
                        recordingName,
                        override.getMinRequiredDensityRatio(),
                        override.getMinRequiredRSquared(),
                        override.getMaxAllowableVariability(),
                        override.getNeighbourMode());

                Table updatedSquaresTable = squareListToTable(squares);
                squaresTable.clear();                   // This is a trick to ensure that old references are not invalidated
                squaresTable.append(updatedSquaresTable);
            }
        }
    }

    /**
     * Applies all SquareOverride rows to the squares table.
     * Only squares matching (experiment, recording, squareNumber) are updated.
     *
     * @param squaresTable target squares table
     * @param overrides    list of SquareOverride objects
     */
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

        // PaintLogger.infof("Square overrides applied: " + applied);
    }

    /**
     * Composite key utility for (experiment, recording, square).
     */
    private static String key(String exp, String rec, int sq) {
        return exp + "§" + rec + "§" + sq;
    }

    /**
     * Composite key utility for (experiment, recording).
     */
    private static String key(String exp, String rec) {
        return exp + "§" + rec;
    }
}