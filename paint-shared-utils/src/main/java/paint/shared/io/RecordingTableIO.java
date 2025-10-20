package paint.shared.io;

import paint.shared.objects.Recording;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintConstants.*;

/**
 * Table IO for Recording entities.
 */
public class RecordingTableIO extends BaseTableIO {

    public Table emptyTable() {
        return newEmptyTable("Recordings", RECORDINGS_COLS, RECORDINGS_TYPES);
    }

    public Table toTable(List<Recording> recordings) {
        Table table = emptyTable();
        for (Recording recording : recordings) {
            Row row = table.appendRow();

            // @formatter:off
            row.setString(  "Experiment Name",                   recording.getExperimentName());
            row.setString(  "Recording Name",                    recording.getRecordingName());
            row.setInt(     "Condition Number",                  recording.getConditionNumber());
            row.setInt(     "Replicate Number",                  recording.getReplicateNumber());
            row.setString(  "Probe Name",                        recording.getProbeName());
            row.setString(  "Probe Type",                        recording.getProbeType());
            row.setString(  "Cell Type",                         recording.getCellType());
            row.setString(  "Adjuvant",                          recording.getAdjuvant());
            row.setDouble(  "Concentration",                     recording.getConcentration());
            row.setBoolean( "Process Flag",                      recording.isProcessFlag());
            row.setDouble(  "Threshold",                         recording.getThreshold());
            row.setInt(     "Number of Spots",                   recording.getNumberOfSpots());
            row.setInt(     "Number of Tracks",                  recording.getNumberOfTracks());
            row.setInt(     "Number of Squares in Background",   recording.getNumberOfSquaresInBackground());
            row.setInt(     "Number of Tracks in Background",    recording.getNumberOfTracksInBackground());
            row.setDouble(  "Average Tracks in Background",      recording.getAverageTracksInBackGround());
            row.setInt(     "Number of Spots in All Tracks",     recording.getNumberOfSpotsInAllTracks());
            row.setInt(     "Number of Frames",                  recording.getNumberOfFrames());
            row.setDouble(  "Run Time",                          recording.getRunTime());
            row.setDateTime("Time Stamp",                        recording.getTimeStamp());
            row.setBoolean( "Exclude",                           recording.isExclude());
            row.setDouble(  "Tau",                               recording.getTau());
            row.setDouble(  "R Squared",                         recording.getRSquared());
            row.setDouble(  "Density",                           recording.getDensity());
            // @formatter:on
        }
        return table;
    }

    public List<Recording> toEntities(Table table) {
        List<Recording> recordings = new ArrayList<>();
        for (Row row : table) {
            Recording recording = new Recording();

            // @formatter:off
            recording.setExperimentName(              row.getString( "Experiment Name"));
            recording.setRecordingName(               row.getString(  "Recording Name"));
            recording.setConditionNumber(             row.getInt(     "Condition Number"));
            recording.setReplicateNumber(             row.getInt(     "Replicate Number"));
            recording.setProbeName(                   row.getString(  "Probe Name"));
            recording.setProbeType(                   row.getString(  "Probe Type"));
            recording.setCellType(                    row.getString(  "Cell Type"));
            recording.setAdjuvant(                    row.getString(  "Adjuvant"));
            recording.setConcentration(               row.getDouble(  "Concentration"));
            recording.setProcessFlag(                 row.getBoolean( "Process Flag"));
            recording.setThreshold(                   row.getDouble(  "Threshold"));
            recording.setNumberOfSpots(               row.getInt(     "Number of Spots"));
            recording.setNumberOfTracks(              row.getInt(     "Number of Tracks"));
            recording.setNumberOfSquaresInBackground( row.getInt(     "Number of Squares in Background"));
            recording.setNumberOfTracksInBackground(  row.getInt(     "Number of Tracks in Background"));
            recording.setAverageTracksInBackGround(   row.getDouble(  "Average Tracks in Background"));
            recording.setNumberOfSpotsInAllTracks(    row.getInt(     "Number of Spots in All Tracks"));
            recording.setNumberOfFrames(              row.getInt(     "Number of Frames"));
            recording.setRunTime(                     row.getDouble(  "Run Time"));
            recording.setTimeStamp(                   row.getDateTime("Time Stamp"));
            recording.setExclude(                     row.getBoolean( "Exclude"));
            recording.setTau(                         row.getDouble(  "Tau"));
            recording.setRSquared(                    row.getDouble(  "R Squared"));
            recording.setDensity(                     row.getDouble("Density"));
            // @formatter:on

            recordings.add(recording);
        }
        return recordings;
    }

    public Table readCsv(Path csvPath) throws IOException {
        return readCsvWithSchema(csvPath, RECORDINGS, RECORDINGS_COLS, RECORDINGS_TYPES, false);
    }

    public void appendInPlace(Table target, Table source) {
        for (Row row : source) {
            Row newRow = target.appendRow();
            for (String col : RECORDINGS_COLS) {
                Column<?> targetCol = target.column(col);
                if (targetCol.type() == ColumnType.STRING) {
                    newRow.setString(col, row.getString(col));
                } else if (targetCol.type() == ColumnType.INTEGER) {
                    newRow.setInt(col, row.getInt(col));
                } else if (targetCol.type() == ColumnType.DOUBLE) {
                    newRow.setDouble(col, row.getDouble(col));
                } else if (targetCol.type() == ColumnType.BOOLEAN) {
                    newRow.setBoolean(col, row.getBoolean(col));
                }
            }
        }
    }
}