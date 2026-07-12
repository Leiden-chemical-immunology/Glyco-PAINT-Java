/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

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
 * {@link paint.shared.objects.ExperimentInfo.Column ExperimentInfo.Column} instead of ExperimentInfoSchema.</p>
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

    /**
     * Validates that the actual CSV headers match the expected headers defined in
     * {@link paint.shared.objects.ExperimentInfo.Column ExperimentInfo.Column}.
     *
     * @param actualHeader the list of headers found in the CSV file
     * @param result       the {@link ValidationResult} to record any mismatches
     */
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

    /**
     * Returns the expected {@link ColumnType} for each column in the experiment info CSV,
     * as defined in {@link paint.shared.objects.ExperimentInfo.Column ExperimentInfo.Column}.
     *
     * @return an array of expected column types
     */
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
     *
     * @param file the experiment info CSV file to validate
     * @return the aggregated {@link ValidationResult}
     */
    public ValidationResult validateWithConsistency(File file) {

        ValidationResult result = validate(file);

        if (result.isValid()) {
            result.merge(ConditionConsistencyChecker.check(file));
        }

        return result;
    }
}