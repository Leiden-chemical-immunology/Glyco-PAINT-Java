package io;

import paint.shared.objects.Recording;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintConstants.RECORDING_COLS;
import static paint.shared.constants.PaintConstants.RECORDING_TYPES;

/**
 * Table IO for Recording entities.
 */
public class RecordingTableIO extends BaseTableIO {

    public Table emptyTable() {
        return newEmptyTable("Recordings", RECORDING_COLS, RECORDING_TYPES);
    }

    public Table toTable(List<Recording> recordings) {
        Table table = emptyTable();
        for (Recording rec : recordings) {
            Row row = table.appendRow();
            row.setString("Recording Name", rec.getRecordingName());
            row.setInt("Condition Number", rec.getConditionNumber());
            row.setInt("Replicate Number", rec.getReplicateNumber());
            row.setString("Probe Name", rec.getProbeName());
            row.setString("Probe Type", rec.getProbeType());
            row.setString("Cell Type", rec.getCellType());
            row.setString("Adjuvant", rec.getAdjuvant());
            row.setDouble("Concentration", rec.getConcentration());
            row.setBoolean("Process Flag", rec.isProcessFlag());
            row.setDouble("Threshold", rec.getThreshold());
            row.setInt("Number of Spots", rec.getNumberOfSpots());
            row.setInt("Number of Tracks", rec.getNumberOfTracks());
            row.setInt("Number of Spots in All Tracks", rec.getNumberOfSpotsInAllTracks());
            row.setInt("Number of Frames", rec.getNumberOfFrames());
            row.setDouble("Run Time", rec.getRunTime());
            row.setDateTime("Time Stamp", rec.getTimeStamp());
            row.setBoolean("Exclude", rec.isExclude());
            row.setDouble("Tau", rec.getTau());
            row.setDouble("R Squared", rec.getRSquared());
            row.setDouble("Density", rec.getDensity());
        }
        return table;
    }

    public List<Recording> toEntities(Table table) {
        List<Recording> recordings = new ArrayList<>();
        for (Row row : table) {
            Recording rec = new Recording();
            rec.setRecordingName(row.getString("Recording Name"));
            rec.setConditionNumber(row.getInt("Condition Number"));
            rec.setReplicateNumber(row.getInt("Replicate Number"));
            rec.setProbeName(row.getString("Probe Name"));
            rec.setProbeType(row.getString("Probe Type"));
            rec.setCellType(row.getString("Cell Type"));
            rec.setAdjuvant(row.getString("Adjuvant"));
            rec.setConcentration(row.getDouble("Concentration"));
            rec.setProcessFlag(row.getBoolean("Process Flag"));
            rec.setThreshold(row.getDouble("Threshold"));
            rec.setNumberOfSpots(row.getInt("Number of Spots"));
            rec.setNumberOfTracks(row.getInt("Number of Tracks"));
            rec.setNumberOfSpotsInAllTracks(row.getInt("Number of Spots in All Tracks"));
            rec.setNumberOfFrames(row.getInt("Number of Frames"));
            rec.setRunTime(row.getDouble("Run Time"));
            rec.setTimeStamp(row.getDateTime("Time Stamp"));
            rec.setExclude(row.getBoolean("Exclude"));
            rec.setTau(row.getDouble("Tau"));
            rec.setRSquared(row.getDouble("R Squared"));
            rec.setDensity(row.getDouble("Density"));
            recordings.add(rec);
        }
        return recordings;
    }

    public Table readCsv(Path csvPath) throws IOException {
        return readCsvWithSchema(csvPath, "Recordings", RECORDING_COLS, RECORDING_TYPES, false);
    }

    public void appendInPlace(Table target, Table source) {
        for (Row row : source) {
            Row newRow = target.appendRow();
            for (String col : RECORDING_COLS) {
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