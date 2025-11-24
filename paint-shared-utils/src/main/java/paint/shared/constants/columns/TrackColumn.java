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
import paint.shared.constants.PaintColumnNames;

public enum TrackColumn {

    UNIQUE_KEY                (PaintColumnNames.UNIQUE_KEY,                ColumnType.STRING),
    EXPERIMENT_NAME           (PaintColumnNames.EXPERIMENT_NAME,           ColumnType.STRING),
    RECORDING_NAME            (PaintColumnNames.RECORDING_NAME,            ColumnType.STRING),
    TRACK_ID                  (PaintColumnNames.TRACK_ID,                  ColumnType.INTEGER),
    NUMBER_OF_SPOTS           (PaintColumnNames.NUMBER_OF_SPOTS,           ColumnType.INTEGER),
    NUMBER_OF_GAPS            (PaintColumnNames.NUMBER_OF_GAPS,            ColumnType.INTEGER),
    LONGEST_GAP               (PaintColumnNames.LONGEST_GAP,               ColumnType.INTEGER),
    TRACK_DURATION            (PaintColumnNames.TRACK_DURATION,            ColumnType.DOUBLE),
    TRACK_X_LOCATION          (PaintColumnNames.TRACK_X_LOCATION,          ColumnType.DOUBLE),
    TRACK_Y_LOCATION          (PaintColumnNames.TRACK_Y_LOCATION,          ColumnType.DOUBLE),
    TRACK_DISPLACEMENT        (PaintColumnNames.TRACK_DISPLACEMENT,        ColumnType.DOUBLE),
    TRACK_MAX_SPEED           (PaintColumnNames.TRACK_MAX_SPEED,           ColumnType.DOUBLE),
    TRACK_MEDIAN_SPEED        (PaintColumnNames.TRACK_MEDIAN_SPEED,        ColumnType.DOUBLE),
    DIFFUSION_COEFFICIENT     (PaintColumnNames.DIFFUSION_COEFFICIENT,     ColumnType.DOUBLE),
    DIFFUSION_COEFFICIENT_EXT (PaintColumnNames.DIFFUSION_COEFFICIENT_EXT, ColumnType.DOUBLE),
    TOTAL_DISTANCE            (PaintColumnNames.TOTAL_DISTANCE,            ColumnType.DOUBLE),
    CONFINEMENT_RATIO         (PaintColumnNames.CONFINEMENT_RATIO,         ColumnType.DOUBLE),
    SQUARE_NUMBER             (PaintColumnNames.SQUARE_NUMBER,             ColumnType.INTEGER),
    LABEL_NUMBER              (PaintColumnNames.LABEL_NUMBER,              ColumnType.INTEGER);

    public final String header;
    public final ColumnType type;

    TrackColumn(String header, ColumnType type) {
        this.header = header;
        this.type = type;
    }
}