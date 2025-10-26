/******************************************************************************
 *  Class:        GenerateSquaresProcessor.java
 *  Package:      generatesquares.calc
 *
 *  PURPOSE:
 *    Orchestrates the full square-generation and analysis pipeline for an experiment.
 *    Handles square grid creation, track assignment, attribute calculation, and
 *    persistence of results.
 *
 *  DESCRIPTION:
 *    This class drives the "Generate Squares" workflow for the Paint project.
 *    It segments recordings into grid squares, assigns tracks to each square,
 *    calculates both per-square and per-recording attributes (via CalculateAttributes),
 *    and compiles the final results into experiment-level tables written to disk.
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
 *    - generatesquares.calc.CalculateAttributes
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
 ******************************************************************************/

package generatesquares.calc;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.io.SquareTableIO;
import paint.shared.io.TrackTableIO;
import paint.shared.objects.*;
import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static paint.shared.config.PaintConfig.getBoolean;
import static paint.shared.constants.PaintConstants.*;
import static paint.shared.io.HelperIO.*;
import static paint.shared.io.ExperimentDataLoader.loadExperiment;
import static paint.shared.utils.Miscellaneous.formatDuration;
import static paint.shared.utils.SquareUtils.filterTracksInSquare;


public class GenerateSquaresProcessor {

    // @formatter:off
    private static int     numberOfSquaresInRecording; // Total number of squares per recording.
    private static Path    projectPath;
    private static boolean plotFittingCurves;
    // @formatter:on

    /**
     * Processes an experiment to generate square regions for each recording, compute attributes,
     * and compile data tables for all squares and tracks. The method applies geometric segmentations,
     * assigns tracks to the generated squares, and calculates additional square and recording-level attributes.
     * Finally, it writes compiled results to the file system.
     *
     * @param project the project containing configurations and experiment data
     * @param experimentName the name of the experiment to process
     * @return {@code true} if the experiment was successfully processed and saved, {@code false} otherwise
     */
    public static boolean generateSquaresForExperiment(Project project, String experimentName) {

        // @formatter:off
        GenerateSquaresConfig generateSquaresConfig = project.getGenerateSquaresConfig();
        projectPath                                 = project.getProjectRootPath();
        plotFittingCurves                           = getBoolean("Generate Squares", "Plot Curve Fitting", false);
        Experiment experiment                       = null;
        // @formatter:on

        LocalDateTime start = LocalDateTime.now();
        PaintLogger.debugf("Loading Experiment '%s'", experimentName);

        // EARLY EXIT if the user cancelled before we start
        if (Thread.currentThread().isInterrupted()) {
            PaintLogger.infof("Cancelled before starting experiment %s", experimentName);
            return false;
        }
        try {
            experiment = loadExperiment(project.getProjectRootPath(), experimentName, false);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to load Experiment '%s'", experimentName);
        }

        if (experiment == null) {
            PaintLogger.errorf("Failed to load experiment: %s", experimentName);
            return false;
        }

        PaintLogger.infof("Starting processing experiment '%s'", experimentName);

        for (Recording recording : experiment.getRecordings()) {

            // CHECK before starting each recording
            if (Thread.currentThread().isInterrupted()) {
                PaintLogger.infof("Cancelled before processing recording %s", recording.getRecordingName());
                return false;
            }
            PaintLogger.infof("   Processing: %s", recording.getRecordingName());
            PaintLogger.debugf(recording.toString());

            // Create the squares with basic geometric information
            List<Square> squares = generateSquaresForRecording(recording, generateSquaresConfig);
            recording.setSquaresOfRecording(squares);

            // Assign the recording tracks to the squares
            assignTracksToSquares(recording);

            // CHECK mid-work, before calculating attributes
            if (Thread.currentThread().isInterrupted()) {
                PaintLogger.infof("Cancelled before attribute calculation for %s", recording.getRecordingName());
                return false;
            }

            // Calculate square-level and recording-level attributes
            Path experimentPath = project.getProjectRootPath().resolve(experiment.getExperimentName());
            CalculateAttributes.calculateSquareAttributes(experimentPath, experimentName, recording, generateSquaresConfig);
            CalculateAttributes.calculateRecordingAttributes(recording, generateSquaresConfig);
        }

        Duration duration = Duration.between(start, LocalDateTime.now());
        PaintLogger.infof("Finished processing experiment '%s' in %s", experimentName, formatDuration(duration));
        PaintLogger.blankline();

        // 5️⃣ CHECK before writing output files
        if (Thread.currentThread().isInterrupted()) {
            PaintLogger.infof("Cancelled before writing output for %s", experimentName);
            return false;
        }

        // Compile all squares and write
        Table allSquaresTable = compileAllSquares(experiment);
        Path experimentPath = project.getProjectRootPath().resolve(experiment.getExperimentName());
        writeAllSquares(experimentPath, allSquaresTable);

        // Write recordings
        writeAllRecordings(experimentPath, experiment.getRecordings());

        // All Tracks
        Table allTracksTable = compileAllTracks(experiment);
        allTracksTable = allTracksTable.sortOn("Recording Name", "Track Id");
        writeAllTracks(experimentPath, allTracksTable);

        return true;
    }

