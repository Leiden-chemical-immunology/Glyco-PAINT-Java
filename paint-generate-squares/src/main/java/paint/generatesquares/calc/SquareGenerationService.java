package paint.generatesquares.calc;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Pure, in-memory square-generation computation for a single recording.
 *
 * <p>Operates on a loaded {@link Recording} (with its tracks) and mutates it in
 * place with the generated squares and their computed attributes. It performs no
 * experiment loading and writes no output files — those I/O steps stay in the
 * orchestrator ({@code GenerateSquaresProcessor.generateSquaresForExperiment}),
 * so this computation can be exercised directly in tests.</p>
 *
 * <p>The only side outputs reachable from here are optional and off by default:
 * a debug CSV (enabled by the {@code Debug} flag) and plot PNGs (enabled by the
 * plot flags); {@code experimentPath} is used only for the latter.</p>
 */
public final class SquareGenerationService {

    private SquareGenerationService() {
    }

    /**
     * Runs the full per-recording computation: grid generation, track assignment,
     * and square-level and recording-level attribute calculation. The recording
     * is mutated in place.
     *
     * @param recording      the loaded recording (with its tracks)
     * @param config         the generate-squares configuration
     * @param experimentPath experiment directory, used only for optional plot output
     * @return {@code true} if the computation completed; {@code false} if the
     *         thread was interrupted mid-computation (before attribute calculation)
     * @throws IOException only if optional debug output is enabled and fails
     */
    public static boolean computeRecording(Recording recording,
                                           GenerateSquaresConfig config,
                                           Path experimentPath) throws IOException {

        // Create the squares with basic geometric information.
        List<Square> squares = GenerateSquaresProcessor.generateSquaresForRecording(recording, config);
        recording.setSquaresOfRecording(squares);

        // Assign the recording's tracks to the squares.
        GenerateSquaresProcessor.assignTracksToSquares(recording, config);

        // Cancellation point mid-computation (preserved from the original loop).
        if (Thread.currentThread().isInterrupted()) {
            return false;
        }

        // Calculate square-level and recording-level attributes.
        CalculateSquareAttributes.calculateSquareAttributes(experimentPath, recording, config);
        CalculateSquareAttributes.calculateRecordingAttributes(recording, config);
        return true;
    }
}
