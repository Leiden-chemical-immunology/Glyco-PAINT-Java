/*=============================================================================
 *  Class:        TrackSchema.java
 *  Package:      paint.shared.schema
 *
 *  PURPOSE:
 *    Provides schema metadata for Tracks.csv, including the ordered
 *    column names, column types, and helpers for validation.
 *
 *  DESCRIPTION:
 *    The schema is generated directly from the TrackColumn enum to ensure
 *    consistency between column names, types, and order.
 *
 *  KEY FEATURES:
 *    - COLUMN_COUNT
 *    - COLUMNS[] and TYPES[]
 *    - matches() for header validation
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-11-24
 *============================================================================*/

package paint.shared.schema;

import java.util.Arrays;
import tech.tablesaw.api.ColumnType;
import paint.shared.constants.columns.TrackColumn;

public final class TrackSchema {

    private TrackSchema() {
        // Prevent instantiation
    }

    /** Ordered list of column names for Tracks.csv */
    public static final String[] COLUMNS =
            Arrays.stream(TrackColumn.values())
                  .map(col -> col.header)
                  .toArray(String[]::new);

    /** Ordered list of Tablesaw column types matching COLUMNS */
    public static final ColumnType[] TYPES =
            Arrays.stream(TrackColumn.values())
                  .map(col -> col.type)
                  .toArray(ColumnType[]::new);

    /** Number of expected columns in Tracks.csv */
    public static final int COLUMN_COUNT = COLUMNS.length;

    /**
     * Checks if a CSV header exactly matches the Tracks schema.
     *
     * @param header the header row from a CSV file
     * @return true if header matches expected schema
     */
    public static boolean matches(String[] header) {
        return Arrays.equals(header, COLUMNS);
    }
}