    /**
     * Generates a list of {@code Square} objects for the given recording. Each square corresponds to a
     * segment of the recording area based on the configuration provided.
     *
     * @param recording the recording for which squares are to be generated
     * @param generateSquaresConfig the configuration specifying the number of squares and related parameters
     * @return a list of {@code Square} objects representing the segmented areas of the recording
     */
    public static List<Square> generateSquaresForRecording(Recording recording, GenerateSquaresConfig generateSquaresConfig) {

        numberOfSquaresInRecording    = generateSquaresConfig.getNumberOfSquaresInRecording();
        // Number of squares in one dimension (e.g. 20 for 20x20).
        int numberOfSquaresInOneDimension = (int) Math.sqrt(numberOfSquaresInRecording);

        // @formatter:off
        List<Square> squares = new ArrayList<>();
        double squareWidth   = IMAGE_WIDTH / numberOfSquaresInOneDimension;
        double squareHeight  = IMAGE_HEIGHT / numberOfSquaresInOneDimension;
        // @formatter:on

        int squareNumber = 0;
        for (int rowNumber = 0; rowNumber < numberOfSquaresInOneDimension; rowNumber++) {
            for (int columnNumber = 0; columnNumber < numberOfSquaresInOneDimension; columnNumber++) {
                double X0 = columnNumber * squareWidth;
                double Y0 = rowNumber * squareHeight;
                double X1 = (columnNumber + 1) * squareWidth;
                double Y1 = (rowNumber + 1) * squareHeight;

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
                        Y1));

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
    public static void assignTracksToSquares(Recording recording) {

        // @formatter:off
        Table tracksOfRecording   = recording.getTracksTable();
        TrackTableIO trackTableIO = new TrackTableIO();
        Table recordingTrackTable = trackTableIO.emptyTable();
        // @formatter:on

        int lastRowCol = numberOfSquaresInRecording - 1;
        int labelNumber = 0;
        int incrementalTrackCount = 0;

        PaintLogger.debugf("Assigning tracks to squares (%d total tracks)", tracksOfRecording.rowCount());
        for (Square square : recording.getSquaresOfRecording()) {

            Table squareTracksTable = filterTracksInSquare(tracksOfRecording, square, lastRowCol);
            incrementalTrackCount += squareTracksTable.rowCount();

            if (squareTracksTable.rowCount() == 0) {
                square.setTracks(Collections.emptyList());
                square.setTracksTable(squareTracksTable);
                square.setNumberOfTracks(0);
                continue;
            }

            // Convert rows to Track entities
            List<Track> tracks = trackTableIO.toEntities(squareTracksTable);

            // Update the fields on each Track
            for (Track track : tracks) {
                track.setSquareNumber(square.getSquareNumber());
                track.setLabelNumber(labelNumber);
            }

            // Rebuild the table from the modified tracks (ensures table reflects the updates)
            Table updatedSquareTracks = trackTableIO.toTable(tracks);

            // Append updated tracks into the global recording table
            recordingTrackTable.append(updatedSquareTracks);

            // Update the square
            square.setTracks(tracks);
            square.setTracksTable(updatedSquareTracks);
            square.setNumberOfTracks(tracks.size());

            // Log info
            PaintLogger.debugf("Square %d: %d tracks assigned (label %d)",
                               square.getSquareNumber(), tracks.size(), labelNumber);

            labelNumber++;
        }
        PaintLogger.debugf("assignTracksToSquare - The numbers of tracks assigned is %d  the recording is %s", incrementalTrackCount, tracksOfRecording.rowCount());

        // Update the recording table
        recording.setTracksTable(recordingTrackTable);

        PaintLogger.debugf("✅ Total %d tracks assigned to %d squares.",
                           recordingTrackTable.rowCount(), recording.getSquaresOfRecording().size());
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
        SquareTableIO squaresTableIO = new SquareTableIO();
        Table allSquaresTable = squaresTableIO.emptyTable();

        for (Recording recording : experiment.getRecordings()) {
            Table table = squaresTableIO.toTable(recording.getSquaresOfRecording());
            if (table != null) {
                squaresTableIO.appendInPlace(allSquaresTable, table);
            } else {
                PaintLogger.errorf("compileAllSquares - squares table does not exist for '%s'", recording.getRecordingName());
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
        TrackTableIO trackTableIO = new TrackTableIO();
        Table allTracksTable = trackTableIO.emptyTable();

        for (Recording recording : experiment.getRecordings()) {
            PaintLogger.debugf("Processing squares for experiment '%s'  - recording '%s'", experiment.getExperimentName(), recording.getRecordingName());
            Table table = recording.getTracksTable();
            if (table != null) {
                trackTableIO.appendInPlace(allTracksTable, table);
            }
            else {
                PaintLogger.errorf("compileAllSquares - squares table does not exist for '%s'", recording.getRecordingName());
            }
        }
        return allTracksTable;
    }
}