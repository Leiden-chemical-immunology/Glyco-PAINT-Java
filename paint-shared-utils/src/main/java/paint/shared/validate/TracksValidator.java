package paint.shared.validate;

import paint.shared.constants.PaintConstants;
import tech.tablesaw.api.ColumnType;

import java.util.Arrays;
import java.util.List;

/**
 * Validator for tracks.csv.
 * <p>
 * This validator ensures:
 * <ul>
 *     <li>The CSV header matches {@link PaintConstants#TRACKS_COLS}</li>
 *     <li>Each column value matches the expected type from {@link PaintConstants#TRACKS_TYPES}</li>
 * </ul>
 */
public class TracksValidator extends AbstractFileValidator {

    @Override
    protected void validateHeader(List<String> actualHeader, ValidationResult result) {
        List<String> expectedHeader = Arrays.asList(PaintConstants.TRACKS_COLS);
        headersMatch(expectedHeader, actualHeader, result);
    }

    @Override
    protected ColumnType[] getExpectedTypes() {
        return PaintConstants.TRACKS_TYPES;
    }

}