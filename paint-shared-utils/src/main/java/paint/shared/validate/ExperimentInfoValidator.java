package paint.shared.validate;

import paint.shared.objects.ExperimentInfo;
import tech.tablesaw.api.ColumnType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates the structure and data of {@code experiment_info.csv}.
 *
 * <p>This updated version uses the embedded schema in
 * {@link ExperimentInfo.Column} instead of ExperimentInfoSchema.</p>
 *
 * <p>Performs:</p>
 * <ul>
 *   <li>Header validation</li>
 *   <li>Type validation</li>
 *   <li>Condition-level consistency validation</li>
 * </ul>
 */
public final class ExperimentInfoValidator extends AbstractFileValidator {

    // ───────────────────────────────────────────────────────────────────────────────
    // HEADER VALIDATION
    // ───────────────────────────────────────────────────────────────────────────────

    @Override
    protected void validateHeader(List<String> actualHeader, ValidationResult result) {

        List<String> expected = new ArrayList<>();

        for (ExperimentInfo.Column col : ExperimentInfo.Column.values()) {
            expected.add(col.header);
        }

        headersMatch(expected, actualHeader, result);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // TYPE VALIDATION
    // ───────────────────────────────────────────────────────────────────────────────

    @Override
    protected ColumnType[] getExpectedTypes() {

        ExperimentInfo.Column[] cols  = ExperimentInfo.Column.values();
        ColumnType[]            types = new ColumnType[cols.length];

        for (int i = 0; i < cols.length; i++) {
            types[i] = cols[i].type;
        }

        return types;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // CONSISTENCY CHECK
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Performs full validation (header + type) plus logical
     * consistency check across rows sharing the same condition number.
     */
    ValidationResult validateWithConsistency(File file) {

        ValidationResult result = validate(file);

        if (result.isValid()) {
            result.merge(ConditionConsistencyChecker.check(file));
        }

        return result;
    }
}