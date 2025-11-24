/*=============================================================================
 *  Class:        ExperimentInfoSchema.java
 *  Package:      paint.shared.schema
 *
 *  PURPOSE:
 *    Provides schema metadata for Experiment Info.csv.
 *============================================================================*/

package paint.shared.schema;

import java.util.Arrays;
import tech.tablesaw.api.ColumnType;
import paint.shared.constants.columns.ExperimentInfoColumn;

public final class ExperimentInfoSchema {

    private ExperimentInfoSchema() {
        // Prevent instantiation
    }

    /** Ordered column names for Experiment Info.csv */
    public static final String[] COLUMNS =
            Arrays.stream(ExperimentInfoColumn.values())
                  .map(col -> col.header)
                  .toArray(String[]::new);

    /** Ordered column types (aligned with COLUMNS) */
    public static final ColumnType[] TYPES =
            Arrays.stream(ExperimentInfoColumn.values())
                  .map(col -> col.type)
                  .toArray(ColumnType[]::new);

    /** Number of expected columns */
    public static final int COLUMN_COUNT = COLUMNS.length;

    /** Header validation helper */
    public static boolean matches(String[] header) {
        return Arrays.equals(header, COLUMNS);
    }
}