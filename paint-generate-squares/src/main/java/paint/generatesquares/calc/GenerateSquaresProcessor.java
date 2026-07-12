/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.generatesquares.calc;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.objects.Experiment;
import paint.shared.objects.Project;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.objects.Track;
import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static paint.shared.constants.PaintStringConstants.*;
import static paint.shared.io.ExperimentDataLoader.*;
import static paint.shared.io.MainIOInterface.*;
import static paint.shared.utils.Miscellaneous.formatDuration;
import static paint.shared.utils.SharedSquareUtils.filterTracksInSquare;

import static paint.shared.constants.PaintGeometry.IMAGE_HEIGHT;
import static paint.shared.constants.PaintGeometry.IMAGE_WIDTH;

public class GenerateSquaresProcessor {

    /**
     * Processes an experiment to generate square regions for each recording, compute attributes,
     * and compile data tables for all squares and tracks. The method applies geometric segmentation,
     * assigns tracks to generated squares, and calculates both square-level and recording-level attributes.
     * Finally, it writes compiled results to the file system.
     *
     * @param project        the project containing configurations and experiment data
     * @param experimentName the name of the experiment to process
     */
    public static void generateSquaresForExperiment(Project project, String experimentName) throws IOException {

        GenerateSquaresConfig generateSquaresConfig = project.getGenerateSquaresConfig();
        LocalDateTime         start                 = LocalDateTime.now();

        // ── LOAD ──────────────────────────────────────────────────────────────────────
        PaintLogger.debugf("Loading Experiment '%s'", experimentName);

        Experiment experiment = loadExperiment(
                project.getProjectRootPath(),
                experimentName,
                false,   // Don't load Squares
                true     // But do load Tracks
        );

        if (experiment == null) {
            PaintLogger.errorf("Failed to load experiment: %s", experimentName);
            return;
        }

        PaintLogger.infof("Starting processing experiment '%s'", experimentName);

        Path experimentPath = project.getProjectRootPath()
                                     .resolve(experiment.getExperimentName());

        // ── COMPUTE ───────────────────────────────────────────────────────────────────
        // Everything scientific happens here, in memory. It reads nothing and writes
        // nothing, so it can be tested without a project directory.
        if (!SquareGenerationService.computeExperiment(experiment, generateSquaresConfig, experimentPath)) {
            return;  // cancelled; the service has already said why
        }

        Duration duration = Duration.between(start, LocalDateTime.now());
        PaintLogger.infof("Finished processing experiment '%s' in %s",
                          experimentName, formatDuration(duration));
        PaintLogger.blankline();

        if (Thread.currentThread().isInterrupted()) {
            PaintLogger.infof("Cancelled before writing output for %s", experimentName);
            return;
        }

        // ── WRITE ─────────────────────────────────────────────────────────────────────
        writeSquares(experimentPath, compileAllSquares(experiment));
        writeRecordings(experimentPath, experiment.getRecordings());
        writeTracks(experimentPath, compileAllTracks(experiment).sortOn(RECORDING_NAME, TRACK_ID));
    }

    /**
     * Segments a single recording into square regions based on configuration.
     *
     * @param recording             the recording for which squares are to be generated
     * @param generateSquaresConfig the configuration specifying the number of squares and related parameters
     * @return a list of generated {@link Square} objects
     */
    public static List<Square> generateSquaresForRecording(Recording recording,
            GenerateSquaresConfig generateSquaresConfig) {

        // Total number of squares per recording, and the side of the grid that holds them.
        int numberOfSquaresInRecording = generateSquaresConfig.getNumberOfSquaresInRecording();
        int gridSize                   = generateSquaresConfig.getGridSize();

        List<Square> squares      = new ArrayList<>();
        double       squareWidth  = IMAGE_WIDTH  / gridSize;
        double       squareHeight = IMAGE_HEIGHT / gridSize;

        int squareNumber = 0;
        for (int rowNumber = 0; rowNumber < gridSize; rowNumber++) {
            for (int columnNumber = 0; columnNumber < gridSize; columnNumber++) {
                double X0 = columnNumber * squareWidth;
                double Y0 = rowNumber    * squareHeight;
                double X1 = (columnNumber + 1) * squareWidth;
                double Y1 = (rowNumber    + 1) * squareHeight;

                squares.add(new Square(
                        recording.getRecordingName() + '-' + squareNumber,
                        recording.getExperimentName(),
                        recording.getRecordingName(),
                        squareNumber,
                        rowNumber,
                        columnNumber,
                        X0,
                        Y0,
                        X1,
                        Y1
                ));

                squareNumber++;
            }
        }
        return squares;
    }

