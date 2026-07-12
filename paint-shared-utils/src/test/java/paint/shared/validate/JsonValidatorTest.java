package paint.shared.validate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link JsonValidator}, which is what stands between a hand-edited configuration file
 * and a confusing failure deep in the pipeline. It must reject malformed JSON with a useful
 * message, and never throw.
 */
class JsonValidatorTest {

    private static Path write(Path dir, String name, String content) throws IOException {
        Path file = dir.resolve(name);
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    @Test
    @DisplayName("well-formed JSON is accepted")
    void wellFormedJsonIsValid(@TempDir Path dir) throws IOException {
        Path file = write(dir, "good.json",
                "{ \"Generate Squares\": { \"Number of Squares in Recording\": 400 } }");

        JsonValidator.Result result = JsonValidator.validate(file);

        assertTrue(result.valid, "well-formed JSON should validate");
        assertNull(result.error, "a valid result carries no error");
    }

    @Test
    @DisplayName("a trailing comma is rejected — the classic hand-edit mistake")
    void trailingCommaIsRejected(@TempDir Path dir) throws IOException {
        Path file = write(dir, "trailing-comma.json",
                "{ \"a\": 1, \"b\": 2, }");

        JsonValidator.Result result = JsonValidator.validate(file);

        assertFalse(result.valid, "a trailing comma is not strict JSON and must be rejected");
        assertNotNull(result.error, "a rejection must explain itself");
    }

    @Test
    @DisplayName("an unterminated object is rejected")
    void unterminatedObjectIsRejected(@TempDir Path dir) throws IOException {
        Path file = write(dir, "unterminated.json", "{ \"a\": 1");

        JsonValidator.Result result = JsonValidator.validate(file);

        assertFalse(result.valid);
        assertNotNull(result.error);
    }

    @Test
    @DisplayName("an empty file is rejected rather than silently treated as empty config")
    void emptyFileIsRejected(@TempDir Path dir) throws IOException {
        Path file = write(dir, "empty.json", "");

        JsonValidator.Result result = JsonValidator.validate(file);

        assertFalse(result.valid, "an empty file is not a valid JSON object");
    }

    @Test
    @DisplayName("a missing file is reported, not thrown")
    void missingFileIsReported(@TempDir Path dir) {
        JsonValidator.Result result = JsonValidator.validate(dir.resolve("no-such-file.json"));

        assertFalse(result.valid, "a missing file must be invalid");
        assertNotNull(result.error, "a missing file must explain itself rather than throw");
    }
}
