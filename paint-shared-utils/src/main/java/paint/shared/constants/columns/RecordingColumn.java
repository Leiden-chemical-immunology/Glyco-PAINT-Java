/*=============================================================================
 *  Enum:         RecordingColumn.java
 *  Package:      paint.shared.constants.columns
 *
 *  PURPOSE:
 *    Defines the ordered set of columns (name + type) for Recordings.csv.
 *============================================================================*/

package paint.shared.constants.columns;

import tech.tablesaw.api.ColumnType;
import paint.shared.constants.PaintStringConstants;

public enum RecordingColumn {

    EXPERIMENT_NAME                  (PaintStringConstants.EXPERIMENT_NAME, ColumnType.STRING),
    RECORDING_NAME                   (PaintStringConstants.RECORDING_NAME, ColumnType.STRING),
    CONDITION_NUMBER                 (PaintStringConstants.CONDITION_NUMBER, ColumnType.INTEGER),
    REPLICATE_NUMBER                 (PaintStringConstants.REPLICATE_NUMBER, ColumnType.INTEGER),
    PROBE_NAME                       (PaintStringConstants.PROBE_NAME, ColumnType.STRING),
    PROBE_TYPE                       (PaintStringConstants.PROBE_TYPE, ColumnType.STRING),
    CELL_TYPE                        (PaintStringConstants.CELL_TYPE, ColumnType.STRING),
    ADJUVANT                         (PaintStringConstants.ADJUVANT, ColumnType.STRING),
    CONCENTRATION                    (PaintStringConstants.CONCENTRATION, ColumnType.DOUBLE),
    PROCESS_FLAG                     (PaintStringConstants.PROCESS_FLAG, ColumnType.BOOLEAN),
    THRESHOLD                        (PaintStringConstants.THRESHOLD, ColumnType.DOUBLE),
    NUMBER_OF_SPOTS                  (PaintStringConstants.NUMBER_OF_SPOTS, ColumnType.INTEGER),
    NUMBER_OF_TRACKS                 (PaintStringConstants.NUMBER_OF_TRACKS, ColumnType.INTEGER),
    NUMBER_OF_TRACKS_IN_BACKGROUND   (PaintStringConstants.NUMBER_OF_TRACKS_IN_BACKGROUND, ColumnType.INTEGER),
    NUMBER_OF_SQUARES_IN_BACKGROUND  (PaintStringConstants.NUMBER_OF_SQUARES_IN_BACKGROUND, ColumnType.INTEGER),
    AVERAGE_TRACKS_IN_BACKGROUND     (PaintStringConstants.AVERAGE_TRACKS_IN_BACKGROUND, ColumnType.DOUBLE),
    NUMBER_OF_SPOTS_IN_ALL_TRACKS    (PaintStringConstants.NUMBER_OF_SPOTS_IN_ALL_TRACKS, ColumnType.INTEGER),
    NUMBER_OF_FRAMES                 (PaintStringConstants.NUMBER_OF_FRAMES, ColumnType.INTEGER),
    RUN_TIME                         (PaintStringConstants.RUN_TIME, ColumnType.DOUBLE),
    TIME_STAMP                       (PaintStringConstants.TIME_STAMP, ColumnType.LOCAL_DATE_TIME),
    EXCLUDE                          (PaintStringConstants.EXCLUDE, ColumnType.BOOLEAN),
    TAU                              (PaintStringConstants.TAU, ColumnType.DOUBLE),
    R_SQUARED                        (PaintStringConstants.R_SQUARED, ColumnType.DOUBLE),
    DENSITY                          (PaintStringConstants.DENSITY, ColumnType.DOUBLE),
    MIN_REQUIRED_DENSITY_RATIO       (PaintStringConstants.MIN_REQUIRED_DENSITY_RATIO, ColumnType.DOUBLE),
    MIN_REQUIRED_R_SQUARED           (PaintStringConstants.MIN_REQUIRED_R_SQUARED, ColumnType.DOUBLE),
    MAX_ALLOWABLE_VARIABILITY        (PaintStringConstants.MAX_ALLOWABLE_VARIABILITY, ColumnType.DOUBLE),
    NEIGHBOUR_MODE                   (PaintStringConstants.NEIGHBOUR_MODE, ColumnType.STRING);

    public final String header;
    public final ColumnType type;

    RecordingColumn(String header, ColumnType type) {
        this.header = header;
        this.type = type;
    }
}