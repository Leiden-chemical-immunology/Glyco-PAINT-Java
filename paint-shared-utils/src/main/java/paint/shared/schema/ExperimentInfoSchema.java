/*=============================================================================
 *  Class:        ExperimentInfoSchema.java
 *  Package:      paint.shared.schema
 *
 *  PURPOSE:
 *    Defines the complete schema (column names + Tablesaw column types)
 *    for Experiment Info CSV tables. This schema is the single authoritative
 *    source describing column order, names, and data types.
 *
 *  DESCRIPTION:
 *    • Uses an inner enum {@code Col} to bind each column header to its
 *      Tablesaw {@link tech.tablesaw.api.ColumnType}.
 *    • Automatically generates ordered {@code COLUMNS[]} and {@code TYPES[]}
 *      arrays that align with the enum declaration order.
 *    • Consumed by all Experiment Info I/O classes:
 *          – BaseTableIO (validation, reading)
 *          – ExperimentInfoTableIO (conversion, writing)
 *    • Eliminates the need for a separate ExperimentInfoColumn enum.
 *
 *  DESIGN NOTES:
 *    • Column order is defined strictly by the enum declaration.
 *    • Enum values should never be reordered without migrating existing CSVs.
 *    • Fully compatible with Java 8 and Tablesaw 0.43+.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-11-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *============================================================================*/

package paint.shared.schema;

import java.util.Arrays;
import tech.tablesaw.api.ColumnType;

/**
 * Schema definition for Experiment Info CSV tables.
 *
 * <p>This class exposes:</p>
 * <ul>
 *   <li>{@link #COLUMNS} – ordered list of CSV column headers</li>
 *   <li>{@link #TYPES}   – ordered list of Tablesaw column types</li>
 *   <li>{@link #COLUMN_COUNT} – number of expected columns</li>
 * </ul>
 *
 * <p>The schema is derived directly from the {@link Col} enum to enforce
 * consistency between name, order, and column type.</p>
 */
public final class ExperimentInfoSchema {

    /** Prevent instantiation. */
    private ExperimentInfoSchema() {}

    /**
     * Enumeration defining each column in the Experiment Info schema.
     *
     * <p>Each enum value binds a human-readable CSV header to a Tablesaw
     * {@link ColumnType}. The order of enum values defines the official order
     * of columns in Experiment Info CSV files.</p>
     */
    public enum Col {

        EXPERIMENT_NAME(  "Experiment Name",  ColumnType.STRING),
        RECORDING_NAME(   "Recording Name",   ColumnType.STRING),
        CONDITION_NUMBER( "Condition Number", ColumnType.INTEGER),
        REPLICATE_NUMBER( "Replicate Number", ColumnType.INTEGER),
        PROBE_NAME(       "Probe Name",       ColumnType.STRING),
        PROBE_TYPE(       "Probe Type",       ColumnType.STRING),
        CELL_TYPE(        "Cell Type",        ColumnType.STRING),
        ADJUVANT(         "Adjuvant",         ColumnType.STRING),
        CONCENTRATION(    "Concentration",    ColumnType.DOUBLE),
        PROCESS_FLAG(     "Process Flag",     ColumnType.BOOLEAN),
        THRESHOLD(        "Threshold",        ColumnType.DOUBLE);

        /** Column header in the CSV file. */
        public final String header;

        /** Tablesaw column type associated with the column. */
        public final ColumnType type;

        Col(String header, ColumnType type) {
            this.header = header;
            this.type = type;
        }
    }

    /** Ordered column names for Experiment Info CSV files. */
    public static final String[] COLUMNS =
            Arrays.stream(Col.values())
                  .map(col -> col.header)
                  .toArray(String[]::new);

    /** Ordered Tablesaw column types aligned with {@link #COLUMNS}. */
    public static final ColumnType[] TYPES =
            Arrays.stream(Col.values())
                  .map(col -> col.type)
                  .toArray(ColumnType[]::new);

    /** Number of expected columns in the Experiment Info schema. */
    public static final int COLUMN_COUNT = Col.values().length;
}