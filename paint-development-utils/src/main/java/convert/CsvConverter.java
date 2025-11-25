package convert;

import java.util.List;
import java.util.Map;

/**
 * Simple interface for CSV converters.
 * Each converter:
 *  - maps input rows to output rows
 *  - defines a target header (column order)
 */
public interface CsvConverter {

    /**
     * Converts a list of input CSV rows into output rows.
     * Input and output rows contain column->value mappings.
     */
    List<Map<String,String>> convert(List<Map<String,String>> rows);

    /**
     * Returns the output CSV header in correct column order.
     */
    List<String> getOutputHeader();
}

