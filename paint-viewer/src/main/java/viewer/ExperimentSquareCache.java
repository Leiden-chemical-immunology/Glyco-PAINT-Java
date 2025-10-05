package viewer;

import paint.shared.objects.Square;
import paint.shared.utils.PaintLogger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class ExperimentSquareCache {

    // experimentName -> (recordingName -> squares)
    private static final Map<String, Map<String, List<Square>>> CACHE = new HashMap<>();

    private ExperimentSquareCache() {}

    public static synchronized List<Square> getSquaresForRecording(
            Path projectPath,
            String experimentName,
            String recordingName,
            int expectedNumberOfSquares
    ) throws IOException {

        Map<String, List<Square>> experimentMap = CACHE.get(experimentName);

        if (experimentMap == null) {
            PaintLogger.infof("Loading all squares for experiment: %s", experimentName);
            experimentMap = new HashMap<>();

            List<Square> allSquares = SquareCsvLoader.loadAllSquaresForExperiment(
                    projectPath, experimentName
            );

            for (Square s : allSquares) {
                experimentMap
                        .computeIfAbsent(s.getRecordingName(), k -> new ArrayList<>())
                        .add(s);
            }
            CACHE.put(experimentName, experimentMap);
        }

        List<Square> result = experimentMap.get(recordingName);
        if (result == null) {
            PaintLogger.warningf("No squares found for recording %s in experiment %s",
                    recordingName, experimentName);
            return Collections.emptyList();
        }

        if (expectedNumberOfSquares > 0 && result.size() != expectedNumberOfSquares) {
            PaintLogger.warningf("Recording %s expected %d squares, found %d",
                    recordingName, expectedNumberOfSquares, result.size());
        }

        return result;
    }

    public static synchronized void clear() {
        CACHE.clear();
    }
}