package paint.shared.io;

import paint.shared.io.internal.ExperimentInfoTableIO;
import paint.shared.io.internal.RecordingsTableIO;
import paint.shared.io.internal.SquaresTableIO;
import paint.shared.io.internal.TracksTableIO;

import paint.shared.objects.ExperimentInfo;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.objects.Track;

import tech.tablesaw.api.ColumnType;
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
    //  INTERNAL HELPERS — Extract schema arrays from Column enums
    // =====================================================================

    private static String[] extractHeadersExperimentInfo() {
        ExperimentInfo.Column[] cols = ExperimentInfo.Column.values();
        String[]                arr  = new String[cols.length];
        for (int i = 0; i < cols.length; i++) {
            arr[i] = cols[i].header;
        }
        return arr;
    }

    private static ColumnType[] extractTypesExperimentInfo() {
        ExperimentInfo.Column[] cols = ExperimentInfo.Column.values();
        ColumnType[]            arr  = new ColumnType[cols.length];
        for (int i = 0; i < cols.length; i++) {
            arr[i] = cols[i].type;
        }
        return arr;
    }

    private static String[] extractHeadersRecording() {
        Recording.Column[] cols = Recording.Column.values();
        String[]           arr  = new String[cols.length];
        for (int i = 0; i < cols.length; i++) {
            arr[i] = cols[i].header;
        }
        return arr;
    }

    private static ColumnType[] extractTypesRecording() {
        Recording.Column[] cols = Recording.Column.values();
        ColumnType[]       arr  = new ColumnType[cols.length];
        for (int i = 0; i < cols.length; i++) {
            arr[i] = cols[i].type;
        }
        return arr;
    }

    private static String[] extractHeadersSquare() {
        Square.Column[] cols = Square.Column.values();
        String[]        arr  = new String[cols.length];
        for (int i = 0; i < cols.length; i++) {
            arr[i] = cols[i].header;
        }
        return arr;
    }

    private static ColumnType[] extractTypesSquare() {
        Square.Column[] cols = Square.Column.values();
        ColumnType[]    arr  = new ColumnType[cols.length];
        for (int i = 0; i < cols.length; i++) {
            arr[i] = cols[i].type;
        }
        return arr;
    }

    private static String[] extractHeadersTrack() {
        Track.Column[] cols = Track.Column.values();
        String[]       arr  = new String[cols.length];
        for (int i = 0; i < cols.length; i++) {
            arr[i] = cols[i].header;
        }
        return arr;
    }

    private static ColumnType[] extractTypesTrack() {
        Track.Column[] cols = Track.Column.values();
        ColumnType[]   arr  = new ColumnType[cols.length];
        for (int i = 0; i < cols.length; i++) {
            arr[i] = cols[i].type;
        }
        return arr;
    }


    // =====================================================================
    //  EXPERIMENT INFO
    // =====================================================================

    public static List<ExperimentInfo> readExperimentInfo(Path experimentPath) {
        ExperimentInfoTableIO experimentInfoTableIO = new ExperimentInfoTableIO();
        try {
            Table table = experimentInfoTableIO.readCsvWithSchema(
                    experimentPath.resolve(EXPERIMENT_INFO_CSV),
                    extractHeadersExperimentInfo(),
                    extractTypesExperimentInfo(),
                    false
            );
            return experimentInfoTableIO.toEntities(table);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s : %s",  EXPERIMENT_INFO_CSV, friendlyMessage(e));
            return null;
        }
    }

    public static void writeExperimentInfo(Path experimentPath, List<ExperimentInfo> list) {
        try {
            writeSpecificExperimentInfoFile(experimentPath.resolve(EXPERIMENT_INFO_CSV), list);
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
        writeSpecificExperimentInfoFile(file, experimentInfoTableIO.toTable(list));
    }

    public static List<ExperimentInfo> experimentInfoTableToList(Table t) {
        return new ExperimentInfoTableIO().toEntities(t);
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

    public static List<Recording> readRecordings(Path experimentPath) {
        RecordingsTableIO recordingsTableIO = new RecordingsTableIO();
        try {
            Table table = recordingsTableIO.readCsvWithSchema(
                    experimentPath.resolve(RECORDINGS_CSV),
                    extractHeadersRecording(),
                    extractTypesRecording(),
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
                    extractHeadersRecording(),
                    extractTypesRecording(),
                    false
            );
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s : %s", RECORDINGS_CSV, friendlyMessage(e));
            return null;
        }
    }

    public static void writeRecordings(Path experimentPath, List<Recording> list) {
        try {
            writeSpecificRecordingsFile(experimentPath.resolve(RECORDINGS_CSV), list);
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
        writeSpecificRecordingsFile(file, new RecordingsTableIO().toTable(list));
    }

    public static List<Recording> recordingTableToList(Table table) {
        return new RecordingsTableIO().toEntities(table);
    }

    public static Table recordingListToTable(List<Recording> list) {
        return new RecordingsTableIO().toTable(list);
    }

    public static Table newEmptyRecordingTable() {
        return new RecordingsTableIO().emptyTable();
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
                    extractHeadersSquare(),
                    extractTypesSquare(),
                    false
            );
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s : %s", SQUARES_CSV, friendlyMessage(e));
            return null;
        }
    }

    public static void writeSquares(Path experimentPath, List<Square> list) {
        try {
            writeSpecificSquaresFile(
                    experimentPath.resolve(SQUARES_CSV),
                    new SquaresTableIO().toTable(list)
            );
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", SQUARES_CSV, friendlyMessage(e));
        }
    }

    public static void writeSquares(Path experimentPath, Table table) {
        try {
            writeSpecificSquaresFile(experimentPath.resolve(SQUARES_CSV), table);
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

    public static List<Square> squareTableToList(Table table) {
        return new SquaresTableIO().toEntities(table);
    }

    public static Table squareListToTable(List<Square> squares) {
        return new SquaresTableIO().toTable(squares);
    }

    public static Table newEmptySquareTable() {
        return new SquaresTableIO().emptyTable();
    }

    public static void appendSquareTableInPlace(Table target, Table source) {
        new SquaresTableIO().appendInPlace(target, source);
    }


    // =====================================================================
    //  TRACKS
    // =====================================================================

    public static List<Track> readTracks(Path experimentPath) {
        try {
            Table table = readTracksTable(experimentPath);
            return new TracksTableIO().toEntities(table);
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
                    extractHeadersTrack(),
                    extractTypesTrack(),
                    false
            );
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s : %s", TRACKS_CSV, friendlyMessage(e));
            return null;
        }
    }

    public static void writeTracks(Path experimentPath, Table table) {
        try {
            writeSpecificTracksFile(experimentPath.resolve(TRACKS_CSV), table);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", TRACKS_CSV, friendlyMessage(e));
        }
    }

    public static void writeSpecificTracksFile(Path file, Table table) {
        TracksTableIO tracksTableIO = new TracksTableIO();
        try {
            tracksTableIO.writeCsv(table, file);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s",  file, friendlyMessage(e));
        }
    }

    public static List<Track> trackTableToList(Table table) {
        return new TracksTableIO().toEntities(table);
    }

    public static Table trackListToTable(List<Track> tracks) {
        return new TracksTableIO().toTable(tracks);
    }

    public static Table newEmptyTrackTable() {
        return new TracksTableIO().emptyTable();
    }

    public static void appendTrackTableInPlace(Table target, Table source) {
        new TracksTableIO().appendInPlace(target, source);
    }
}