/*=============================================================================
 *  Class:        ExperimentInfoTableIO.java
 *  Package:      paint.shared.io
 *
 *  PURPOSE:
 *    Provides table input/output utilities for {@link paint.shared.objects.ExperimentInfo}
 *    records, enforcing the schema used for per-recording experiment metadata.
 *
 *  DESCRIPTION:
 *    This class defines conversion logic between Tablesaw {@link tech.tablesaw.api.Table}
 *    objects and {@link paint.shared.objects.ExperimentInfo} instances. It validates
 *    and enforces the schema defined in {@code PaintConstants.EXPERIMENT_INFO_COLS}
 *    and {@code PaintConstants.EXPERIMENT_INFO_TYPES}.
 *
 *  KEY FEATURES:
 *    • Reads and validates Experiment Info CSV files against the expected schema.
 *    • Converts between lists of {@link paint.shared.objects.ExperimentInfo} and Tablesaw tables.
 *    • Creates empty Experiment Info tables with predefined columns.
 *    • Supports type-safe row appending with automatic INTEGER→DOUBLE upcasting.
 *    • Extends {@link BaseTableIO} for consistent schema validation and CSV handling.
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static paint.shared.constants.PaintConstants.*;

/**
 * Provides table input/output utilities for {@code ExperimentInfo}
 * records (per-recording metadata).
 *
 * <p>This class enforces a fixed schema defined by
 * {@code EXPERIMENT_INFO_COLS} and {@code EXPERIMENT_INFO_TYPES}.
 * It supports:</p>
 * <ul>
 *   <li>Creating an empty table with the correct schema
 *       via {@link #emptyTable()}.</li>
 *   <li>Reading CSV files into validated tables
 *       via {@link #readCsv(java.nio.file.Path)}.</li>
 *   <li>Appending rows from one table to another with type-safe coercion
 *       via {@link #appendInPlace(tech.tablesaw.api.Table, tech.tablesaw.api.Table)}.</li>
 * </ul>
 *
 * <p>Validation ensures that column order, names, and types match the expected
 * schema, while allowing some flexibility (e.g. {@code INTEGER -> DOUBLE} upcasts).</p>
 */
public class ExperimentInfoTableIO extends BaseTableIO {

    /**
     * Creates an empty {@link Table} with the {@code Experiment Info} schema.
     *
     * <p>The table has all expected columns defined by
     * {@code EXPERIMENT_INFO_COLS} and {@code EXPERIMENT_INFO_TYPES}, but
     * contains zero rows.</p>
     *
     * @return a new empty {@code Table} ready to receive rows with the
     * Experiment Info schema
     */
    public Table emptyTable() {
        return newEmptyTable("Experiment Info", EXPERIMENT_INFO_COLS, EXPERIMENT_INFO_TYPES);
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
    public Table toTable(List<ExperimentInfo> infos) {
        Table table = emptyTable();
        for (ExperimentInfo info : infos) {
            Row row = table.appendRow();
            row.setString(EXPERIMENT_NAME,  info.getExperimentName());
            row.setString(RECORDING_NAME,   info.getRecordingName());
            row.setInt(   CONDITION_NUMBER, info.getConditionNumber());
            row.setInt(   REPLICATE_NUMBER, info.getReplicateNumber());
            row.setString(PROBE_NAME,       info.getProbeName());
            row.setString(PROBE_TYPE,       info.getProbeType());
            row.setString(CELL_TYPE,        info.getCellType());
            row.setString(ADJUVANT,         info.getAdjuvant());
            row.setDouble(CONCENTRATION,    info.getConcentration());
            row.setBoolean(PROCESS_FLAG,    info.isProcessFlag());
            row.setDouble(THRESHOLD,        info.getThreshold());
        }
        return table;
    }



    /**
     * Reads a CSV file as a {@link Table}, enforcing the
     * {@code Experiment Info} schema.
     *
     * <p>The schema is defined by {@code EXPERIMENT_INFO_COLS} (column names) and
     * {@code EXPERIMENT_INFO_TYPES} (column types). Header order and types are
     * validated before returning the table.</p>
     *
     * @param csvPath the path to the CSV file
     * @return a {@code Table} containing the experiment info data
     * @throws IOException if the file cannot be read or parsed
     */
    public Table readCsv(Path csvPath) throws IOException {
        return readCsvWithSchema(csvPath, "Experiment Info",
                                 EXPERIMENT_INFO_COLS, EXPERIMENT_INFO_TYPES, false);
    }

    /**
     * Appends all rows from a source {@link Table} into a target {@link Table},
     * enforcing the {@code Experiment Info} schema.
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>Rows are appended one by one to the {@code target}.</li>
     *   <li>Columns are matched against {@code EXPERIMENT_INFO_COLS} with types from
     *       {@code EXPERIMENT_INFO_TYPES}.</li>
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
    public void appendInPlace(Table target, Table source) {
        if (source.isEmpty()) {
            return; // nothing to do
        }

        for (Row srcRow : source) {
            Row dst = target.appendRow();
            int r   = srcRow.getRowNumber();

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