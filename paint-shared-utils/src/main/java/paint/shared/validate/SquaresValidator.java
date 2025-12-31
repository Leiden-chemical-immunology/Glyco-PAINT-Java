/*=============================================================================
 *  Class:        SquaresValidator.java
 *  Package:      paint.shared.validate
 *
 *  PURPOSE:
 *    Validates the structure and data types of "squares.csv" files against
 *    the expected schema defined in the {@link Square} object.
 *
 *  DESCRIPTION:
 *    The {@code SquaresValidator} extends {@link AbstractFileValidator} to
 *    enforce schema consistency for square-level data. It verifies that
 *    the file contains all required columns with the correct headers and
 *    compatible data types (e.g., Integer, Double, String).
 *
 *  KEY FEATURES:
 *    • Automated header validation using the {@link Square.Column} enum.
 *    • Column type checking based on the Tablesaw {@link ColumnType} API.
 *    • Lightweight integration with the validation pipeline.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *=============================================================================*/

package paint.shared.validate;

import paint.shared.objects.Square;
import tech.tablesaw.api.ColumnType;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates the schema of {@code squares.csv} by checking header correctness
 * and column data types according to {@link Square.Column}.
 *
 * <p>This version no longer depends on SquareSchema; it uses the
 * embedded schema inside the entity class.</p>
 */
final class SquaresValidator extends AbstractFileValidator {

    // ───────────────────────────────────────────────────────────────────────────────
    // HEADER VALIDATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Validates that the actual CSV headers match the expected headers defined in
     * {@link Square.Column}.
     *
     * @param actualHeader the list of headers found in the CSV file
     * @param result       the {@link ValidationResult} to record any mismatches
     */
    @Override
    protected void validateHeader(List<String> actualHeader, ValidationResult result) {

        // Build expected header from Square.Column enum
        List<String> expected = new ArrayList<>();
        for (Square.Column col : Square.Column.values()) {
            expected.add(col.header);
        }

        headersMatch(expected, actualHeader, result);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // TYPE VALIDATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns the expected {@link ColumnType} for each column in the squares CSV,
     * as defined in {@link Square.Column}.
     *
     * @return an array of expected column types
     */
    @Override
    protected ColumnType[] getExpectedTypes() {
        Square.Column[] cols  = Square.Column.values();
        ColumnType[]    types = new ColumnType[cols.length];

        for (int i = 0; i < cols.length; i++) {
            types[i] = cols[i].type;
        }

        return types;
    }
}