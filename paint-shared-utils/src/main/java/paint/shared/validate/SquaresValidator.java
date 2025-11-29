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

    @Override
    protected ColumnType[] getExpectedTypes() {
        Square.Column[] cols = Square.Column.values();
        ColumnType[] types = new ColumnType[cols.length];

        for (int i = 0; i < cols.length; i++) {
            types[i] = cols[i].type;
        }

        return types;
    }
}