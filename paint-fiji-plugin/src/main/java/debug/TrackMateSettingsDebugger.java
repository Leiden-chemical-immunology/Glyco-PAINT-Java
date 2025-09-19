package debug;

import fiji.plugin.trackmate.Settings;
import paint.shared.utils.AppLogger;

import java.util.Map;

public class TrackMateSettingsDebugger {

    public static void logSettings(Settings settings) {
        AppLogger.infof("==== Detector Settings (%s) ====",
                settings.detectorFactory.getKey());

        for (Map.Entry<String, Object> entry : settings.detectorSettings.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String type = (value == null) ? "null" : value.getClass().getSimpleName();
            AppLogger.infof("  %s = %s (type=%s)", key, value, type);
        }

        AppLogger.infof("==== Tracker Settings (%s) ====",
                settings.trackerFactory.getKey());

        for (Map.Entry<String, Object> entry : settings.trackerSettings.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String type = (value == null) ? "null" : value.getClass().getSimpleName();
            AppLogger.infof("  %s = %s (type=%s)", key, value, type);
        }
    }
}