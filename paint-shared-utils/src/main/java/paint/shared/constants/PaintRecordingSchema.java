/*=============================================================================
 *  Class:        PaintRecordingSchema.java
 *  Package:      paint.shared.constants
 *
 *  PURPOSE:
 *    Defines the column schema for the Recordings table.
 *============================================================================*/

package paint.shared.constants;

import tech.tablesaw.api.ColumnType;

import static paint.shared.constants.PaintColumnNames.*;

public final class PaintRecordingSchema {

    private PaintRecordingSchema() {
        // Prevent instantiation
    }

    // =====================
    // Recording schema
    // =====================

    public static final String[] RECORDINGS_COLS = {
            EXPERIMENT_NAME,
            RECORDING_NAME,
            CONDITION_NUMBER,
            REPLICATE_NUMBER,
            PROBE_NAME,
            PROBE_TYPE,
            CELL_TYPE,
            ADJUVANT,
            CONCENTRATION,
            PROCESS_FLAG,
            THRESHOLD,
            NUMBER_OF_SPOTS,
            NUMBER_OF_TRACKS,
            NUMBER_OF_TRACKS_IN_BACKGROUND,
            NUMBER_OF_SQUARES_IN_BACKGROUND,
            AVERAGE_TRACKS_IN_BACKGROUND,
            NUMBER_OF_SPOTS_IN_ALL_TRACKS,
            NUMBER_OF_FRAMES,
            RUN_TIME,
            TIME_STAMP,
            EXCLUDE,
            TAU,
            R_SQUARED,
            DENSITY,
            MIN_REQUIRED_DENSITY_RATIO,
            MIN_REQUIRED_R_SQUARED,
            MAX_ALLOWABLE_VARIABILITY,
            NEIGHBOUR_MODE
    };

    public static final ColumnType[] RECORDINGS_TYPES = {
            ColumnType.STRING,          // EXPERIMENT_NAME
            ColumnType.STRING,          // RECORDING_NAME
            ColumnType.INTEGER,         // CONDITION_NUMBER
            ColumnType.INTEGER,         // REPLICATE_NUMBER
            ColumnType.STRING,          // PROBE_NAME
            ColumnType.STRING,          // PROBE_TYPE
            ColumnType.STRING,          // CELL_TYPE
            ColumnType.STRING,          // ADJUVANT
            ColumnType.DOUBLE,          // CONCENTRATION
            ColumnType.BOOLEAN,         // PROCESS_FLAG
            ColumnType.DOUBLE,          // THRESHOLD
            ColumnType.INTEGER,         // NUMBER_OF_SPOTS
            ColumnType.INTEGER,         // NUMBER_OF_TRACKS
            ColumnType.INTEGER,         // NUMBER_OF_TRACKS_IN_BACKGROUND
            ColumnType.INTEGER,         // NUMBER_OF_SQUARES_IN_BACKGROUND
            ColumnType.DOUBLE,          // AVERAGE_TRACKS_IN_BACKGROUND
            ColumnType.INTEGER,         // NUMBER_OF_SPOTS_IN_ALL_TRACKS
            ColumnType.INTEGER,         // NUMBER_OF_FRAMES
            ColumnType.DOUBLE,          // RUN_TIME
            ColumnType.LOCAL_DATE_TIME, // TIME_STAMP
            ColumnType.BOOLEAN,         // EXCLUDE
            ColumnType.DOUBLE,          // TAU
            ColumnType.DOUBLE,          // R_SQUARED
            ColumnType.DOUBLE,          // DENSITY
            ColumnType.DOUBLE,          // MIN_REQUIRED_DENSITY_RATIO
            ColumnType.DOUBLE,          // MIN_REQUIRED_R_SQUARED
            ColumnType.DOUBLE,          // MAX_ALLOWABLE_VARIABILITY
            ColumnType.STRING           // NEIGHBOUR_MODE
    };
}