/*=============================================================================
 *  Class:        SquareSchema.java
 *  Package:      paint.shared.schema
 *
 *  PURPOSE:
 *    Defines the complete schema (ordered column names + Tablesaw column
 *    types) for Squares.csv. This schema is the authoritative reference for
 *    the square-level tables used throughout PAINT.
 *
 *  DESCRIPTION:
 *    • Embeds an inner enum {@code Col} that binds each CSV header string
 *      directly to its {@link tech.tablesaw.api.ColumnType}.
 *    • Automatically generates {@link #COLUMNS} and {@link #TYPES} arrays
 *      aligned with the enum declaration order.
 *    • Used by:
 *         – BaseTableIO (CSV reading + validation)
 *         – SquaresTableIO (entity conversion + writing)
 *
 *    This replaces the former SquareColumn enum by integrating the schema
 *    directly into this class.
 *
 *  DESIGN NOTES:
 *    • Column order is defined strictly by the order of the enum constants.
 *    • Modifying header names or order requires explicit CSV migration.
 *    • Compatible with Java 8 and Tablesaw 0.43+.
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
 * Schema definition for {@code Squares.csv} tables.
 *
 * <p>Provides:</p>
 * <ul>
 *   <li>{@link #COLUMNS} – canonical CSV column names</li>
 *   <li>{@link #TYPES} – aligned column types</li>
 *   <li>{@link #COLUMN_COUNT} – number of schema entries</li>
 * </ul>
 *
 * <p>The schema is guaranteed to match PAINT’s expected column order, since
 * it is generated directly from the {@link Col} enum.</p>
 */
public final class SquareSchema {

    /** Prevent instantiation. */
    private SquareSchema() {}

    /**
     * Enumeration defining each column in the Squares schema.
     *
     * <p>Each enum entry binds a human-readable CSV header to a
     * {@link ColumnType}. The enum declaration order is the official CSV
     * column order throughout PAINT.</p>
     */
    public enum Col {
        UNIQUE_KEY(                       "Unique Key",                       ColumnType.STRING),
        EXPERIMENT_NAME(                  "Experiment Name",                  ColumnType.STRING),
        RECORDING_NAME(                   "Recording Name",                   ColumnType.STRING),
        SQUARE_NUMBER(                    "Square Number",                    ColumnType.INTEGER),
        ROW_NUMBER(                       "Row Number",                       ColumnType.INTEGER),
        COLUMN_NUMBER(                    "Column Number",                    ColumnType.INTEGER),
        LABEL_NUMBER(                     "Label Number",                     ColumnType.INTEGER),
        CELL_ID(                          "Cell Id",                          ColumnType.INTEGER),
        VISIBLE(                          "Visible",                          ColumnType.BOOLEAN),
        SQUARE_MANUALLY_EXCLUDED(         "Square Manually Excluded",         ColumnType.BOOLEAN),
        IMAGE_EXCLUDED(                   "Image Excluded",                   ColumnType.BOOLEAN),
        X0(                               "X0",                               ColumnType.DOUBLE),
        Y0(                               "Y0",                               ColumnType.DOUBLE),
        X1(                               "X1",                               ColumnType.DOUBLE),
        Y1(                               "Y1",                               ColumnType.DOUBLE),
        NUMBER_OF_TRACKS(                 "Number of Tracks",                 ColumnType.INTEGER),
        VARIABILITY(                      "Variability",                      ColumnType.DOUBLE),
        DENSITY(                          "Density",                          ColumnType.DOUBLE),
        DENSITY_RATIO(                    "Density Ratio",                    ColumnType.DOUBLE),
        DENSITY_RATIO_ORI(                "Density Ratio Ori",                ColumnType.DOUBLE),
        TAU(                              "Tau",                              ColumnType.DOUBLE),
        R_SQUARED(                        "R Squared",                        ColumnType.DOUBLE),
        MEDIAN_DIFFUSION_COEFFICIENT(     "Median Diffusion Coefficient",     ColumnType.DOUBLE),
        MEDIAN_DIFFUSION_COEFFICIENT_EXT( "Median Diffusion Coefficient Ext", ColumnType.DOUBLE),
        MEDIAN_DISPLACEMENT(              "Median Displacement",              ColumnType.DOUBLE),
        MAX_DISPLACEMENT(                 "Max Displacement",                 ColumnType.DOUBLE),
        TOTAL_DISPLACEMENT(               "Total Displacement",               ColumnType.DOUBLE),
        MEDIAN_MAX_SPEED(                 "Median Max Speed",                 ColumnType.DOUBLE),
        MAX_MAX_SPEED(                    "Max Max Speed",                    ColumnType.DOUBLE),
        MEDIAN_MEDIAN_SPEED(              "Median Median Speed",              ColumnType.DOUBLE),
        MAX_MEDIAN_SPEED(                 "Max Median Speed",                 ColumnType.DOUBLE),
        MAX_TRACK_DURATION(               "Max Track Duration",               ColumnType.DOUBLE),
        TOTAL_TRACK_DURATION(             "Total Track Duration",             ColumnType.DOUBLE),
        MEDIAN_TRACK_DURATION(            "Median Track Duration",            ColumnType.DOUBLE);

        /** CSV header string. */
        public final String header;

        /** Tablesaw column type. */
        public final ColumnType type;

        Col(String header, ColumnType type) {
            this.header = header;
            this.type = type;
        }
    }

    /** Ordered CSV column names for the Squares schema. */
    public static final String[] COLUMNS =
            Arrays.stream(Col.values())
                  .map(col -> col.header)
                  .toArray(String[]::new);

    /** Ordered Tablesaw column types aligned with {@link #COLUMNS}. */
    public static final ColumnType[] TYPES =
            Arrays.stream(Col.values())
                  .map(col -> col.type)
                  .toArray(ColumnType[]::new);

    /** Number of schema columns. */
    public static final int COLUMN_COUNT = Col.values().length;
}