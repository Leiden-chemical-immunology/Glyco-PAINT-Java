package paint.loaders;

import config.GenerateSquaresConfig;
import paint.io.RecordingTableIO;
import paint.io.TrackTableIO;

import paint.io.SquareTableIO;
import objects.Experiment;
import objects.Project;
import objects.Recording;
import objects.Square;
import objects.Track;
import utilities.AppLogger;
import config.PaintConfig;
import tech.tablesaw.api.*;
import tech.tablesaw.selection.Selection;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static generatesquares.calc.CalculateDensity.calculateAverageTrackCountOfBackground;
import static constants.PaintConstants.*;
import static paint.localUtilities.Miscellaneous.friendlyMessage;

public final class ProjectDataLoader {

    private ProjectDataLoader() {}

    public static void main(String[] args) {
        Project project = null;
        AppLogger.init("Load Project");

        try {
            if (args == null || args.length == 0) {
                System.out.println("Usage: java -cp <jar> paint.loaders.PainProjectLoader <project-root-path> [experiments...] [--mature]");
                System.out.println("  <project-root-path>   Path containing Experiment Info.csv and experiment directories");
                System.out.println("  [experiments...]      Zero or more experiment names to load (default: all)");
                System.out.println("  --mature              Expect squares file (All Squares.csv) in experiments");
                return;
            }

            Path projectPath = java.nio.file.Paths.get(args[0]);

            boolean matureProject = false;
            List<String> experimentNames = new ArrayList<>();

            // Parse remaining args
            for (int i = 1; i < args.length; i++) {
                if ("--mature".equalsIgnoreCase(args[i])) {
                    matureProject = true;
                } else {
                    experimentNames.add(args[i]);
                }
            }

            project = loadProject(projectPath, experimentNames, matureProject);

        } catch (Exception e) {
            System.err.println("Failed to load project: " + e.getMessage());
            System.exit(1);
        }

        System.out.println(project);

        // evaluateProject(project);

        System.out.println("\n\n\n\n");
        cycleThroughProject(project);
    }


    // ---------- Public API ----------

    public static void cycleThroughProject(Project project) {

        int numberOfTracksInExperiment;
        int numberOfTracksInProject = 0;

        System.out.printf("Project: %s has the following context:\n",project.getProjectName());
        // System.out.println(project.getContext());
        System.out.printf("Project %s has %d experiments:\n", project.getProjectName(), project.getExperiments().size());

        for (Experiment exp : project.getExperiments()) {
            System.out.printf("\t%s\n", exp.getExperimentName());
        }
        System.out.println();

        for (Experiment exp : project.getExperiments()) {
            numberOfTracksInExperiment = 0;
            System.out.printf("\n\nExperiment: %s has %d recordings.\n", exp.getExperimentName(), exp.getRecordings().size());
            for (Recording rec : exp.getRecordings()) {
                System.out.printf("\t%s\n", rec.getRecordingName());
            }

            for (Recording rec : exp.getRecordings()) {
                System.out.println();
                System.out.printf("Recording: %s\n", rec.getRecordingName());
                System.out.println(rec);

                int numberOfTracksInSquare = 0;
                for (Square square: rec.getSquares()) {
                    // calcSquare(square);
                    if (square.getTracks().size() != 0) {
                        numberOfTracksInSquare += square.getTracks().size();
                    }
                }

                numberOfTracksInProject += numberOfTracksInSquare;
                numberOfTracksInExperiment += numberOfTracksInSquare;

                calculateAverageTrackCountOfBackground(rec, 60);
            }
            System.out.printf("Number of tracks in experiment: %d\n", numberOfTracksInExperiment);
        }
        System.out.printf("\nNumber of tracks in project: %d\n", numberOfTracksInProject);

    }


