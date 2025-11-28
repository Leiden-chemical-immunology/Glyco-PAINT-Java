/*=============================================================================
 *  Class:        GenerateSquaresProcessor.java
 *  Package:      generatesquares.calc
 *
 *  PURPOSE:
 *    Orchestrates the full square-generation and analysis pipeline for an experiment.
 *    Handles square grid creation, track assignment, attribute calculation, and
 *    persistence of results.
 *
 *  DESCRIPTION:
 *    This class drives the GENERATE_SQUARES workflow for the Paint project.
 *    It segments recordings into grid squares, assigns tracks to each square,
 *    calculates both per-square and per-recording attributes (via
 *    CalculateSquareAttributes), and compiles the final results into
 *    experiment-level tables written to disk.
 *
 *  RESPONSIBILITIES:
 *    • Generate geometric square grids for each recording
 *    • Assign tracks to their corresponding square regions
 *    • Trigger square and recording-level attribute calculations
 *    • Compile and persist all squares, tracks, and recording results
 *
 *  USAGE EXAMPLE:
 *    GenerateSquaresProcessor.generateSquaresForExperiment(project, "MyExperiment");
 *
 *  DEPENDENCIES:
 *    - paint.shared.config.GenerateSquaresConfig
 *    - paint.shared.objects.{Project, Experiment, Recording, Square, Track}
 *    - paint.shared.io.{SquareTableIO, TrackTableIO}
 *    - generatesquares.calc.CalculateSquareAttributes
 *    - tech.tablesaw.api.Table
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-23
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
=============================================================================*/

package paint.generatesquares.calc;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.config.paintconfig.PaintConfig;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static paint.shared.constants.PaintStringConstants.*;
import static paint.shared.io.ExperimentDataLoader.loadExperiment;
import static paint.shared.io.MainDataInterface.*;
import static paint.shared.io.SquaresTableIO.*;
import static paint.shared.io.TracksTableIO.*;
import static paint.shared.utils.Miscellaneous.formatDuration;
import static paint.shared.utils.SharedSquareUtils.filterTracksInSquare;

import static paint.shared.constants.PaintGeometry.IMAGE_HEIGHT;
import static paint.shared.constants.PaintGeometry.IMAGE_WIDTH;

public class GenerateSquaresProcessor {

    // Total number of squares in one dimension (e.g., 20 for a 20×20 grid).
    private static       int     numberOfSquaresInOneDimension;
    private static final boolean debugGenerateSquaresForExperiment = PaintConfig.getBoolean("Debug", DEBUG_GENERATE_SQUARES_FOR_EXPERIMENT, false);

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
        Experiment            experiment;
        List<Recording>       recordings;

        LocalDateTime start = LocalDateTime.now();
        PaintLogger.debugf("Loading Experiment '%s'", experimentName);

        // Load the experiment (without squares, with tracks)
        experiment = loadExperiment(
                project.getProjectRootPath(),
                experimentName,
                false,   // Don't load Squares
                true                // But do load Tracks
        );

        if (experiment == null) {
            PaintLogger.errorf("Failed to load experiment: %s", experimentName);
            return;
        }

        PaintLogger.infof("Starting processing experiment '%s'", experimentName);

        recordings = experiment.getRecordings();
        for (Recording recording : recordings) {

            if (!recording.isProcessFlagSet()) {
                continue;
            }

            // CHECK before starting each recording
            if (Thread.currentThread().isInterrupted()) {
                PaintLogger.infof("Cancelled before processing recording %s",
                                  recording.getRecordingName());
                return;
            }

            PaintLogger.infof("   Processing: %s", recording.getRecordingName());
            PaintLogger.debugf(recording.toString());

            // Create the squares with basic geometric information
            List<Square> squares = generateSquaresForRecording(recording, generateSquaresConfig);
            recording.setSquaresOfRecording(squares);

            // Assign the recording tracks to the squares
            assignTracksToSquares(recording);

            // CHECK mid-work before calculating attributes
            if (Thread.currentThread().isInterrupted()) {
                PaintLogger.infof("Cancelled before attribute calculation for %s",
                                  recording.getRecordingName());
                return;
            }

            // Calculate square-level and recording-level attributes
            Path experimentPath = project.getProjectRootPath()
                                         .resolve(experiment.getExperimentName());
            CalculateSquareAttributes.calculateSquareAttributes(
                    experimentPath,
                    recording,
                    generateSquaresConfig
            );
            CalculateSquareAttributes.calculateRecordingAttributes(
                    recording,
                    generateSquaresConfig
            );
        }

        Duration duration = Duration.between(start, LocalDateTime.now());
        PaintLogger.infof("Finished processing experiment '%s' in %s",
                          experimentName, formatDuration(duration));
        PaintLogger.blankline();

