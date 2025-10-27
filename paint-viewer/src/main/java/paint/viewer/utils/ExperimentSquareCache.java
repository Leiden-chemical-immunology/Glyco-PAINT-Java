package paint.viewer.utils;

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
 * Utility class for managing and caching square data for experiments.
 * Ensures that data for a specific experiment and recording is loaded only once
 * and requires manual intervention to clear the cache when needed.
 *
 * This class is thread-safe and designed to operate within a synchronized context
 * to ensure consistency of cached data during concurrent access.
 */
public final class ExperimentSquareCache {

    private static final Map<String, Map<String, List<Square>>> experimentCache = new HashMap<>();

    private ExperimentSquareCache() {
    }

    /**
     * Retrieves the squares associated with a specific recording within an experiment.
     * If the experiment data is not cached, it is loaded from the provided project path,
     * grouped by recording name, and stored in memory for future access.
     * This method ensures thread safety and synchronized access to the experiment cache.
     *
     * @param projectPath the base path of the project containing experiment data
     * @param experimentName the name of the experiment to query
     * @param recordingName the name of the recording within the experiment for which squares are requested
     * @param expectedNumberOfSquares the expected number of squares for the recording; a warning is logged if the actual number of squares does not match
     * @return a list of squares corresponding to the specified recording, or an empty list if no such recording exists
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
     * Clears all cached experiment data from the cache.
     *
     * This method ensures thread-safe access to the cache and removes all entries
     * from the experimentCache, effectively resetting it. It is useful for scenarios
     * where the cached data needs to be refreshed or discarded.
     *
     * Additionally, a log statement is generated to confirm that the cache has been cleared.
     */
    public static synchronized void clearCache() {
        experimentCache.clear();
        PaintLogger.infof("Cleared ExperimentSquareCache");
    }

    /**
     * Clears the cache for a specific experiment.
     *
     * This method removes all data associated with the given experiment name
     * from the experiment cache in a thread-safe manner. A log entry is created
     * to confirm the operation.
     *
     * @param experimentName the name of the experiment whose cache should be cleared
     */
    public static synchronized void clearExperiment(String experimentName) {
        experimentCache.remove(experimentName);
        PaintLogger.infof("Cleared cache for experiment: %s", experimentName);
    }
}