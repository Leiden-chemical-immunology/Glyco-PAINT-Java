/******************************************************************************
 *  Class:        SquaresValidator.java
 *  Package:      paint.shared.validate
 *
 *  PURPOSE:
 *    Validates the structural integrity of the `squares.csv` file, ensuring it
 *    adheres to the expected schema definition for square-level experiment data.
 *
 *  DESCRIPTION:
 *    • Verifies that the header matches {@link PaintConstants#SQUARES_COLS}.
 *    • Validates that each column conforms to its expected data type as defined
 *      in {@link PaintConstants#SQUARES_TYPES}.
 *
 *  RESPONSIBILITIES:
 *    • Detect schema inconsistencies in `squares.csv`.
 *    • Enforce header order and field data types.
 *    • Provide detailed validation output for diagnostics and user feedback.
 *
 *  USAGE EXAMPLE:
 *    File csv = new File("squares.csv");
 *    SquaresValidator validator = new SquaresValidator();
 *    ValidationResult result = validator.validate(csv);
 *    if (!result.isValid()) { result.printSummary(); }
 *
 *  DEPENDENCIES:
 *    – paint.shared.constants.PaintConstants
 *    – paint.shared.validate.{AbstractFileValidator, ValidationResult}
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

import java.util.Arrays;
import java.util.List;

/**
 * Validates the schema of {@code squares.csv} by checking header correctness
 * and column data types according to {@link PaintConstants#SQUARES_COLS} and
 * {@link PaintConstants#SQUARES_TYPES}.
 */
public final class SquaresValidator extends AbstractFileValidator {

    // ───────────────────────────────────────────────────────────────────────────────
    // HEADER VALIDATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Validates that the header matches {@link PaintConstants#SQUARES_COLS}.
     *
     * @param actualHeader the CSV header read from the file
     * @param result       validation result collector
     */
    @Override
    protected void validateHeader(List<String> actualHeader, ValidationResult result) {
        List<String> expectedHeader = Arrays.asList(PaintConstants.SQUARES_COLS);
        headersMatch(expectedHeader, actualHeader, result);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // TYPE VALIDATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns the expected column data types as defined in
     * {@link PaintConstants#SQUARES_TYPES}.
     *
     * @return array of expected {@link ColumnType}s
     */
    @Override
    protected ColumnType[] getExpectedTypes() {
        return PaintConstants.SQUARES_TYPES;
    }
}