    public static Project loadProject(Path projectPath, List<String> experimentNames, boolean matureProject) {
        List<Experiment> experiments = new ArrayList<>();

        // Read the context information from the Paint Configuration.json file
        // Context context = loadContextFromJsonConfig(projectPath);   ToDO

        // If no Experiment was specified, assume that all experiments should be loaded
        if (experimentNames == null || experimentNames.isEmpty()) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(projectPath, Files::isDirectory)) {
                for (Path path : stream) {
                    experimentNames.add(path.getFileName().toString());
                }
                experimentNames.sort(String::compareToIgnoreCase);
                System.out.println("Loading all experiments in the project.");
            } catch (IOException e) {
                System.err.println("Failed to read experiments: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }

        // Load each experiment if it seems valid and add it to the project
        for (String experimentName : experimentNames) {
            Path experimentPath = projectPath.resolve(experimentName);
            if (experimentSeemsValid(experimentPath, matureProject)) {
                AppLogger.infof("Loading experiment: " + experimentName);
                try {
                    PaintConfig paintConfig = PaintConfig.from(projectPath.resolve(PAINT_JSON));
                    GenerateSquaresConfig generateSquaresConfig =  GenerateSquaresConfig.from(paintConfig);
                    Experiment experiment = loadExperiment(projectPath, experimentName, generateSquaresConfig, matureProject);
                            // Don't store @@@@
//                    if (experiment != null) {
//                        experiments.add(experiment);
//                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            else {
                AppLogger.errorf("Experiment '" + experimentName + "' does not seem valid.");
                AppLogger.errorf(reasonForExperimentProblem(experimentPath, matureProject));
            }
        }

        // Create and return the Project object
        // return new Project(projectPath, context, experiments);
        return new Project();
    }


    public static Experiment loadExperimentForSquaresCalc(Path projectPath, String experimentName) {
        Path experimentPath = projectPath.resolve(experimentName);

        Table tracksTable = null;
        List<Recording> recordings = null;
        Experiment experiment = new Experiment(experimentName);

        // Load recordings, but do not bother with tracks yet.
        RecordingTableIO recordingsTableIO = new RecordingTableIO();
        try {
            Path recordingsCsvPath = experimentPath.resolve(RECORDINGS_CSV);
            Table recordingsTable = recordingsTableIO.readCsv(recordingsCsvPath);
            recordings = recordingsTableIO.toEntities(recordingsTable);
            for (Recording rec : recordings) {
                experiment.addRecording(rec);
            }
        } catch (Exception e) {
            AppLogger.errorf("Failed to read %s in %s : likely reason is that columns do not match", RECORDINGS_CSV, experimentName);
            // System.exit(0);
            return null;
        }

        // Read the experiment 'All Tracks' file
        TrackTableIO trackTableIO = new TrackTableIO();
        try {
            tracksTable = trackTableIO.readCsv(experimentPath.resolve(TRACKS_CSV));
        }
        catch (Exception e) {
            AppLogger.errorf("Failed to read %s in %s: %s", TRACKS_CSV, experimentName, friendlyMessage(e));
            return null;
        }

        try {
            for (Recording recording : recordings) {

                // Find the track records for this recording
                Table tracksOfRecording = tracksTable.where(
                        tracksTable.stringColumn(COL_RECORDING_NAME)
                                .matchesRegex("^" + recording.getRecordingName() + "(?:-threshold-\\d{1,3})?$"));

                // Create the Tracks objects for this recording and add the Tracks objects to the recording
                List<Track> tracks = trackTableIO.toEntities(tracksOfRecording);
                recording.setTracks(tracks);  // ToDo - be consistent, use the same call as for squares above
                recording.setTracksTable(tracksOfRecording);
            }
        }
        catch (Exception e) {
            AppLogger.errorf("Failed to split Tracks Table of experiment %s - %s.", experimentName, friendlyMessage(e));
            return null;
        }

        // Return an experiment filled with recordings and with a tracks table for each recording
        return experiment;
    }

    /**
     * Loads a single experiment from a project directory.
     * <p>
     * Attempts to construct an {@link Experiment} instance based on the given
     * project path, experiment name, and square generation configuration.
     * </p>
     *
     * @param projectPath          the path to the project directory containing the experiment
     * @param experimentName       the name of the experiment to load
     * @param generateSquaresConfig the configuration used for square generation
     * @param matureProject        {@code true} if the project is in a mature state,
     *                             {@code false} if it is still in progress
     * @return an {@code Experiment} object representing the loaded experiment
     * @throws Exception if the experiment cannot be found, read, or parsed
     */
    public static Experiment loadExperiment(Path projectPath, String experimentName, GenerateSquaresConfig generateSquaresConfig, boolean matureProject) throws Exception {
        Path experimentPath = projectPath.resolve(experimentName);

        Table tracksTable = null;
        Table squaresTable = null;
        List<Recording> recordings = null;
        Experiment experiment = new Experiment(experimentName);

        // Load recordings, but do not bother with squares and tracks yet.
        RecordingTableIO recordingsTableIO = new RecordingTableIO();
        try {
            Table recordingsTable = recordingsTableIO.readCsv(experimentPath.resolve(RECORDINGS_CSV));
            recordings = recordingsTableIO.toEntities(recordingsTable);
            for (Recording rec : recordings) {
                experiment.addRecording(rec);
            }
        } catch (Exception e) {
            AppLogger.errorf("Failed to read %s in %s : %s", RECORDINGS_CSV, experimentName, friendlyMessage(e));
            return null;
        }

        // Read the experiment 'All Tracks' file
        TrackTableIO trackTableIO = new TrackTableIO();
        try {
            tracksTable = trackTableIO.readCsv(experimentPath.resolve(TRACKS_CSV));
        }
        catch (Exception e) {
            AppLogger.errorf("Failed to read %s in %s: %s", TRACKS_CSV, experimentName, friendlyMessage(e));
            return null;
        }

        SquareTableIO squareTableIO = new SquareTableIO();
        try {
            squaresTable = squareTableIO.readCsv(experimentPath.resolve(SQUARES_CSV));
        }
        catch (Exception e) {
            AppLogger.errorf("Failed to read %s in %s: %s", SQUARES_CSV, experimentName, friendlyMessage(e));
            return null;
        }

        // Assign the squares to each recording.
        try {
            for (Recording recording : recordings) {

                // Find the square records for this recording
                Table squaresOfRecording = squaresTable.where(
                        squaresTable.stringColumn(COL_RECORDING_NAME)
                                .matchesRegex("^" + recording.getRecordingName() + "(?:-threshold-\\d{1,3})?$"));

                // Create the Square objects for this recording and add the Square objects to the recording
                List<Square> squares = squareTableIO.toEntities(squaresOfRecording);
                recording.addSquares(squares);

                // Find the track records for this recording
                Table tracksOfRecording = tracksTable.where(
                        tracksTable.stringColumn(COL_RECORDING_NAME)
                                .matchesRegex("^" + recording.getRecordingName() + "(?:-threshold-\\d{1,3})?$"));

                // Create the Tracks objects for this recording and add the Tracks objects to the recording
                List<Track> tracks = trackTableIO.toEntities(tracksOfRecording);
                recording.setTracks(tracks);  // ToDo - be consistent, use the same call as for squares above
                recording.setTracksTable(tracksOfRecording);

                // Assign the Tracks to specific squares in each recording
                int lastRowCol = generateSquaresConfig.getNrSquaresInRow() - 1;

                for (Square square : recording.getSquares()) {
                    Table squareTracksTable = filterTracksInSquare(tracksOfRecording, square, lastRowCol);
                    tracks = trackTableIO.toEntities(squareTracksTable);
                    square.setTracks(tracks);
                }
            }
        }
        catch (Exception e) {
            AppLogger.errorf("In %s failed to assign tracks squares: %s", experimentName, friendlyMessage(e));
            return null;
        }

        // Return with a valid mature experiment
        return experiment;
    }


    private static boolean experimentSeemsValid(Path experimentPath, boolean matureProject) {

        return (Files.isDirectory(experimentPath) &&
                Files.isRegularFile(experimentPath.resolve(RECORDINGS_CSV)) &&
                Files.isRegularFile(experimentPath.resolve(TRACKS_CSV)) &&
                Files.isDirectory(experimentPath.resolve(DIR_TRACKMATE_IMAGES)) &&
                Files.isDirectory(experimentPath.resolve(DIR_BRIGHTFIELD_IMAGES)) &&
                (!matureProject || Files.isRegularFile(experimentPath.resolve(SQUARES_CSV))));
    }


    private static String reasonForExperimentProblem(Path experimentPath, boolean matureProject) {

        StringBuilder sb = new StringBuilder();

        if (!Files.isDirectory(experimentPath)) {
            sb.append("\tExperiment directory does not exist: " + experimentPath + "\n");
            return sb.toString();
        }
        if (!Files.isRegularFile(experimentPath.resolve(RECORDINGS_CSV))) {
            sb.append("\tFile '" + RECORDINGS_CSV + "' does not exist.\n");
        }
        if (!Files.isRegularFile(experimentPath.resolve(TRACKS_CSV))) {
            sb.append("\tFile '" + TRACKS_CSV + "' does not exist.\n");
        }
        if (!Files.isDirectory(experimentPath.resolve(DIR_TRACKMATE_IMAGES))) {
            sb.append("\tDirectory '" + DIR_TRACKMATE_IMAGES + "' does not exist\n.");
        }
        if (!Files.isDirectory(experimentPath.resolve(DIR_BRIGHTFIELD_IMAGES))) {
            sb.append("\tDirectory '" + DIR_BRIGHTFIELD_IMAGES + "' does not exist.\n");
        }

        // If the experiment is marked as 'Mature', there needs to be an 'All Squares' CSV file.
        if (matureProject && !Files.isRegularFile(experimentPath.resolve(SQUARES_CSV))) {
            sb.append("\tFile '" + SQUARES_CSV + "' does not exist.\n");
        }

        return sb.toString();
    }


    /*
    Select the tracks that are within the square's bounding box.
     */

    public static Table filterTracksInSquare(Table tracks, Square square, int lastRowCol) {

        double x0 = square.getX0();
        double y0 = square.getY0();
        double x1 = square.getX1();
        double y1 = square.getY1();

        boolean isLastColumn = square.getColNumber() == lastRowCol;
        boolean isLastRow = square.getRowNumber() == lastRowCol;

        double left   = Math.min(x0, x1);
        double right  = Math.max(x0, x1);
        double top    = Math.min(y0, y1);
        double bottom = Math.max(y0, y1);

        DoubleColumn x = tracks.doubleColumn("Track X Location");
        DoubleColumn y = tracks.doubleColumn("Track Y Location");

        // Build X-range selection
        Selection selX;
        try {
            // Prefer a single-pass range when available
            selX = isLastColumn
                    ? x.isBetweenInclusive(left, right)
                    : x.isGreaterThanOrEqualTo(left).and(x.isLessThan(right));
        } catch (UnsupportedOperationException | NoSuchMethodError e) {
            // Fallback if isBetweenInclusive is not available in this Tablesaw version
            selX = x.isGreaterThanOrEqualTo(left).and(isLastColumn ? x.isLessThanOrEqualTo(right) : x.isLessThan(right));
        }

        // Build Y-range selection
        Selection selY;
        try {
            selY = isLastRow
                    ? y.isBetweenInclusive(top, bottom)
                    : y.isGreaterThanOrEqualTo(top).and(y.isLessThan(bottom));
        } catch (UnsupportedOperationException | NoSuchMethodError e) {
            selY = y.isGreaterThanOrEqualTo(top).and(isLastRow ? y.isLessThanOrEqualTo(bottom) : y.isLessThan(bottom));
        }

        // Combine once to minimize temporary allocations
        Selection sel = selX.and(selY);

        return tracks.where(sel);
    }



}