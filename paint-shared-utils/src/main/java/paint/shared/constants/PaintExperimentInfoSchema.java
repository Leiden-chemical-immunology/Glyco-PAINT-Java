/*=============================================================================
 *  Class:        PaintExperimentInfoSchema.java
 *  Package:      paint.shared.constants
 *
 *  PURPOSE:
 *    Defines the column schema for the Experiment Info table.
 *============================================================================*/

package paint.shared.constants;

import tech.tablesaw.api.ColumnType;

import static paint.shared.constants.PaintColumnNames.*;

public final class PaintExperimentInfoSchema {

    private PaintExperimentInfoSchema() {
        // Prevent instantiation
    }

    // =====================
    // Experiment info schema
    // =====================

    public static final String[] EXPERIMENT_INFO_COLS = {
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
            THRESHOLD
    };

    public static final ColumnType[] EXPERIMENT_INFO_TYPES = {
            ColumnType.STRING,   // EXPERIMENT_NAME
            ColumnType.STRING,   // RECORDING_NAME
            ColumnType.INTEGER,  // CONDITION_NUMBER
            ColumnType.INTEGER,  // REPLICATE_NUMBER
            ColumnType.STRING,   // PROBE_NAME
            ColumnType.STRING,   // PROBE_TYPE
            ColumnType.STRING,   // CELL_TYPE
            ColumnType.STRING,   // ADJUVANT
            ColumnType.DOUBLE,   // CONCENTRATION
            ColumnType.BOOLEAN,  // PROCESS_FLAG
            ColumnType.DOUBLE    // THRESHOLD
    };
}