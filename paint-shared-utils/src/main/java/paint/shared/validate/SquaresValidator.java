package paint.shared.validate;

import paint.shared.constants.PaintConstants;
import tech.tablesaw.api.ColumnType;

import java.util.Arrays;
import java.util.List;

/**
 * Validator for squares.csv.
 * <p>
 * This validator ensures:
 * <ul>
 *     <li>The CSV header matches {@link PaintConstants#SQUARES_COLS}</li>
 *     <li>Each column value matches the expected type from {@link PaintConstants#SQUARES_TYPES}</li>
 * </ul>
 */
public class SquaresValidator extends AbstractFileValidator {

    @Override
    protected void validateHeader(List<String> actualHeader, ValidationResult result) {
        List<String> expectedHeader = Arrays.asList(PaintConstants.SQUARES_COLS);
        headersMatch(expectedHeader, actualHeader, result);
    }


    @Override
    protected ColumnType[] getExpectedTypes() {
        return PaintConstants.SQUARES_TYPES;
    }
}