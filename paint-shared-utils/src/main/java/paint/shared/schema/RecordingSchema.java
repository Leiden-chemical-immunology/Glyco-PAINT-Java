/*=============================================================================
 *  Class:        RecordingSchema.java
 *  Package:      paint.shared.schema
 *
 *  PURPOSE:
 *    Provides schema metadata for Recordings.csv.
 *============================================================================*/

package paint.shared.schema;

import java.util.Arrays;
import tech.tablesaw.api.ColumnType;
import paint.shared.constants.columns.RecordingColumn;

public final class RecordingSchema {

    private RecordingSchema() {
        // Prevent instantiation
    }

    /** Ordered column names for Recordings.csv */
    public static final String[] COLUMNS =
            Arrays.stream(RecordingColumn.values())
                  .map(col -> col.header)
                  .toArray(String[]::new);

    /** Ordered column types for Recordings.csv */
    public static final ColumnType[] TYPES =
            Arrays.stream(RecordingColumn.values())
                  .map(col -> col.type)
                  .toArray(ColumnType[]::new);

    /** Number of expected columns */
    public static final int COLUMN_COUNT = COLUMNS.length;

    /** Header equality check */
    public static boolean matches(String[] header) {
        return Arrays.equals(header, COLUMNS);
    }
}