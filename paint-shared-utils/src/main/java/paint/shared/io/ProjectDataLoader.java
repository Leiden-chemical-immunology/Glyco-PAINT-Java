package paint.shared.io;

import paint.shared.config.PaintConfig;
import paint.shared.objects.Experiment;
import paint.shared.objects.Project;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.selection.Selection;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintConstants.*;
import static paint.shared.utils.Miscellaneous.friendlyMessage;

public final class ProjectDataLoader {

    private ProjectDataLoader() {
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // Public API
    // ───────────────────────────────────────────────────────────────────────────────

    public static void cycleThroughProject(Project project) {
        int numberOfTracksInProject = 0;

        System.out.printf("Project %s has %d experiments:%n",
                          project.getProjectName(), project.getExperiments().size());

        for (Experiment exp : project.getExperiments()) {
            System.out.printf("\t%s%n", exp.getExperimentName());
        }

        for (Experiment exp : project.getExperiments()) {
            int numberOfTracksInExperiment = 0;
            System.out.printf("%nExperiment %s has %d recordings.%n",
                              exp.getExperimentName(), exp.getRecordings().size());

            for (Recording rec : exp.getRecordings()) {
                int numberOfTracksInSquare = 0;
                for (Square square : rec.getSquaresOfRecording()) {
                    numberOfTracksInSquare += square.getTracks().size();
                }
                numberOfTracksInProject += numberOfTracksInSquare;
                numberOfTracksInExperiment += numberOfTracksInSquare;
            }
            System.out.printf("Tracks in experiment: %d%n", numberOfTracksInExperiment);
        }
        System.out.printf("%nTracks in project: %d%n", numberOfTracksInProject);
    }

    public static Project loadProject(Path projectPath, List<String> experimentNames, boolean matureProject) {
        List<Experiment> experiments = new ArrayList<>();

        // ─── Find experiments if none provided ────────────────────────────────
        if (experimentNames == null || experimentNames.isEmpty()) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(projectPath, Files::isDirectory)) {
                for (Path path : stream) {
                    experimentNames.add(path.getFileName().toString());
                }
                experimentNames.sort(String::compareToIgnoreCase);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read experiments: " + e.getMessage(), e);
            }
        }

        // ─── Load each experiment ─────────────────────────────────────────────
        for (String experimentName : experimentNames) {
            try {
                PaintConfig paintConfig = PaintConfig.instance();
                Experiment experiment = loadExperiment(projectPath, experimentName, matureProject);

                if (experiment != null) {
                    experiments.add(experiment);
                    PaintLogger.debugf("Loaded experiment: %s", experimentName);
                } else {
                    PaintLogger.errorf("Skipping invalid or failed experiment: %s", experimentName);
                }
            } catch (Exception e) {
                PaintLogger.errorf("Failed to load experiment %s: %s", experimentName, e.getMessage());
            }
        }

        return new Project(projectPath, experiments);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // Experiment Loader
    // ───────────────────────────────────────────────────────────────────────────────

