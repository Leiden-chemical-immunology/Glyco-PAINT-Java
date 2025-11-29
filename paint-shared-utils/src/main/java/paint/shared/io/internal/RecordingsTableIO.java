package paint.shared.io.internal;

import paint.shared.io.MainIOInterface;
import paint.shared.objects.Recording;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintStringConstants.*;

public class RecordingsTableIO extends BaseTableIO {

    // =====================================================================
    //  INTERNAL HELPERS (extract schema from Recording.Column)
    // =====================================================================

    private String[] getColumnHeaders() {
        Recording.Column[] cols = Recording.Column.values();
        String[] headers = new String[cols.length];
        for (int i = 0; i < cols.length; i++) {
            headers[i] = cols[i].header;
        }
        return headers;
    }

    private ColumnType[] getColumnTypes() {
        Recording.Column[] cols = Recording.Column.values();
        ColumnType[] types = new ColumnType[cols.length];
        for (int i = 0; i < cols.length; i++) {
            types[i] = cols[i].type;
        }
        return types;
    }

    // =====================================================================
    //  TABLE CREATION
    // =====================================================================

    public Table emptyTable() {
        return newEmptyTable(
                "Recordings",
                getColumnHeaders(),
                getColumnTypes()
        );
    }

    // =====================================================================
    //  ENTITY → TABLE CONVERSION
    // =====================================================================

    public Table toTable(List<Recording> recordings) {
        Table table = emptyTable();

        for (Recording recording : recordings) {
            Row row = table.appendRow();

            row.setString(  EXPERIMENT_NAME,                 recording.getExperimentName());
            row.setString(  RECORDING_NAME,                  recording.getRecordingName());
            row.setInt(     CONDITION_NUMBER,                recording.getConditionNumber());
            row.setInt(     REPLICATE_NUMBER,                recording.getReplicateNumber());
            row.setString(  PROBE_NAME,                      recording.getProbeName());
            row.setString(  PROBE_TYPE,                      recording.getProbeType());
            row.setString(  CELL_TYPE,                       recording.getCellType());
            row.setString(  ADJUVANT,                        recording.getAdjuvant());
            row.setDouble(  CONCENTRATION,                   recording.getConcentration());
            row.setBoolean( PROCESS_FLAG,                    recording.isProcessFlagSet());
            row.setDouble(  THRESHOLD,                       recording.getThreshold());
            row.setInt(     NUMBER_OF_SPOTS,                 recording.getNumberOfSpots());
            row.setInt(     NUMBER_OF_TRACKS,                recording.getNumberOfTracks());
            row.setInt(     NUMBER_OF_SQUARES_IN_BACKGROUND, recording.getNumberOfSquaresInBackground());
            row.setInt(     NUMBER_OF_TRACKS_IN_BACKGROUND,  recording.getNumberOfTracksInBackground());
            row.setDouble(  AVERAGE_TRACKS_IN_BACKGROUND,    recording.getAverageTracksInBackGround());
            row.setInt(     NUMBER_OF_SPOTS_IN_ALL_TRACKS,   recording.getNumberOfSpotsInAllTracks());
            row.setInt(     NUMBER_OF_FRAMES,                recording.getNumberOfFrames());
            row.setDouble(  RUN_TIME,                        recording.getRunTime());
            row.setDateTime(TIME_STAMP,                      recording.getTimeStamp());
            row.setBoolean( EXCLUDE,                         recording.isExclude());
            row.setDouble(  TAU,                             recording.getTau());
            row.setDouble(  R_SQUARED,                       recording.getRSquared());
            row.setDouble(  DENSITY,                         recording.getDensity());
            row.setDouble(  MIN_REQUIRED_DENSITY_RATIO,      recording.getMinRequiredDensityRatio());
            row.setDouble(  MIN_REQUIRED_R_SQUARED,          recording.getMinRequiredRSquared());
            row.setDouble(  MAX_ALLOWABLE_VARIABILITY,       recording.getMaxAllowableVariability());
            row.setString(  NEIGHBOUR_MODE,                  recording.getNeighbourMode());
        }

        return table;
    }

    // =====================================================================
    //  TABLE → ENTITY CONVERSION
    // =====================================================================

    public List<Recording> toEntities(Table table) {
        List<Recording> recordings = new ArrayList<>();

        for (Row tablesawRow : table) {
            Recording recording = new Recording();

            recording.setExperimentName(              tablesawRow.getString(   EXPERIMENT_NAME));
            recording.setRecordingName(               tablesawRow.getString(   RECORDING_NAME));
            recording.setConditionNumber(             tablesawRow.getInt(      CONDITION_NUMBER));
            recording.setReplicateNumber(             tablesawRow.getInt(      REPLICATE_NUMBER));
            recording.setProbeName(                   tablesawRow.getString(   PROBE_NAME));
            recording.setProbeType(                   tablesawRow.getString(   PROBE_TYPE));
            recording.setCellType(                    tablesawRow.getString(   CELL_TYPE));
            recording.setAdjuvant(                    tablesawRow.getString(   ADJUVANT));
            recording.setConcentration(               tablesawRow.getDouble(   CONCENTRATION));
            recording.setProcessFlag(                 tablesawRow.getBoolean(  PROCESS_FLAG));
            recording.setThreshold(                   tablesawRow.getDouble(   THRESHOLD));
            recording.setNumberOfSpots(               tablesawRow.getInt(      NUMBER_OF_SPOTS));
            recording.setNumberOfTracks(              tablesawRow.getInt(      NUMBER_OF_TRACKS));
            recording.setNumberOfSquaresInBackground( tablesawRow.getInt(      NUMBER_OF_SQUARES_IN_BACKGROUND));
            recording.setNumberOfTracksInBackground(  tablesawRow.getInt(      NUMBER_OF_TRACKS_IN_BACKGROUND));
            recording.setAverageTracksInBackGround(   tablesawRow.getDouble(   AVERAGE_TRACKS_IN_BACKGROUND));
            recording.setNumberOfSpotsInAllTracks(    tablesawRow.getInt(      NUMBER_OF_SPOTS_IN_ALL_TRACKS));
            recording.setNumberOfFrames(              tablesawRow.getInt(      NUMBER_OF_FRAMES));
            recording.setRunTime(                     tablesawRow.getDouble(   RUN_TIME));
            recording.setTimeStamp(                   tablesawRow.getDateTime( TIME_STAMP));
            recording.setExclude(                     tablesawRow.getBoolean(  EXCLUDE));
            recording.setTau(                         tablesawRow.getDouble(   TAU));
            recording.setRSquared(                    tablesawRow.getDouble(   R_SQUARED));
            recording.setDensity(                     tablesawRow.getDouble(   DENSITY));
            recording.setMinRequiredDensityRatio(     tablesawRow.getDouble(   MIN_REQUIRED_DENSITY_RATIO));
            recording.setMinRequiredRSquared(         tablesawRow.getDouble(   MIN_REQUIRED_R_SQUARED));
            recording.setMaxAllowableVariability(     tablesawRow.getDouble(   MAX_ALLOWABLE_VARIABILITY));
            recording.setNeighbourMode(               tablesawRow.getString(   NEIGHBOUR_MODE));

            recordings.add(recording);
        }

        return recordings;
    }

    // =====================================================================
    //  APPEND / MERGE
    // =====================================================================

    public void appendInPlace(Table target, Table source) {
        for (Row row : source) {
            Row newRow = target.appendRow();

            for (Recording.Column colEnum : Recording.Column.values()) {
                String col = colEnum.header;
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