/*==============================================================================
 *  Class:        ExportOverridesFromViewer.java
 *  Package:      paint.viewer.override
 *
 *  PURPOSE:
 *    Standalone command-line utility for applying Viewer-generated override data
 *    to a PAINT project. It reads the project's core CSV files (Recordings,
 *    Squares, and optionally Tracks), loads override/exclude instructions from
 *    the <project>/Viewer folder, applies the mutations in-memory, and writes
 *    new CSV outputs with a safe suffix (default: "-override").
 *
 *  DESCRIPTION:
 *    ExportOverridesFromViewer performs up to three independent operations:
 *
 *      1) Recording overrides
 *         • Reads "Recording Override.csv" (if present)
 *         • Applies per-recording filtering thresholds (density ratio, R²,
 *           variability, neighbour mode)
 *         • Recomputes square visibility for affected recordings
 *
 *      2) Recording excludes
 *         • Reads "Recording Exclude.csv" (if present)
 *         • Sets the Recordings "Exclude" flag for listed recordings
 *         • Removes excluded recordings from Squares and Tracks tables
 *           (Tracks is only loaded/updated when an exclude file exists)
 *
 *      3) Square overrides
 *         • Reads "Square Override.csv" (if present)
 *         • Applies per-square Cell ID corrections
 *
 *    Outputs are written as new CSV files in the project root to avoid
 *    accidental overwrites of the original inputs.
 *
 *  KEY FEATURES:
 *    • Fully CLI-driven (no GUI dependencies).
 *    • Uses Tablesaw for CSV I/O and column-level mutations.
 *    • Applies recording overrides, recording excludes, and square overrides.
 *    • Recomputes visibility for squares when thresholds change.
 *    • Writes results to new files using a user-defined suffix.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-12-31
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
import static paint.viewer.override.recording_exclude.ImportRecordingExclude.*;

import static paint.shared.constants.PaintStringConstants.*;
import static paint.viewer.override.recording_override.ImportRecordingOverride.loadRecordingOverride;
import static paint.viewer.override.square_override.ImportSquareOverride.loadSquareOverride;


/**
 * Provides functionality to apply and export manual overrides and exclusions
 * from the PAINT viewer. This class processes recording-level overrides,
 * square-level overrides, and recording-level exclusions, merging them back
 * into the main project CSV files.
 */
public class ExportOverridesFromViewer {

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
            System.err.println("Usage: java -cp paint-viewer.jar paint.viewer.cli.ExportOverridesFromViewer <Project-Path> <Extension>");
            System.exit(1);
        }

        Path projectPath = Paths.get(args[0]);

        // Optional suffix for newly written CSVs
        if (args.length == 2) {
            extension = "-" + args[1];
        } else {
            extension = "-override";
        }

        exportOverrides(projectPath, extension);
    }

    /**
     * Executes the full override/exclude procedure on the given project path.
     *
     * <p>Inputs are read from the project root. Override/exclude instructions are
     * read from {@code <project>/Viewer}. Outputs are written back into the
     * project root with a suffix (default: {@code -override}).</p>
     *
     * @param projectPath project root directory
     * @param extension   suffix added to output CSV files (e.g. {@code -override})
     */
    public static void exportOverrides(Path projectPath, String extension) {

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
            // Throw (consistent with the other error paths in this method) rather
            // than System.exit: exportOverrides is called from the viewer GUI, where
            // exiting would kill the whole application. The GUI caller catches this.
            throw new RuntimeException("Project root does not exist or is not a directory: " + projectPath);
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
     * Applies all {@link RecordingOverride} rows to the recordings table and
     * recomputes square visibility for affected recordings.
     *
     * @param recordingsTable target Recordings table (mutated in-place)
     * @param squaresTable    target Squares table (mutated in-place)
     * @param overrides       parsed override rows
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
                long visBefore = squaresTable.where(squaresTable.stringColumn(RECORDING_NAME).isEqualTo(recordingName))
                                             .booleanColumn(VISIBLE).countTrue();

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

                long visAfter = squaresTable.where(squaresTable.stringColumn(RECORDING_NAME).isEqualTo(recordingName))
                                            .booleanColumn(VISIBLE).countTrue();
            }
        }
    }

    /**
     * Applies all {@link SquareOverride} rows to the squares table.
     *
     * <p>Only rows matching the composite key
     * {@code (experimentName, recordingName, squareNumber)} are updated.</p>
     *
     * @param squaresTable target Squares table (mutated in-place)
     * @param overrides    parsed override rows
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


    /**
     * Applies recording excludes from {@code Viewer/Recording Exclude.csv} by
     * setting the "Exclude" flag in the provided Recordings table.
     *
     * <p>This method does not delete rows from other tables; the caller may
     * choose to remove excluded recordings from Squares/Tracks.</p>
     *
     * @param recordingsTable target Recordings table (mutated in-place)
     * @param projectPath     project root directory
     */
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

    /** Composite key utility for (experiment, recording, squareNumber). */
    private static String key(String exp, String rec, int sq) {
        return exp + "§" + rec + "§" + sq;
    }

    /** Composite key utility for (experiment, recording). */
    private static String key(String exp, String rec) {
        return exp + "§" + rec;
    }

}