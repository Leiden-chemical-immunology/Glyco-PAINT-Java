package paint.regression.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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

    /**
     * Human-readable report grouped by Recording &rarr; Square, mirroring the
     * legacy {@code CsvComparatorRegression} layout.
     *
     * @param splitSquareFromKey when {@code true} (squares, keyed on
     *        {@code Unique Key}) the row key is split on its last {@code '-'} into
     *        a recording name and a square number; when {@code false} (recordings,
     *        keyed on {@code Recording Name}) the whole key is the recording and
     *        there is no square.
     */
    public String reportGrouped(boolean splitSquareFromKey) {
        // recording -> square -> differences   (sorted the way the old tool sorted them)
        Map<String, Map<String, List<Difference>>> grouped = new TreeMap<>();
        int fieldWidth = 2;
        for (Difference d : differences) {
            String rec;
            String sq;
            int idx = splitSquareFromKey ? d.key.lastIndexOf('-') : -1;
            if (idx > 0 && idx < d.key.length() - 1) {
                rec = d.key.substring(0, idx);
                sq  = d.key.substring(idx + 1);
            } else {
                rec = d.key;
                sq  = "—"; // em dash, for keys without a square suffix (recordings)
            }
            grouped.computeIfAbsent(rec, r -> new TreeMap<>())
                   .computeIfAbsent(sq, s -> new ArrayList<>())
                   .add(d);
            if (d.field != null && d.field.length() + 2 > fieldWidth) {
                fieldWidth = d.field.length() + 2;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "Compared %d matched-key rows: %d identical, %d differing, %d missing, %d extra, %d duplicate keys.%n",
                comparedRows, identicalRows, comparedRows - identicalRows,
                count(Difference.Kind.MISSING), count(Difference.Kind.EXTRA),
                count(Difference.Kind.DUPLICATE_KEY)));
        sb.append(System.lineSeparator());
        sb.append("🔎 Differences grouped by Square").append(System.lineSeparator());
        sb.append("───────────────────────────────").append(System.lineSeparator());

        int total = 0;
        Set<String> squaresWithDiffs = new TreeSet<>();
        for (Map.Entry<String, Map<String, List<Difference>>> recEntry : grouped.entrySet()) {
            sb.append("Recording: ").append(recEntry.getKey()).append(System.lineSeparator());
            for (Map.Entry<String, List<Difference>> sqEntry : recEntry.getValue().entrySet()) {
                String sq = sqEntry.getKey();
                if (!"—".equals(sq)) {
                    squaresWithDiffs.add(sq);
                }
                sb.append("  ▫ Square ").append(sq).append(":").append(System.lineSeparator());
                for (Difference d : sqEntry.getValue()) {
                    if (d.kind == Difference.Kind.VALUE) {
                        String label = (isNumeric(d.baseline) && isNumeric(d.test))
                                ? "NUMERIC DIFFERENCE" : "TEXT DIFFERENCE";
                        sb.append(String.format("     - %-" + fieldWidth + "s: '%s' vs '%s' (%s)%n",
                                d.field, d.baseline, d.test, label));
                    } else {
                        String label;
                        switch (d.kind) {
                            case MISSING: label = "Missing in NEW"; break;
                            case EXTRA:   label = "Extra in NEW";   break;
                            default:      label = "Duplicate key";  break;
                        }
                        sb.append(String.format("     - %-" + fieldWidth + "s: '%s' vs '%s' (%s)%n",
                                "", "", "", label));
                    }
                    total++;
                }
            }
            sb.append(System.lineSeparator());
        }

        sb.append(String.format("📊 Total differences listed: %d%n", total));
        if (!squaresWithDiffs.isEmpty()) {
            StringBuilder join = new StringBuilder();
            for (String s : squaresWithDiffs) {
                if (join.length() > 0) {
                    join.append(", ");
                }
                join.append(s);
            }
            sb.append(String.format("🟧 Squares with at least one difference: %d (%s)%n",
                    squaresWithDiffs.size(), join));
        }
        return sb.toString();
    }

    private static boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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
