/******************************************************************************
 *  Class:        RecordingsValidator.java
 *  Package:      paint.shared.validate
 *
 *  PURPOSE:
 *    Validates the `recordings.csv` file within a PAINT experiment project.
 *    Ensures correct header order, data types, and logical consistency across
 *    condition groups shared with `experiment_info.csv`.
 *
 *  DESCRIPTION:
 *    • Verifies that the header matches {@link PaintConstants#RECORDINGS_COLS}.
 *    • Checks that each column’s data type conforms to
 *      {@link PaintConstants#RECORDINGS_TYPES}.
 *    • Performs a consistency check ensuring that all rows with the same
 *      “Condition Number” share identical Probe, Cell Type, Adjuvant, and
 *      Concentration values, consistent with `experiment_info.csv`.
 *
 *  RESPONSIBILITIES:
 *    • Detect header mismatches or missing columns.
 *    • Validate cell-level data types against the schema definition.
 *    • Detect logical inconsistencies across repeated condition groups.
 *
 *  USAGE EXAMPLE:
 *    File csv = new File("recordings.csv");
 *    RecordingsValidator validator = new RecordingsValidator();
 *    ValidationResult result = validator.validateWithConsistency(csv, "Experiment A");
 *    if (!result.isValid()) { result.printSummary(); }
 *
 *  DEPENDENCIES:
 *    – paint.shared.constants.PaintConstants
 *    – paint.shared.validate.{AbstractFileValidator, ConditionConsistencyChecker, ValidationResult}
 *    – tech.tablesaw.api.ColumnType
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.shared.validate;

import paint.shared.constants.PaintConstants;
import tech.tablesaw.api.ColumnType;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Validates the structural and logical integrity of {@code recordings.csv}.
 * <p>
 * Performs schema validation (header + types) and an additional
 * condition-based consistency check to ensure uniformity of experimental metadata.
 * </p>
 */
public final class RecordingsValidator extends AbstractFileValidator {

    // ───────────────────────────────────────────────────────────────────────────────
    // HEADER VALIDATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Validates that the header matches {@link PaintConstants#RECORDINGS_COLS}.
     *
     * @param actualHeader the CSV header read from file
     * @param result       validation result collector
     */
    @Override
    protected void validateHeader(List<String> actualHeader, ValidationResult result) {
        List<String> expectedHeader = Arrays.asList(PaintConstants.RECORDINGS_COLS);
        headersMatch(expectedHeader, actualHeader, result);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // TYPE VALIDATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns the expected column data types as defined in
     * {@link PaintConstants#RECORDINGS_TYPES}.
     *
     * @return array of expected {@link ColumnType}s
     */
    @Override
    protected ColumnType[] getExpectedTypes() {
        return PaintConstants.RECORDINGS_TYPES;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // CONSISTENCY CHECK
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Performs full validation — including header, type, and condition-based consistency checks.
     *
     * @param file           the {@code recordings.csv} file to validate
     * @return {@link ValidationResult} summarizing detected issues or confirming validity
     */
    public ValidationResult validateWithConsistency(File file) {
        ValidationResult result = validate(file);
        if (result.isValid()) {
            result.merge(ConditionConsistencyChecker.check(file));
        }
        return result;
    }
}