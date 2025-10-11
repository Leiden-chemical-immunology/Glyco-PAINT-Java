package debug;

/* IGNORE START
import fiji.plugin.trackmate.Settings;
import paint.shared.utils.PaintLogger;

import java.util.Map;

public class TrackMateSettingsDebugger {

    public static void logSettings(Settings settings) {
        PaintLogger.infof("==== Detector Settings (%s) ====",
                          settings.detectorFactory.getKey());

        for (Map.Entry<String, Object> entry : settings.detectorSettings.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String type = (value == null) ? "null" : value.getClass().getSimpleName();
            PaintLogger.infof("  %s = %s (type=%s)", key, value, type);
        }

        PaintLogger.infof("==== Tracker Settings (%s) ====",
                          settings.trackerFactory.getKey());

        for (Map.Entry<String, Object> entry : settings.trackerSettings.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String type = (value == null) ? "null" : value.getClass().getSimpleName();
            PaintLogger.infof("  %s = %s (type=%s)", key, value, type);
        }
    }
}

IGNORE END */