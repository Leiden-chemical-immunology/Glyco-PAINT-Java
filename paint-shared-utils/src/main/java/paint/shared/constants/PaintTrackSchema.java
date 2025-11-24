/*=============================================================================
 *  Class:        PaintTrackSchema.java
 *  Package:      paint.shared.constants
 *
 *  PURPOSE:
 *    Defines the column schema for the Tracks table.
 *============================================================================*/

package paint.shared.constants;

import tech.tablesaw.api.ColumnType;

import static paint.shared.constants.PaintColumnNames.*;

public final class PaintTrackSchema {

    private PaintTrackSchema() {
        // Prevent instantiation
    }

    // =====================
    // Track schema
    // =====================

    public static final String[] TRACKS_COLS = {
            UNIQUE_KEY,
            EXPERIMENT_NAME,
            RECORDING_NAME,
            TRACK_ID,
            NUMBER_OF_SPOTS,
            NUMBER_OF_GAPS,
            LONGEST_GAP,
            TRACK_DURATION,
            TRACK_X_LOCATION,
            TRACK_Y_LOCATION,
            TRACK_DISPLACEMENT,
            TRACK_MAX_SPEED,
            TRACK_MEDIAN_SPEED,
            DIFFUSION_COEFFICIENT,
            DIFFUSION_COEFFICIENT_EXT,
            TOTAL_DISTANCE,
            CONFINEMENT_RATIO,
            SQUARE_NUMBER,
            LABEL_NUMBER
    };

    public static final ColumnType[] TRACKS_TYPES = {
            ColumnType.STRING,  // UNIQUE_KEY
            ColumnType.STRING,  // EXPERIMENT_NAME
            ColumnType.STRING,  // RECORDING_NAME
            ColumnType.INTEGER, // TRACK_ID
            ColumnType.INTEGER, // NUMBER_OF_SPOTS
            ColumnType.INTEGER, // NUMBER_OF_GAPS
            ColumnType.INTEGER, // LONGEST_GAP
            ColumnType.DOUBLE,  // TRACK_DURATION
            ColumnType.DOUBLE,  // TRACK_X_LOCATION
            ColumnType.DOUBLE,  // TRACK_Y_LOCATION,
            ColumnType.DOUBLE,  // TRACK_DISPLACEMENT
            ColumnType.DOUBLE,  // TRACK_MAX_SPEED
            ColumnType.DOUBLE,  // TRACK_MEDIAN_SPEED
            ColumnType.DOUBLE,  // DIFFUSION_COEFFICIENT
            ColumnType.DOUBLE,  // DIFFUSION_COEFFICIENT_EXT
            ColumnType.DOUBLE,  // TOTAL_DISTANCE
            ColumnType.DOUBLE,  // CONFINEMENT_RATIO
            ColumnType.INTEGER, // SQUARE_NUMBER
            ColumnType.INTEGER  // LABEL_NUMBER
    };
}