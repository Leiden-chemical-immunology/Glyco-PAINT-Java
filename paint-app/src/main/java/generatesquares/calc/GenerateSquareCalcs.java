package generatesquares.calc;

import config.GenerateSquaresConfig;
import paint.io.TrackTableIO;
import objects.Project;
import objects.Experiment;
import objects.Recording;
import objects.Square;
import objects.Track;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static generatesquares.calc.CalculateDensity.calculateAverageTrackCountOfBackground;
import static generatesquares.calc.CalculateDensity.calculateDensity;
import static generatesquares.calc.CalculateTau.calcTau;
import static generatesquares.calc.CalculateVariability.calcVariability;
import static paint.loaders.ProjectDataLoader.*;

import static constants.PaintConstants.IMAGE_HEIGHT;
import static constants.PaintConstants.IMAGE_WIDTH;

import utils.AppLogger;

public class GenerateSquareCalcs {

    public static boolean calculateSquaresForExperiment(Project project, String experimentName) {

        GenerateSquaresConfig generateSquaresConfig = project.generateSquaresConfig;

        Experiment experiment = loadExperimentForSquaresCalc(project.projectPath, experimentName);
        if (experiment != null) {
            AppLogger.infof("Processing Experiment: %s", experimentName);
            for (Recording recording : experiment.getRecordings()) {
                AppLogger.infof("   Processing: %s", recording.getRecordingName());
                AppLogger.debugf(recording.toString());

                // Create the squares with basic geometric information
                List<Square> squares = generateSquaresForRecording(generateSquaresConfig, recording);
                recording.setSquares(squares);

                // Assign the recording tracks to the squares
                assignTracksToSquares(recording, generateSquaresConfig);

                // Calculate recording attributes
                calculateRecordingAttributes(recording, generateSquaresConfig);

                // Calculate squares attributes
                calculateSquareAttributes(recording, generateSquaresConfig);
            }
            AppLogger.infof("Finished processing Experiment: %s", experimentName);
            AppLogger.infof("");
            project.addExperiment(experiment);
            return true;
        }
        else {
            AppLogger.errorf("Failed to load experiment: %s", experimentName);
            return false;
        }
    }


    public static List<Square> generateSquaresForRecording(GenerateSquaresConfig generateSquaresConfig, Recording recording) {

        int n = generateSquaresConfig.getNrSquaresInRow();

        List<Square> squares = new ArrayList<>();
        double squareWidth  = IMAGE_WIDTH / n;
        double squareHeight = IMAGE_HEIGHT / n;

        int squareNumber = 0;
        for (int rowNumber = 0; rowNumber < n; rowNumber++) {
            for (int columnNumber = 0; columnNumber < n; columnNumber++) {
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


    public static void assignTracksToSquares(Recording recording, GenerateSquaresConfig context) {

        Table tracksOfRecording = recording.getTracksTable();
        TrackTableIO trackTableIO = new TrackTableIO();

        int lastRowCol = context.getNrSquaresInRow() - 1;

        for (Square square : recording.getSquares()) {
            Table squareTracksTable = filterTracksInSquare(tracksOfRecording, square, lastRowCol);
            List<Track> tracks = trackTableIO.toEntities(squareTracksTable);
            square.setTracks(tracks);
            square.setTracksTable(squareTracksTable);
            square.setNumberOfTracks(tracks.size());
        }
    }

    public static void calculateRecordingAttributes(Recording recording, GenerateSquaresConfig generateSquaresConfig) {

        double minRequiredRSquared = generateSquaresConfig.getMinRequiredRSquared();
        int minTracksForTau = generateSquaresConfig.getMinTracksToCalculateTau();
        CalculateTau.CalculateTauResult results = calcTau(recording.getTracks(), minTracksForTau,  minRequiredRSquared);
        if (results.getStatus() == CalculateTau.CalculateTauResult.Status.TAU_SUCCESS) {
            recording.setTau(results.getTau());
            recording.setRSquared(results.getRSquared());
        }
        else {
            recording.setTau(Double.NaN);
            recording.setRSquared(Double.NaN);
        }
    }

    public static void calculateSquareAttributes(Recording recording, GenerateSquaresConfig generateSquaresConfig) {

        double imageWidth = 1;
        double imageHeight = 1;
        double minRequiredRSquared = generateSquaresConfig.getMinRequiredRSquared();
        int minTracksForTau = generateSquaresConfig.getMinTracksToCalculateTau();
        int numberOfSquaresInRow = generateSquaresConfig.getNrSquaresInRow();
        double area = imageWidth * imageWidth/ (generateSquaresConfig.getNrSquaresInRow() * generateSquaresConfig.getNrSquaresInRow());
        double concentration = recording.getConcentration();
        double time = 100;
        int nrOfAverageCountSquares = 10;

        double averageTracks = calculateAverageTrackCountOfBackground(recording, nrOfAverageCountSquares);

        for (Square square : recording.getSquares()) {
            int squareNumber = square.getSquareNumber();
            List<Track> tracksInSquare = square.getTracks();
            Table tracksInSquareTable = square.getTracksTable();

            if (tracksInSquare == null || tracksInSquare.isEmpty()) {
                continue;
            }

            // Calculate Tau
            CalculateTau.CalculateTauResult results = calcTau(tracksInSquare, minTracksForTau,  minRequiredRSquared);
            if (results.getStatus() == CalculateTau.CalculateTauResult.Status.TAU_SUCCESS) {
                square.setTau(results.getTau());
                square.setRSquared(results.getRSquared());
            }
            else {
                square.setTau(Double.NaN);
                square.setRSquared(Double.NaN);
            }

            square.setMedianDiffusionCoefficient(tracksInSquareTable.doubleColumn("Diffusion Coefficient").median());
            square.setMedianDiffusionCoefficientExt(tracksInSquareTable.doubleColumn("Diffusion Coefficient Ext").median());

            square.setMedianLongTrackDuration(calculateMedianLongTrack(tracksInSquareTable, 0.1));
            square.setMedianShortTrackDuration(calculateMedianShortTrack(tracksInSquareTable, 0.1));

            square.setMedianDisplacement(tracksInSquareTable.doubleColumn("Track Displacement").mean());
            square.setMaxDisplacement(tracksInSquareTable.doubleColumn("Track Displacement").max());
            square.setTotalDisplacement(tracksInSquareTable.doubleColumn("Track Displacement").sum());

            square.setMedianMaxSpeed(tracksInSquareTable.doubleColumn("Track Max Speed").median());
            square.setMaxMaxSpeed(tracksInSquareTable.doubleColumn("Track Max Speed").max());

            square.setMaxTrackDuration(tracksInSquareTable.doubleColumn("Track Duration").max());
            square.setTotalTrackDuration(tracksInSquareTable.doubleColumn("Track Duration").sum());
            square.setMedianTrackDuration(tracksInSquareTable.doubleColumn("Track Duration").median());

            double variability = calcVariability(tracksInSquareTable, squareNumber, numberOfSquaresInRow, 10);
            square.setVariability(variability);

            double density  = calculateDensity(tracksInSquare.size(), area, time, concentration);
            square.setDensity(density);

            double densityRatio = tracksInSquare.size() / averageTracks;
            square.setDensityRatio(densityRatio);
        }

    }

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

    // Utility function to calculate the median of a list of doubles
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


}
