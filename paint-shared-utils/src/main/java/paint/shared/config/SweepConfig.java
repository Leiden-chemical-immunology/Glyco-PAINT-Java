package paint.shared.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SweepConfig loads and provides access to sweep configuration JSON.
 * <p>
 * Features:
 * <ul>
 *   <li>Parse a JSON file once and expose structured access</li>
 *   <li>Return active sweep values for TrackMate (numeric lists)</li>
 *   <li>Convenience boolean getter for flags (true/false, 1/0, strings)</li>
 * </ul>
 * <p>
 * Typical JSON structure:
 * <pre>{@code
 * {
 *   "Sweep Settings": {
 *     "Sweep": true
 *   },
 *   "TrackMate Sweep": {
 *     "LINKING_MAX_DISTANCE": true,
 *     "MAX_FRAME_GAP": false
 *   },
 *   "LINKING_MAX_DISTANCE": {
 *     "v1": 0.3,
 *     "v2": 0.4
 *   }
 * }
 * }</pre>
 */
public class SweepConfig {

    private final JsonObject root;

    /**
     * Load sweep configuration JSON from the given file path.
     *
     * @param filePath JSON file containing sweep configuration
     * @throws IOException if the file cannot be read
     */
    public SweepConfig(String filePath) throws IOException {
        try (FileReader reader = new FileReader(filePath)) {
            this.root = JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    /**
     * Returns the active sweep values as a map of attribute -> list of Numbers.
     * JSON integers will become Integer, decimals will become Double.
     *
     * @param category name of the sweep category section (e.g. "TrackMate Sweep")
     * @return map of parameter name -> list of numeric values
     */
    public Map<String, List<Number>> getActiveSweepValues(String category) {
        Map<String, List<Number>> activeValues = new LinkedHashMap<>();

        JsonObject sweep = (root.has(category) && root.get(category).isJsonObject())
                ? root.getAsJsonObject(category)
                : null;
        if (sweep == null) {
            System.out.println("Sweep Section '" + category + "' not found");
            return activeValues;
        }

        for (Map.Entry<String, JsonElement> entry : sweep.entrySet()) {
            String attribute = entry.getKey();
            boolean enabled = entry.getValue().getAsBoolean();

            if (enabled && root.has(attribute) && root.get(attribute).isJsonObject()) {
                JsonObject section = root.getAsJsonObject(attribute);
                List<Number> values = new ArrayList<>();

                for (Map.Entry<String, JsonElement> valEntry : section.entrySet()) {
                    JsonElement val = valEntry.getValue();
                    if (val.isJsonPrimitive() && val.getAsJsonPrimitive().isNumber()) {
                        Number num = val.getAsNumber();
                        // Keep as Integer if it has no decimal part
                        if (num.doubleValue() == num.intValue()) {
                            values.add(num.intValue());
                        } else {
                            values.add(num.doubleValue());
                        }
                    }
                }
                activeValues.put(attribute, values);
            }
        }

        return activeValues;
    }

    /**
     * Convenience getter for booleans under a section/key.
     * Accepts true/false, "true"/"false" (case-insensitive), or 1/0.
     *
     * @param section      section name (e.g. "Sweep Settings")
     * @param key          key name (e.g. "Sweep")
     * @param defaultValue value to return if not found or invalid
     * @return resolved boolean value
     */
    public boolean getBoolean(String section, String key, boolean defaultValue) {
        if (!root.has(section) || !root.get(section).isJsonObject()) {
            return defaultValue;
        }
        JsonObject sec = root.getAsJsonObject(section);
        if (!sec.has(key)) return defaultValue;

        JsonElement el = sec.get(key);
        if (!el.isJsonPrimitive()) return defaultValue;

        if (el.getAsJsonPrimitive().isBoolean()) {
            return el.getAsBoolean();
        }
        if (el.getAsJsonPrimitive().isString()) {
            String s = el.getAsString().trim();
            if ("true".equalsIgnoreCase(s)) return true;
            if ("false".equalsIgnoreCase(s)) return false;
        }
        if (el.getAsJsonPrimitive().isNumber()) {
            return el.getAsInt() != 0;
        }
        return defaultValue;
    }
}