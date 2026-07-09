package paint.regression.compare;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Compares two tables represented as lists of {@code column -> value} row maps.
 *
 * <p>Rows are matched <b>by key, 1:1</b> (not positionally), so a difference in
 * row ordering between the two files is never reported as a difference — the
 * caller supplies a key function that uniquely identifies a row (e.g. the
 * {@code Unique Key} column for tracks, or {@code Recording + Square} for
 * squares). For each matched pair, every non-ignored field is compared: numeric
 * fields within a tolerance, everything else as trimmed strings. Rows present on
 * only one side are reported as missing/extra; a repeated key is reported as a
 * duplicate (the key is then not unique and its comparison is unreliable).</p>
 *
 * <p>Loading CSVs is deliberately <b>not</b> this class's concern — it operates
 * on already-parsed rows, so the comparison logic is pure and unit-testable.</p>
 */
public final class TableComparer {

    private TableComparer() {
    }

    /**
     * @param baseline        rows of the reference/old file
     * @param test            rows of the new file
     * @param keyFn           extracts a unique row key
     * @param ignoreColumns   column names to skip entirely
     * @param numericTolerance max absolute difference tolerated between two numeric values
     * @return a structured {@link ComparisonResult}
     */
    public static ComparisonResult compare(
            List<Map<String, String>> baseline,
            List<Map<String, String>> test,
            Function<Map<String, String>, String> keyFn,
            Set<String> ignoreColumns,
            double numericTolerance) {

        ComparisonResult result = new ComparisonResult();

        Map<String, Map<String, String>> baseByKey = index(baseline, keyFn, result);
        Map<String, Map<String, String>> testByKey = index(test, keyFn, result);

        for (Map.Entry<String, Map<String, String>> entry : baseByKey.entrySet()) {
            String              key = entry.getKey();
            Map<String, String> b   = entry.getValue();
            Map<String, String> t   = testByKey.get(key);

            if (t == null) {
                result.addMissing(key);
                continue;
            }

            result.incComparedRows();

            Set<String> fields = new LinkedHashSet<>();
            fields.addAll(b.keySet());
            fields.addAll(t.keySet());

            boolean anyDifference = false;
            for (String field : fields) {
                if (ignoreColumns.contains(field)) {
                    continue;
                }
                String bv = trim(b.get(field));
                String tv = trim(t.get(field));
                if (!equalWithinTolerance(bv, tv, numericTolerance)) {
                    result.addValueDifference(key, field, bv, tv);
                    anyDifference = true;
                }
            }
            if (!anyDifference) {
                result.incIdenticalRows();
            }
        }

        for (String key : testByKey.keySet()) {
            if (!baseByKey.containsKey(key)) {
                result.addExtra(key);
            }
        }

        return result;
    }

    /**
     * Compares two tables using a pluggable {@link FieldComparator}, which decides
     * per column whether to skip it and whether two values count as a difference.
     * Row matching (by key, 1:1), missing/extra/duplicate handling, and result
     * accounting are identical to the tolerance-based overload above.
     *
     * @param baseline   rows of the reference/old file
     * @param test       rows of the new file
     * @param keyFn      extracts a unique row key
     * @param comparator the per-field comparison policy
     * @return a structured {@link ComparisonResult}
     */
    public static ComparisonResult compare(
            List<Map<String, String>> baseline,
            List<Map<String, String>> test,
            Function<Map<String, String>, String> keyFn,
            FieldComparator comparator) {

        ComparisonResult result = new ComparisonResult();

        Map<String, Map<String, String>> baseByKey = index(baseline, keyFn, result);
        Map<String, Map<String, String>> testByKey = index(test, keyFn, result);

        for (Map.Entry<String, Map<String, String>> entry : baseByKey.entrySet()) {
            String              key = entry.getKey();
            Map<String, String> b   = entry.getValue();
            Map<String, String> t   = testByKey.get(key);

            if (t == null) {
                result.addMissing(key);
                continue;
            }

            result.incComparedRows();

            Set<String> fields = new LinkedHashSet<>();
            fields.addAll(b.keySet());
            fields.addAll(t.keySet());

            boolean anyDifference = false;
            for (String field : fields) {
                if (comparator.isIgnored(field)) {
                    continue;
                }
                String bv = trim(b.get(field));
                String tv = trim(t.get(field));
                if (!comparator.equal(field, bv, tv, b, t)) {
                    result.addValueDifference(key, field, bv, tv);
                    anyDifference = true;
                }
            }
            if (!anyDifference) {
                result.incIdenticalRows();
            }
        }

        for (String key : testByKey.keySet()) {
            if (!baseByKey.containsKey(key)) {
                result.addExtra(key);
            }
        }

        return result;
    }

    /** Index rows by key; a repeated key is recorded as a duplicate (last wins in the map). */
    private static Map<String, Map<String, String>> index(
            List<Map<String, String>> rows,
            Function<Map<String, String>, String> keyFn,
            ComparisonResult result) {

        Map<String, Map<String, String>> byKey = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            String key = keyFn.apply(row);
            if (byKey.put(key, row) != null) {
                result.addDuplicateKey(key);
            }
        }
        return byKey;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Two values are equal if their trimmed strings match, or if both parse as
     * numbers within {@code tol} (with NaN treated as equal to NaN).
     */
    static boolean equalWithinTolerance(String a, String b, double tol) {
        if (a.equals(b)) {
            return true;
        }
        Double da = parseDouble(a);
        Double db = parseDouble(b);
        if (da != null && db != null) {
            if (Double.isNaN(da) && Double.isNaN(db)) {
                return true;
            }
            return Math.abs(da - db) <= tol;
        }
        return false;
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