    /**
     * Assigns tracks to the predefined square regions of a recording.
     * It processes the tracks table of the recording, assigns each track to the relevant square,
     * updates the square attributes, and compiles a complete tracks table for the recording.
     *
     * @param recording             the {@code Recording} instance containing track and square data.
     *                              The method modifies this object by assigning tracks to the corresponding squares
     *                              and updating their track-related attributes.
     * @param generateSquaresConfig the configuration specifying the number of squares and related parameters
     */
    public static void assignTracksToSquares(Recording recording, GenerateSquaresConfig generateSquaresConfig) throws IOException {

        Table tracksOfRecording   = recording.getTracksTable();
        Table recordingTrackTable = newEmptyTrackTable();

        int numberOfSquaresInRecording = generateSquaresConfig.getNumberOfSquaresInRecording();
        int gridSize                   = generateSquaresConfig.getGridSize();

        int incrementalTrackCount = 0;

        PaintLogger.debugf("Assigning tracks to squares (%d total tracks)",
                           tracksOfRecording.rowCount());

        // ------------------------------------------------------------------
        // DEBUG CSV SETUP (developer diagnostic; off unless explicitly enabled)
        // ------------------------------------------------------------------
        // Enable per run with -Dpaint.debug.dumpTrackAssignmentCsv=true (same convention as
        // the regression gate's -Dpaint.* switches). Deliberately NOT a configuration key:
        // it is not a user option, and a config entry could silently linger switched on in
        // a project. Read at call time, so it is never frozen at class-load.
        final boolean dumpTrackAssignmentCsv = Boolean.getBoolean("paint.debug.dumpTrackAssignmentCsv");

        Path debugCsvPath = null;

        if (dumpTrackAssignmentCsv) {
            Path debugDirPath = Paths.get(System.getProperty("user.home")).resolve("Downloads").resolve("Debug");
            Files.createDirectories(debugDirPath);
            debugCsvPath = debugDirPath.resolve("all_square_tracks.csv");

            // Write header once
            Files.write(
                    debugCsvPath,
                    "RecordingName,Square,TrackId,X,Y,Duration,MaxSpeed,MedianSpeed,Displacement,Row,Col\n".getBytes(),
                    StandardOpenOption.CREATE
            );
        }
        // ------------------------------------------------------------------

        // We optimize track assignment by calculating the square index for each track directly.
        // O(N_tracks) instead of O(N_squares * N_tracks).

        List<Track> allTracks = trackTableToList(tracksOfRecording);
        Map<Integer, List<Track>> tracksBySquare = new HashMap<>();

        double squareWidth  = IMAGE_WIDTH  / gridSize;
        double squareHeight = IMAGE_HEIGHT / gridSize;

        for (Track track : allTracks) {
            double tx = track.getTrackXLocation();
            double ty = track.getTrackYLocation();

            int col = (int) (tx / squareWidth);
            int row = (int) (ty / squareHeight);

            // Boundary handling (ensure it doesn't exceed grid dimensions)
            if (col >= gridSize) {
                col = gridSize - 1;
            }
            if (row >= gridSize) {
                row = gridSize - 1;
            }
            if (col < 0) {
                col = 0;
            }
            if (row < 0) {
                row = 0;
            }

            int squareIndex = row * gridSize + col;
            tracksBySquare.computeIfAbsent(squareIndex, k -> new ArrayList<>()).add(track);
        }

        int labelNumber = 0;
        for (Square square : recording.getSquaresOfRecording()) {
            int squareIndex = square.getSquareNumber();
            List<Track> tracksInSquare = tracksBySquare.getOrDefault(squareIndex, Collections.emptyList());

            incrementalTrackCount += tracksInSquare.size();

            // Update the fields on each Track
            for (Track track : tracksInSquare) {
                track.setSquareNumber(squareIndex);
                track.setLabelNumber(labelNumber);
            }

            Table updatedSquareTracks = trackListToTable(tracksInSquare);
            recordingTrackTable.append(updatedSquareTracks);

            // Update the square
            square.setTracksList(tracksInSquare);
            square.setTracksTable(updatedSquareTracks);
            square.setNumberOfTracks(tracksInSquare.size());

            // ------------------------------------------------------------------
            // 🔥 DEBUG CSV APPEND (only when flag enabled)
            // ------------------------------------------------------------------
            if (dumpTrackAssignmentCsv && debugCsvPath != null) {
                StringBuilder sb = new StringBuilder();
                for (Track track : tracksInSquare) {
                    sb.append(String.format(
                            "%s,%d,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%d%n",
                            square.getRecordingName(),
                            squareIndex,
                            track.getTrackId(),
                            track.getTrackXLocation(),
                            track.getTrackYLocation(),
                            track.getTrackDuration(),
                            track.getTrackMaxSpeed(),
                            track.getTrackMedianSpeed(),
                            track.getTrackDisplacement(),
                            square.getRowNumber(),
                            square.getColNumber()
                    ));
                }
                Files.write(debugCsvPath, sb.toString().getBytes(), StandardOpenOption.APPEND);
            }
            // ------------------------------------------------------------------

            labelNumber++;
        }

        recording.setTracksTable(recordingTrackTable);

        PaintLogger.debugf("assignTracksToSquare - number of tracks assigned: %d; in recording: %d",
                           incrementalTrackCount,
                           tracksOfRecording.rowCount());

        PaintLogger.debugf("✅ Total %d tracks assigned to %d squares.",
                           recordingTrackTable.rowCount(),
                           recording.getSquaresOfRecording().size());
    }


