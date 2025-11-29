/*=============================================================================
 *  Class:        ExperimentInfoTableIO.java
 *  Package:      paint.shared.io
 *
 *  PURPOSE:
 *    Internal implementation of CSV I/O for {@link paint.shared.objects.ExperimentInfo}
 *    records. Handles schema-validated table creation, entity/table conversion,
 *    and controlled append operations for the Experiment Info data layer.
 *
 *  DESCRIPTION:
 *    This class is an internal helper used exclusively by
 *    {@link paint.shared.io.MainDataInterface}.  It is not part of PAINT’s
 *    public API and must not be accessed directly by external modules.
 *
 *    Responsibilities include:
 *      • Creating schema-compliant Tablesaw tables for Experiment Info rows.
 *      • Converting between {@link ExperimentInfo} entities and
 *        {@link tech.tablesaw.api.Table} instances.
 *      • Reading CSV files with strict schema enforcement via {@link BaseTableIO}.
 *      • Performing schema-aware append operations with safe type coercion.
 *
 *  DESIGN NOTES:
 *    • Class visibility is intentionally package-private to preserve encapsulation.
 *    • All column order, names, and data types are enforced using
 *      {@link paint.shared.schema.ExperimentInfoSchema}.
 *    • All external callers must use {@link MainDataInterface} for reading/writing.
 *    • Fully compatible with Java 8 and Tablesaw 0.43+.
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

import paint.shared.objects.ExperimentInfo;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.util.ArrayList;
import java.util.List;

import paint.shared.schema.ExperimentInfoSchema;
import static paint.shared.constants.PaintStringConstants.*;

/**
 * Provides table input/output utilities for {@code ExperimentInfo}
 * records (per-recording metadata).
 *
 * <p>This class enforces a fixed schema defined by
 * {@link ExperimentInfoSchema#COLUMNS} and {@link ExperimentInfoSchema#TYPES}
 * It supports:</p>
 * <ul>
 *   <li>Creating an empty table with the correct schema
 *       via {@link #emptyTable()}.</li>
 *   <li>Reading CSV files into validated tables </li>
 *   <li>Appending rows from one table to another with type-safe coercion
 *       via {@link #appendInPlace(tech.tablesaw.api.Table, tech.tablesaw.api.Table)}.</li>
 * </ul>
 *
 * <p>Validation ensures that column order, names, and types match the expected
 * schema, while allowing some flexibility (e.g. {@code INTEGER -> DOUBLE} upcasts).</p>
 */
class ExperimentInfoTableIO extends BaseTableIO {

    /**
     * Creates an empty {@link Table} with the {@code Experiment Info} schema.
     *
     * <p>The table has all expected columns defined by
     * {@link ExperimentInfoSchema#COLUMNS} and {@link ExperimentInfoSchema#TYPES}, but
     * contains zero rows.</p>
     *
     * @return a new empty {@code Table} ready to receive rows with the
     * Experiment Info schema
     */
    Table emptyTable() {
        return newEmptyTable("Experiment Info", ExperimentInfoSchema.COLUMNS, ExperimentInfoSchema.TYPES);
    }

    /**
     * Converts a list of {@link ExperimentInfo} objects into a {@link Table}
     * with a fixed schema.
     *
     * <p>Each {@code ExperimentInfo} is mapped to a single row in the table with
     * the following columns:</p>
     * <ul>
     *   <li>{@code Recording Name} (String)</li>
     *   <li>{@code Condition Number} (int)</li>
     *   <li>{@code Replicate Number} (int)</li>
     *   <li>{@code Probe Name} (String)</li>
     *   <li>{@code Probe Type} (String)</li>
     *   <li>{@code Cell Type} (String)</li>
     *   <li>{@code Adjuvant} (String)</li>
     *   <li>{@code Concentration} (double)</li>
     *   <li>{@code Process Flag} (boolean)</li>
     *   <li>{@code Threshold} (double)</li>
     * </ul>
     *
     * <p>The schema is enforced by starting from an {@link #emptyTable()} with all
     * expected columns pre-defined.</p>
     *
     * @param infos the list of {@code ExperimentInfo} objects to convert
     * @return a {@code Table} containing one row per experiment
     */

    Table toTable(List<ExperimentInfo> infos) {
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

    List<ExperimentInfo> toEntities(Table table) {
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

    /**
     * Appends all rows from a source {@link Table} into a target {@link Table},
     * enforcing the {@code Experiment Info} schema.
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>Rows are appended one by one to the {@code target}.</li>
     *   <li>Columns are matched against {@link ExperimentInfoSchema#COLUMNS} with types from
     *       {@link ExperimentInfoSchema#TYPES}.</li>
     *   <li>If the source table is missing a column, that column is skipped.</li>
     *   <li>Missing cell values in the source remain missing in the destination.</li>
     *   <li>{@code INTEGER -> DOUBLE} upcasts are allowed when the schema expects a double.</li>
     *   <li>Other type mismatches throw an {@link IllegalArgumentException}.</li>
     * </ul>
     *
     * @param target the destination table to which rows will be appended
     * @param source the source table providing rows and column values
     * @throws IllegalArgumentException if the source contains an unsupported column type
     */
    void appendInPlace(Table target, Table source) {
        if (source.isEmpty()) {
            return; // nothing to do
        }

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