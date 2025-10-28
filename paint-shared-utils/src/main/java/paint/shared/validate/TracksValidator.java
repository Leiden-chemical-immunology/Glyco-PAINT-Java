/******************************************************************************
 *  Class:        TracksValidator.java
 *  Package:      paint.shared.validate
 *
 *  PURPOSE:
 *    Validates the structural and type integrity of the `tracks.csv` file,
 *    ensuring correct schema adherence for track-level experiment data.
 *
 *  DESCRIPTION:
 *    • Verifies that the header matches {@link PaintConstants#TRACKS_COLS}.
 *    • Validates that each column conforms to the expected data type defined in
 *      {@link PaintConstants#TRACKS_TYPES}.
 *
 *  RESPONSIBILITIES:
 *    • Detect mismatched or missing columns in `tracks.csv`.
 *    • Validate per-column data types against defined schema.
 *    • Provide structured error reporting for downstream diagnostics.
 *
 *  USAGE EXAMPLE:
 *    File csv = new File("tracks.csv");
 *    TracksValidator validator = new TracksValidator();
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
 * Validates the schema of {@code tracks.csv} by checking header correctness
 * and column data types according to {@link PaintConstants#TRACKS_COLS} and
 * {@link PaintConstants#TRACKS_TYPES}.
 */
public final class TracksValidator extends AbstractFileValidator {

    // ───────────────────────────────────────────────────────────────────────────────
    // HEADER VALIDATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Validates that the header matches {@link PaintConstants#TRACKS_COLS}.
     *
     * @param actualHeader the CSV header read from the file
     * @param result       validation result collector
     */
    @Override
    protected void validateHeader(List<String> actualHeader, ValidationResult result) {
        List<String> expectedHeader = Arrays.asList(PaintConstants.TRACKS_COLS);
        headersMatch(expectedHeader, actualHeader, result);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // TYPE VALIDATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns the expected column data types as defined in
     * {@link PaintConstants#TRACKS_TYPES}.
     *
     * @return array of expected {@link ColumnType}s
     */
    @Override
    protected ColumnType[] getExpectedTypes() {
        return PaintConstants.TRACKS_TYPES;
    }
}