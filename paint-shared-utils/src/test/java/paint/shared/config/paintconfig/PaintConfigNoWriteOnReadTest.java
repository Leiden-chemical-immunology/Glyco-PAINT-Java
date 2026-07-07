package paint.shared.config.paintconfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static paint.shared.constants.PaintFileNames.PAINT_CONFIGURATION_JSON;

/**
 * Pins the A4 fix: a configuration <em>read</em> must have no disk side effect.
 *
 * <p>Reading a key that is absent from the config returns the supplied default,
 * but must not persist that default back to the config file — a getter that
 * writes to disk is surprising and creates reentrancy/concurrency hazards.</p>
 *
 * <p>Runs against an isolated {@code @TempDir} project so no real user
 * configuration is touched.</p>
 */
@DisplayName("PaintConfig — reads must not write to disk (A4)")
class PaintConfigNoWriteOnReadTest {

    @Test
    @DisplayName("reading an absent key returns the default without modifying the config file")
    void readingAbsentKeyDoesNotWriteToDisk(@TempDir Path projectDir) throws Exception {
        PaintConfig.reinitialise(projectDir);

        Path configFile = projectDir.resolve(PAINT_CONFIGURATION_JSON);
        assertTrue(Files.isRegularFile(configFile),
                "reinitialise should have created the config file");

        byte[] before = Files.readAllBytes(configFile);

        // A key that DefaultConfigLoader does not seed -> exercises the fallback path.
        int v = PaintConfig.getInt("Generate Squares", "Absent Test Key", 4242);
        assertEquals(4242, v, "an absent key should return the supplied default");

        byte[] after = Files.readAllBytes(configFile);
        assertArrayEquals(before, after,
                "reading an absent key must not rewrite the config file on disk");
    }
}
