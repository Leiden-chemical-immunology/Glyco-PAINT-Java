package paint.regression.compare;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured outcome of comparing two keyed tables (see {@link TableComparer}).
 *
 * <p>Every issue found is recorded as a {@link Difference}: a per-field value
 * difference on a matched-key row, a row missing from the test side, a row extra
 * on the test side, or a duplicate key (which means the chosen key is not unique
 * and the comparison for that key is unreliable).</p>
 *
 * <p>The result also tracks how many keyed rows were compared and how many were
 * identical, so callers can assert a diff budget (typically: zero differences).</p>
 */
public final class ComparisonResult {

    /** A single recorded issue. */
    public static final class Difference {
        public enum Kind { VALUE, MISSING, EXTRA, DUPLICATE_KEY }

        public final Kind   kind;
        public final String key;
        public final String field;     // set only for VALUE
        public final String baseline;  // set only for VALUE
        public final String test;      // set only for VALUE

        Difference(Kind kind, String key, String field, String baseline, String test) {
            this.kind     = kind;
            this.key      = key;
            this.field    = field;
            this.baseline = baseline;
            this.test     = test;
        }

        @Override
        public String toString() {
            switch (kind) {
                case MISSING:       return "MISSING       [" + key + "]";
                case EXTRA:         return "EXTRA         [" + key + "]";
                case DUPLICATE_KEY: return "DUPLICATE KEY [" + key + "]";
                default:            return "DIFFER        [" + key + "] " + field
                                            + ": '" + baseline + "' -> '" + test + "'";
            }
        }
    }

    private final List<Difference> differences = new ArrayList<>();
    private int comparedRows;   // rows whose key matched on both sides
    private int identicalRows;  // of those, rows with no field differences

    void addValueDifference(String key, String field, String baseline, String test) {
        differences.add(new Difference(Difference.Kind.VALUE, key, field, baseline, test));
    }

    void addMissing(String key)      { differences.add(new Difference(Difference.Kind.MISSING, key, null, null, null)); }
    void addExtra(String key)        { differences.add(new Difference(Difference.Kind.EXTRA, key, null, null, null)); }
    void addDuplicateKey(String key) { differences.add(new Difference(Difference.Kind.DUPLICATE_KEY, key, null, null, null)); }

    void incComparedRows()  { comparedRows++; }
    void incIdenticalRows() { identicalRows++; }

    public List<Difference> differences() { return differences; }
    public boolean hasDifferences()       { return !differences.isEmpty(); }
    public int comparedRows()             { return comparedRows; }
    public int identicalRows()            { return identicalRows; }

    public long count(Difference.Kind kind) {
        return differences.stream().filter(d -> d.kind == kind).count();
    }

    /** A concise human-readable summary followed by each difference. */
    public String report() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "Compared %d matched-key rows: %d identical, %d differing, %d missing, %d extra, %d duplicate keys.%n",
                comparedRows,
                identicalRows,
                comparedRows - identicalRows,
                count(Difference.Kind.MISSING),
                count(Difference.Kind.EXTRA),
                count(Difference.Kind.DUPLICATE_KEY)));
        for (Difference d : differences) {
            sb.append("  ").append(d).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