        // CHECK before writing output files
        if (Thread.currentThread().isInterrupted()) {
            PaintLogger.infof("Cancelled before writing output for %s", experimentName);
            return;
        }

        // Compile all squares and write
        Table allSquaresTable = compileAllSquares(experiment);
        Path  experimentPath  = project.getProjectRootPath()
                                       .resolve(experiment.getExperimentName());
        writeSquares(experimentPath, allSquaresTable);

        // Update recordings with filter information
        for (Recording recording : experiment.getRecordings()) {
            recording.setMinRequiredRSquared(generateSquaresConfig.getMinRequiredRSquared());
            recording.setMaxAllowableVariability(generateSquaresConfig.getMaxAllowableVariability());
            recording.setMinRequiredDensityRatio(generateSquaresConfig.getMinRequiredDensityRatio());
            recording.setNeighbourMode(generateSquaresConfig.getNeighbourMode());
        }

        // Write recordings
        writeRecordings(experimentPath, experiment.getRecordings());

        // All tracks
        Table allTracksTable = compileAllTracks(experiment);
        allTracksTable = allTracksTable.sortOn(RECORDING_NAME, TRACK_ID);
        writeTracks(experimentPath, allTracksTable);
    }

    /**
     * Generates a list of {@code Square} objects for the given recording. Each square corresponds to a
     * segment of the recording area based on the configuration provided.
     *
     * @param recording             the recording for which squares are to be generated
     * @param generateSquaresConfig the configuration specifying the number of squares and related parameters
     * @return a list of {@code Square} objects representing the segmented areas of the recording
     */
    public static List<Square> generateSquaresForRecording(Recording recording,
            GenerateSquaresConfig generateSquaresConfig) {

        // Total number of squares per recording.
        int numberOfSquaresInRecording = generateSquaresConfig.getNumberOfSquaresInRecording();
        numberOfSquaresInOneDimension  = (int) Math.sqrt(numberOfSquaresInRecording);

        List<Square> squares      = new ArrayList<>();
        double       squareWidth  = IMAGE_WIDTH  / numberOfSquaresInOneDimension;
        double       squareHeight = IMAGE_HEIGHT / numberOfSquaresInOneDimension;

        int squareNumber = 0;
        for (int rowNumber = 0; rowNumber < numberOfSquaresInOneDimension; rowNumber++) {
            for (int columnNumber = 0; columnNumber < numberOfSquaresInOneDimension; columnNumber++) {
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
     * @param recording the {@code Recording} instance containing track and square data.
     *                  The method modifies this object by assigning tracks to the corresponding squares
     *                  and updating their track-related attributes.
     */
    public static void assignTracksToSquares(Recording recording) throws IOException {

        Table         tracksOfRecording   = recording.getTracksTable();
        Table         recordingTrackTable = newEmptyTrackTable();

        int lastRowCol            = numberOfSquaresInOneDimension - 1;
        int labelNumber           = 0;
        int incrementalTrackCount = 0;

        PaintLogger.debugf("Assigning tracks to squares (%d total tracks)",
                           tracksOfRecording.rowCount());

        // ------------------------------------------------------------------
        // 🔥 DEBUG CSV SETUP (only if flag enabled)
        // ------------------------------------------------------------------
        Path debugCsvPath = null;

        if (debugGenerateSquaresForExperiment) {

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

        for (Square square : recording.getSquaresOfRecording()) {

            Table squareTracksTable = filterTracksInSquare(tracksOfRecording, square, lastRowCol);
            incrementalTrackCount += squareTracksTable.rowCount();

            if (squareTracksTable.rowCount() == 0) {
                square.setTracksList(Collections.emptyList());
                square.setTracksTable(squareTracksTable);
                square.setNumberOfTracks(0);
                continue;
            }

            List<Track> tracks = trackTableToList(squareTracksTable);

            // Update the fields on each Track
            for (Track track : tracks) {
                track.setSquareNumber(square.getSquareNumber());
                track.setLabelNumber(labelNumber);
            }

            Table updatedSquareTracks = trackListToTable(tracks);
            recordingTrackTable.append(updatedSquareTracks);

            // Update the square
            square.setTracksList(tracks);
            square.setTracksTable(updatedSquareTracks);
            square.setNumberOfTracks(tracks.size());

            // ------------------------------------------------------------------
            // 🔥 DEBUG CSV APPEND (only when flag enabled)
            // ------------------------------------------------------------------
            if (debugGenerateSquaresForExperiment && debugCsvPath != null) {

                StringBuilder sb = new StringBuilder();

                for (Track track : tracks) {
                    sb.append(String.format(
                            "%s,%d,%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%d%n",
                            square.getRecordingName(),
                            square.getSquareNumber(),
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

            PaintLogger.debugf("Square %3d: %3d tracks assigned (label %d)",
                               square.getSquareNumber(), tracks.size(), labelNumber);

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