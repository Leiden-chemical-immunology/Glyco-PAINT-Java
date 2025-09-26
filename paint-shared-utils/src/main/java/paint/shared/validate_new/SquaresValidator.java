package paint.shared.validate_new;

import paint.shared.constants.PaintConstants;
import tech.tablesaw.api.ColumnType;

import java.util.Arrays;
import java.util.List;

/**
 * Validator for squares.csv.
 * <p>
 * This validator ensures:
 * <ul>
 *     <li>The CSV header matches {@link PaintConstants#SQUARE_COLS}</li>
 *     <li>Each column value matches the expected type from {@link PaintConstants#SQUARE_TYPES}</li>
 * </ul>
 */
public class SquaresValidator extends AbstractFileValidator {

    @Override
    protected void validateHeader(List<String> actualHeader, ValidationResult result) {
        List<String> expectedHeader = Arrays.asList(PaintConstants.SQUARE_COLS);
        headersMatch(expectedHeader, actualHeader, result);
    }


    @Override
    protected ColumnType[] getExpectedTypes() {
        return PaintConstants.SQUARE_TYPES;
    }
}