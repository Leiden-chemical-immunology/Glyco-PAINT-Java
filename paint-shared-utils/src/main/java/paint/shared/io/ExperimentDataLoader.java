package paint.shared.io;

import paint.shared.objects.Experiment;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.util.List;

import static paint.shared.constants.PaintConstants.*;
import static paint.shared.utils.SquareUtils.filterTracksInSquare;

public final class ExperimentDataLoader {

    private ExperimentDataLoader() {
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // Experiment Loader
    // ───────────────────────────────────────────────────────────────────────────────

    public static Experiment loadExperiment(Path projectPath,
                                            String experimentName,
                                            boolean matureProject)  {

        Path experimentPath   = projectPath.resolve(experimentName);
        Experiment experiment = new Experiment(experimentName);

        // ─── Recordings ───────────────────────────────────────────────────────
        RecordingTableIO recIO = new RecordingTableIO();
        List<Recording> recordings;
        try {
            Table recTable = recIO.readCsvWithSchema(
                    experimentPath.resolve(RECORDINGS_CSV),
                    RECORDINGS,
                    RECORDINGS_COLS,
                    RECORDINGS_TYPES,
                    false
            );
            recordings = recIO.toEntities(recTable);
            recordings.forEach(experiment::addRecording);
        } catch (Exception e) {
            // PaintLogger.errorf("Failed to read %s in %s: %s", RECORDING_CSV, experimentName, friendlyMessage(e));
            PaintLogger.errorf("Failed to read %s in %s", RECORDINGS_CSV, experimentName);
            return null;
        }

        // ─── Tracks ───────────────────────────────────────────────────────────
        TrackTableIO trackIO = new TrackTableIO();
        Table tracksTable;
        try {
            tracksTable = trackIO.readCsvWithSchema(
                    experimentPath.resolve(TRACKS_CSV),
                    TRACKS,
                    TRACKS_COLS,
                    TRACKS_TYPES,
                    false
            );
        } catch (Exception e) {
            //PaintLogger.errorf("Failed to read %s in %s: %s", TRACKS_CSV, experimentName, friendlyMessage(e));
            PaintLogger.errorf("Failed to read %s in %s", TRACKS_CSV, experimentName);
            return null;
        }

        // Attach tracks to recordings
        PaintLogger.debugf("Found %d tracks", tracksTable.rowCount());
        for (Recording recording : recordings) {
            if (!recording.isProcessFlag()) {
                continue;
            }
            String recName = recording.getRecordingName();
            Table recTracks = tracksTable.where(
                    tracksTable.stringColumn("Recording Name").isEqualTo(recording.getRecordingName()));;
            PaintLogger.debugf("Found %d tracks for recording '%s'", recTracks.rowCount(), recording.getRecordingName());
            recording.setTracks(trackIO.toEntities(recTracks));
            recording.setTracksTable(recTracks);
        }

        // ─── Squares (ONLY for mature projects) ───────────────────────────────
        if (matureProject) {
            SquareTableIO squareIO = new SquareTableIO();
            Table squaresTable;
            try {
                squaresTable = squareIO.readCsvWithSchema(
                        experimentPath.resolve(SQUARES_CSV),
                        SQUARES,
                        SQUARES_COLS,
                        SQUARES_TYPES,
                        false
                );
            } catch (Exception e) {
                //PaintLogger.errorf("Failed to read %s in %s: %s", SQUARES_CSV, experimentName, friendlyMessage(e));
                PaintLogger.errorf("Failed to read %s in %s", SQUARES_CSV, experimentName);
                return null;
            }

            int numberOfRecordings          = recordings.size();
            int numberOfSquares             = squaresTable.rowCount();
            int numberOfSquaresPerRecording = (numberOfRecordings == 0) ? 0 : numberOfSquares / numberOfRecordings;

            int numberOfRows;
            if (numberOfSquaresPerRecording > 0) {
                numberOfRows = (int) Math.round(Math.sqrt(numberOfSquaresPerRecording));
            } else {
                numberOfRows = 0;
            }

            if (numberOfRows > 0 && numberOfRows * numberOfRows != numberOfSquaresPerRecording) {
                PaintLogger.errorf("Invalid squares layout in experiment '%s'", experimentName);
                System.exit(-1);
            }

            // Assign squares and map tracks into them
            for (Recording rec : recordings) {
                Table recSquares = squaresTable.where(
                        squaresTable.stringColumn("Recording Name")
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
}