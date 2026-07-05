package paint.shared.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization tests for {@link BooleanUtils}, the single source of truth
 * for interpreting boolean-like tokens across all PAINT modules. These pin the
 * accepted TRUE/FALSE vocabularies and the trim/case/invalid handling so a later
 * change to the token sets cannot slip through unnoticed. Pure; no I/O.
 */
@DisplayName("BooleanUtils — boolean token parsing")
class BooleanUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {"y", "ye", "yes", "ok", "true", "t", "1"})
    @DisplayName("recognized TRUE tokens are truthy and valid")
    void trueTokens(String token) {
        assertTrue(BooleanUtils.isBooleanTrue(token), token + " should be truthy");
        assertTrue(BooleanUtils.checkBooleanValue(token), token + " should be a valid token");
        assertEquals("true", BooleanUtils.normalizeBoolean(token));
    }

    @ParameterizedTest
    @ValueSource(strings = {"n", "no", "false", "f", "0"})
    @DisplayName("recognized FALSE tokens are valid but not truthy")
    void falseTokens(String token) {
        assertFalse(BooleanUtils.isBooleanTrue(token), token + " should not be truthy");
        assertTrue(BooleanUtils.checkBooleanValue(token), token + " should be a valid token");
        assertEquals("false", BooleanUtils.normalizeBoolean(token));
    }

    @Test
    @DisplayName("classification ignores surrounding whitespace and case")
    void trimsAndLowercases() {
        assertTrue(BooleanUtils.isBooleanTrue("  YES  "));
        assertTrue(BooleanUtils.checkBooleanValue("TrUe"));
        assertEquals("false", BooleanUtils.normalizeBoolean("  No "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"maybe", "yeah", "2", "-1", "on", "off", "  "})
    @DisplayName("unrecognized tokens are invalid, not truthy, and normalize to null")
    void invalidTokens(String token) {
        assertFalse(BooleanUtils.checkBooleanValue(token), token + " should be invalid");
        assertFalse(BooleanUtils.isBooleanTrue(token));
        assertNull(BooleanUtils.normalizeBoolean(token));
    }

    @Test
    @DisplayName("null and empty are handled without throwing")
    void nullAndEmpty() {
        assertFalse(BooleanUtils.checkBooleanValue(null));
        assertFalse(BooleanUtils.isBooleanTrue(null));
        assertNull(BooleanUtils.normalizeBoolean(null));

        assertFalse(BooleanUtils.checkBooleanValue(""));
        assertNull(BooleanUtils.normalizeBoolean(""));
    }
}
