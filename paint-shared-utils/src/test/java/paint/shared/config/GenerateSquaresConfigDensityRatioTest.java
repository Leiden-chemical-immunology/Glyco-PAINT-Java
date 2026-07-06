package paint.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import paint.shared.config.paintconfig.PaintConfig;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static paint.shared.constants.PaintStringConstants.GENERATE_SQUARES;
import static paint.shared.constants.PaintStringConstants.MIN_REQUIRED_DENSITY_RATIO;

/**
 * Guards the "minimum required density ratio" default against the two sources of
 * truth diverging. {@code DefaultConfigLoader} seeds this value as 2.0, so both
 * the freshly seeded config and the {@link GenerateSquaresConfig} fallback (used
 * when the key is absent) must resolve to 2.0.
 *
 * <p>Each test points {@link PaintConfig} at an isolated {@code @TempDir}, so no
 * real user configuration is touched.</p>
 */
@DisplayName("GenerateSquaresConfig — minimum required density ratio default")
class GenerateSquaresConfigDensityRatioTest {

    @Test
    @DisplayName("a freshly seeded config exposes 2.0")
    void seededDefaultIsTwo(@TempDir Path projectDir) {
        PaintConfig.reinitialise(projectDir);
        assertEquals(2.0,
                PaintConfig.getDouble(GENERATE_SQUARES, MIN_REQUIRED_DENSITY_RATIO, -1.0),
                1e-9,
                "DefaultConfigLoader should seed the density ratio as 2.0");
    }

    @Test
    @DisplayName("with the key absent, GenerateSquaresConfig falls back to the seeded default (2.0)")
    void fallbackMatchesSeededDefault(@TempDir Path projectDir) {
        PaintConfig.reinitialise(projectDir);
        // Simulate a config file that is missing this key entirely.
        PaintConfig.remove(GENERATE_SQUARES, MIN_REQUIRED_DENSITY_RATIO);

        GenerateSquaresConfig cfg = new GenerateSquaresConfig();

        assertEquals(2.0, cfg.getMinRequiredDensityRatio(), 1e-9,
                "the fallback must match the seeded factory default, not diverge from it");
    }
}
