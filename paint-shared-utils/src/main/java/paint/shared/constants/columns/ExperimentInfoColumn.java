/*=============================================================================
 *  Enum:         ExperimentInfoColumn.java
 *  Package:      paint.shared.constants.columns
 *
 *  PURPOSE:
 *    Defines the ordered set of columns (name + type) for Experiment Info.csv.
 *============================================================================*/

package paint.shared.constants.columns;

import tech.tablesaw.api.ColumnType;
import paint.shared.constants.PaintStringConstants;

public enum ExperimentInfoColumn {

    EXPERIMENT_NAME   (PaintStringConstants.EXPERIMENT_NAME, ColumnType.STRING),
    RECORDING_NAME    (PaintStringConstants.RECORDING_NAME, ColumnType.STRING),
    CONDITION_NUMBER  (PaintStringConstants.CONDITION_NUMBER, ColumnType.INTEGER),
    REPLICATE_NUMBER  (PaintStringConstants.REPLICATE_NUMBER, ColumnType.INTEGER),
    PROBE_NAME        (PaintStringConstants.PROBE_NAME, ColumnType.STRING),
    PROBE_TYPE        (PaintStringConstants.PROBE_TYPE, ColumnType.STRING),
    CELL_TYPE         (PaintStringConstants.CELL_TYPE, ColumnType.STRING),
    ADJUVANT          (PaintStringConstants.ADJUVANT, ColumnType.STRING),
    CONCENTRATION     (PaintStringConstants.CONCENTRATION, ColumnType.DOUBLE),
    PROCESS_FLAG      (PaintStringConstants.PROCESS_FLAG, ColumnType.BOOLEAN),
    THRESHOLD         (PaintStringConstants.THRESHOLD, ColumnType.DOUBLE);

    public final String header;
    public final ColumnType type;

    ExperimentInfoColumn(String header, ColumnType type) {
        this.header = header;
        this.type = type;
    }
}