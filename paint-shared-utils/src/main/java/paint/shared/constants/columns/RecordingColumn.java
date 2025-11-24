/*=============================================================================
 *  Enum:         RecordingColumn.java
 *  Package:      paint.shared.constants.columns
 *
 *  PURPOSE:
 *    Defines the ordered set of columns (name + type) for Recordings.csv.
 *============================================================================*/

package paint.shared.constants.columns;

import tech.tablesaw.api.ColumnType;
import paint.shared.constants.PaintColumnNames;

public enum RecordingColumn {

    EXPERIMENT_NAME                  (PaintColumnNames.EXPERIMENT_NAME,                 ColumnType.STRING),
    RECORDING_NAME                   (PaintColumnNames.RECORDING_NAME,                  ColumnType.STRING),
    CONDITION_NUMBER                 (PaintColumnNames.CONDITION_NUMBER,                ColumnType.INTEGER),
    REPLICATE_NUMBER                 (PaintColumnNames.REPLICATE_NUMBER,                ColumnType.INTEGER),
    PROBE_NAME                       (PaintColumnNames.PROBE_NAME,                      ColumnType.STRING),
    PROBE_TYPE                       (PaintColumnNames.PROBE_TYPE,                      ColumnType.STRING),
    CELL_TYPE                        (PaintColumnNames.CELL_TYPE,                       ColumnType.STRING),
    ADJUVANT                         (PaintColumnNames.ADJUVANT,                        ColumnType.STRING),
    CONCENTRATION                    (PaintColumnNames.CONCENTRATION,                   ColumnType.DOUBLE),
    PROCESS_FLAG                     (PaintColumnNames.PROCESS_FLAG,                    ColumnType.BOOLEAN),
    THRESHOLD                        (PaintColumnNames.THRESHOLD,                       ColumnType.DOUBLE),
    NUMBER_OF_SPOTS                  (PaintColumnNames.NUMBER_OF_SPOTS,                 ColumnType.INTEGER),
    NUMBER_OF_TRACKS                 (PaintColumnNames.NUMBER_OF_TRACKS,                ColumnType.INTEGER),
    NUMBER_OF_TRACKS_IN_BACKGROUND   (PaintColumnNames.NUMBER_OF_TRACKS_IN_BACKGROUND,  ColumnType.INTEGER),
    NUMBER_OF_SQUARES_IN_BACKGROUND  (PaintColumnNames.NUMBER_OF_SQUARES_IN_BACKGROUND, ColumnType.INTEGER),
    AVERAGE_TRACKS_IN_BACKGROUND     (PaintColumnNames.AVERAGE_TRACKS_IN_BACKGROUND,    ColumnType.DOUBLE),
    NUMBER_OF_SPOTS_IN_ALL_TRACKS    (PaintColumnNames.NUMBER_OF_SPOTS_IN_ALL_TRACKS,   ColumnType.INTEGER),
    NUMBER_OF_FRAMES                 (PaintColumnNames.NUMBER_OF_FRAMES,                ColumnType.INTEGER),
    RUN_TIME                         (PaintColumnNames.RUN_TIME,                        ColumnType.DOUBLE),
    TIME_STAMP                       (PaintColumnNames.TIME_STAMP,                      ColumnType.LOCAL_DATE_TIME),
    EXCLUDE                          (PaintColumnNames.EXCLUDE,                         ColumnType.BOOLEAN),
    TAU                              (PaintColumnNames.TAU,                             ColumnType.DOUBLE),
    R_SQUARED                        (PaintColumnNames.R_SQUARED,                       ColumnType.DOUBLE),
    DENSITY                          (PaintColumnNames.DENSITY,                         ColumnType.DOUBLE),
    MIN_REQUIRED_DENSITY_RATIO       (PaintColumnNames.MIN_REQUIRED_DENSITY_RATIO,      ColumnType.DOUBLE),
    MIN_REQUIRED_R_SQUARED           (PaintColumnNames.MIN_REQUIRED_R_SQUARED,          ColumnType.DOUBLE),
    MAX_ALLOWABLE_VARIABILITY        (PaintColumnNames.MAX_ALLOWABLE_VARIABILITY,       ColumnType.DOUBLE),
    NEIGHBOUR_MODE                   (PaintColumnNames.NEIGHBOUR_MODE,                  ColumnType.STRING);

    public final String header;
    public final ColumnType type;

    RecordingColumn(String header, ColumnType type) {
        this.header = header;
        this.type = type;
    }
}