/*=============================================================================
 *  Enum:         ExperimentInfoColumn.java
 *  Package:      paint.shared.constants.columns
 *
 *  PURPOSE:
 *    Defines the ordered set of columns (name + type) for Experiment Info.csv.
 *============================================================================*/

package paint.shared.constants.columns;

import tech.tablesaw.api.ColumnType;
import paint.shared.constants.PaintColumnNames;

public enum ExperimentInfoColumn {

    EXPERIMENT_NAME   (PaintColumnNames.EXPERIMENT_NAME,   ColumnType.STRING),
    RECORDING_NAME    (PaintColumnNames.RECORDING_NAME,    ColumnType.STRING),
    CONDITION_NUMBER  (PaintColumnNames.CONDITION_NUMBER,  ColumnType.INTEGER),
    REPLICATE_NUMBER  (PaintColumnNames.REPLICATE_NUMBER,  ColumnType.INTEGER),
    PROBE_NAME        (PaintColumnNames.PROBE_NAME,        ColumnType.STRING),
    PROBE_TYPE        (PaintColumnNames.PROBE_TYPE,        ColumnType.STRING),
    CELL_TYPE         (PaintColumnNames.CELL_TYPE,         ColumnType.STRING),
    ADJUVANT          (PaintColumnNames.ADJUVANT,          ColumnType.STRING),
    CONCENTRATION     (PaintColumnNames.CONCENTRATION,     ColumnType.DOUBLE),
    PROCESS_FLAG      (PaintColumnNames.PROCESS_FLAG,      ColumnType.BOOLEAN),
    THRESHOLD         (PaintColumnNames.THRESHOLD,         ColumnType.DOUBLE);

    public final String header;
    public final ColumnType type;

    ExperimentInfoColumn(String header, ColumnType type) {
        this.header = header;
        this.type = type;
    }
}