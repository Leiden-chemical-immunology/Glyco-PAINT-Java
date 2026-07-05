package paint.shared.validate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization tests for {@link ValidationResult}, the aggregation type
 * returned by every validator. Pins the error/warning/valid contract, the merge
 * semantics, and the immutability of the accessor views. Pure; no I/O.
 */
@DisplayName("ValidationResult — validation aggregation")
class ValidationResultTest {

    @Test
    @DisplayName("a fresh result is valid with no errors or warnings")
    void emptyIsValid() {
        ValidationResult r = new ValidationResult();
        assertTrue(r.isValid());
        assertFalse(r.hasErrors());
        assertFalse(r.hasWarnings());
    }

    @Test
    @DisplayName("adding an error makes the result invalid")
    void errorInvalidates() {
        ValidationResult r = new ValidationResult();
        r.addError("boom");
        assertTrue(r.hasErrors());
        assertFalse(r.isValid());
        assertEquals(1, r.getErrors().size());
        assertEquals("boom", r.getErrors().get(0));
    }

    @Test
    @DisplayName("warnings do not invalidate the result")
    void warningDoesNotInvalidate() {
        ValidationResult r = new ValidationResult();
        r.addWarning("careful");
        assertTrue(r.hasWarnings());
        assertTrue(r.isValid(), "warnings alone should keep the result valid");
    }

    @Test
    @DisplayName("merge combines errors and warnings from another result")
    void mergeCombines() {
        ValidationResult a = new ValidationResult();
        a.addError("e1");
        a.addWarning("w1");

        ValidationResult b = new ValidationResult();
        b.addError("e2");

        a.merge(b);
        assertEquals(2, a.getErrors().size());
        assertEquals(1, a.getWarnings().size());
        assertTrue(a.getErrors().contains("e1"));
        assertTrue(a.getErrors().contains("e2"));
    }

    @Test
    @DisplayName("merge tolerates a null argument")
    void mergeNullIsSafe() {
        ValidationResult r = new ValidationResult();
        r.addError("e1");
        r.merge(null);
        assertEquals(1, r.getErrors().size());
    }

    @Test
    @DisplayName("accessor lists are unmodifiable views")
    void accessorsAreUnmodifiable() {
        ValidationResult r = new ValidationResult();
        r.addError("e1");
        assertThrows(UnsupportedOperationException.class,
                () -> r.getErrors().add("injected"));
    }

    @Test
    @DisplayName("toString of an issue-free result reports no issues")
    void toStringEmpty() {
        ValidationResult r = new ValidationResult();
        assertTrue(r.toString().contains("No issues found"));
    }
}
