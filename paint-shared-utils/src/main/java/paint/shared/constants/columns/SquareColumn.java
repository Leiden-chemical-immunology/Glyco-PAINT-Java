/*=============================================================================
 *  Enum:         SquareColumn.java
 *  Package:      paint.shared.constants.columns
 *
 *  PURPOSE:
 *    Defines the ordered set of columns (name + type) for Squares.csv.
 *============================================================================*/

package paint.shared.constants.columns;

import tech.tablesaw.api.ColumnType;
import paint.shared.constants.PaintColumnNames;

public enum SquareColumn {

    UNIQUE_KEY                       (PaintColumnNames.UNIQUE_KEY,                       ColumnType.STRING),
    EXPERIMENT_NAME                  (PaintColumnNames.EXPERIMENT_NAME,                  ColumnType.STRING),
    RECORDING_NAME                   (PaintColumnNames.RECORDING_NAME,                   ColumnType.STRING),
    SQUARE_NUMBER                    (PaintColumnNames.SQUARE_NUMBER,                    ColumnType.INTEGER),
    ROW_NUMBER                       (PaintColumnNames.ROW_NUMBER,                       ColumnType.INTEGER),
    COLUMN_NUMBER                    (PaintColumnNames.COLUMN_NUMBER,                    ColumnType.INTEGER),
    LABEL_NUMBER                     (PaintColumnNames.LABEL_NUMBER,                     ColumnType.INTEGER),
    CELL_ID                          (PaintColumnNames.CELL_ID,                          ColumnType.INTEGER),
    VISIBLE                          (PaintColumnNames.VISIBLE,                          ColumnType.BOOLEAN),
    SQUARE_MANUALLY_EXCLUDED         (PaintColumnNames.SQUARE_MANUALLY_EXCLUDED,         ColumnType.BOOLEAN),
    IMAGE_EXCLUDED                   (PaintColumnNames.IMAGE_EXCLUDED,                   ColumnType.BOOLEAN),
    X0                               (PaintColumnNames.X0,                               ColumnType.DOUBLE),
    Y0                               (PaintColumnNames.Y0,                               ColumnType.DOUBLE),
    X1                               (PaintColumnNames.X1,                               ColumnType.DOUBLE),
    Y1                               (PaintColumnNames.Y1,                               ColumnType.DOUBLE),
    NUMBER_OF_TRACKS                 (PaintColumnNames.NUMBER_OF_TRACKS,                 ColumnType.INTEGER),
    VARIABILITY                      (PaintColumnNames.VARIABILITY,                      ColumnType.DOUBLE),
    DENSITY                          (PaintColumnNames.DENSITY,                          ColumnType.DOUBLE),
    DENSITY_RATIO                    (PaintColumnNames.DENSITY_RATIO,                    ColumnType.DOUBLE),
    DENSITY_RATIO_ORI                (PaintColumnNames.DENSITY_RATIO_ORI,                ColumnType.DOUBLE),
    TAU                              (PaintColumnNames.TAU,                              ColumnType.DOUBLE),
    R_SQUARED                        (PaintColumnNames.R_SQUARED,                        ColumnType.DOUBLE),
    MEDIAN_DIFFUSION_COEFFICIENT     (PaintColumnNames.MEDIAN_DIFFUSION_COEFFICIENT,     ColumnType.DOUBLE),
    MEDIAN_DIFFUSION_COEFFICIENT_EXT (PaintColumnNames.MEDIAN_DIFFUSION_COEFFICIENT_EXT, ColumnType.DOUBLE),
    MEDIAN_DISPLACEMENT              (PaintColumnNames.MEDIAN_DISPLACEMENT,              ColumnType.DOUBLE),
    MAX_DISPLACEMENT                 (PaintColumnNames.MAX_DISPLACEMENT,                 ColumnType.DOUBLE),
    TOTAL_DISPLACEMENT               (PaintColumnNames.TOTAL_DISPLACEMENT,               ColumnType.DOUBLE),
    MEDIAN_MAX_SPEED                 (PaintColumnNames.MEDIAN_MAX_SPEED,                 ColumnType.DOUBLE),
    MAX_MAX_SPEED                    (PaintColumnNames.MAX_MAX_SPEED,                    ColumnType.DOUBLE),
    MEDIAN_MEDIAN_SPEED              (PaintColumnNames.MEDIAN_MEDIAN_SPEED,              ColumnType.DOUBLE),
    MAX_MEDIAN_SPEED                 (PaintColumnNames.MAX_MEDIAN_SPEED,                 ColumnType.DOUBLE),
    MAX_TRACK_DURATION               (PaintColumnNames.MAX_TRACK_DURATION,               ColumnType.DOUBLE),
    TOTAL_TRACK_DURATION             (PaintColumnNames.TOTAL_TRACK_DURATION,             ColumnType.DOUBLE),
    MEDIAN_TRACK_DURATION            (PaintColumnNames.MEDIAN_TRACK_DURATION,            ColumnType.DOUBLE);

    public final String header;
    public final ColumnType type;

    SquareColumn(String header, ColumnType type) {
        this.header = header;
        this.type = type;
    }
}