    /**
     * Compiles all square data from the recordings in the specified experiment into a single table.
     * The method iterates through each recording in the experiment, retrieves its square data,
     * and appends it to a cumulative table. If a recording does not have square data available,
     * an error is logged.
     *
     * @param experiment the experiment containing recordings whose square data is to be combined
     * @return a {@code Table} containing the aggregated square data from all recordings in the experiment,
     *         or an empty table if no square data exists
     */
    private static Table compileAllSquares(Experiment experiment) {
        Table          allSquaresTable  = newEmptySquareTable();

        for (Recording recording : experiment.getRecordings()) {
            Table table = squareListToTable(recording.getSquaresOfRecording());
            if (table != null) {
                appendSquareTableInPlace(allSquaresTable, table);
            } else {
                PaintLogger.errorf("compileAllSquares - squares table does not exist for '%s'",
                                   recording.getRecordingName());
            }
        }
        return allSquaresTable;
    }

    /**
     * Compiles all track data from the recordings in the specified experiment into a single table.
     * The method iterates through each recording in the experiment, retrieves its track data,
     * and appends it to an aggregate table. If a recording does not have track data available,
     * an error is logged.
     *
     * @param experiment the experiment containing recordings whose track data is to be combined
     * @return a {@code Table} containing the aggregated track data from all recordings in the experiment,
     *         or an empty table if no track data exists
     */
    private static Table compileAllTracks(Experiment experiment) {
        Table allTracksTable = newEmptyTrackTable();

        for (Recording recording : experiment.getRecordings()) {
            PaintLogger.debugf("Compiling tracks for experiment '%s' - recording '%s'",
                               experiment.getExperimentName(),
                               recording.getRecordingName());

            Table table = recording.getTracksTable();
            if (table != null) {
                appendTrackTableInPlace(allTracksTable, table);
            } else {
                PaintLogger.errorf("compileAllTracks - tracks table does not exist for '%s'",
                                   recording.getRecordingName());
            }
        }
        PaintLogger.debugf("");
        return allTracksTable;
    }
}