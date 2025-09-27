package paint.shared.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Reads and interprets sweep configuration from a JSON file.
 * <p>
 * A sweep is only active if the JSON contains:
 * <pre>
 * {
 *   "Sweep Settings": {
 *     "Sweep": true
 *   }
 * }
 * </pre>
 * When enabled, sweep values are collected from the configured categories.
 */
public class SweepConfig {

    private final JsonObject root;

    /**
     * Load sweep configuration from a JSON file.
     *
     * @param filePath path to the JSON configuration file
     * @throws IOException if the file cannot be read
     */
    public SweepConfig(String filePath) throws IOException {
        try (FileReader reader = new FileReader(filePath)) {
            this.root = JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    /**
     * Returns the active sweep values as a map of attribute -> list of Numbers.
     * <p>
     * Sweep is only performed if "Sweep Settings" -> "Sweep" is true.
     * JSON integers will be returned as {@link Integer}, decimals as {@link Double}.
     *
     * @param category category name under which sweep attributes are defined
     * @return map of attributes to lists of numeric values, or an empty map if no sweep is active
     */
    public Map<String, List<Number>> getActiveSweepValues(String category) {
        Map<String, List<Number>> activeValues = new LinkedHashMap<>();

        // --- Check global sweep setting ---
        JsonObject sweepSettings = root.getAsJsonObject("Sweep Settings");
        if (sweepSettings != null) {
            JsonElement sweepEnabled = sweepSettings.get("Sweep");
            if (sweepEnabled != null && !sweepEnabled.getAsBoolean()) {
                // Sweep is globally disabled
                System.out.println("Sweep disabled in configuration.");
                return activeValues;
            }
        }

        // --- Look up the requested category ---
        JsonObject sweep = root.getAsJsonObject(category);
        if (sweep == null) {
            System.out.println("Sweep Section '" + category + "' not found");
            return activeValues;
        }

        // --- Iterate over enabled attributes ---
        for (Map.Entry<String, JsonElement> entry : sweep.entrySet()) {
            String attribute = entry.getKey();
            boolean enabled = entry.getValue().getAsBoolean();

            if (enabled && root.has(attribute)) {
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

    public static void main(String[] args) throws IOException {
        // Replace with your actual JSON file path
        SweepConfig config = new SweepConfig(
                "/Users/hans/JavaPaintProjects/paint-shared-utils/src/main/resources/Sweep Config.json");

        Map<String, List<Number>> sweeps = config.getActiveSweepValues("Generate Squares Sweep");
        sweeps.forEach((key, values) -> {
            System.out.println(key + " -> " + values);
            for (Number n : values) {
                System.out.println("   as int: " + n.intValue() + ", as double: " + n.doubleValue());
            }
        });

        sweeps = config.getActiveSweepValues("TrackMate Sweep");
        sweeps.forEach((key, values) -> {
            System.out.println(key + " -> " + values);
            for (Number n : values) {
                System.out.println("   as int: " + n.intValue() + ", as double: " + n.doubleValue());
            }
        });
    }
}