/*=============================================================================
 *  Class:        RecordingSchema.java
 *  Package:      paint.shared.schema
 *
 *  PURPOSE:
 *    Defines the complete schema (column names + Tablesaw column types)
 *    for Recordings.csv. This schema is the authoritative source describing
 *    column order, names, and data types for all Recording-level tables.
 *
 *  DESCRIPTION:
 *    • Uses an inner enum {@code Col} to couple each CSV header name with its
 *      Tablesaw {@link tech.tablesaw.api.ColumnType}.
 *    • Automatically builds aligned {@code COLUMNS[]} and {@code TYPES[]} arrays
 *      based on the enum declaration order.
 *    • Consumed by:
 *          – BaseTableIO (schema validation, reading)
 *          – RecordingsTableIO (entity conversion, writing)
 *
 *    This replaces the former RecordingColumn enum by embedding the schema
 *    definition directly inside the RecordingSchema class.
 *
 *  DESIGN NOTES:
 *    • Column order is fixed by the enum declaration order.
 *    • Changing the order requires explicit migration of existing CSVs.
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
 * Schema definition for {@code Recordings.csv} tables.
 *
 * <p>This schema exposes:</p>
 * <ul>
 *   <li>{@link #COLUMNS} – ordered list of CSV column names</li>
 *   <li>{@link #TYPES} – ordered list of Tablesaw column types</li>
 *   <li>{@link #COLUMN_COUNT} – the total number of schema columns</li>
 * </ul>
 *
 * <p>The schema is derived directly from the {@link Col} enum to guarantee
 * perfect alignment between the official CSV header and Tablesaw types.</p>
 */
public final class RecordingSchema {

    /** Prevent instantiation. */
    private RecordingSchema() {}

    /**
     * Enumeration defining each column in the Recording schema.
     *
     * <p>Each enum entry specifies both the human-readable CSV header and its
     * associated Tablesaw {@link ColumnType}. The order of enum values defines
     * the canonical order used in all exported CSV files.</p>
     */
    public enum Col {
        EXPERIMENT_NAME(                 "Experiment Name",                 ColumnType.STRING),
        RECORDING_NAME(                  "Recording Name",                  ColumnType.STRING),
        CONDITION_NUMBER(                "Condition Number",                ColumnType.INTEGER),
        REPLICATE_NUMBER(                "Replicate Number",                ColumnType.INTEGER),
        PROBE_NAME(                      "Probe Name",                      ColumnType.STRING),
        PROBE_TYPE(                      "Probe Type",                      ColumnType.STRING),
        CELL_TYPE(                       "Cell Type",                       ColumnType.STRING),
        ADJUVANT(                        "Adjuvant",                        ColumnType.STRING),
        CONCENTRATION(                   "Concentration",                   ColumnType.DOUBLE),
        PROCESS_FLAG(                    "Process Flag",                    ColumnType.BOOLEAN),
        THRESHOLD(                       "Threshold",                       ColumnType.DOUBLE),
        NUMBER_OF_SPOTS(                 "Number of Spots",                 ColumnType.INTEGER),
        NUMBER_OF_TRACKS(                "Number of Tracks",                ColumnType.INTEGER),
        NUMBER_OF_SQUARES_IN_BACKGROUND( "Number of Squares in Background", ColumnType.INTEGER),
        NUMBER_OF_TRACKS_IN_BACKGROUND(  "Number of Tracks in Background",  ColumnType.INTEGER),
        AVERAGE_TRACKS_IN_BACKGROUND(    "Average Tracks in Background",    ColumnType.DOUBLE),
        NUMBER_OF_SPOTS_IN_ALL_TRACKS(   "Number of Spots in All Tracks",   ColumnType.INTEGER),
        NUMBER_OF_FRAMES(                "Number of Frames",                ColumnType.INTEGER),
        RUN_TIME(                        "Run Time",                        ColumnType.DOUBLE),
        TIME_STAMP(                      "Time Stamp",                      ColumnType.LOCAL_DATE_TIME),
        EXCLUDE(                         "Exclude",                         ColumnType.BOOLEAN),
        TAU(                             "Tau",                             ColumnType.DOUBLE),
        R_SQUARED(                       "R Squared",                       ColumnType.DOUBLE),
        DENSITY(                         "Density",                         ColumnType.DOUBLE),
        MIN_REQUIRED_DENSITY_RATIO(      "Min Required Density Ratio",      ColumnType.DOUBLE),
        MIN_REQUIRED_R_SQUARED(          "Min Required R Squared",          ColumnType.DOUBLE),
        MAX_ALLOWABLE_VARIABILITY(       "Max Allowable Variability",       ColumnType.DOUBLE),
        NEIGHBOUR_MODE(                  "Neighbour Mode",                  ColumnType.STRING);

        /** Column header in the CSV. */
        public final String header;

        /** Tablesaw column type. */
        public final ColumnType type;

        Col(String header, ColumnType type) {
            this.header = header;
            this.type = type;
        }
    }

    /** Ordered CSV header names for Recording tables. */
    public static final String[] COLUMNS =
            Arrays.stream(Col.values())
                  .map(col -> col.header)
                  .toArray(String[]::new);

    /** Ordered Tablesaw column types aligned with {@link #COLUMNS}. */
    public static final ColumnType[] TYPES =
            Arrays.stream(Col.values())
                  .map(col -> col.type)
                  .toArray(ColumnType[]::new);

    /** Total number of columns in the Recording schema. */
    public static final int COLUMN_COUNT = Col.values().length;
}