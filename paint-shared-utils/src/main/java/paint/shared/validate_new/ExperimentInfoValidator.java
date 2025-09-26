package paint.shared.validate_new;

import paint.shared.constants.PaintConstants;
import tech.tablesaw.api.ColumnType;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Validator for experiment_info.csv.
 * <p>
 * Performs:
 * <ul>
 *   <li>Header validation against {@link PaintConstants#EXPERIMENT_INFO_COLS}</li>
 *   <li>Row type validation against {@link PaintConstants#EXPERIMENT_INFO_TYPES}</li>
 *   <li>Consistency check: all rows with the same "Condition Number"
 *       must have identical Probe/Cell/Adjuvant/Concentration values.</li>
 * </ul>
 */
public class ExperimentInfoValidator extends AbstractFileValidator {

    @Override
    protected void validateHeader(List<String> actualHeader, ValidationResult result) {
        List<String> expectedHeader = Arrays.asList(PaintConstants.EXPERIMENT_INFO_COLS);
        headersMatch(expectedHeader, actualHeader, result);
    }

    @Override
    protected ColumnType[] getExpectedTypes() {
        return PaintConstants.EXPERIMENT_INFO_TYPES;
    }

    /**
     * Run the extra consistency check on top of base validation.
     *
     * @param file           experiment_info.csv
     * @param experimentName name of the experiment (for error messages)
     * @return ValidationResult with errors if inconsistencies found
     */
    public ValidationResult validateWithConsistency(File file, String experimentName) {
        ValidationResult result = validate(file);
        if (result.isValid()) {
            result.merge(ConditionConsistencyChecker.check(file, experimentName));
        }
        return result;
    }
}