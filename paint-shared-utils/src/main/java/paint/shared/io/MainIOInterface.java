/*=============================================================================
 *  Class:        MainIOInterface.java
 *  Package:      paint.shared.io
 *
 *  PURPOSE:
 *    Public façade providing unified, stable read/write operations for all
 *    major PAINT experiment data types:
 *
 *        • ExperimentInfo
 *        • Recording
 *        • Square
 *        • Track
 *
 *    This is the ONLY public I/O entry point external modules should depend on.
 *    All low-level CSV parsing, schema validation, and entity conversion is
 *    delegated to package-private TableIO classes:
 *
 *        ExperimentInfoTableIO
 *        RecordingsTableIO
 *        SquaresTableIO
 *        TracksTableIO
 *
 *    These classes reside inside paint.shared.io.internal and remain hidden
 *    from external consumers.
 *
 *  DESCRIPTION:
 *    For each data type, this interface exposes:
 *
 *        • Reading validated CSV → Tablesaw Table
 *        • Reading validated CSV → List<E>
 *        • Writing Table or List<E> → CSV
 *        • Convenience helpers:
 *              – Entity list ↔ Table conversion
 *              – Schema-compliant empty table creation
 *              – Append operations
 *
 *    All error reporting is routed through PaintLogger using friendlyMessage().
 *    All schema enforcement relies on paint.shared.schema.* definitions.
 *
 *  DESIGN NOTES:
 *    • Class is stateless; all methods are static.
 *    • TableIO instances are created on demand and contain no state.
 *    • Strict header + type validation is performed by BaseTableIO.
 *    • Fully compatible with Java 8 and Tablesaw 0.43+.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-11-29
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *============================================================================*/

package paint.shared.io;

import paint.shared.io.internal.ExperimentInfoTableIO;
import paint.shared.io.internal.RecordingsTableIO;
import paint.shared.io.internal.SquaresTableIO;
import paint.shared.io.internal.TracksTableIO;

import paint.shared.objects.ExperimentInfo;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.objects.Track;

import paint.shared.schema.ExperimentInfoSchema;
import paint.shared.schema.RecordingSchema;
import paint.shared.schema.SquareSchema;
import paint.shared.schema.TrackSchema;

import tech.tablesaw.api.Table;

import paint.shared.utils.PaintLogger;

import java.nio.file.Path;
import java.util.List;

import static paint.shared.constants.PaintFileNames.EXPERIMENT_INFO_CSV;
import static paint.shared.constants.PaintFileNames.RECORDINGS_CSV;
import static paint.shared.constants.PaintFileNames.SQUARES_CSV;
import static paint.shared.constants.PaintFileNames.TRACKS_CSV;

import static paint.shared.utils.Miscellaneous.friendlyMessage;

public final class MainIOInterface {

    private MainIOInterface() { }

    // =====================================================================
    //  EXPERIMENT INFO
    // =====================================================================

    /** Reads experiment-info.csv into a List<ExperimentInfo>. */
    public static List<ExperimentInfo> readExperimentInfo(Path experimentPath) {
        ExperimentInfoTableIO experimentInfoTableIO = new ExperimentInfoTableIO();
        try {
            Table table = experimentInfoTableIO.readCsvWithSchema(
                    experimentPath.resolve(EXPERIMENT_INFO_CSV),
                    ExperimentInfoSchema.COLUMNS,
                    ExperimentInfoSchema.TYPES,
                    false
            );
            return experimentInfoTableIO.toEntities(table);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s : %s", EXPERIMENT_INFO_CSV, friendlyMessage(e));
            return null;
        }
    }

    /** Writes List<ExperimentInfo> to experiment-info.csv. */
    public static void writeExperimentInfo(Path experimentPath, List<ExperimentInfo> list) {
        try {
            writeSpecificExperimentInfoFile(experimentPath.resolve(EXPERIMENT_INFO_CSV), list);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", EXPERIMENT_INFO_CSV, friendlyMessage(e));
        }
    }

    /** Writes a Table to experiment-info.csv. */
    public static void writeSpecificExperimentInfoFile(Path file, Table table) {
        ExperimentInfoTableIO experimentInfoTableIO = new ExperimentInfoTableIO();
        try {
            experimentInfoTableIO.writeCsv(table, file);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", file, friendlyMessage(e));
        }
    }

    /** Converts list → table → writes to CSV. */
    public static void writeSpecificExperimentInfoFile(Path file, List<ExperimentInfo> list) {
        ExperimentInfoTableIO experimentInfoTableIO = new ExperimentInfoTableIO();
        writeSpecificExperimentInfoFile(file, experimentInfoTableIO.toTable(list));
    }

    // Convenience Helpers (Experiment Info)
    public static List<ExperimentInfo> experimentInfoTableToList(Table table) {
        return new ExperimentInfoTableIO().toEntities(table);
    }

    public static Table experimentInfoListToTable(List<ExperimentInfo> list) {
        return new ExperimentInfoTableIO().toTable(list);
    }

    public static Table newEmptyExperimentInfoTable() {
        return new ExperimentInfoTableIO().emptyTable();
    }

    // =====================================================================
    //  RECORDINGS
    // =====================================================================

