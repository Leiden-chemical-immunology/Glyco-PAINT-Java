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
import paint.shared.utils.PaintLogger;
import paint.viewer.override.recording_exclude.RecordingExclude;
import paint.viewer.override.recording_override.RecordingOverride;
import paint.viewer.override.square_override.SquareOverride;
import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static paint.shared.constants.PaintFileNames.*;
import static paint.shared.io.MainIOInterface.*;
import static paint.shared.utils.SharedSquareUtils.applyVisibilityFilterOnRecording;
import static paint.viewer.override.recording_exclude.RecordingExcludeApplier.*;

import static paint.shared.constants.PaintStringConstants.*;

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

        Table                   recordingsTable;
        Table                   squaresTable;
        Table                   tracksTable             = null;

        Path                    recordingsCsvPath       = projectPath.resolve(RECORDINGS_CSV);
        Path                    squaresCsvPath          = projectPath.resolve(SQUARES_CSV);
        Path                    tracksCsvPath           = projectPath.resolve(TRACKS_CSV);

        Path                    recordingOverridePath   = projectPath.resolve("Viewer").resolve("Recording Override.csv");
        Path                    recordingExcludePath    = projectPath.resolve("Viewer").resolve("Recording Exclude.csv");
        Path                    squareOverridePath      = projectPath.resolve("Viewer").resolve("Square Override.csv");

        List<RecordingOverride> recordingOverrides;
        List<RecordingExclude>  recordingExcludes;
        List<SquareOverride>    squareOverrides;

        boolean                 recordingsUpdated       = false;
        boolean                 squaresUpdated          = false;
        boolean                 tracksUpdated           = false;

        boolean                 recordingExcludeExists  = Files.exists(recordingExcludePath);
        boolean                 recordingOverrideExists = Files.exists(recordingOverridePath);
        boolean                 squareOverrideExists    = Files.exists(squareOverridePath);

        // Start logging
        PaintLogger.infof("Applying manual overrides and exclusions on Squares, Recordings and Tracks");
        PaintLogger.blankline();

        // Does the project root exist?
        if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            PaintLogger.errorf("Error: Specified Project root '%s' does not exist or is not a directory", projectPath);
            System.exit(2);
        }


        ////////////////////////////////////////
        // Read Squares and Recordings
        ////////////////////////////////////////

        // Read Squares
        if (!Files.exists(squaresCsvPath)) {
            System.err.println("Squares file does not exist: " + squaresCsvPath);
            PaintLogger.errorf("Squares file '%s' does not exist", squaresCsvPath);
            throw new RuntimeException();
        }
        try {
            squaresTable = readSquaresTable(projectPath);
        } catch (Exception e) {
            PaintLogger.errorf("Squares file '%s' could not be read.",  squaresCsvPath);
            throw new RuntimeException(e);
        }

        // Read Recordings
        if (!Files.exists(recordingsCsvPath)) {
            System.err.println("Recordings file does not exist: " + recordingsCsvPath);
            PaintLogger.errorf("Recordings file '%s' does not exist.",  recordingsCsvPath);
            throw new RuntimeException();
        }
        try {
            recordingsTable = readRecordingsTable(projectPath);
        } catch (Exception e) {
            PaintLogger.errorf("Recordings file '%s' could not be read.",  recordingsCsvPath);
            throw new RuntimeException(e);
        }

        // Read Tracks but only if there is a 'recording exclude' file
        if (recordingExcludeExists) {
            if (!Files.exists(tracksCsvPath)) {
                System.err.println("Tracks file does not exist: " + tracksCsvPath);
                PaintLogger.errorf("Tracks file '%s' does not exist.", tracksCsvPath);
                throw new RuntimeException();
            }
            try {
                tracksTable = readTracksTable(projectPath);
            } catch (Exception e) {
                PaintLogger.errorf("Tracks file '%s' could not be read.", recordingsCsvPath);
                throw new RuntimeException(e);
            }
        }

        /////////////////////////////////////////////
        // Process recordings overrides and excludes
        /////////////////////////////////////////////

        // Read the Recordings Override if it exists
        if (recordingOverrideExists) {
            recordingOverrides = loadRecordingOverride(recordingOverridePath);
            if (!recordingOverrides.isEmpty()) {
                applyRecordingOverrides(recordingsTable, squaresTable, recordingOverrides);
                recordingsUpdated  = true;
                squaresUpdated     = true;
            }
        }
        else {
            PaintLogger.infof("No Recording Overrides to apply.");
        }

        // Read the Recordings Exclude if it exists
        if (recordingExcludeExists) {
            recordingExcludes = loadRecordingExclude(recordingExcludePath);
            if (!recordingExcludes.isEmpty()) {
                applyRecordingExcludes(recordingsTable, projectPath);
                recordingsUpdated  = true;
                squaresUpdated     = true;

                // Delete the corresponding records from Squares and tracks
                Set<String> excludedRecordingNames = recordingExcludes.stream()
                                                                      .map(RecordingExclude::getRecordingName)
                                                                      .collect(Collectors.toSet());

                // Update Squares
                StringColumn recordingSquaresCol = squaresTable.stringColumn("Recording Name");
                squaresTable = squaresTable.where(recordingSquaresCol.isNotIn(excludedRecordingNames));

                // Update Tracks
                StringColumn recordingTracksCol = tracksTable.stringColumn("Recording Name");
                tracksTable    = tracksTable.where(recordingTracksCol.isNotIn(excludedRecordingNames));
                tracksUpdated  = true;
            }
        }
        else {
            PaintLogger.blankline();
            PaintLogger.infof("No Recording Excludes to apply.");
        }

        ////////////////////////////////////////
        // Process squares
        ////////////////////////////////////////

        if (squareOverrideExists) {
            squareOverrides = loadSquareOverride(squareOverridePath);
            if (!squareOverrides.isEmpty()) {
                applySquareOverrides(squaresTable, squareOverrides);
                squaresUpdated = true;
            }
        }
        else {
            PaintLogger.blankline();
            PaintLogger.infof("No Square Overrides to apply.");
        }

        // Save files
        String fileName;
        if (tracksUpdated || squaresUpdated || recordingsUpdated) {
            PaintLogger.blankline();
            PaintLogger.infof("Saving updated files:");
        }
        if (recordingsUpdated) {
            fileName = RECORDINGS_CSV.replaceFirst("(?i)\\.csv$", "");   // remove .csv (any case)
            fileName = fileName + extension + ".csv";
            Path overrideRecordingsCsvPath = projectPath.resolve(fileName);
            writeSpecificRecordingsFile(overrideRecordingsCsvPath, recordingsTable);
            PaintLogger.infof("     Saved updated Recordings file : " + overrideRecordingsCsvPath);
        }

        if (squaresUpdated) {
            fileName = SQUARES_CSV.replaceFirst("(?i)\\.csv$", "");   // remove .csv (any case)
            fileName = fileName + extension + ".csv";
            Path overrideSquaresCsvPath = projectPath.resolve(fileName);
            writeSpecificSquaresFile(overrideSquaresCsvPath, squaresTable);
            PaintLogger.infof("     Saved updated Squares file    : " + overrideSquaresCsvPath);
        }

        if (tracksUpdated) {
            fileName = TRACKS_CSV.replaceFirst("(?i)\\.csv$", "");   // remove .csv (any case)
            fileName = fileName + extension + ".csv";
            Path overrideTracksCsvPath = projectPath.resolve(fileName);
            writeSpecificTracksFile(overrideTracksCsvPath, tracksTable);
            PaintLogger.infof("     Saved updated Tracks file     : " + overrideTracksCsvPath);
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
    public static void applyRecordingOverrides(Table recordingsTable, Table squaresTable, List<RecordingOverride> overrides) {

        Map<String, RecordingOverride> map = new HashMap<>();
        for (RecordingOverride override : overrides) {
            map.put(key(override.getExperimentName(), override.getRecordingName()), override);
        }

        PaintLogger.infof("Processing Recording overrides - different selection criteria for squares in recording:" );
        PaintLogger.blankline();

        for (int row = 0; row < recordingsTable.rowCount(); row++) {
            String            experimentName = recordingsTable.stringColumn(EXPERIMENT_NAME).get(row);
            String            recordingName  = recordingsTable.stringColumn(RECORDING_NAME).get(row);
            RecordingOverride override       = map.get(key(experimentName, recordingName));

            if (override != null) {
                // Update columns directly in the table
                recordingsTable.doubleColumn(MIN_REQUIRED_DENSITY_RATIO).set( row, override.getMinRequiredDensityRatio());
                recordingsTable.doubleColumn(MIN_REQUIRED_R_SQUARED).set(     row, override.getMinRequiredRSquared());
                recordingsTable.doubleColumn(MAX_ALLOWABLE_VARIABILITY).set(  row, override.getMaxAllowableVariability());
                recordingsTable.stringColumn(NEIGHBOUR_MODE).set(             row, override.getNeighbourMode());

                // LOG full detail for this recording
                PaintLogger.infof("    Applied Recording override on %s", recordingName);

                PaintLogger.infof("          Density Ratio:  %-6.2f",  override.getMinRequiredDensityRatio());
                PaintLogger.infof("          R²:             %-6.2f",  override.getMinRequiredRSquared());
                PaintLogger.infof("          Variability:    %-6.2f ", override.getMaxAllowableVariability());
                PaintLogger.infof("          Neighbour Mode: %-6s",    override.getNeighbourMode());
                PaintLogger.blankline();

                // Now apply the filter criteria to the Squares of the Recordings

                List<Square> squareList = squareTableToList(squaresTable);
                applyVisibilityFilterOnRecording(
                        squareList,
                        recordingName,
                        override.getMinRequiredDensityRatio(),
                        override.getMaxAllowableVariability(),
                        override.getMinRequiredRSquared(),
                        override.getNeighbourMode());

                Table updatedSquaresTable = squareListToTable(squareList);
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
    public static void applySquareOverrides(Table squaresTable, List<SquareOverride> overrides) {

        PaintLogger.infof("Processing Square overrides - different cell IDs for squares:");
        // Build lookup map: "exp§rec§squareNumber" → SquareOverride
        Map<String, SquareOverride> overrideCellIds = new java.util.HashMap<>();

        for (SquareOverride override : overrides) {
            String key = key(override.getExperimentName(),
                             override.getRecordingName(),
                             override.getSquareNumber());
            overrideCellIds.put(key, override);
        }

        int applied = 0;

        for (int row = 0; row < squaresTable.rowCount(); row++) {

            String experimentName = squaresTable.stringColumn(EXPERIMENT_NAME).get(row);
            String recordingName = squaresTable.stringColumn(RECORDING_NAME).get(row);
            int squareNumber = squaresTable.intColumn(SQUARE_NUMBER).get(row);

            String key = key(experimentName, recordingName, squareNumber);
            SquareOverride override = overrideCellIds.get(key);

            if (override != null) {
                squaresTable.intColumn(CELL_ID).set(row, override.getCellId());
                applied++;
            }
        }

        if (applied > 0) {
            Map<String, Long> squaresPerRecording =
                    overrides.stream()
                             .collect(Collectors.groupingBy(
                                     SquareOverride::getRecordingName,
                                     Collectors.counting()
                             ));

            PaintLogger.blankline();
            PaintLogger.infof("Applied squares overrides:");
            for (String rec : squaresPerRecording.keySet()) {
                PaintLogger.infof("                    %s: %d squares", rec, squaresPerRecording.get(rec));
            }
        }
    }



    public static void applyRecordingExcludes(Table recordingsTable, Path projectPath) {

        PaintLogger.infof("Processing Recordings that were exclude");
        Path csvPath = projectPath.resolve("Viewer").resolve("Recording Exclude.csv");

        List<RecordingExclude> excludes = loadRecordingExclude(csvPath);

        BooleanColumn excludeCol = recordingsTable.booleanColumn("Exclude");

        for (int i = 0; i < excludeCol.size(); i++) {
            excludeCol.set(i, false);
        }

        boolean first = true;
        for (RecordingExclude exclude : excludes) {
            StringColumn  nameCol      = recordingsTable.stringColumn("Recording Name");
            String        recordingName = exclude.getRecordingName();

            for (int row = 0; row < recordingsTable.rowCount(); row++) {
                if (recordingName.equals(nameCol.get(row))) {
                    excludeCol.set(row, true);
                    if (first) {
                        PaintLogger.blankline();
                        first = false;
                    }
                    PaintLogger.infof("    Recording excluded: " + recordingName);
                    break;
                }
            }
        }
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

    /**
     * Loads all rows from the `Recording Override.csv` file into a list of
     * RecordingOverride objects.
     *
     * @param csvFile path to Recording Override.csv
     * @return list of parsed RecordingOverride objects
     */
    public static List<RecordingOverride> loadRecordingOverride(Path csvFile) {

        List<RecordingOverride> list = new ArrayList<>();

        try {
            Table table = Table.read().csv(csvFile.toString());

            for (int i = 0; i < table.rowCount(); i++) {
                RecordingOverride recordingOverride = new RecordingOverride();

                recordingOverride.setExperimentName(          table.column(EXPERIMENT_NAME).get(i).toString());
                recordingOverride.setRecordingName(           table.column(RECORDING_NAME).get(i).toString());
                recordingOverride.setMinRequiredDensityRatio( Double.parseDouble(table.column(MIN_REQUIRED_DENSITY_RATIO).get(i).toString()));
                recordingOverride.setMinRequiredRSquared(     Double.parseDouble(table.column(MIN_REQUIRED_R_SQUARED).get(i).toString()));
                recordingOverride.setMaxAllowableVariability( Double.parseDouble(table.column(MAX_ALLOWABLE_VARIABILITY).get(i).toString()));
                recordingOverride.setNeighbourMode(           table.column(NEIGHBOUR_MODE).get(i).toString());

                list.add(recordingOverride);
            }
        } catch (Exception ex) {
            PaintLogger.errorf( "Error reading Recording Override.csv → " + ex.getMessage());
        }

        return list;
    }

    /**
     * Loads square overrides from "Square Override.csv".
     *
     * @param csvFile full path to the override file
     * @return a list of SquareOverride objects
     */
    public static List<SquareOverride> loadSquareOverride(Path csvFile) {

        List<SquareOverride> list = new ArrayList<>();

        try {
            Table table = Table.read().csv(csvFile.toString());

            for (int i = 0; i < table.rowCount(); i++) {

                SquareOverride squareOverride = new SquareOverride();

                squareOverride.setExperimentName(table.column(EXPERIMENT_NAME).get(i).toString());
                squareOverride.setRecordingName(table.column(RECORDING_NAME).get(i).toString());
                squareOverride.setSquareNumber(Integer.parseInt(table.column(SQUARE_NUMBER).get(i).toString()));
                squareOverride.setCellId(Integer.parseInt(table.column(CELL_ID).get(i).toString()));
                squareOverride.setTimestamp(table.column(TIME_STAMP).get(i).toString());

                list.add(squareOverride);
            }

        } catch (Exception ex) {
            PaintLogger.errorf("Error reading Square Override.csv → " + ex.getMessage());
        }

        return list;
    }

}