/******************************************************************************
 *  Class:        ExperimentDataLoader.java
 *  Package:      paint.shared.io
 *
 *  PURPOSE:
 *    Loads experiment data including recordings, tracks, and (for mature projects)
 *    squares from CSV files into structured {@link paint.shared.objects.Experiment}
 *    objects.
 *
 *  DESCRIPTION:
 *    Reads Tablesaw CSV files for recordings, tracks, and optionally squares using
 *    schema validation provided by their respective TableIO classes. Constructs
 *    {@link paint.shared.objects.Recording} and {@link paint.shared.objects.Square}
 *    entities, associates tracks with recordings, and maps tracks to individual
 *    squares based on spatial filtering.
 *
 *  KEY FEATURES:
 *    • Reads and validates CSV data for recordings, tracks, and squares.
 *    • Builds complete {@link paint.shared.objects.Experiment} structures ready
 *      for processing.
 *    • Supports “mature project” mode that includes square-level mapping.
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
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

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
 * Provides centralized functionality for loading all experiment-related data:
 * recordings, tracks, and (for mature projects) squares.
 *
 * <p>The class orchestrates reading and schema validation of CSV files into
 * structured {@link Experiment} objects. It leverages the specialized
 * {@link RecordingTableIO}, {@link TrackTableIO}, and {@link SquareTableIO}
 * classes to ensure consistent I/O and validation logic.</p>
 *
 * <p>This class is a static-only loader with a single entry point
 * {@link #loadExperiment(Path, String, boolean)}.</p>
 */
public final class ExperimentDataLoader {

    /** Private constructor to prevent instantiation. */
    private ExperimentDataLoader() {}

    // ───────────────────────────────────────────────────────────────────────────────
    // EXPERIMENT LOADER
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Loads an entire experiment from a given project folder.
     *
     * <p>This method sequentially performs the following steps:</p>
     * <ol>
     *   <li>Loads and validates {@code recordings.csv}, converting rows into
     *       {@link Recording} entities.</li>
     *   <li>Loads and validates {@code tracks.csv}, associating tracks with each
     *       recording based on the “Recording Name” column.</li>
     *   <li>If {@code matureProject} is {@code true}, loads and validates
     *       {@code squares.csv}, mapping tracks into squares using the spatial
     *       boundaries defined for each recording.</li>
     * </ol>
     *
     * <p>All CSVs are validated for column names, order, and data types
     * according to schema definitions in {@link paint.shared.constants.PaintConstants}.</p>
     *
     * @param projectPath   the root project directory
     * @param experimentName the name of the experiment folder (relative to the project path)
     * @param matureProject  {@code true} to include square-level loading and mapping
     * @return a fully populated {@link Experiment} object, or {@code null} if any
     *         data file fails to load or validate
     */
    public static Experiment loadExperiment(Path projectPath,
                                            String experimentName,
                                            boolean matureProject) {

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
            PaintLogger.errorf("Failed to read %s in %s", TRACKS_CSV, experimentName);
            return null;
        }

        // ─── Attach tracks to recordings ──────────────────────────────────────
        PaintLogger.debugf("Found %d tracks", tracksTable.rowCount());
        for (Recording recording : recordings) {
            if (!recording.isProcessFlag()) {
                continue; // skip non-processed recordings
            }

            Table recTracks = tracksTable.where(
                    tracksTable.stringColumn("Recording Name")
                            .isEqualTo(recording.getRecordingName()));

            PaintLogger.debugf("Found %d tracks for recording '%s'",
                               recTracks.rowCount(), recording.getRecordingName());

            recording.setTracks(trackIO.toEntities(recTracks));
            recording.setTracksTable(recTracks);
        }

        // ─── Squares (only for mature projects) ───────────────────────────────
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

            // Validate square layout geometry
            if (numberOfRows > 0 && numberOfRows * numberOfRows != numberOfSquaresPerRecording) {
                PaintLogger.errorf("Invalid squares layout in experiment '%s'", experimentName);
                System.exit(-1);
            }

            // ─── Assign squares and map tracks into them ──────────────────────
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