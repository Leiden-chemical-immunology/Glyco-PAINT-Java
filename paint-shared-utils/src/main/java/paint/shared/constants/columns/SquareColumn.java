/*=============================================================================
 *  Enum:         SquareColumn.java
 *  Package:      paint.shared.constants.columns
 *
 *  PURPOSE:
 *    Defines the ordered set of columns (name + type) for Squares.csv.
 *============================================================================*/

package paint.shared.constants.columns;

import tech.tablesaw.api.ColumnType;
import paint.shared.constants.PaintStringConstants;

public enum SquareColumn {

    UNIQUE_KEY                       (PaintStringConstants.UNIQUE_KEY, ColumnType.STRING),
    EXPERIMENT_NAME                  (PaintStringConstants.EXPERIMENT_NAME, ColumnType.STRING),
    RECORDING_NAME                   (PaintStringConstants.RECORDING_NAME, ColumnType.STRING),
    SQUARE_NUMBER                    (PaintStringConstants.SQUARE_NUMBER, ColumnType.INTEGER),
    ROW_NUMBER                       (PaintStringConstants.ROW_NUMBER, ColumnType.INTEGER),
    COLUMN_NUMBER                    (PaintStringConstants.COLUMN_NUMBER, ColumnType.INTEGER),
    LABEL_NUMBER                     (PaintStringConstants.LABEL_NUMBER, ColumnType.INTEGER),
    CELL_ID                          (PaintStringConstants.CELL_ID, ColumnType.INTEGER),
    VISIBLE                          (PaintStringConstants.VISIBLE, ColumnType.BOOLEAN),
    SQUARE_MANUALLY_EXCLUDED         (PaintStringConstants.SQUARE_MANUALLY_EXCLUDED, ColumnType.BOOLEAN),
    IMAGE_EXCLUDED                   (PaintStringConstants.IMAGE_EXCLUDED, ColumnType.BOOLEAN),
    X0                               (PaintStringConstants.X0, ColumnType.DOUBLE),
    Y0                               (PaintStringConstants.Y0, ColumnType.DOUBLE),
    X1                               (PaintStringConstants.X1, ColumnType.DOUBLE),
    Y1                               (PaintStringConstants.Y1, ColumnType.DOUBLE),
    NUMBER_OF_TRACKS                 (PaintStringConstants.NUMBER_OF_TRACKS, ColumnType.INTEGER),
    VARIABILITY                      (PaintStringConstants.VARIABILITY, ColumnType.DOUBLE),
    DENSITY                          (PaintStringConstants.DENSITY, ColumnType.DOUBLE),
    DENSITY_RATIO                    (PaintStringConstants.DENSITY_RATIO, ColumnType.DOUBLE),
    DENSITY_RATIO_ORI                (PaintStringConstants.DENSITY_RATIO_ORI, ColumnType.DOUBLE),
    TAU                              (PaintStringConstants.TAU, ColumnType.DOUBLE),
    R_SQUARED                        (PaintStringConstants.R_SQUARED, ColumnType.DOUBLE),
    MEDIAN_DIFFUSION_COEFFICIENT     (PaintStringConstants.MEDIAN_DIFFUSION_COEFFICIENT, ColumnType.DOUBLE),
    MEDIAN_DIFFUSION_COEFFICIENT_EXT (PaintStringConstants.MEDIAN_DIFFUSION_COEFFICIENT_EXT, ColumnType.DOUBLE),
    MEDIAN_DISPLACEMENT              (PaintStringConstants.MEDIAN_DISPLACEMENT, ColumnType.DOUBLE),
    MAX_DISPLACEMENT                 (PaintStringConstants.MAX_DISPLACEMENT, ColumnType.DOUBLE),
    TOTAL_DISPLACEMENT               (PaintStringConstants.TOTAL_DISPLACEMENT, ColumnType.DOUBLE),
    MEDIAN_MAX_SPEED                 (PaintStringConstants.MEDIAN_MAX_SPEED, ColumnType.DOUBLE),
    MAX_MAX_SPEED                    (PaintStringConstants.MAX_MAX_SPEED, ColumnType.DOUBLE),
    MEDIAN_MEDIAN_SPEED              (PaintStringConstants.MEDIAN_MEDIAN_SPEED, ColumnType.DOUBLE),
    MAX_MEDIAN_SPEED                 (PaintStringConstants.MAX_MEDIAN_SPEED, ColumnType.DOUBLE),
    MAX_TRACK_DURATION               (PaintStringConstants.MAX_TRACK_DURATION, ColumnType.DOUBLE),
    TOTAL_TRACK_DURATION             (PaintStringConstants.TOTAL_TRACK_DURATION, ColumnType.DOUBLE),
    MEDIAN_TRACK_DURATION            (PaintStringConstants.MEDIAN_TRACK_DURATION, ColumnType.DOUBLE);

    public final String header;
    public final ColumnType type;

    SquareColumn(String header, ColumnType type) {
        this.header = header;
        this.type = type;
    }
}