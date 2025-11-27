/*=============================================================================
 *  Enum:         TrackColumn.java
 *  Package:      paint.shared.constants.columns
 *
 *  PURPOSE:
 *    Defines the ordered set of columns (name + type) for Tracks.csv.
 *
 *  DESCRIPTION:
 *    Each enum value represents a single column in the Tracks schema.
 *    The order of enum values defines the order in the CSV file.
 *
 *  KEY FEATURES:
 *    - Strong typing (header + ColumnType)
 *    - Guarantees correct schema order
 *    - Eliminates duplicated string constants
 *    - Used directly by TrackSchema
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-11-24
 *============================================================================*/

package paint.shared.constants.columns;

import tech.tablesaw.api.ColumnType;
import paint.shared.constants.PaintStringConstants;

public enum TrackColumn {

    UNIQUE_KEY                (PaintStringConstants.UNIQUE_KEY, ColumnType.STRING),
    EXPERIMENT_NAME           (PaintStringConstants.EXPERIMENT_NAME, ColumnType.STRING),
    RECORDING_NAME            (PaintStringConstants.RECORDING_NAME, ColumnType.STRING),
    TRACK_ID                  (PaintStringConstants.TRACK_ID, ColumnType.INTEGER),
    NUMBER_OF_SPOTS           (PaintStringConstants.NUMBER_OF_SPOTS, ColumnType.INTEGER),
    NUMBER_OF_GAPS            (PaintStringConstants.NUMBER_OF_GAPS, ColumnType.INTEGER),
    LONGEST_GAP               (PaintStringConstants.LONGEST_GAP, ColumnType.INTEGER),
    TRACK_DURATION            (PaintStringConstants.TRACK_DURATION, ColumnType.DOUBLE),
    TRACK_X_LOCATION          (PaintStringConstants.TRACK_X_LOCATION, ColumnType.DOUBLE),
    TRACK_Y_LOCATION          (PaintStringConstants.TRACK_Y_LOCATION, ColumnType.DOUBLE),
    TRACK_DISPLACEMENT        (PaintStringConstants.TRACK_DISPLACEMENT, ColumnType.DOUBLE),
    TRACK_MAX_SPEED           (PaintStringConstants.TRACK_MAX_SPEED, ColumnType.DOUBLE),
    TRACK_MEDIAN_SPEED        (PaintStringConstants.TRACK_MEDIAN_SPEED, ColumnType.DOUBLE),
    DIFFUSION_COEFFICIENT     (PaintStringConstants.DIFFUSION_COEFFICIENT, ColumnType.DOUBLE),
    DIFFUSION_COEFFICIENT_EXT (PaintStringConstants.DIFFUSION_COEFFICIENT_EXT, ColumnType.DOUBLE),
    TOTAL_DISTANCE            (PaintStringConstants.TOTAL_DISTANCE, ColumnType.DOUBLE),
    CONFINEMENT_RATIO         (PaintStringConstants.CONFINEMENT_RATIO, ColumnType.DOUBLE),
    SQUARE_NUMBER             (PaintStringConstants.SQUARE_NUMBER, ColumnType.INTEGER),
    LABEL_NUMBER              (PaintStringConstants.LABEL_NUMBER, ColumnType.INTEGER);

    public final String header;
    public final ColumnType type;

    TrackColumn(String header, ColumnType type) {
        this.header = header;
        this.type = type;
    }
}