    /** Reads recordings.csv into a List<Recording>. */
    public static List<Recording> readRecordings(Path experimentPath) {
        RecordingsTableIO recordingsTableIO = new RecordingsTableIO();
        try {
            Table table = recordingsTableIO.readCsvWithSchema(
                    experimentPath.resolve(RECORDINGS_CSV),
                    RecordingSchema.COLUMNS,
                    RecordingSchema.TYPES,
                    false
            );
            return recordingsTableIO.toEntities(table);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s : %s", RECORDINGS_CSV, friendlyMessage(e));
            return null;
        }
    }

    /** Reads recordings.csv → Table. */
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
            PaintLogger.errorf("Failed to read %s : %s", RECORDINGS_CSV, friendlyMessage(e));
            return null;
        }
    }

    /** Writes List<Recording> → recordings.csv. */
    public static void writeRecordings(Path experimentPath, List<Recording> list) {
        try {
            writeSpecificRecordingsFile(experimentPath.resolve(RECORDINGS_CSV), list);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", RECORDINGS_CSV, friendlyMessage(e));
        }
    }

    /** Writes Table → recordings.csv. */
    public static void writeSpecificRecordingsFile(Path file, Table table) {
        RecordingsTableIO recordingsTableIO = new RecordingsTableIO();
        try {
            recordingsTableIO.writeCsv(table, file);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", file, friendlyMessage(e));
        }
    }

    /** Converts list → table → writes to CSV. */
    public static void writeSpecificRecordingsFile(Path file, List<Recording> list) {
        writeSpecificRecordingsFile(file, new RecordingsTableIO().toTable(list));
    }

    // Convenience Helpers (Recordings)
    public static List<Recording> recordingTableToList(Table table) {
        return new RecordingsTableIO().toEntities(table);
    }

    public static Table recordingListToTable(List<Recording> recordings) {
        return new RecordingsTableIO().toTable(recordings);
    }

    public static Table newEmptyRecordingTable() {
        return new RecordingsTableIO().emptyTable();
    }

    // =====================================================================
    //  SQUARES
    // =====================================================================

    /** Reads squares.csv into a List<Square>. */
    public static List<Square> readSquares(Path experimentPath) {
        try {
            return new SquaresTableIO().toEntities(readSquaresTable(experimentPath));
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s : %s", SQUARES_CSV, friendlyMessage(e));
            return null;
        }
    }

    /** Reads squares.csv → Table. */
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
            PaintLogger.errorf("Failed to read %s : %s", SQUARES_CSV, friendlyMessage(e));
            return null;
        }
    }

    /** Writes List<Square> → squares.csv. */
    public static void writeSquares(Path experimentPath, List<Square> list) {
        try {
            writeSpecificSquaresFile(experimentPath.resolve(SQUARES_CSV),
                                     new SquaresTableIO().toTable(list));
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", SQUARES_CSV, friendlyMessage(e));
        }
    }

    /** Writes Table → squares.csv. */
    public static void writeSquares(Path experimentPath, Table table) {
        try {
            writeSpecificSquaresFile(experimentPath.resolve(SQUARES_CSV), table);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", SQUARES_CSV, friendlyMessage(e));
        }
    }

    /** Writes a Table to squares.csv. */
    public static void writeSpecificSquaresFile(Path file, Table table) {
        SquaresTableIO squaresTableIO = new SquaresTableIO();
        try {
            squaresTableIO.writeCsv(table, file);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", file, friendlyMessage(e));
        }
    }

    // Convenience Helpers (Squares)
    public static List<Square> squareTableToList(Table table) {
        return new SquaresTableIO().toEntities(table);
    }

    public static Table squareListToTable(List<Square> squares) {
        return new SquaresTableIO().toTable(squares);
    }

    public static Table newEmptySquareTable() {
        return new SquaresTableIO().emptyTable();
    }

    /** Appends rows from one Square table to another in place. */
    public static void appendSquareTableInPlace(Table target, Table source) {
        new SquaresTableIO().appendInPlace(target, source);
    }

    // =====================================================================
    //  TRACKS
    // =====================================================================

    /** Reads tracks.csv into a List<Track>. */
    public static List<Track> readTracks(Path experimentPath) {
        try {
            Table readTracksTable = readTracksTable(experimentPath);
            return new TracksTableIO().toEntities(readTracksTable);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s : %s", TRACKS_CSV, friendlyMessage(e));
            return null;
        }
    }

    /** Reads tracks.csv → Table. */
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
            PaintLogger.errorf("Failed to read %s : %s", TRACKS_CSV, friendlyMessage(e));
            return null;
        }
    }

    /** Writes Table → tracks.csv. */
    public static void writeTracks(Path experimentPath, Table table) {
        try {
            writeSpecificTracksFile(experimentPath.resolve(TRACKS_CSV), table);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", TRACKS_CSV, friendlyMessage(e));
        }
    }

    /** Writes a Table to tracks.csv. */
    public static void writeSpecificTracksFile(Path file, Table table) {
        TracksTableIO tracksTableIO = new TracksTableIO();
        try {
            tracksTableIO.writeCsv(table, file);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", file, friendlyMessage(e));
        }
    }

    // Convenience Helpers (Tracks)
    public static List<Track> trackTableToList(Table table) {
        return new TracksTableIO().toEntities(table);
    }

    public static Table trackListToTable(List<Track> tracks) {
        return new TracksTableIO().toTable(tracks);
    }

    public static Table newEmptyTrackTable() {
        return new TracksTableIO().emptyTable();
    }

    /** Appends rows from one Track table to another in place. */
    public static void appendTrackTableInPlace(Table target, Table source) {
        new TracksTableIO().appendInPlace(target, source);
    }
}