package paint.regression.compare;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TableComparer — keyed, order-independent, tolerant comparison")
class TableComparerTest {

    /** Builds a row map from key,value,key,value,... */
    private static Map<String, String> row(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    private static final Function<Map<String, String>, String> KEY = r -> r.get("id");

    private ComparisonResult compare(List<Map<String, String>> a, List<Map<String, String>> b) {
        return TableComparer.compare(a, b, KEY, Collections.emptySet(), 1e-3);
    }

    @Test
    @DisplayName("identical tables report no differences")
    void identical() {
        List<Map<String, String>> a = Arrays.asList(row("id", "1", "x", "1.0"), row("id", "2", "x", "2.0"));
        List<Map<String, String>> b = Arrays.asList(row("id", "1", "x", "1.0"), row("id", "2", "x", "2.0"));
        ComparisonResult r = compare(a, b);
        assertFalse(r.hasDifferences(), r.report());
        assertEquals(2, r.identicalRows());
    }

    @Test
    @DisplayName("row order does not matter (keyed, not positional)")
    void orderIndependent() {
        List<Map<String, String>> a = Arrays.asList(row("id", "1", "x", "1.0"), row("id", "2", "x", "2.0"));
        List<Map<String, String>> b = Arrays.asList(row("id", "2", "x", "2.0"), row("id", "1", "x", "1.0"));
        assertFalse(compare(a, b).hasDifferences());
    }

    @Test
    @DisplayName("a numeric difference beyond tolerance is reported; within tolerance is not")
    void numericTolerance() {
        assertTrue(compare(
                Collections.singletonList(row("id", "1", "x", "1.000")),
                Collections.singletonList(row("id", "1", "x", "1.010"))).hasDifferences());

        assertFalse(compare(
                Collections.singletonList(row("id", "1", "x", "1.0000")),
                Collections.singletonList(row("id", "1", "x", "1.0005"))).hasDifferences());
    }

    @Test
    @DisplayName("a differing string field is reported")
    void stringDifference() {
        ComparisonResult r = compare(
                Collections.singletonList(row("id", "1", "name", "alpha")),
                Collections.singletonList(row("id", "1", "name", "beta")));
        assertEquals(1, r.count(ComparisonResult.Difference.Kind.VALUE));
    }

    @Test
    @DisplayName("missing and extra rows are reported")
    void missingAndExtra() {
        ComparisonResult r = compare(
                Arrays.asList(row("id", "1", "x", "1"), row("id", "2", "x", "2")),
                Arrays.asList(row("id", "1", "x", "1"), row("id", "3", "x", "3")));
        assertEquals(1, r.count(ComparisonResult.Difference.Kind.MISSING)); // id 2 gone
        assertEquals(1, r.count(ComparisonResult.Difference.Kind.EXTRA));   // id 3 new
    }

    @Test
    @DisplayName("ignored columns are skipped")
    void ignoredColumns() {
        ComparisonResult r = TableComparer.compare(
                Collections.singletonList(row("id", "1", "x", "1", "note", "old")),
                Collections.singletonList(row("id", "1", "x", "1", "note", "new")),
                KEY, new HashSet<>(Collections.singletonList("note")), 1e-3);
        assertFalse(r.hasDifferences());
    }

    @Test
    @DisplayName("NaN compares equal to NaN")
    void nanEqualsNan() {
        assertFalse(compare(
                Collections.singletonList(row("id", "1", "x", "NaN")),
                Collections.singletonList(row("id", "1", "x", "NaN"))).hasDifferences());
    }

    @Test
    @DisplayName("a repeated key is flagged as a duplicate")
    void duplicateKey() {
        List<Map<String, String>> a = new ArrayList<>(Arrays.asList(
                row("id", "1", "x", "1"), row("id", "1", "x", "2")));
        ComparisonResult r = compare(a, Collections.singletonList(row("id", "1", "x", "1")));
        assertTrue(r.count(ComparisonResult.Difference.Kind.DUPLICATE_KEY) >= 1);
    }
}
