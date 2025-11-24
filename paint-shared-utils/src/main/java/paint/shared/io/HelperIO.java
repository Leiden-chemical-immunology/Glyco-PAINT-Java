/*=============================================================================
 *  Class:        HelperIO.java
 *  Package:      paint.shared.io
 *
 *  PURPOSE:
 *    Provides high-level, convenience methods for reading and writing all
 *    primary experiment data files — Recordings, Tracks, and Squares — used
 *    throughout the PAINT system.
 *
 *  DESCRIPTION:
 *    This utility class acts as a facade over the specific TableIO classes
 *    (RecordingsTableIO, TracksTableIO, SquaresTableIO). It offers a simplified
 *    interface for bulk input/output operations involving Tablesaw tables
 *    and entity lists, including schema validation, CSV read/write, and
 *    logging with user-friendly error messages.
 *
 *  KEY FEATURES:
 *    • Unified access to schema-validated CSV I/O for all major PAINT entities.
 *    • Converts seamlessly between Table and entity List representations.
 *    • Provides graceful error handling via PaintLogger with human-readable
 *      exception messages.
 *    • Supports both per-entity and full-table operations.
 *    • Fully compatible with Java 8 and Tablesaw-based schema validation.
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
=============================================================================*/

package paint.shared.io;

import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.objects.Track;
import paint.shared.schema.TrackSchema;
import paint.shared.schema.SquareSchema;
import paint.shared.schema.RecordingSchema;

import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.util.List;

import static paint.shared.utils.Miscellaneous.friendlyMessage;

import static paint.shared.constants.PaintFileNames.RECORDINGS_CSV;
import static paint.shared.constants.PaintFileNames.SQUARES_CSV;
import static paint.shared.constants.PaintFileNames.TRACKS_CSV;

/**
 * Utility class providing static helper methods for reading and writing
 * {@link Recording}, {@link Square}, and {@link Track} data in a PAINT
 * experiment directory.
 *
 * <p>This class wraps the specialized I/O classes
 * ({@link RecordingsTableIO}, {@link SquaresTableIO}, and {@link TracksTableIO})
 * to simplify loading and saving of experiment data.</p>
 *
 * <p>Each method performs schema validation and detailed logging, ensuring
 * reliable data integrity across all experiment operations.</p>
 */
public final class HelperIO {

