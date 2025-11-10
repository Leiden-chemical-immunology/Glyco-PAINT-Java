package paint.shared.config.paintconfig;

import com.google.gson.JsonObject;

/** Case-insensitive JSON helpers kept separate for reuse. */
class JsonCase {

    String findKeyIgnoreCase(JsonObject obj, String key) {
        if (obj == null || key == null) return null;
        for (String k : obj.keySet()) {
            if (k.equalsIgnoreCase(key)) return k;
        }
        return null;
    }
}