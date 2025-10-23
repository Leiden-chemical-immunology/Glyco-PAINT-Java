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

/**
 * ============================================================================
 *  GenerateSquareCalcs.java
 *  Orchestrates all calculations for the "Generate Squares" workflow.
 *
 *  <p><b>Purpose:</b> Coordinates square-level and recording-level metrics
 *  computation, invoking {@link CalculateAttributes} and related modules.</p>
 *
 *  <p><b>Usage:</b> Called internally by {@code GenerateSquares} or executed
 *  as part of automated batch processing.</p>
 *
 *  <p><b>Author:</b> Hans Bakker<br>
 *  <b>Version:</b> 1.0<br>
 *  <b>Module:</b> paint-generate-squares</p>
 * ============================================================================
 */

public class GenerateSquareCalcs {

    // @formatter:off
    private static int     numberOfSquaresInRecording; // Total number of squares per recording.
    private static Path    projectPath;
    private static boolean plotFittingCurves;
    // @formatter:on

    /**
     * Runs the "Generate Squares" process for a single experiment.
     *
     * @param project        the {@link Project} containing experiment data
     * @param experimentName the name of the experiment
     * @return {@code true} if successful, {@code false} otherwise
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
            PaintLogger.infof("   Processing: %s", recording.getRecordingName());
            PaintLogger.debugf(recording.toString());

            // Create the squares with basic geometric information
            List<Square> squares = generateSquaresForRecording(recording, generateSquaresConfig);
            recording.setSquaresOfRecording(squares);

            // Assign the recording tracks to the squares
            assignTracksToSquares(recording);

            // Calculate square-level and recording-level attributes
            Path experimentPath = project.getProjectRootPath().resolve(experiment.getExperimentName());
            CalculateAttributes.calculateSquareAttributes(experimentPath, experimentName, recording, generateSquaresConfig);
            CalculateAttributes.calculateRecordingAttributes(recording, generateSquaresConfig);
        }

        Duration duration = Duration.between(start, LocalDateTime.now());
        PaintLogger.infof("Finished processing experiment '%s' in %s", experimentName, formatDuration(duration));
        PaintLogger.infof();

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
     * Generates all square regions for a given recording.
     *
     * @param generateSquaresConfig configuration defining number and layout of squares
     * @param recording             the recording to segment into squares
     * @return list of {@link Square} objects representing the grid
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
     * Assigns track data to each square based on spatial coordinates.
     *
     * @param recording the recording containing track information
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
     * Compiles all squares of all recordings in an experiment into one combined table.
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
     * Compiles all tracks of all recordings in an experiment into one combined table.
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