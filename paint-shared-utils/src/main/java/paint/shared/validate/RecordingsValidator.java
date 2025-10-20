package paint.shared.validate;

import paint.shared.constants.PaintConstants;
import tech.tablesaw.api.ColumnType;

import java.io.File;
import java.util.Arrays;
import java.util.List;


/**
 * Validator for recordings.csv.
 * <p>
 * Performs:
 * <ul>
 *   <li>Header validation against {@link PaintConstants#RECORDINGS_COLS}</li>
 *   <li>Row type validation against {@link PaintConstants#RECORDINGS_TYPES}</li>
 *   <li>Consistency check: all rows with the same "Condition Number"
 *       must have identical Probe/Cell/Adjuvant/Concentration values
 *       (shared with ExperimentInfoValidator).</li>
 * </ul>
 */
public class RecordingsValidator extends AbstractFileValidator {

    @Override
    protected void validateHeader(List<String> actualHeader, ValidationResult result) {
        List<String> expectedHeader = Arrays.asList(PaintConstants.RECORDINGS_COLS);
        headersMatch(expectedHeader, actualHeader, result);
    }

    @Override
    protected ColumnType[] getExpectedTypes() {
        return PaintConstants.RECORDINGS_TYPES;
    }

    /**
     * Run full validation: header + type checks + consistency check.
     *
     * @param file           recordings.csv file
     * @param experimentName experiment name (for error messages)
     * @return ValidationResult with errors if validation fails
     */
    public ValidationResult validateWithConsistency(File file, String experimentName) {
        ValidationResult result = validate(file);
        if (result.isValid()) {
            result.merge(ConditionConsistencyChecker.check(file, experimentName));
        }
        return result;
    }
}