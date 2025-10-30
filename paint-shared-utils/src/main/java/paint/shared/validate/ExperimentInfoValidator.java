/******************************************************************************
 *  Class:        ExperimentInfoValidator.java
 *  Package:      paint.shared.validate
 *
 *  PURPOSE:
 *    Validates the structure and content of the `experiment_info.csv` file.
 *    Ensures header correctness, type integrity, and logical consistency across
 *    experiment condition groups.
 *
 *  DESCRIPTION:
 *    • Verifies headers match {@link PaintConstants#EXPERIMENT_INFO_COLS}.
 *    • Verifies column types match {@link PaintConstants#EXPERIMENT_INFO_TYPES}.
 *    • Performs a post-validation consistency check via
 *      {@link ConditionConsistencyChecker} to ensure all rows with the same
 *      "Condition Number" have identical probe/cell/adjuvant/concentration values.
 *
 *  RESPONSIBILITIES:
 *    • Provide schema-level validation for experiment metadata CSV files.
 *    • Detect header mismatches, invalid data types, or cross-row inconsistencies.
 *    • Integrate both structural and logical checks into a unified validation result.
 *
 *  USAGE EXAMPLE:
 *    File csv = new File("experiment_info.csv");
 *    ExperimentInfoValidator validator = new ExperimentInfoValidator();
 *    ValidationResult result = validator.validateWithConsistency(csv, "Experiment A");
 *    if (!result.isValid()) { result.printSummary(); }
 *
 *  DEPENDENCIES:
 *    – paint.shared.constants.PaintConstants
 *    – paint.shared.validate.AbstractFileValidator
 *    – paint.shared.validate.ConditionConsistencyChecker
 *    – paint.shared.validate.ValidationResult
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
 * Validates the structure and data of {@code experiment_info.csv}.
 * <p>
 * In addition to header and type validation, this class runs an additional
 * consistency check to ensure uniform attribute values across all rows sharing
 * the same condition number.
 * </p>
 */
public final class ExperimentInfoValidator extends AbstractFileValidator {

    // ───────────────────────────────────────────────────────────────────────────────
    // HEADER VALIDATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Validates that the CSV header matches {@link PaintConstants#EXPERIMENT_INFO_COLS}.
     *
     * @param actualHeader actual CSV header read from file
     * @param result       validation result collector
     */
    @Override
    protected void validateHeader(List<String> actualHeader, ValidationResult result) {
        List<String> expectedHeader = Arrays.asList(PaintConstants.EXPERIMENT_INFO_COLS);
        headersMatch(expectedHeader, actualHeader, result);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // TYPE VALIDATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns the expected column data types as defined in
     * {@link PaintConstants#EXPERIMENT_INFO_TYPES}.
     *
     * @return array of expected {@link ColumnType}s
     */
    @Override
    protected ColumnType[] getExpectedTypes() {
        return PaintConstants.EXPERIMENT_INFO_TYPES;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // CONSISTENCY CHECK
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Runs header and type validation, followed by a consistency check ensuring
     * all rows with the same "Condition Number" have identical associated attributes.
     *
     * @param file           the {@code experiment_info.csv} file to validate
     * @return a {@link ValidationResult} summarizing all detected issues
     */
    public ValidationResult validateWithConsistency(File file) {
        ValidationResult result = validate(file);
        if (result.isValid()) {
            result.merge(ConditionConsistencyChecker.check(file));
        }
        return result;
    }
}