    public static Experiment loadExperiment(Path projectPath,
                                            String experimentName,
                                            boolean matureProject) throws Exception {

        Path experimentPath = projectPath.resolve(experimentName);

        // ─── Validate experiment structure first ──────────────────────────────
        if (!experimentSeemsValid(experimentPath, matureProject)) {
            PaintLogger.errorf("Experiment '%s' is invalid.", experimentName);
            PaintLogger.errorf(reasonForExperimentProblem(experimentPath, matureProject));
            return null;
        }

        Experiment experiment = new Experiment(experimentName);

        // ─── Recordings ───────────────────────────────────────────────────────
        RecordingTableIO recIO = new RecordingTableIO();
        List<Recording> recordings;
        try {
            Table recTable = recIO.readCsvWithSchema(
                    experimentPath.resolve(RECORDINGS_CSV),
                    "Recordings",
                    RECORDING_COLS,
                    RECORDING_TYPES,
                    false
            );
            recordings = recIO.toEntities(recTable);
            recordings.forEach(experiment::addRecording);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s in %s: %s", RECORDINGS_CSV, experimentName, friendlyMessage(e));
            return null;
        }

        // ─── Tracks ───────────────────────────────────────────────────────────
        TrackTableIO trackIO = new TrackTableIO();
        Table tracksTable;
        try {
            tracksTable = trackIO.readCsvWithSchema(
                    experimentPath.resolve(TRACKS_CSV),
                    "Tracks",
                    TRACK_COLS,
                    TRACK_TYPES,
                    false
            );
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s in %s: %s", TRACKS_CSV, experimentName, friendlyMessage(e));
            return null;
        }

        // Attach tracks to recordings
        for (Recording rec : recordings) {
            Table recTracks = tracksTable.where(
                    tracksTable.stringColumn(COL_RECORDING_NAME)
                            .matchesRegex("^" + rec.getRecordingName() + "(?:-threshold-\\d{1,3})?$"));
            rec.setTracks(trackIO.toEntities(recTracks));
            rec.setTracksTable(recTracks);
        }

        // ─── Squares (ONLY for mature projects) ───────────────────────────────
        if (matureProject) {
            SquareTableIO squareIO = new SquareTableIO();
            Table squaresTable;
            try {
                squaresTable = squareIO.readCsvWithSchema(
                        experimentPath.resolve(SQUARES_CSV),
                        "Squares",
                        SQUARE_COLS,
                        SQUARE_TYPES,
                        false
                );
            } catch (Exception e) {
                PaintLogger.errorf("Failed to read %s in %s: %s", SQUARES_CSV, experimentName, friendlyMessage(e));
                return null;
            }

            int numberOfRecordings = recordings.size();
            int numberOfSquares = squaresTable.rowCount();
            int numberOfSquaresPerRecording = (numberOfRecordings == 0) ? 0 : numberOfSquares / numberOfRecordings;
            int numberOfRows = (numberOfSquaresPerRecording > 0)
                    ? (int) Math.round(Math.sqrt(numberOfSquaresPerRecording))
                    : 0;

            if (numberOfRows > 0 && numberOfRows * numberOfRows != numberOfSquaresPerRecording) {
                PaintLogger.errorf("Invalid squares layout in experiment '%s'", experimentName);
                System.exit(-1);
            }

            // Assign squares and map tracks into them
            for (Recording rec : recordings) {
                Table recSquares = squaresTable.where(
                        squaresTable.stringColumn(COL_RECORDING_NAME)
                                .matchesRegex("^" + rec.getRecordingName() + "(?:-threshold-\\d{1,3})?$"));
                rec.addSquares(squareIO.toEntities(recSquares));

                if (numberOfRows > 0) {
                    int lastRowCol = numberOfRows - 1;
                    Table recTracks = rec.getTracksTable();
                    for (Square square : rec.getSquaresOfRecording()) {
                        Table squareTracks = filterTracksInSquare(recTracks, square, lastRowCol);
                        square.setTracks(trackIO.toEntities(squareTracks));
                    }
                }
            }
        }

        return experiment;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // Validation Helpers
    // ───────────────────────────────────────────────────────────────────────────────

    private static boolean experimentSeemsValid(Path experimentPath, boolean matureProject) {
        return (Files.isDirectory(experimentPath) &&
                Files.isRegularFile(experimentPath.resolve(RECORDINGS_CSV)) &&
                Files.isRegularFile(experimentPath.resolve(TRACKS_CSV)) &&
                (!matureProject || Files.isRegularFile(experimentPath.resolve(SQUARES_CSV))));
    }

    private static String reasonForExperimentProblem(Path experimentPath, boolean matureProject) {
        StringBuilder sb = new StringBuilder();

        if (!Files.isDirectory(experimentPath)) {
            sb.append("\tExperiment directory does not exist: ").append(experimentPath).append("\n");
            return sb.toString();
        }
        if (!Files.isRegularFile(experimentPath.resolve(RECORDINGS_CSV))) {
            sb.append("\tMissing ").append(RECORDINGS_CSV).append("\n");
        }
        if (!Files.isRegularFile(experimentPath.resolve(TRACKS_CSV))) {
            sb.append("\tMissing ").append(TRACKS_CSV).append("\n");
        }
        if (matureProject && !Files.isRegularFile(experimentPath.resolve(SQUARES_CSV))) {
            sb.append("\tMissing ").append(SQUARES_CSV).append("\n");
        }
        return sb.toString();
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // Track Filtering Utility
    // ───────────────────────────────────────────────────────────────────────────────

    public static Table filterTracksInSquare(Table tracks, Square square, int lastRowCol) {
        double x0 = square.getX0(), y0 = square.getY0(), x1 = square.getX1(), y1 = square.getY1();

        boolean isLastCol = square.getColNumber() == lastRowCol;
        boolean isLastRow = square.getRowNumber() == lastRowCol;

        double left = Math.min(x0, x1), right = Math.max(x0, x1);
        double top = Math.min(y0, y1), bottom = Math.max(y0, y1);

        DoubleColumn x = tracks.doubleColumn("Track X Location");
        DoubleColumn y = tracks.doubleColumn("Track Y Location");

        Selection selX = isLastCol
                ? x.isBetweenInclusive(left, right)
                : x.isGreaterThanOrEqualTo(left).and(x.isLessThan(right));

        Selection selY = isLastRow
                ? y.isBetweenInclusive(top, bottom)
                : y.isGreaterThanOrEqualTo(top).and(y.isLessThan(bottom));

        return tracks.where(selX.and(selY));
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // CLI Entrypoint
    // ───────────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        Project project;
        Path loggerPath = Paths.get(System.getProperty("user.home"), "Paint").resolve("Logger");
        PaintLogger.initialise(loggerPath, "Load Project");

        try {
            if (args == null || args.length == 0) {
                System.out.println("Usage: java -cp <jar> io.ProjectDataLoader <project-root-path> [experiments...] [--mature]");
                return;
            }

            Path projectPath = Paths.get(args[0]);
            boolean matureProject = false;
            List<String> experimentNames = new ArrayList<>();

            // Parse args
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
            return;
        }

        System.out.println(project);
        cycleThroughProject(project);
    }
}