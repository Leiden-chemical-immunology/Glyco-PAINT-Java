package paint.io;

import objects.ExperimentInfo;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import objects.ExperimentInfo;

import static constants.PaintConstants.EXPERIMENT_INFO_COLS;
import static constants.PaintConstants.EXPERIMENT_INFO_TYPES;

/**
 * Table IO for Experiment Info (per-recording metadata).
 */
public class ExperimentTableIO extends BaseTableIO {

    /** Create an empty table with the Experiment Info schema. */
    public Table emptyTable() {
        return newEmptyTable("Experiment Info", EXPERIMENT_INFO_COLS, EXPERIMENT_INFO_TYPES);
    }

    /** Convert a list of ExperimentInfo objects to a Table. */
    public Table toTable(List<ExperimentInfo> infos) {
        Table table = emptyTable();
        for (ExperimentInfo info : infos) {
            Row row = table.appendRow();
            row.setString("Recording Name", info.getRecordingName());
            row.setInt("Condition Number", info.getConditionNumber());
            row.setInt("Replicate Number", info.getReplicateNumber());
            row.setString("Probe Name", info.getProbeName());
            row.setString("Probe Type", info.getProbeType());
            row.setString("Cell Type", info.getCellType());
            row.setString("Adjuvant", info.getAdjuvant());
            row.setDouble("Concentration", info.getConcentration());
            row.setBoolean("Process Flag", info.isProcessFlag());
            row.setDouble("Threshold", info.getThreshold());
        }
        return table;
    }

    /** Convert a Table (already validated with the schema) into ExperimentInfo objects. */
    public List<ExperimentInfo> toEntities(Table table) {
        List<ExperimentInfo> infos = new ArrayList<>();
        for (Row row : table) {
            ExperimentInfo info = new ExperimentInfo();
            info.setRecordingName(row.getString("Recording Name"));
            info.setConditionNumber(row.getInt("Condition Number"));
            info.setReplicateNumber(row.getInt("Replicate Number"));
            info.setProbeName(row.getString("Probe Name"));
            info.setProbeType(row.getString("Probe Type"));
            info.setCellType(row.getString("Cell Type"));
            info.setAdjuvant(row.getString("Adjuvant"));
            info.setConcentration(row.getDouble("Concentration"));
            info.setProcessFlag(row.getBoolean("Process Flag"));
            info.setThreshold(row.getDouble("Threshold"));
            infos.add(info);
        }
        return infos;
    }

    /** Read CSV enforcing the Experiment Info schema. */
    public Table readCsv(Path csvPath) throws IOException {
        return readCsvWithSchema(csvPath, "Experiment Info",
                EXPERIMENT_INFO_COLS, EXPERIMENT_INFO_TYPES, false);
    }

    /**
     * Append all rows from 'source' into 'target' respecting the Experiment Info schema.
     * - Skips missing cells
     * - Allows INTEGER → DOUBLE upcast when expected type is DOUBLE
     */
    public void appendInPlace(Table target, Table source) {
        if (source.isEmpty()) return; // nothing to do

        for (Row srcRow : source) {
            Row dst = target.appendRow();
            int r = srcRow.getRowNumber();

            for (int i = 0; i < EXPERIMENT_INFO_COLS.length; i++) {
                String col = EXPERIMENT_INFO_COLS[i];
                ColumnType expected = EXPERIMENT_INFO_TYPES[i];

                if (!source.columnNames().contains(col)) {
                    continue; // Source missing the column — skip
                }

                Column<?> sCol = source.column(col);
                if (sCol.isMissing(r)) {
                    continue; // Leave as missing in the destination
                }

                // Java 8 style: use if/else instead of switch on enum
                if (expected.equals(ColumnType.STRING)) {
                    dst.setString(col, source.stringColumn(col).get(r));
                }
                else if (expected.equals(ColumnType.INTEGER)) {
                    dst.setInt(col, source.intColumn(col).getInt(r));
                }
                else if (expected.equals(ColumnType.DOUBLE)) {
                    if (sCol.type().equals(ColumnType.INTEGER)) {
                        dst.setDouble(col, source.intColumn(col).getInt(r));
                    } else {
                        dst.setDouble(col, source.doubleColumn(col).getDouble(r));
                    }
                }
                else if (expected.equals(ColumnType.BOOLEAN)) {
                    dst.setBoolean(col, source.booleanColumn(col).get(r));
                }
                else {
                    throw new IllegalArgumentException("Unsupported type: " + expected);
                }
            }
        }
    }
}