package viewer.utils;

import paint.shared.objects.Square;
import paint.shared.utils.PaintLogger;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static paint.shared.io.HelperIO.readAllSquares;

/**
 * Maintains a per-experiment cache of squares loaded from disk,
 * to avoid reading the same SQUARES_CSV  multiple times.
 */
public final class ExperimentSquareCache {

    private static final Map<String, Map<String, List<Square>>> experimentCache = new HashMap<>();

    private ExperimentSquareCache() {
    }

    /**
     * Returns all squares for a specific recording within an experiment.
     * Loads the experiment's CSV only once and splits it per recording.
     */
    public static synchronized List<Square> getSquaresForRecording(
            Path projectPath,
            String experimentName,
            String recordingName,
            int expectedNumberOfSquares) {


        // If experiment not yet cached, load and split
        if (!experimentCache.containsKey(experimentName)) {
            PaintLogger.debugf("Loading all squares for experiment: %s", experimentName);
            Path experimentPath = projectPath.resolve(experimentName);
            List<Square> allSquares = readAllSquares(experimentPath);

            if  (allSquares == null) {
                PaintLogger.errorf("Failed to load all squares for experiment: %s", experimentName);
                Exception e =  new Exception();
                e.printStackTrace();
            }
            // Group by recording name
            Map<String, List<Square>> grouped = allSquares.stream()
                    .collect(Collectors.groupingBy(Square::getRecordingName));

            experimentCache.put(experimentName, grouped);
            PaintLogger.debugf("Cached %d recordings for experiment: %s", grouped.size(), experimentName);
        }

        Map<String, List<Square>> expMap = experimentCache.get(experimentName);
        List<Square> result = expMap.getOrDefault(recordingName, Collections.emptyList());

        if (expectedNumberOfSquares != 0 && result.size() != expectedNumberOfSquares) {
            PaintLogger.warnf(
                    "Recording %s expected %d squares but has %d",
                    recordingName, expectedNumberOfSquares, result.size()
            );
        }

        return result;
    }

    /**
     * Clears all cached experiments (optional manual reset).
     */
    public static synchronized void clearCache() {
        experimentCache.clear();
        PaintLogger.infof("Cleared ExperimentSquareCache");
    }

    /**
     * Clears only a specific experiment's cached squares.
     */
    public static synchronized void clearExperiment(String experimentName) {
        experimentCache.remove(experimentName);
        PaintLogger.infof("Cleared cache for experiment: %s", experimentName);
    }
}