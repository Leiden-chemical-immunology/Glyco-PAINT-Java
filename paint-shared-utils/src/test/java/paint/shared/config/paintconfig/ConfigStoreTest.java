package paint.shared.config.paintconfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static paint.shared.constants.PaintFileNames.PAINT_CONFIGURATION_JSON;
import static paint.shared.constants.PaintStringConstants.GENERATE_SQUARES;
import static paint.shared.constants.PaintStringConstants.NUMBER_OF_SQUARES_IN_RECORDING;

/**
 * Tests the configuration file lifecycle.
 *
 * <p>The property that matters here is that a user can never be left stuck: whatever state the
 * config file is in — missing, corrupt, or hand-edited into invalid JSON — the application must
 * still start with sane values, and must not destroy what the user had. A broken config is moved
 * aside as {@code Paint Configuration.invalid.json} rather than deleted, so it can be inspected.
 */
class ConfigStoreTest {

    private static Path configIn(Path projectDir) {
        return projectDir.resolve(PAINT_CONFIGURATION_JSON);
    }

    private static Path backupIn(Path projectDir) {
        return projectDir.resolve("Paint Configuration.invalid.json");
    }

    @Test
    @DisplayName("a missing config file is created with defaults")
    void missingConfigIsCreatedWithDefaults(@TempDir Path projectDir) {
        assertFalse(Files.exists(configIn(projectDir)), "precondition: no config yet");

        PaintConfig.reinitialise(projectDir);

        assertTrue(Files.exists(configIn(projectDir)), "a config file should have been created");
        assertEquals(400, PaintConfig.getInt(GENERATE_SQUARES, NUMBER_OF_SQUARES_IN_RECORDING, -1),
                "the seeded default should be readable");
    }

    @Test
    @DisplayName("a valid config file is honoured, not overwritten")
    void validConfigIsHonoured(@TempDir Path projectDir) throws IOException {
        Files.write(configIn(projectDir),
                ("{ \"Generate Squares\": { \"Number of Squares in Recording\": 900 } }")
                        .getBytes(StandardCharsets.UTF_8));

        PaintConfig.reinitialise(projectDir);

        assertEquals(900, PaintConfig.getInt(GENERATE_SQUARES, NUMBER_OF_SQUARES_IN_RECORDING, -1),
                "the user's value must win over the default");
        assertFalse(Files.exists(backupIn(projectDir)),
                "a valid config must not be moved aside");
    }

    @Test
    @DisplayName("an invalid config file is moved aside, not destroyed, and defaults take over")
    void invalidConfigIsBackedUpAndReplaced(@TempDir Path projectDir) throws IOException {
        String broken = "{ \"Generate Squares\": { \"Number of Squares in Recording\": 900, }";  // trailing comma, unterminated
        Files.write(configIn(projectDir), broken.getBytes(StandardCharsets.UTF_8));

        PaintConfig.reinitialise(projectDir);

        // The broken file is preserved beside the new one, so the user can see what they wrote.
        assertTrue(Files.exists(backupIn(projectDir)),
                "the invalid config should have been moved aside for inspection");
        assertEquals(broken,
                new String(Files.readAllBytes(backupIn(projectDir)), StandardCharsets.UTF_8),
                "the backup must be the user's original bytes, unmodified");

        // And the application is usable again, on defaults.
        assertTrue(Files.exists(configIn(projectDir)), "a fresh config should have been written");
        assertEquals(400, PaintConfig.getInt(GENERATE_SQUARES, NUMBER_OF_SQUARES_IN_RECORDING, -1),
                "defaults should take over after the invalid file is set aside");
    }

    @Test
    @DisplayName("an empty config file is treated as invalid, not as an empty configuration")
    void emptyConfigIsTreatedAsInvalid(@TempDir Path projectDir) throws IOException {
        // A truncated or half-written file. Reading it as "an empty configuration" would silently
        // discard the user's settings; it must be handled like any other corrupt file.
        Files.write(configIn(projectDir), new byte[0]);

        PaintConfig.reinitialise(projectDir);

        assertTrue(Files.exists(backupIn(projectDir)),
                "an empty config should be moved aside like any other invalid file");
        assertEquals(400, PaintConfig.getInt(GENERATE_SQUARES, NUMBER_OF_SQUARES_IN_RECORDING, -1),
                "defaults should take over");
    }
}
