/*=============================================================================
 *  Class:        RecordingsValidator.java
 *  Package:      paint.shared.validate
 *
 *  PURPOSE:
 *    Validates the structure, data types, and logical consistency of
 *    "recordings.csv" files.
 *
 *  DESCRIPTION:
 *    The {@code RecordingsValidator} ensures that recording metadata
 *    files conform to the schema defined in {@link Recording}. It performs
 *    header and type validation, and ensures that shared experimental
 *    conditions are consistently documented across all recordings.
 *
 *  KEY FEATURES:
 *    • Automated header and type validation using {@link Recording.Column}.
 *    • Consistency checks for shared experimental conditions.
 *    • Robust integration with the Tablesaw and PAINT validation framework.
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

import paint.shared.objects.Recording;
import tech.tablesaw.api.ColumnType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates the structural and logical integrity of {@code recordings.csv}.
 *
 * <p>This version has been updated to use the embedded schema defined in
 * {@link Recording.Column} instead of RecordingSchema.</p>
 *
 * <p>Performs:</p>
 * <ul>
 *   <li>Header validation using Recording.Column.header</li>
 *   <li>Type validation using Recording.Column.type</li>
 *   <li>Condition-based metadata consistency checks</li>
 * </ul>
 */
final class RecordingsValidator extends AbstractFileValidator {

    // ───────────────────────────────────────────────────────────────────────────────
    // HEADER VALIDATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Validates that the actual CSV headers match the expected headers defined in
     * {@link Recording.Column}.
     *
     * @param actualHeader the list of headers found in the CSV file
     * @param result       the {@link ValidationResult} to record any mismatches
     */
    @Override
    protected void validateHeader(List<String> actualHeader, ValidationResult result) {

        List<String> expected = new ArrayList<>();
        for (Recording.Column col : Recording.Column.values()) {
            expected.add(col.header);
        }

        headersMatch(expected, actualHeader, result);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // TYPE VALIDATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns the expected {@link ColumnType} for each column in the recordings CSV,
     * as defined in {@link Recording.Column}.
     *
     * @return an array of expected column types
     */
    @Override
    protected ColumnType[] getExpectedTypes() {

        Recording.Column[] cols  = Recording.Column.values();
        ColumnType[]       types = new ColumnType[cols.length];

        for (int i = 0; i < cols.length; i++) {
            types[i] = cols[i].type;
        }

        return types;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // CONSISTENCY CHECK
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Performs full validation — including header, type, and condition-level consistency checks.
     *
     * @param file the recordings CSV file to validate
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