package paint.shared.validate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link ConditionConsistencyChecker}, which enforces a real scientific data rule:
 * every row carrying the same Condition Number must agree on what that condition <em>is</em>
 * — same probe, probe type, cell type, adjuvant and concentration.
 *
 * <p>A violation means two different experimental conditions have been filed under one number,
 * which would silently pool unlike measurements. Worth pinning.
 */
class ConditionConsistencyCheckerTest {

    private static final String HEADER =
            "Condition Number,Probe Name,Probe Type,Cell Type,Adjuvant,Concentration";

    private static File csv(Path dir, String name, String... rows) throws IOException {
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (String row : rows) {
            sb.append(row).append('\n');
        }
        Path file = dir.resolve(name);
        Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
        return file.toFile();
    }

    @Test
    @DisplayName("rows sharing a condition number and agreeing on every attribute are accepted")
    void consistentConditionsPass(@TempDir Path dir) throws IOException {
        File file = csv(dir, "ok.csv",
                "1,6 Tri,Simple,CHO-MR,LPS,5.0",
                "1,6 Tri,Simple,CHO-MR,LPS,5.0",   // same condition, identical attributes
                "2,2 Tri,Simple,CHO-MR,None,10.0", // a different condition is independent
                "2,2 Tri,Simple,CHO-MR,None,10.0");

        ValidationResult result = ConditionConsistencyChecker.check(file);

        assertTrue(result.isValid(), "consistent conditions should pass, got: " + result.getErrors());
    }

    @Test
    @DisplayName("the same condition number with a different concentration is rejected")
    void divergingConcentrationIsRejected(@TempDir Path dir) throws IOException {
        File file = csv(dir, "bad-concentration.csv",
                "1,6 Tri,Simple,CHO-MR,LPS,5.0",
                "1,6 Tri,Simple,CHO-MR,LPS,50.0");   // same condition 1, different concentration

        ValidationResult result = ConditionConsistencyChecker.check(file);

        assertFalse(result.isValid(), "condition 1 has two concentrations and must be rejected");
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Concentration")),
                "the error should name the offending attribute, got: " + result.getErrors());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Condition 1")),
                "the error should name the offending condition, got: " + result.getErrors());
    }

    @Test
    @DisplayName("a diverging probe name is rejected")
    void divergingProbeNameIsRejected(@TempDir Path dir) throws IOException {
        File file = csv(dir, "bad-probe.csv",
                "3,6 Tri,Simple,CHO-MR,LPS,5.0",
                "3,2 Tri,Simple,CHO-MR,LPS,5.0");   // same condition 3, different probe

        ValidationResult result = ConditionConsistencyChecker.check(file);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Probe Name")),
                "the error should name Probe Name, got: " + result.getErrors());
    }

    @Test
    @DisplayName("one divergence is reported once, not once per repeated row")
    void duplicateViolationsAreReportedOnce(@TempDir Path dir) throws IOException {
        File file = csv(dir, "repeated.csv",
                "1,6 Tri,Simple,CHO-MR,LPS,5.0",
                "1,6 Tri,Simple,CHO-MR,LPS,50.0",   // the same divergence...
                "1,6 Tri,Simple,CHO-MR,LPS,50.0",   // ...repeated
                "1,6 Tri,Simple,CHO-MR,LPS,50.0");

        ValidationResult result = ConditionConsistencyChecker.check(file);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size(),
                "the identical divergence should be reported once, got: " + result.getErrors());
    }

    @Test
    @DisplayName("a missing file is reported, not thrown")
    void missingFileIsReported(@TempDir Path dir) {
        ValidationResult result =
                ConditionConsistencyChecker.check(dir.resolve("no-such-file.csv").toFile());

        assertFalse(result.isValid(), "a missing file must be invalid rather than throw");
    }
}