    /**
     * Private constructor to prevent instantiation.
     */
    private HelperIO() {
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // RECORDINGS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Reads all recordings from an experiment directory.
     *
     * @param experimentPath the experiment directory containing {@code recordings.csv}
     * @return a list of {@link Recording} entities, or {@code null} if reading fails
     */
    public static List<Recording> readRecordings(Path experimentPath) {
        RecordingsTableIO recordingsTableIO = new RecordingsTableIO();
        try {
            Table recTable = recordingsTableIO.readCsvWithSchema(
                    experimentPath.resolve(RECORDINGS_CSV),
                    RecordingSchema.COLUMNS,
                    RecordingSchema.TYPES,
                    false
            );
            return recordingsTableIO.toEntities(recTable);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s : %s", RECORDINGS_CSV, friendlyMessage(e));
            return null;
        }
    }


    /**
     * Reads all square data into a {@link Table}.
     *
     * @param experimentPath the experiment directory containing {@code squares.csv}
     * @return the loaded table, or {@code null} if reading fails
     */
    public static Table readRecordingsTable(Path experimentPath) {
        RecordingsTableIO recordingsTableIO = new RecordingsTableIO();
        try {
            return recordingsTableIO.readCsvWithSchema(
                    experimentPath.resolve(RECORDINGS_CSV),
                    RecordingSchema.COLUMNS,
                    RecordingSchema.TYPES,
                    false
            );
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s: %s", RECORDINGS_CSV, friendlyMessage(e));
            return null;
        }
    }

    /**
     * Writes a list of {@link Recording} objects to {@code recordings.csv}
     * in the given experiment directory.
     *
     * @param experimentPath the experiment folder path
     * @param recordings     the list of recordings to write
     */
    public static void writeRecordings(Path experimentPath, List<Recording> recordings) {
        RecordingsTableIO recordingsTableIO = new RecordingsTableIO();
        try {
            Table table = recordingsTableIO.toTable(recordings);
            Path  path  = experimentPath.resolve(RECORDINGS_CSV);
            recordingsTableIO.writeCsv(table, path);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", RECORDINGS_CSV, friendlyMessage(e));
        }
    }


    // ───────────────────────────────────────────────────────────────────────────────
    // SQUARES
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Reads all {@link Square} entities from the experiment directory.
     *
     * @param experimentPath the experiment folder path
     * @return a list of squares, or {@code null} if reading fails
     */
    public static List<Square> readSquares(Path experimentPath) {
        SquaresTableIO squaresTableIO = new SquaresTableIO();
        try {
            Table squaresTable = readSquaresTable(experimentPath);
            return squaresTableIO.toEntities(squaresTable);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s: %s", SQUARES_CSV, friendlyMessage(e));
            return null;
        }
    }

    /**
     * Reads all square data into a {@link Table}.
     *
     * @param experimentPath the experiment directory containing {@code squares.csv}
     * @return the loaded table, or {@code null} if reading fails
     */
    public static Table readSquaresTable(Path experimentPath) {
        SquaresTableIO squaresTableIO = new SquaresTableIO();
        try {
            return squaresTableIO.readCsvWithSchema(
                    experimentPath.resolve(SQUARES_CSV),
                    SquareSchema.COLUMNS,
                    SquareSchema.TYPES,
                    false
            );
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s: %s", SQUARES_CSV, friendlyMessage(e));
            return null;
        }
    }

    /**
     * Writes a full {@link Table} of square data to {@code squares.csv}.
     *
     * @param experimentPath the experiment folder path
     * @param squaresTable   the table containing all square data
     */
    public static void writeSquares(Path experimentPath, Table squaresTable) {

        SquaresTableIO squaresTableIO = new SquaresTableIO();
        try {
            Path path = experimentPath.resolve(SQUARES_CSV);
            squaresTableIO.writeCsv(squaresTable, path);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", SQUARES_CSV, friendlyMessage(e));
        }
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // TRACKS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Reads all {@link Track} entities from the experiment directory.
     *
     * @param experimentPath the experiment folder path
     * @return a list of tracks, or {@code null} if reading fails
     */
    public static List<Track> readTracks(Path experimentPath) {
        TracksTableIO tracksTableIO = new TracksTableIO();
        try {
            Table tracksTable = readTracksTable(experimentPath);
            return tracksTableIO.toEntities(tracksTable);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s: %s", TRACKS_CSV, friendlyMessage(e));
            return null;
        }
    }

    /**
     * Reads all track data into a {@link Table}.
     *
     * @param experimentPath the experiment directory containing {@code tracks.csv}
     * @return the loaded table, or {@code null} if reading fails
     */
    public static Table readTracksTable(Path experimentPath) {
        TracksTableIO tracksTableIO = new TracksTableIO();
        try {
            return tracksTableIO.readCsvWithSchema(
                    experimentPath.resolve(TRACKS_CSV),
                    TrackSchema.COLUMNS,
                    TrackSchema.TYPES,
                    false
            );
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s: %s", TRACKS_CSV, friendlyMessage(e));
            return null;
        }
    }


    /**
     * Writes a full {@link Table} of track data to {@code tracks.csv}.
     *
     * @param experimentPath the experiment folder path
     * @param tracksTable    the table containing all track data
     */
    public static void writeTracks(Path experimentPath, Table tracksTable) {
        TracksTableIO tracksTableIO = new TracksTableIO();
        try {
            Path path = experimentPath.resolve(TRACKS_CSV);
            tracksTableIO.writeCsv(tracksTable, path);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", TRACKS_CSV, friendlyMessage(e));
        }
    }
}