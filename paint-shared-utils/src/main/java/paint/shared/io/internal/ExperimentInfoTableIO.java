package paint.shared.io.internal;

import paint.shared.objects.ExperimentInfo;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal schema-validated table I/O implementation for {@link ExperimentInfo}.
 *
 * <p>This class handles:</p>
 * <ul>
 *   <li>Schema-correct table creation</li>
 *   <li>Entity → table row conversion</li>
 *   <li>Table row → entity conversion</li>
 *   <li>Schema-aware append operations</li>
 * </ul>
 *
 * <p>External modules must use MainIOInterface, never this class directly.</p>
 */
public class ExperimentInfoTableIO extends BaseTableIO {

    // =====================================================================
    //  INTERNAL HELPERS
    // =====================================================================

    private String[] getColumnHeaders() {
        ExperimentInfo.Column[] cols = ExperimentInfo.Column.values();
        String[] headers = new String[cols.length];
        for (int i = 0; i < cols.length; i++) headers[i] = cols[i].header;
        return headers;
    }

    private ColumnType[] getColumnTypes() {
        ExperimentInfo.Column[] cols = ExperimentInfo.Column.values();
        ColumnType[] types = new ColumnType[cols.length];
        for (int i = 0; i < cols.length; i++) types[i] = cols[i].type;
        return types;
    }

    // =====================================================================
    //  TABLE CREATION
    // =====================================================================


    public Table emptyTable() {
        return newEmptyTable("Experiment Info", getColumnHeaders(), getColumnTypes());
    }

    // =====================================================================
    //  ENTITY → TABLE CONVERSION
    // =====================================================================


    public Table toTable(List<ExperimentInfo> infos) {
        Table table = emptyTable();

        for (ExperimentInfo info : infos) {
            Row row = table.appendRow();

            row.setString(ExperimentInfo.Column.EXPERIMENT_NAME.header,  info.getExperimentName());
            row.setString(ExperimentInfo.Column.RECORDING_NAME.header,   info.getRecordingName());
            row.setInt(   ExperimentInfo.Column.CONDITION_NUMBER.header, info.getConditionNumber());
            row.setInt(   ExperimentInfo.Column.REPLICATE_NUMBER.header, info.getReplicateNumber());
            row.setString(ExperimentInfo.Column.PROBE_NAME.header,       info.getProbeName());
            row.setString(ExperimentInfo.Column.PROBE_TYPE.header,       info.getProbeType());
            row.setString(ExperimentInfo.Column.CELL_TYPE.header,        info.getCellType());
            row.setString(ExperimentInfo.Column.ADJUVANT.header,         info.getAdjuvant());
            row.setDouble(ExperimentInfo.Column.CONCENTRATION.header,    info.getConcentration());
            row.setBoolean(ExperimentInfo.Column.PROCESS_FLAG.header,    info.isProcessFlagSet());
            row.setDouble(ExperimentInfo.Column.THRESHOLD.header,        info.getThreshold());
        }

        return table;
    }

    // =====================================================================
    //  TABLE → ENTITY CONVERSION
    // =====================================================================

    public List<ExperimentInfo> toEntities(Table table) {
        List<ExperimentInfo> list = new ArrayList<>();

        for (Row row : table) {
            ExperimentInfo info = new ExperimentInfo();

            info.setExperimentName( row.getString( ExperimentInfo.Column.EXPERIMENT_NAME.header ));
            info.setRecordingName(  row.getString( ExperimentInfo.Column.RECORDING_NAME.header ));
            info.setConditionNumber(row.getInt(    ExperimentInfo.Column.CONDITION_NUMBER.header ));
            info.setReplicateNumber(row.getInt(    ExperimentInfo.Column.REPLICATE_NUMBER.header ));
            info.setProbeName(      row.getString( ExperimentInfo.Column.PROBE_NAME.header ));
            info.setProbeType(      row.getString( ExperimentInfo.Column.PROBE_TYPE.header ));
            info.setCellType(       row.getString( ExperimentInfo.Column.CELL_TYPE.header ));
            info.setAdjuvant(       row.getString( ExperimentInfo.Column.ADJUVANT.header ));
            info.setConcentration(  row.getDouble( ExperimentInfo.Column.CONCENTRATION.header ));
            info.setProcessFlag(    row.getBoolean(ExperimentInfo.Column.PROCESS_FLAG.header ));
            info.setThreshold(      row.getDouble( ExperimentInfo.Column.THRESHOLD.header ));

            list.add(info);
        }

        return list;
    }
}