package paint.shared.config.paintconfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static paint.shared.constants.PaintStringConstants.GENERATE_SQUARES;
import static paint.shared.constants.PaintStringConstants.MIN_REQUIRED_DENSITY_RATIO;

/**
 * Pins the defensive backfill behaviour: when a config file is missing a seeded
 * key (an old file, or one hand-edited), loading it must restore that key to its
 * factory default rather than silently leaving it absent.
 *
 * <p>Runs against an isolated {@code @TempDir}, so no real user configuration is
 * touched.</p>
 */
@DisplayName("PaintConfig — backfill restores missing keys on load")
class PaintConfigBackfillTest {

    @Test
    @DisplayName("a config file missing a seeded key gets it restored on reload")
    void backfillRestoresMissingKey(@TempDir Path projectDir) {
        // Start from a complete config.
        PaintConfig.reinitialise(projectDir);
        assertEquals(2.0,
                PaintConfig.getDouble(GENERATE_SQUARES, MIN_REQUIRED_DENSITY_RATIO, -1.0), 1e-9);

        // Simulate an incomplete config file: remove the key and persist to disk.
        PaintConfig.remove(GENERATE_SQUARES, MIN_REQUIRED_DENSITY_RATIO);

        // Reload from disk -> backfill must restore the missing key to its default.
        PaintConfig.reinitialise(projectDir);
        assertEquals(2.0,
                PaintConfig.getDouble(GENERATE_SQUARES, MIN_REQUIRED_DENSITY_RATIO, -999.0), 1e-9,
                "backfill should have restored the missing key to its seeded default");
    }
}
