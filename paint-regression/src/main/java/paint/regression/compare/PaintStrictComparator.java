package paint.regression.compare;

import paint.regression.RegressionRules;

import java.util.Map;

/**
 * A {@link FieldComparator} that reproduces the <b>strict</b> per-field decision
 * sequence of the original {@code CsvComparatorRegression} — so the generic
 * {@link TableComparer} yields the same Java-vs-Java verdicts the old tool did.
 *
 * <p>The rules themselves are reused verbatim from {@link RegressionRules}
 * (rather than re-implemented) so behaviour cannot silently drift from the
 * original. In order, two values for a column are treated as <em>equal</em> when:</p>
 * <ol>
 *   <li>the column is a case-insensitive field and the values match ignoring case;</li>
 *   <li>one side is empty and the other is a zero/sentinel value;</li>
 *   <li>the trimmed strings match, or both round to three decimals equally;</li>
 *   <li>one numeric side is missing (empty/NaN) — treated as not a difference;</li>
 *   <li>both parse as numbers and, after any track-count-dependent correction,
 *       fall within the field's strict rounding/relative tolerance.</li>
 * </ol>
 * Columns flagged by {@link RegressionRules#isIgnoredColumn} (strict), and blank
 * column names, are skipped entirely.
 */
public final class PaintStrictComparator implements FieldComparator {

    @Override
    public boolean isIgnored(String column) {
        return column == null
                || column.trim().isEmpty()
                || RegressionRules.isIgnoredColumn(column, false);
    }

    @Override
    public boolean equal(String column,
                         String baselineValue,
                         String testValue,
                         Map<String, String> baselineRow,
                         Map<String, String> testRow) {

        String ov = RegressionRules.clean(baselineValue);
        String nv = RegressionRules.clean(testValue);

        if (RegressionRules.isIgnoreCaseField(column) && ov.equalsIgnoreCase(nv)) {
            return true;
        }
        if (RegressionRules.emptyAndZeroEquiv(ov, nv)) {
            return true;
        }
        if (RegressionRules.valuesEqual(ov, nv)) {
            return true;
        }

        Double od = RegressionRules.parseDouble(ov);
        Double nd = RegressionRules.parseDouble(nv);

        if (RegressionRules.numericMissingSkipDifference(od, nd)) {
            return true;
        }

        if (od != null && nd != null) {
            Double corrected = RegressionRules.correctedValueIfTrackDependent(
                    column, od, nd, baselineRow, testRow);
            return RegressionRules.numericEqualWithTolerance(column, od, corrected, false);
        }

        return false;
    }
}
