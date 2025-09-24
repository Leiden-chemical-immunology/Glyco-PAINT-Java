package debug;

import fiji.plugin.trackmate.Settings;
import paint.shared.utils.PaintLogger;

import java.util.Map;

public class TrackMateSettingsValidator {

    public static void validate(Settings settings) {
        PaintLogger.infof("Validating TrackMate settings...");

        // Detector
        String detectorKey = settings.detectorFactory.getKey();
        PaintLogger.infof("Detector: %s", detectorKey);
        validateDetectorSettings(settings.detectorSettings);

        // Tracker
        String trackerKey = settings.trackerFactory.getKey();
        PaintLogger.infof("Tracker: %s", trackerKey);
        validateTrackerSettings(settings.trackerSettings);

        PaintLogger.infof("Validation passed.");
    }

    private static void validateDetectorSettings(Map<String, Object> detectorSettings) {
        expect(detectorSettings, "RADIUS", Double.class);
        expect(detectorSettings, "THRESHOLD", Double.class);
        expect(detectorSettings, "TARGET_CHANNEL", Integer.class);
        expect(detectorSettings, "DO_SUBPIXEL_LOCALIZATION", Boolean.class);
        expect(detectorSettings, "DO_MEDIAN_FILTERING", Boolean.class);
    }

    private static void validateTrackerSettings(Map<String, Object> trackerSettings) {
        expect(trackerSettings, "LINKING_MAX_DISTANCE", Double.class);
        expect(trackerSettings, "ALTERNATIVE_LINKING_COST_FACTOR", Double.class);
        expect(trackerSettings, "ALLOW_GAP_CLOSING", Boolean.class);
        expect(trackerSettings, "GAP_CLOSING_MAX_DISTANCE", Double.class);
        expect(trackerSettings, "MAX_FRAME_GAP", Integer.class);
        expect(trackerSettings, "ALLOW_TRACK_SPLITTING", Boolean.class);
        expect(trackerSettings, "SPLITTING_MAX_DISTANCE", Double.class);
        expect(trackerSettings, "ALLOW_TRACK_MERGING", Boolean.class);
        expect(trackerSettings, "MERGING_MAX_DISTANCE", Double.class);
    }

    private static void expect(Map<String, Object> settings, String key, Class<?> expectedType) {
        Object value = settings.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required setting: " + key);
        }
        if (!expectedType.isInstance(value)) {
            throw new IllegalArgumentException(
                    String.format("Setting '%s' has wrong type: expected %s but got %s (value=%s)",
                            key, expectedType.getSimpleName(), value.getClass().getSimpleName(), value)
            );
        }
        PaintLogger.debugf("  %s = %s (type=%s)", key, value, value.getClass().getSimpleName());
    }
}