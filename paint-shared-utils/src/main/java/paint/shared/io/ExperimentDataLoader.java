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

import static paint.shared.io.MainDataInterface.*;
import static paint.shared.io.SquaresTableIO.squareTableToList;
import static paint.shared.io.TracksTableIO.trackTableToList;
import static paint.shared.utils.SharedSquareUtils.filterTracksInSquare;

import static paint.shared.constants.PaintFileNames.RECORDINGS_CSV;
import static paint.shared.constants.PaintFileNames.TRACKS_CSV;
import static paint.shared.constants.PaintFileNames.SQUARES_CSV;

import static paint.shared.constants.PaintStringConstants.*;

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
        List<Recording> recordings;
        try {
            recordings = readRecordings(experimentPath);
            for (Recording recording : recordings) {
                experiment.addRecording(recording);
            }
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s in %s", RECORDINGS_CSV, experimentName);
            return null;
        }

        // ─── Tracks (optional) ────────────────────────────────────────────────
        Table         tracksTable = null;

        if (loadTracks) {
            try {
                tracksTable = readTracksTable(experimentPath);
            } catch (Exception e) {
                PaintLogger.errorf("Failed to read %s in %s", TRACKS_CSV, experimentName);
                return null;
            }

            PaintLogger.debugf("Found %d tracks", tracksTable.rowCount());

            for (Recording recording : recordings) {
                if (!recording.isProcessFlagSet()) {
                    continue;
                }

                Table recTracksTable = tracksTable.where(
                        tracksTable.stringColumn(RECORDING_NAME)
                                   .isEqualTo(recording.getRecordingName()));

                PaintLogger.debugf("Found %d tracks for recording '%s'",
                                   recTracksTable.rowCount(), recording.getRecordingName());

                recording.setTracksList(trackTableToList(recTracksTable));
                recording.setTracksTable(recTracksTable);
            }
        }

        // ─── Squares (optional) ───────────────────────────────────────────────
        if (loadSquares) {
            Table squaresTable;
            try {
                squaresTable = readSquaresTable(experimentPath);
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
                        squaresTable.stringColumn(RECORDING_NAME)
                                    .matchesRegex("^" + rec.getRecordingName() + "(?:-threshold-\\d{1,3})?$"));

                rec.addSquares(squareTableToList(recSquares));

                // Only map tracks into squares if tracks were loaded
                if (loadTracks && numberOfRows > 0) {
                    int   lastRowCol = numberOfRows - 1;
                    Table recTracks  = rec.getTracksTable();

                    for (Square square : rec.getSquaresOfRecording()) {
                        Table SquaresTracks = filterTracksInSquare(recTracks, square, lastRowCol);
                        square.setTracksList(trackTableToList(SquaresTracks));
                    }
                }
            }
        }

        return experiment;
    }
}