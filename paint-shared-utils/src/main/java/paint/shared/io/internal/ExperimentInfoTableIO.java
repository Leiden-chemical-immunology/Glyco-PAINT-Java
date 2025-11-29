/*=============================================================================
 *  Class:        ExperimentInfoTableIO.java
 *  Package:      paint.shared.io.internal
 *
 *  PURPOSE:
 *    Internal implementation of CSV and table I/O for
 *    {@link paint.shared.objects.ExperimentInfo}.  Although this class is
 *    declared public so it can be accessed by
 *    {@link paint.shared.io.MainDataInterface}, it is NOT part of PAINT’s
 *    public API. External modules must never call this class directly.
 *
 *  DESCRIPTION:
 *    Provides the low-level, schema-validated I/O logic for Experiment Info:
 *
 *      • Creating schema-compliant Tablesaw tables
 *      • Converting between {@link ExperimentInfo} entities and table rows
 *      • Reading CSV files with strict header and type enforcement (via BaseTableIO)
 *      • Performing safe append operations with controlled type coercion
 *
 *    {@link MainDataInterface} is the only supported entry point for external
 *    read/write operations. This class remains an internal implementation detail.
 *
 *  DESIGN NOTES:
 *    • Visibility is public ONLY because package-private classes inside
 *      'paint.shared.io.internal' cannot be accessed by MainDataInterface
 *      (in a different package).
 *    • Despite being public, this class is considered INTERNAL API.
 *    • All schema structure is defined in {@link paint.shared.schema.ExperimentInfoSchema}.
 *    • Fully compatible with Java 8 and Tablesaw 0.43+.
 *
 *  AUTHOR:       Hans Bakker
 *  MODULE:       paint-shared-utils
 *  UPDATED:      2025-10-28
 *  COPYRIGHT:    © 2025 Hans Bakker. All rights reserved.
 *============================================================================*/

package paint.shared.io.internal;

import paint.shared.io.MainIOInterface;
import paint.shared.objects.ExperimentInfo;
import paint.shared.schema.ExperimentInfoSchema;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintStringConstants.*;

/**
 * Internal table I/O implementation for {@link ExperimentInfo}.
 *
 * <p>This class enforces the schema defined by
 * {@link ExperimentInfoSchema#COLUMNS} and {@link ExperimentInfoSchema#TYPES}
 * and provides:</p>
 *
 * <ul>
 *   <li>Creation of empty schema-correct Tablesaw tables</li>
 *   <li>Conversion between entities and Tablesaw rows</li>
 *   <li>Schema-validated CSV read operations (via BaseTableIO)</li>
 *   <li>Safe append operations with type checking</li>
 * </ul>
 *
 * <p>External callers must use {@link MainIOInterface}.</p>
 */
public class ExperimentInfoTableIO extends BaseTableIO {

    // =====================================================================
    //  TABLE CREATION
    // =====================================================================

    /**
     * Creates a new empty Experiment Info table with the correct schema.
     */
    public Table emptyTable() {
        return newEmptyTable("Experiment Info",
                             ExperimentInfoSchema.COLUMNS,
                             ExperimentInfoSchema.TYPES);
    }

    // =====================================================================
    //  ENTITY → TABLE CONVERSION
    // =====================================================================

    /**
     * Converts a list of {@link ExperimentInfo} into a schema-validated table.
     */
    public Table toTable(List<ExperimentInfo> infos) {
        Table table = emptyTable();

        for (ExperimentInfo experimentInfo : infos) {
            Row row = table.appendRow();

            row.setString(  EXPERIMENT_NAME,  experimentInfo.getExperimentName());
            row.setString(  RECORDING_NAME,   experimentInfo.getRecordingName());
            row.setInt(     CONDITION_NUMBER, experimentInfo.getConditionNumber());
            row.setInt(     REPLICATE_NUMBER, experimentInfo.getReplicateNumber());
            row.setString(  PROBE_NAME,       experimentInfo.getProbeName());
            row.setString(  PROBE_TYPE,       experimentInfo.getProbeType());
            row.setString(  CELL_TYPE,        experimentInfo.getCellType());
            row.setString(  ADJUVANT,         experimentInfo.getAdjuvant());
            row.setDouble(  CONCENTRATION,    experimentInfo.getConcentration());
            row.setBoolean( PROCESS_FLAG,     experimentInfo.isProcessFlagSet());
            row.setDouble(  THRESHOLD,        experimentInfo.getThreshold());
        }

        return table;
    }

    // =====================================================================
    //  TABLE → ENTITY CONVERSION
    // =====================================================================

    /**
     * Converts a schema-validated Experiment Info table into a list of entities.
     */
    public List<ExperimentInfo> toEntities(Table table) {
        List<ExperimentInfo> items = new ArrayList<>();

        for (Row row : table) {
            ExperimentInfo info = new ExperimentInfo();

            info.setExperimentName(  row.getString(0));
            info.setRecordingName(   row.getString(1));
            info.setConditionNumber( row.getInt(2));
            info.setReplicateNumber( row.getInt(3));
            info.setProbeName(       row.getString(4));
            info.setProbeType(       row.getString(5));
            info.setCellType(        row.getString(6));
            info.setAdjuvant(        row.getString(7));
            info.setConcentration(   row.getDouble(8));
            info.setProcessFlag(     row.getBoolean(9));

            items.add(info);
        }

        return items;
    }

    // =====================================================================
    //  APPEND / MERGE OPERATIONS
    // =====================================================================

    /**
     * Appends all rows from {@code source} into {@code target}, enforcing
     * the Experiment Info schema and performing safe type conversions.
     *
     * <p>INTEGER → DOUBLE upcasting is supported where required.</p>
     */
    public void appendInPlace(Table target, Table source) {

        if (source.isEmpty())
            return;

        for (Row srcRow : source) {
            Row dst = target.appendRow();
            int r   = srcRow.getRowNumber();

            for (int i = 0; i < ExperimentInfoSchema.COLUMNS.length; i++) {
                String     col      = ExperimentInfoSchema.COLUMNS[i];
                ColumnType expected = ExperimentInfoSchema.TYPES[i];

                if (!source.columnNames().contains(col)) {
                    continue; // Source missing the column — skip
                }

                Column<?> sCol = source.column(col);
                if (sCol.isMissing(r)) {
                    continue; // Leave as missing in the destination
                }

                if (expected.equals(ColumnType.STRING)) {
                    dst.setString(col, source.stringColumn(col).get(r));

                } else if (expected.equals(ColumnType.INTEGER)) {
                    dst.setInt(col, source.intColumn(col).getInt(r));

                } else if (expected.equals(ColumnType.DOUBLE)) {
                    if (sCol.type().equals(ColumnType.INTEGER)) {
                        dst.setDouble(col, source.intColumn(col).getInt(r));
                    } else {
                        dst.setDouble(col, source.doubleColumn(col).getDouble(r));
                    }

                } else if (expected.equals(ColumnType.BOOLEAN)) {
                    dst.setBoolean(col, source.booleanColumn(col).get(r));

                } else {
                    throw new IllegalArgumentException("Unsupported type: " + expected);
                }
            }
        }
    }
}