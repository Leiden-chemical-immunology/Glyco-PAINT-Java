package paint.shared.io;

import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.objects.Track;
import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static paint.shared.constants.PaintConstants.*;
import static paint.shared.constants.PaintConstants.RECORDINGS_CSV;
import static paint.shared.utils.Miscellaneous.friendlyMessage;

public class HelperIO {

    public static List<Recording> readAllRecordings(Path experimentPath) {

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
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s : %s", RECORDINGS_CSV, friendlyMessage(e));
            return null;
        }
        return recordings;
    }

    public static void writeAllRecordings(Path experimentPath, List<Recording> recordings) {

        RecordingTableIO recordingTableIO = new RecordingTableIO();
        Table table;

        try {
            table = recordingTableIO.toTable(recordings);
        }
        catch (Exception e) {
            PaintLogger.errorf("Failed to create table %s : %s", RECORDINGS_CSV, friendlyMessage(e));
            return;
        }

        try {
            Path path = experimentPath.resolve(RECORDINGS_CSV);
            recordingTableIO.writeCsv(table, path);
        }
        catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", RECORDINGS_CSV, friendlyMessage(e));
        }
    }

    public static List<Square> readAllSquares(Path experimentPath) {

        SquareTableIO squareIO = new SquareTableIO();
        List<Square> squares;
        Table squareTable = readAllSquaresTable(experimentPath);
        try {
            squares = squareIO.toEntities(squareTable);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s: %s", SQUARES_CSV, friendlyMessage(e));
            return null;
        }
        return squares;
    }


    public static Table readAllSquaresTable(Path experimentPath) {

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
            PaintLogger.errorf("Failed to read %s: %s", SQUARES_CSV, friendlyMessage(e));
            return null;
        }
        return squaresTable;
    }


    public static void writeAllSquares(Path experimentPath, List<Square> squares) {

        SquareTableIO squareTableIO = new SquareTableIO();
        Table squaresTable;

        try {
            squaresTable = squareTableIO.toTable(squares);
        }
        catch (Exception e) {
            PaintLogger.errorf("Failed to create table %s : %s", SQUARES_CSV, friendlyMessage(e));
            return;
        }
        writeAllSquares(experimentPath, squaresTable);
    }


    public static void writeAllSquares(Path experimentPath, Table squaresTable) {

        SquareTableIO squareTableIO = new SquareTableIO();

        try {
            Path path = experimentPath.resolve(SQUARES_CSV);
            squareTableIO.writeCsv(squaresTable, path);
        }
        catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", SQUARES_CSV, friendlyMessage(e));
        }
    }

    public static List<Track> readAllTracks(Path experimentPath) {

        TrackTableIO trackIO = new TrackTableIO();
        Table tracksTable;
        List<Track> tracks;

        tracksTable = readAllTracksTable(experimentPath);

        try {
            tracks = trackIO.toEntities(tracksTable);
        } catch (Exception e) {
            PaintLogger.errorf("Failed to read %s: %s", TRACKS_CSV, friendlyMessage(e));
            return null;
        }
        return tracks;
    }


    public static Table readAllTracksTable(Path experimentPath) {

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
            PaintLogger.errorf("Failed to read %s: %s", TRACKS_CSV, friendlyMessage(e));
            return null;
        }
        return tracksTable;
    }


    public static void writeAllTracks(Path experimentPath, List<Track> tracks) {

        TrackTableIO trackTableIO = new TrackTableIO();
        Table tracksTable;

        try {
            tracksTable = trackTableIO.toTable(tracks);
        }
        catch (Exception e) {
            PaintLogger.errorf("Failed to create table %s : %s", TRACKS_CSV, friendlyMessage(e));
            return;
        }
        writeAllTracks(experimentPath, tracksTable);
    }

    public static void writeAllTracks(Path experimentPath, Table tracksTable) {

        TrackTableIO trackTableIO = new TrackTableIO();

        try {
            Path path = experimentPath.resolve(TRACKS_CSV);
            trackTableIO.writeCsv(tracksTable, path);
        }
        catch (Exception e) {
            PaintLogger.errorf("Failed to write %s : %s", TRACKS_CSV, friendlyMessage(e));
        }
    }

    public static void main(String[] args) {

        Path experimentPath = Paths.get("/Users/hans/Paint Test Project/221012");
        List<Recording> recordings = readAllRecordings(experimentPath);
        List<Square> squares = readAllSquares(experimentPath);
        List<Track> tracks = readAllTracks(experimentPath);

        int i  = 1;
    }
}