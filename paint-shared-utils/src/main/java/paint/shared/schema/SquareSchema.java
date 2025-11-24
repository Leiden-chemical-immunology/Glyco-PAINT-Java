/*=============================================================================
 *  Class:        SquareSchema.java
 *  Package:      paint.shared.schema
 *
 *  PURPOSE:
 *    Provides schema metadata for Squares.csv.
 *============================================================================*/

package paint.shared.schema;

import java.util.Arrays;
import tech.tablesaw.api.ColumnType;
import paint.shared.constants.columns.SquareColumn;

public final class SquareSchema {

    private SquareSchema() {
        // Prevent instantiation
    }

    /** Ordered column names for Squares.csv */
    public static final String[] COLUMNS =
            Arrays.stream(SquareColumn.values())
                  .map(col -> col.header)
                  .toArray(String[]::new);

    /** Ordered column types for Squares.csv */
    public static final ColumnType[] TYPES =
            Arrays.stream(SquareColumn.values())
                  .map(col -> col.type)
                  .toArray(ColumnType[]::new);

    /** Number of expected columns */
    public static final int COLUMN_COUNT = COLUMNS.length;

    /** Header equality check */
    public static boolean matches(String[] header) {
        return Arrays.equals(header, COLUMNS);
    }
}