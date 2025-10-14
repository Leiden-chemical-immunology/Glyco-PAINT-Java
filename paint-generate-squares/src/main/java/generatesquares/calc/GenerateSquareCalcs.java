/**
 * ============================================================================
 *  GenerateSquareCalcs.java
 *  Part of the "Generate Squares" module.
 *
 *  Purpose:
 *    Performs the computational workflow for square-based analysis of
 *    TrackMate experiment data. Handles:
 *      - Grid generation per recording
 *      - Track-to-square assignment
 *      - Tau, Variability, and Density calculations
 *      - Aggregation and export of results
 *
 *  Notes:
 *    All methods are static; this class acts as a calculation utility.
 *    No GUI elements or user input handling are included here.
 *
 *  Author: Herr Doctor
 *  Version: 1.0
 *  Module: paint-generate-squares
 * ============================================================================
 */

package generatesquares.calc;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.io.SquareTableIO;
import paint.shared.io.TrackTableIO;
import paint.shared.objects.*;
import paint.shared.utils.PaintLogger;
import paint.shared.utils.SquareUtils;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static generatesquares.calc.CalculateDensity.calculateDensity;
import static generatesquares.calc.CalculateTau.calcTau;
import static generatesquares.calc.CalculateVariability.calcVariability;
import static paint.shared.config.PaintConfig.getBoolean;
import static paint.shared.constants.PaintConstants.*;
import static paint.shared.io.HelperIO.*;
import static paint.shared.io.ProjectDataLoader.filterTracksInSquare;
import static paint.shared.io.ProjectDataLoader.loadExperiment;
import static paint.shared.objects.Square.calcSquareArea;
import static paint.shared.utils.Miscellaneous.formatDuration;
import static paint.shared.utils.Miscellaneous.round;
import static paint.shared.utils.SquareUtils.*;

/**
 * Provides core calculations for the "Generate Squares" workflow.
 * <p>
 * This class handles all computational steps for processing experiments:
 * <ul>
 *     <li>Loading experiment and recording data</li>
 *     <li>Generating square grids</li>
 *     <li>Assigning tracks to squares</li>
 *     <li>Calculating recording and square attributes (Tau, Variability, Density, etc.)</li>
 *     <li>Exporting combined output tables</li>
 * </ul>
 * <p>
 * All methods are static; this class is used as a calculation utility.
 * </p>
 */
public class GenerateSquareCalcs {

    // @formatter:off
    private static int     numberOfSquaresInRecording;      // Total number of squares per recording.
    private static int     numberOfSquaresInOneDimension;   // Number of squares in one dimension (e.g. 20 for 20x20).
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

        GenerateSquaresConfig generateSquaresConfig = project.generateSquaresConfig;
        projectPath                                 = project.projectRootPath;
        plotFittingCurves                           = getBoolean("Generate Squares", "Plot Curve Fitting", false);
        Experiment experiment                       = null;

