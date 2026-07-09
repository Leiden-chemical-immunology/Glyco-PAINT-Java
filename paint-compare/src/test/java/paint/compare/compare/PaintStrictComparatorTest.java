package paint.compare.compare;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PaintStrictComparator — reproduces the old strict regression rules")
class PaintStrictComparatorTest {

    private final PaintStrictComparator cmp = new PaintStrictComparator();

    /** Builds a row from alternating key/value pairs. */
    private static Map<String, String> row(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    @DisplayName("ignores volatile columns and blank names, not data columns")
    void ignoresVolatileColumns() {
        assertTrue(cmp.isIgnored("Run Time"));
        assertTrue(cmp.isIgnored("Time Stamp"));
        assertTrue(cmp.isIgnored(""));
        assertFalse(cmp.isIgnored("Tau"));
    }

    @Test
    @DisplayName("treats empty as equal to zero")
    void emptyEqualsZero() {
        assertTrue(cmp.equal("Number of Tracks", "", "0", row(), row()));
    }

    @Test
    @DisplayName("compares boolean flag columns case-insensitively")
    void booleanCaseInsensitive() {
        assertTrue(cmp.equal("Visible", "TRUE", "true", row(), row()));
    }

    @Test
    @DisplayName("treats NaN/empty on one side as not a difference")
    void nanTreatedAsMissing() {
        assertTrue(cmp.equal("Tau", "nan", "5", row(), row()));
    }

    @Test
    @DisplayName("accepts Tau within its strict relative tolerance")
    void tauWithinTolerance() {
        assertTrue(cmp.equal("Tau", "1.2345", "1.2346", row(), row()));
    }

    @Test
    @DisplayName("flags a clearly different Tau")
    void tauClearlyDifferent() {
        assertFalse(cmp.equal("Tau", "1.0", "2.0", row(), row()));
    }

    @Test
    @DisplayName("applies track-count correction to Density before comparing")
    void densityTrackCorrection() {
        Map<String, String> baseRow = row("Number of Tracks", "50", "Density", "10");
        Map<String, String> testRow = row("Number of Tracks", "100", "Density", "20");
        // new Density 20 * (50/100) = 10 == baseline 10  -> not a difference
        assertTrue(cmp.equal("Density", "10", "20", baseRow, testRow));
    }

    @Test
    @DisplayName("still flags Density when the track-corrected value diverges")
    void densityStillDifferentAfterCorrection() {
        Map<String, String> baseRow = row("Number of Tracks", "50", "Density", "10");
        Map<String, String> testRow = row("Number of Tracks", "100", "Density", "40");
        // new Density 40 * (50/100) = 20 != baseline 10  -> a difference
        assertFalse(cmp.equal("Density", "10", "40", baseRow, testRow));
    }
}
