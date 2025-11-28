/*=============================================================================
 *  Class:        MainDataInterface.java
 *  Package:      paint.shared.io
 *
 *  PURPOSE:
 *    High-level facade providing unified read/write operations for all core
 *    PAINT experiment data types: ExperimentInfo, Recordings, Squares, Tracks.
 *
 *  DESCRIPTION:
 *    Wraps the lower-level TableIO classes (ExperimentInfoTableIO,
 *    RecordingsTableIO, SquaresTableIO, TracksTableIO) and exposes simple
 *    static helpers for:
 *
 *      - Reading validated CSV files
 *      - Converting tables to entity lists
 *      - Writing full tables or entity lists
 *
 *    Ensures consistent error handling and logging across all operations.
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

import paint.shared.objects.*;
import paint.shared.schema.*;

import tech.tablesaw.api.Table;

import paint.shared.utils.PaintLogger;

import java.nio.file.Path;
import java.util.List;

import static paint.shared.constants.PaintFileNames.*;
import static paint.shared.utils.Miscellaneous.friendlyMessage;

public final class MainDataInterface {

    private MainDataInterface() { }

    // =====================================================================
    //  EXPERIMENT INFO
    // =====================================================================

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

    public static void writeExperimentInfo(Path experimentPath, List<ExperimentInfo> list) {
        try {
            Path file = experimentPath.resolve(EXPERIMENT_INFO_CSV);
            writeSpecificExperimentInfoFile(file, list);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", EXPERIMENT_INFO_CSV, friendlyMessage(e));
        }
    }

    public static void writeSpecificExperimentInfoFile(Path file, Table table) {
        ExperimentInfoTableIO experimentInfoTableIO = new ExperimentInfoTableIO();
        try {
            experimentInfoTableIO.writeCsv(table, file);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", file, friendlyMessage(e));
        }
    }

    public static void writeSpecificExperimentInfoFile(Path file, List<ExperimentInfo> list) {
        ExperimentInfoTableIO experimentInfoTableIO = new ExperimentInfoTableIO();
        Table table = experimentInfoTableIO.toTable(list);
        writeSpecificExperimentInfoFile(file, table);
    }

    // =====================================================================
    //  RECORDINGS
    // =====================================================================

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

    public static void writeRecordings(Path experimentPath, List<Recording> list) {
        try {
            Path file = experimentPath.resolve(RECORDINGS_CSV);
            writeSpecificRecordingsFile(file, list);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", RECORDINGS_CSV, friendlyMessage(e));
        }
    }

    public static void writeSpecificRecordingsFile(Path file, Table table) {
        RecordingsTableIO recordingsTableIO = new RecordingsTableIO();
        try {
            recordingsTableIO.writeCsv(table, file);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", file, friendlyMessage(e));
        }
    }

    public static void writeSpecificRecordingsFile(Path file, List<Recording> list) {
        RecordingsTableIO recordingsTableIO = new RecordingsTableIO();
        writeSpecificRecordingsFile(file, recordingsTableIO.toTable(list));
    }

    // =====================================================================
    //  SQUARES
    // =====================================================================

    public static List<Square> readSquares(Path experimentPath) {
        try {
            return new SquaresTableIO().toEntities(readSquaresTable(experimentPath));
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s : %s", SQUARES_CSV, friendlyMessage(e));
            return null;
        }
    }

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

    public static void writeSquares(Path experimentPath, List<Square> list) {
        try {
            Path file = experimentPath.resolve(SQUARES_CSV);
            writeSpecificSquaresFile(file, SquaresTableIO.toTable(list));
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", SQUARES_CSV, friendlyMessage(e));
        }
    }

    public static void writeSquares(Path experimentPath, Table table) {
        try {
            Path file = experimentPath.resolve(SQUARES_CSV);
            writeSpecificSquaresFile(file, table);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", SQUARES_CSV, friendlyMessage(e));
        }
    }

    public static void writeSpecificSquaresFile(Path file, Table table) {
        SquaresTableIO squaresTableIO = new SquaresTableIO();
        try {
            squaresTableIO.writeCsv(table, file);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", file, friendlyMessage(e));
        }
    }

    // =====================================================================
    //  TRACKS
    // =====================================================================

    public static List<Track> readTracks(Path experimentPath) {
        TracksTableIO tracksTableIO = new TracksTableIO();
        try {
            Table table = readTracksTable(experimentPath);
            return tracksTableIO.toEntities(table);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s : %s", TRACKS_CSV, friendlyMessage(e));
            return null;
        }
    }

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

    public static void writeTracks(Path experimentPath, Table table) {
        try {
            Path file = experimentPath.resolve(TRACKS_CSV);
            writeSpecificTracksFile(file, table);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", TRACKS_CSV, friendlyMessage(e));
        }
    }

    public static void writeSpecificTracksFile(Path file, Table table) {
        TracksTableIO tracksTableIO = new TracksTableIO();
        try {
            tracksTableIO.writeCsv(table, file);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", file, friendlyMessage(e));
        }
    }
}