        LocalDateTime start = LocalDateTime.now();
        PaintLogger.debugf("Loading Experiment '%s'", experimentName);
        try {
            experiment = loadExperiment(project.projectRootPath, experimentName, false);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to load Experiment '%s'", experimentName);  // vTODO
        }
        if (experiment != null) {
            PaintLogger.infof("Starting processing experiment '%s'", experimentName);

            for (Recording recording : experiment.getRecordings()) {
                PaintLogger.infof("   Processing: %s", recording.getRecordingName());
                PaintLogger.debugf(recording.toString());

                // Create the squares with basic geometric information
                List<Square> squares = generateSquaresForRecording(recording, generateSquaresConfig);
                recording.setSquaresOfRecording(squares);

                // Assign the recording tracks to the squares
                assignTracksToSquares(recording, generateSquaresConfig);

                // Calculate squares attributes
                calculateSquareAttributes(experimentName, recording, generateSquaresConfig);

                // Calculate recording attributes
                calculateRecordingAttributes(recording, generateSquaresConfig);  //TODO
            }

            Duration duration = Duration.between(start, LocalDateTime.now());
            PaintLogger.infof("Finished processing experiment '%s' in %s", experimentName, formatDuration(duration));
            PaintLogger.infof();

            // Compile all squares and write
            Table allSquaresTable = compileAllSquares(experiment);
            Path experimentPath = project.projectRootPath.resolve(experiment.getExperimentName());
            writeAllSquares(experimentPath, allSquaresTable);

            // Write recordings
            writeAllRecordings(experimentPath, experiment.getRecordings());

            // All Tracks
            Table allTracksTable = compileAllTracks(experiment);
            writeAllTracks(experimentPath, allTracksTable);

            return true;
        } else {
            PaintLogger.errorf("Failed to load experiment: %s", experimentName);
            return false;
        }
    }

    /**
     * Generates all square regions for a given recording.
     *
     * @param generateSquaresConfig configuration defining number and layout of squares
     * @param recording             the recording to segment into squares
     * @return list of {@link Square} objects representing the grid
     */
    public static List<Square> generateSquaresForRecording(Recording recording, GenerateSquaresConfig generateSquaresConfig ) {

        numberOfSquaresInRecording    = generateSquaresConfig.getNumberOfSquaresInRecording();
        numberOfSquaresInOneDimension = (int) Math.sqrt(numberOfSquaresInRecording);

        List<Square> squares = new ArrayList<>();
        double squareWidth = IMAGE_WIDTH / numberOfSquaresInOneDimension;
        double squareHeight = IMAGE_HEIGHT / numberOfSquaresInOneDimension;

        int squareNumber = 0;
        for (int rowNumber = 0; rowNumber < numberOfSquaresInOneDimension; rowNumber++) {
            for (int columnNumber = 0; columnNumber < numberOfSquaresInOneDimension; columnNumber++) {
                double X0 = columnNumber * squareWidth;
                double Y0 = rowNumber * squareHeight;
                double X1 = (columnNumber + 1) * squareWidth;
                double Y1 = (rowNumber + 1) * squareHeight;

                squares.add(new Square(
                        recording.getRecordingName() + '-' + squareNumber,
                        recording.getRecordingName(),
                        squareNumber,
                        rowNumber,
                        columnNumber,
                        X0,
                        Y0,
                        X1,
                        Y1));

                squareNumber += 1;
            }
        }
        return squares;
    }

    /**
     * Assigns track data to each square based on spatial coordinates.
     *
     * @param recording the recording containing track information
     * @param context   the {@link GenerateSquaresConfig} defining grid layout
     */
    public static void assignTracksToSquares(Recording recording, GenerateSquaresConfig context) {

        Table tracksOfRecording = recording.getTracksTable();
        TrackTableIO trackTableIO = new TrackTableIO();
        Table recordingTrackTable = trackTableIO.emptyTable();

        int lastRowCol = numberOfSquaresInRecording - 1;
        int labelNumber = 0;

        for (Square square : recording.getSquaresOfRecording()) {

            Table squareTracksTable = filterTracksInSquare(tracksOfRecording, square, lastRowCol);

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

        // Update the recording table
        recording.setTracksTable(recordingTrackTable);

        PaintLogger.debugf("✅ Total %d tracks assigned to %d squares.",
                          recordingTrackTable.rowCount(), recording.getSquaresOfRecording().size());
    }

    /**
     * Calculates aggregate metrics for an entire recording (Tau, background sau_fatistics, density).
     *
     * @param recording             the recording to analyze
     * @param generateSquaresConfig the {@link GenerateSquaresConfig} parameters
     */
    public static void calculateRecordingAttributes(Recording recording, GenerateSquaresConfig generateSquaresConfig) {

        // Calculate the Tau
        double minRequiredRSquared            = generateSquaresConfig.getMinRequiredRSquared();
        int minTracksForTau                   = generateSquaresConfig.getMinTracksToCalculateTau();

        // Calculate the background track count
        SquareUtils.BackgroundEstimationResult result;
        result = estimateBackgroundDensity(recording.getSquaresOfRecording());
        double numberOfTracksInBackgroundSquares  = result.getBackgroundMean();
        PaintLogger.debugf("Estimated Background track count = %.2f, n = %d%n", numberOfTracksInBackgroundSquares, result.getBackgroundSquares().size());
        int backgroundTracks = result.getBackgroundSquares().stream().mapToInt(Square::getNumberOfTracks).sum();

        recording.setNumberOfSquaresInBackground(result.getBackgroundSquares().size());
        recording.setNumberOfTracksInBackground(backgroundTracks);
        recording.setAverageTracksInBackGround(round(result.getBackgroundMean(), 3));

        List<Track> tracksFromSelectedSquares = SquareUtils.getTracksFromSelectedSquares(recording);
        int numberOfSelectedSquares           = SquareUtils.getNumberOfSelectedSquares(recording);

        CalculateTau.CalculateTauResult results = calcTau(tracksFromSelectedSquares, minTracksForTau, minRequiredRSquared);
        if (results.getStatus() == CalculateTau.CalculateTauResult.Status.TAU_SUCCESS) {
            recording.setTau(round(results.getTau(), 0));
            recording.setRSquared(round(results.getRSquared(), 3));
        } else {
            recording.setTau(Double.NaN);
            recording.setRSquared(Double.NaN);
        }

        // Calculate the density
        double density = calculateDensity(tracksFromSelectedSquares.size(), calcSquareArea(numberOfSelectedSquares), RECORDING_DURATION, recording.getConcentration());
        recording.setDensity(round(density, 2));
    }

    /**
     * Calculates detailed metrics for each square (Tau, variability, density ratio, etc.).
     *
     * @param recording             the recording containing the squares
     * @param generateSquaresConfig the {@link GenerateSquaresConfig} parameters
     */
    public static void calculateSquareAttributes(String experimentName, Recording recording, GenerateSquaresConfig generateSquaresConfig) {

        // @formatter:off
        double minRequiredRSquared        = generateSquaresConfig.getMinRequiredRSquared();
        int    minTracksForTau            = generateSquaresConfig.getMinTracksToCalculateTau();
        double maxAllowableVariability    = generateSquaresConfig.getMaxAllowableVariability();
        double minRequiredDensityRatio    = generateSquaresConfig.getMinRequiredDensityRatio();
        String neighbourMode              = generateSquaresConfig.getNeighbourMode();
        int    numberOfSquaresInRecording = generateSquaresConfig.getNumberOfSquaresInRecording();
        double area                       = calcSquareArea(numberOfSquaresInRecording);
        double concentration              = recording.getConcentration();
        // @formatter:on

        SquareUtils.BackgroundEstimationResult result;
        result = estimateBackgroundDensity(recording.getSquaresOfRecording());
        double numberOfTracksInBackgroundSquares  = result.getBackgroundMean();
        PaintLogger.debugf("Estimated Background track count = %.2f, n = %d%n", numberOfTracksInBackgroundSquares, result.getBackgroundSquares().size());

        int labelNumber = 0;
        for (Square square : recording.getSquaresOfRecording()) {

            // @formatter:off
            int         squareNumber        = square.getSquareNumber();
            List<Track> tracksInSquare      = square.getTracks();
            Table       tracksInSquareTable = square.getTracksTable();
            // @formatter:on

            if (tracksInSquare == null || tracksInSquare.isEmpty()) {
                continue;
            }

            // Calculate Tau

            CalculateTau.CalculateTauResult results = calcTau(tracksInSquare, minTracksForTau, minRequiredRSquared);


            if (plotFittingCurves) {
                if (tracksInSquare.size() >= minTracksForTau) {
                    Path experimentPath = projectPath.resolve(experimentName);
                    TauPlotCollector.saveFitPlot(
                            tracksInSquare,
                            results,
                            experimentPath,
                            recording.getRecordingName(),
                            squareNumber
                    );
                }
            }

            if (results.getStatus() == CalculateTau.CalculateTauResult.Status.TAU_SUCCESS) {
                square.setTau(round(results.getTau(), 0));
                square.setRSquared(round(results.getRSquared(), 3));
            } else {
                square.setTau(Double.NaN);
                square.setRSquared(Double.NaN);
            }

            square.setMedianDiffusionCoefficient(round(tracksInSquareTable.doubleColumn("Diffusion Coefficient").median(), 3));
            square.setMedianDiffusionCoefficientExt(round(tracksInSquareTable.doubleColumn("Diffusion Coefficient Ext").median(), 3));

            square.setMedianLongTrackDuration(round(calculateMedianLongTrack(tracksInSquareTable, 0.1), 1));
            square.setMedianShortTrackDuration(round(calculateMedianShortTrack(tracksInSquareTable, 0.1), 1));

            square.setMedianDisplacement(round(tracksInSquareTable.doubleColumn("Track Displacement").mean(), 1));
            square.setMaxDisplacement(round(tracksInSquareTable.doubleColumn("Track Displacement").max(), 1));
            square.setTotalDisplacement(round(tracksInSquareTable.doubleColumn("Track Displacement").sum(), 1));

            square.setMedianMaxSpeed(round(tracksInSquareTable.doubleColumn("Track Max Speed").median(), 1));
            square.setMaxMaxSpeed(round(tracksInSquareTable.doubleColumn("Track Max Speed").max(), 1));

            square.setMaxTrackDuration(round(tracksInSquareTable.doubleColumn("Track Duration").max(), 1));
            square.setTotalTrackDuration(round(tracksInSquareTable.doubleColumn("Track Duration").sum(), 1));
            square.setMedianTrackDuration(round(tracksInSquareTable.doubleColumn("Track Duration").median(), 1));

            double variability = calcVariability(tracksInSquareTable, squareNumber, numberOfSquaresInRecording, 10);    //TODO
            square.setVariability(round(variability, 1));

            double density = calculateDensity(tracksInSquare.size(), area, RECORDING_DURATION, concentration);
            square.setDensity(round(density, 4));

            double densityRatio = tracksInSquare.size() / numberOfTracksInBackgroundSquares;
            square.setDensityRatio(round(densityRatio, 1));

            // Apply the shared visibility filter logic
            SquareUtils.applyVisibilityFilter(
                    recording,
                    minRequiredDensityRatio,
                    maxAllowableVariability,
                    minRequiredRSquared,
                    neighbourMode
            );

            // Re-assign label numbers to selected squares
            labelNumber = 0;
            for (Square sq : recording.getSquaresOfRecording()) {
                if (sq.isSelected()) {
                    sq.setLabelNumber(labelNumber++);
                }
            }
        }
    }

    /**
     * Calculates the median duration among the longest fraction of tracks.
     *
     * @param tracks   table containing tracks
     * @param fraction fraction (0–1) of longest tracks to consider
     * @return median of long-track durations
     */
    public static double calculateMedianLongTrack(Table tracks, double fraction) {
        int nrOfTracks = tracks.rowCount();
        if (nrOfTracks == 0) {
            return 0.0;
        }

        Table sorted = tracks.sortAscendingOn("Track Duration");
        int nrTracksToAverage = Math.max((int) Math.round(fraction * nrOfTracks), 1);

        // Get the last nrTracksToAverage durations
        DoubleColumn durations = sorted.doubleColumn("Track Duration");
        List<Double> tail = durations.asList()
                .subList(nrOfTracks - nrTracksToAverage, nrOfTracks);
        return median(tail);
    }

    /**
     * Calculates the median duration among the shortest fraction of tracks.
     *
     * @param tracks   table containing tracks
     * @param fraction fraction (0–1) of shortest tracks to consider
     * @return median of short-track durations
     */
    public static double calculateMedianShortTrack(Table tracks, double fraction) {
        int nrOfTracks = tracks.rowCount();
        if (nrOfTracks == 0) {
            return 0.0;
        }

        Table sorted = tracks.sortAscendingOn("Track Duration");
        int nrTracksToAverage = Math.max((int) Math.round(fraction * nrOfTracks), 1);

        // Get the first nrTracksToAverage durations
        DoubleColumn durations = sorted.doubleColumn("Track Duration");
        List<Double> head = durations.asList().subList(0, nrTracksToAverage);

        return median(head);
    }

    /**
     * Calculates the median of a list of numeric values.
     *
     * @param values list of doubles
     * @return median or 0 if list is empty
     */
    private static double median(List<Double> values) {
        values.sort(Comparator.naturalOrder());
        int size = values.size();
        if (size == 0) {
            return 0.0;
        }
        if (size % 2 == 1) {
            return values.get(size / 2);
        } else {
            return (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
        }
    }

    /**
     * Compiles all squares of all recordings in an experiment into one combined table.
     *
     * @param experiment the {@link Experiment} containing multiple recordings
     * @return a combined {@link Table} of all square data
     */
    private static Table compileAllSquares(Experiment experiment) {

        SquareTableIO squaresTableIO = new SquareTableIO();
        Table allSquaresTable = squaresTableIO.emptyTable();


        for (Recording recording : experiment.getRecordings()) {
            PaintLogger.debugf("Processing squares for experiment '%s'  - recording '%s'", experiment.getExperimentName(), recording.getRecordingName());
            Table table = squaresTableIO.toTable(recording.getSquaresOfRecording());
            if  (table != null) {
                squaresTableIO.appendInPlace(allSquaresTable, table);
            }
            else {
                PaintLogger.errorf("compileAllSquares - squares table does not exist for '%s'", recording.getRecordingName());
            }
        }
        return allSquaresTable;
    }

    /**
     * Compiles all squares of all recordings in an experiment into one combined table.
     *
     * @param experiment the {@link Experiment} containing multiple recordings
     * @return a combined {@link Table} of all square data
     */
    private static Table compileAllTracks(Experiment experiment) {

        TrackTableIO trackTableIO = new TrackTableIO();
        Table allTracksTable = trackTableIO.emptyTable();

        for (Recording recording : experiment.getRecordings()) {
            PaintLogger.debugf("Processing squares for experiment '%s'  - recording '%s'", experiment.getExperimentName(), recording.getRecordingName());
            Table table = recording.getTracksTable();
            if  (table != null) {
                trackTableIO.appendInPlace(allTracksTable, table);
            }
            else {
                PaintLogger.errorf("compileAllSquares - squares table does not exist for '%s'", recording.getRecordingName());
            }
        }
        return allTracksTable;
    }

}