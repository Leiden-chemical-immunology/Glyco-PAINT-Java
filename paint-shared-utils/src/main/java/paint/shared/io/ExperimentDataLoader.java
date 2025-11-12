/*=============================================================================
 *  Class:        ExperimentDataLoader.java
 *  Package:      paint.shared.io
 *
 *  PURPOSE:
 *    Loads experiment data including recordings, optionally tracks, and (for
 *    mature projects) squares from CSV files into structured
 *    {@link paint.shared.objects.Experiment} objects.
 *
 *  DESCRIPTION:
 *    Reads Tablesaw CSV files for recordings, tracks, and optionally squares using
 *    schema validation provided by their respective TableIO classes. Constructs
 *    {@link paint.shared.objects.Recording} and {@link paint.shared.objects.Square}
 *    entities, associates tracks with recordings (if requested), and maps tracks
 *    to individual squares (if both tracks and squares are loaded).
 *
 *  KEY FEATURES:
 *    • Reads and validates CSV data for recordings, tracks, and squares.
 *    • Supports partial experiment loading (e.g., only recordings and squares).
 *    • Performs schema enforcement via BaseTableIO-derived classes.
 *    • Integrates with PaintLogger for debug/error tracking.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-11-01
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
=============================================================================*/

package paint.shared.io;

import paint.shared.objects.Experiment;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.util.List;

import static paint.shared.constants.PaintConstants.*;
import static paint.shared.utils.SharedSquareUtils.filterTracksInSquare;

/**
 * Provides centralized functionality for loading experiment-related data:
 * recordings, optionally tracks, and optionally squares.
 */
public final class ExperimentDataLoader {

    private ExperimentDataLoader() {}

    /**
     * Loads an experiment with flexible inclusion of tracks and squares.
     *
     *
     * @param projectPath     the root project directory
     * @param experimentName  the name of the experiment folder
     * @param loadSquares     whether to include squares (mature project mode)
     * @param loadTracks      whether to include track data
     * @return an {@link Experiment} with requested data layers loaded, or {@code null} on failure
     */
    public static Experiment loadExperiment(Path    projectPath,
                                            String  experimentName,
                                            boolean loadSquares,
                                            boolean loadTracks) {

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
            PaintLogger.errorf("Failed to read %s in %s", RECORDINGS_CSV, experimentName);
            return null;
        }

        // ─── Tracks (optional) ────────────────────────────────────────────────
        Table tracksTable    = null;
        TrackTableIO trackIO = new TrackTableIO();

        if (loadTracks) {
            try {
                tracksTable = trackIO.readCsvWithSchema(
                        experimentPath.resolve(TRACKS_CSV),
                        TRACKS,
                        TRACKS_COLS,
                        TRACKS_TYPES,
                        false
                );
            } catch (Exception e) {
                PaintLogger.errorf("Failed to read %s in %s", TRACKS_CSV, experimentName);
                return null;
            }

            PaintLogger.debugf("Found %d tracks", tracksTable.rowCount());

            for (Recording recording : recordings) {
                if (!recording.isProcessFlag()) {
                    continue;
                }

                Table recTracks = tracksTable.where(
                        tracksTable.stringColumn("Recording Name")
                                .isEqualTo(recording.getRecordingName()));

                PaintLogger.debugf("Found %d tracks for recording '%s'",
                                   recTracks.rowCount(), recording.getRecordingName());

                recording.setTracks(trackIO.toEntities(recTracks));
                recording.setTracksTable(recTracks);
            }
        }

        // ─── Squares (optional) ───────────────────────────────────────────────
        if (loadSquares) {
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
                PaintLogger.errorf("Failed to read %s in %s", SQUARES_CSV, experimentName);
                return null;
            }

            int numberOfRecordings          = recordings.size();
            int numberOfSquares             = squaresTable.rowCount();
            int numberOfSquaresPerRecording = (numberOfRecordings == 0) ? 0 : numberOfSquares / numberOfRecordings;

            int numberOfRows = (numberOfSquaresPerRecording > 0)
                    ? (int) Math.round(Math.sqrt(numberOfSquaresPerRecording))
                    : 0;

            if (numberOfRows > 0 && numberOfRows * numberOfRows != numberOfSquaresPerRecording) {
                PaintLogger.errorf("Invalid squares layout in experiment '%s'", experimentName);
                System.exit(-1);
            }

            for (Recording rec : recordings) {
                Table recSquares = squaresTable.where(
                        squaresTable.stringColumn("Recording Name")
                                .matchesRegex("^" + rec.getRecordingName() + "(?:-threshold-\\d{1,3})?$"));

                rec.addSquares(squareIO.toEntities(recSquares));

                // Only map tracks into squares if tracks were loaded
                if (loadTracks && numberOfRows > 0) {
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