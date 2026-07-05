/*=============================================================================
 *  Class:        ExperimentInfoTableIO.java
 *  Package:      paint.shared.io.internal
 *
 *  PURPOSE:
 *    Handles low-level I/O and data conversion between PAINT {@link ExperimentInfo}
 *    objects and Tablesaw {@link Table} structures.
 *
 *  DESCRIPTION:
 *    The {@code ExperimentInfoTableIO} class provides implementation for mapping
 *    experiment metadata fields to CSV columns and vice versa. It manages the
 *    schema specific to "experiment_info.csv" files, ensuring that data types
 *    and headers are correctly applied during conversion.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *=============================================================================*/

package paint.shared.io.internal;

import paint.shared.objects.ExperimentInfo;

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
    //  TABLE CREATION
    // =====================================================================

    /**
     * @return a new empty {@link Table} with the Experiment Info schema.
     */
    public Table emptyTable() {
        return newEmptyTable("Experiment Info", ExperimentInfo.Column.values(), c -> c.header, c -> c.type);
    }

    // =====================================================================
    //  ENTITY → TABLE CONVERSION
    // =====================================================================

    /**
     * Converts a list of {@link ExperimentInfo} entities into a Tablesaw {@link Table}.
     *
     * @param infos list of experiment info objects to convert
     * @return a table containing the metadata
     */
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

    /**
     * Converts a Tablesaw {@link Table} into a list of {@link ExperimentInfo} entities.
     *
     * @param table the table to convert
     * @return a list of experiment info objects
     */
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