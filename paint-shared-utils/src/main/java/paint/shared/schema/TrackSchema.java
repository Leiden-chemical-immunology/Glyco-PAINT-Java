/*=============================================================================
 *  Class:        TrackSchema.java
 *  Package:      paint.shared.schema
 *
 *  PURPOSE:
 *    Defines the full schema (column names + column types) for Tracks.csv.
 *    This schema is the authoritative specification for all track-level
 *    tabular data used in PAINT.
 *
 *  DESCRIPTION:
 *    • Uses an inner enum {@code Col} to bind each column header directly
 *      to its {@link tech.tablesaw.api.ColumnType}.
 *    • Automatically generates {@link #COLUMNS} and {@link #TYPES} arrays
 *      in strict CSV order.
 *    • Ensures perfect alignment between CSV structure, table generation,
 *      and Track entity mapping.
 *
 *    This class replaces the old TrackColumn enum by integrating schema
 *    metadata directly within the schema container class.
 *
 *  DESIGN NOTES:
 *    • Column order is defined exclusively by the enum constant order.
 *    • Changing column order or names requires CSV migration.
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
 * Schema definition for {@code Tracks.csv} tables.
 *
 * <p>Provides:</p>
 * <ul>
 *   <li>{@link #COLUMNS} – canonical CSV header names</li>
 *   <li>{@link #TYPES} – aligned {@link ColumnType} definitions</li>
 *   <li>{@link #COLUMN_COUNT} – number of columns in the schema</li>
 * </ul>
 *
 * <p>The schema arrays are generated directly from the {@link Col} enum,
 * guaranteeing consistency throughout PAINT.</p>
 */
public final class TrackSchema {

    /** Prevent instantiation. */
    private TrackSchema() {}

    /**
     * Enumeration of all columns in the Tracks schema.
     *
     * <p>Each enum value defines:</p>
     * <ul>
     *   <li>a CSV header string</li>
     *   <li>a {@link ColumnType}</li>
     * </ul>
     *
     * <p>The order of enum values is the CSV column order.</p>
     */
    public enum Col {
        UNIQUE_KEY(                "Unique Key",                 ColumnType.STRING),
        EXPERIMENT_NAME(           "Experiment Name",            ColumnType.STRING),
        RECORDING_NAME(            "Recording Name",             ColumnType.STRING),
        TRACK_ID(                  "Track Id",                   ColumnType.INTEGER),
        NUMBER_OF_SPOTS(           "Number of Spots",            ColumnType.INTEGER),
        NUMBER_OF_GAPS(            "Number of Gaps",             ColumnType.INTEGER),
        LONGEST_GAP(               "Longest Gap",                ColumnType.INTEGER),
        TRACK_DURATION(            "Track Duration",             ColumnType.DOUBLE),
        TRACK_X_LOCATION(          "Track X Location",           ColumnType.DOUBLE),
        TRACK_Y_LOCATION(          "Track Y Location",           ColumnType.DOUBLE),
        TRACK_DISPLACEMENT(        "Track Displacement",         ColumnType.DOUBLE),
        TRACK_MAX_SPEED(           "Track Max Speed",            ColumnType.DOUBLE),
        TRACK_MEDIAN_SPEED(        "Track Median Speed",         ColumnType.DOUBLE),
        DIFFUSION_COEFFICIENT(     "Diffusion Coefficient",      ColumnType.DOUBLE),
        DIFFUSION_COEFFICIENT_EXT( "Diffusion Coefficient Ext",  ColumnType.DOUBLE),
        TOTAL_DISTANCE(            "Total Distance",             ColumnType.DOUBLE),
        CONFINEMENT_RATIO(         "Confinement Ratio",          ColumnType.DOUBLE),
        SQUARE_NUMBER(             "Square Number",              ColumnType.INTEGER),
        LABEL_NUMBER(              "Label Number",               ColumnType.INTEGER);

        /** CSV column header. */
        public final String header;

        /** Tablesaw column type. */
        public final ColumnType type;

        Col(String header, ColumnType type) {
            this.header = header;
            this.type = type;
        }
    }

    /** Ordered columnnames for Tracks.csv derived from the enum. */
    public static final String[] COLUMNS =
            Arrays.stream(Col.values())
                  .map(col -> col.header)
                  .toArray(String[]::new);

    /** Ordered column types for Tracks.csv derived from the enum. */
    public static final ColumnType[] TYPES =
            Arrays.stream(Col.values())
                  .map(col -> col.type)
                  .toArray(ColumnType[]::new);

    /** Total number of schema columns. */
    public static final int COLUMN_COUNT = Col.values().length;
}