package paint.generatesquares.calc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import paint.shared.config.GenerateSquaresConfig;
import paint.shared.config.paintconfig.PaintConfig;
import paint.shared.io.ExperimentDataLoader;
import paint.shared.objects.Experiment;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.utils.PaintLogger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static paint.shared.constants.PaintStringConstants.BACKGROUND_PLOTS;
import static paint.shared.constants.PaintStringConstants.GENERATE_SQUARES;
import static paint.shared.constants.PaintStringConstants.TAU_FITTING_PLOTS;

/**
 * Unit test for {@link SquareGenerationService#computeRecording}, exercising the
 * core square-generation computation in isolation — no full pipeline, no output
 * writes. This is the fine-grained test that the A5 extraction unlocked: it loads
 * a real recording's tracks read-only and asserts structural invariants of the
 * computed squares (exact per-field values are covered end-to-end by the
 * regression gate).
 */
@DisplayName("SquareGenerationService.computeRecording — isolated compute")
class SquareGenerationServiceTest {

    @Test
    @DisplayName("computes a full grid of squares with tracks assigned and selection applied")
    void computesSquaresAndAttributes(@TempDir Path configDir) throws Exception {
        // Factory-default config, isolated to a temp dir (plots off).
        PaintLogger.initialise(configDir, "svc-test");
        PaintConfig.reinitialise(configDir);
        PaintConfig.setBoolean(GENERATE_SQUARES, BACKGROUND_PLOTS, false);
        PaintConfig.setBoolean(GENERATE_SQUARES, TAU_FITTING_PLOTS, false);
        GenerateSquaresConfig config = new GenerateSquaresConfig();

        // Load the reference experiment (tracks only) read-only from test resources.
        Path projectRoot = Paths.get(getClass().getResource("/reference-project").toURI());
        Experiment experiment = ExperimentDataLoader.loadExperiment(projectRoot, "221012", false, true);
        assertNotNull(experiment, "reference experiment should load");

        Recording recording = experiment.getRecordings().stream()
                .filter(Recording::isProcessFlagSet)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no processable recording in the fixture"));

        // Compute in isolation. projectRoot is only used for (disabled) plot output.
        boolean completed = SquareGenerationService.computeRecording(recording, config, projectRoot);
        assertTrue(completed, "computation should complete (not interrupted)");

        List<Square> squares = recording.getSquaresOfRecording();

        // Full grid produced.
        assertEquals(config.getNumberOfSquaresInRecording(), squares.size(),
                "should produce one square per grid cell");
        // Tracks were assigned to squares.
        int totalTracks = squares.stream().mapToInt(Square::getNumberOfTracks).sum();
        assertTrue(totalTracks > 0, "tracks should have been assigned to squares");
        // Selection ran and produced at least one visible square.
        long visible = squares.stream().filter(Square::isVisible).count();
        assertTrue(visible > 0, "at least one square should be selected");
        // Every selected square has finite tau and R-squared (attributes computed).
        squares.stream().filter(Square::isVisible).forEach(s ->
                assertTrue(Double.isFinite(s.getTau()) && Double.isFinite(s.getRSquared()),
                        "a selected square should have finite tau and R-squared"));
    }
}
