/*=============================================================================
 *  Class:        PaintSquareSchema.java
 *  Package:      paint.shared.constants
 *
 *  PURPOSE:
 *    Defines the column schema for the Squares table.
 *============================================================================*/

package paint.shared.constants;

import tech.tablesaw.api.ColumnType;

import static paint.shared.constants.PaintColumnNames.*;

public final class PaintSquareSchema {

    private PaintSquareSchema() {
        // Prevent instantiation
    }

    // =====================
    // Square schema
    // =====================

    public static final String[] SQUARES_COLS = {
            UNIQUE_KEY,
            EXPERIMENT_NAME,
            RECORDING_NAME,
            SQUARE_NUMBER,
            ROW_NUMBER,
            COLUMN_NUMBER,
            LABEL_NUMBER,
            CELL_ID,
            VISIBLE,
            SQUARE_MANUALLY_EXCLUDED,
            IMAGE_EXCLUDED,
            X0,
            Y0,
            X1,
            Y1,
            NUMBER_OF_TRACKS,
            VARIABILITY,
            DENSITY,
            DENSITY_RATIO,
            DENSITY_RATIO_ORI,
            TAU,
            R_SQUARED,
            MEDIAN_DIFFUSION_COEFFICIENT,
            MEDIAN_DIFFUSION_COEFFICIENT_EXT,
            MEDIAN_DISPLACEMENT,
            MAX_DISPLACEMENT,
            TOTAL_DISPLACEMENT,
            MEDIAN_MAX_SPEED,
            MAX_MAX_SPEED,
            MEDIAN_MEAN_SPEED,
            MAX_MEAN_SPEED,
            MAX_TRACK_DURATION,
            TOTAL_TRACK_DURATION,
            MEDIAN_TRACK_DURATION
    };

    public static final ColumnType[] SQUARES_TYPES = {
            ColumnType.STRING,   // UNIQUE_KEY
            ColumnType.STRING,   // EXPERIMENT_NAME
            ColumnType.STRING,   // RECORDING_NAME
            ColumnType.INTEGER,  // SQUARE_NUMBER
            ColumnType.INTEGER,  // ROW_NUMBER
            ColumnType.INTEGER,  // COLUMN_NUMBER
            ColumnType.INTEGER,  // LABEL_NUMBER
            ColumnType.INTEGER,  // CELL_ID
            ColumnType.BOOLEAN,  // VISIBLE
            ColumnType.BOOLEAN,  // SQUARE_MANUALLY_EXCLUDED
            ColumnType.BOOLEAN,  // IMAGE_EXCLUDED
            ColumnType.DOUBLE,   // X0
            ColumnType.DOUBLE,   // Y0
            ColumnType.DOUBLE,   // X1
            ColumnType.DOUBLE,   // Y1
            ColumnType.INTEGER,  // NUMBER_OF_TRACKS
            ColumnType.DOUBLE,   // VARIABILITY
            ColumnType.DOUBLE,   // DENSITY
            ColumnType.DOUBLE,   // DENSITY_RATIO
            ColumnType.DOUBLE,   // DENSITY_RATIO_ORI
            ColumnType.DOUBLE,   // TAU
            ColumnType.DOUBLE,   // R_SQUARED
            ColumnType.DOUBLE,   // MEDIAN_DIFFUSION_COEFFICIENT
            ColumnType.DOUBLE,   // MEDIAN_DIFFUSION_COEFFICIENT_EXT
            ColumnType.DOUBLE,   // MEDIAN_DISPLACEMENT
            ColumnType.DOUBLE,   // MAX_DISPLACEMENT
            ColumnType.DOUBLE,   // TOTAL_DISPLACEMENT
            ColumnType.DOUBLE,   // MEDIAN_MAX_SPEED
            ColumnType.DOUBLE,   // MAX_MAX_SPEED
            ColumnType.DOUBLE,   // MEDIAN_MEAN_SPEED
            ColumnType.DOUBLE,   // MAX_MEAN_SPEED
            ColumnType.DOUBLE,   // MAX_TRACK_DURATION
            ColumnType.DOUBLE,   // TOTAL_TRACK_DURATION
            ColumnType.DOUBLE    // MEDIAN_TRACK_DURATION
    };
}