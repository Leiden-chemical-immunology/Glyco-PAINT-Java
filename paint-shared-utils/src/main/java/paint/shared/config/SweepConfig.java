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
 * The {@code SweepConfig} class provides functionality for loading, parsing, and retrieving
 * sweep-related configuration from a JSON file. This configuration is stored internally
 * as a {@link JsonObject}, and methods are provided to access specific types of data.
 */
public class SweepConfig {

    private final JsonObject root;

    /**
     * Constructs a SweepConfig object by parsing a JSON configuration file.
     * This constructor reads the specified file, parses its content as JSON, and initializes
     * the configuration data.
     *
     * @param filePath the path to the JSON file containing the sweep configuration.
     * @throws IOException if an I/O error occurs while reading the file.
     */
    public SweepConfig(String filePath) throws IOException {
        try (FileReader reader = new FileReader(filePath)) {
            this.root = JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    /**
     * Retrieves the active sweep values for a specified category.
     * This method processes the JSON configuration to extract and return
     * a map containing the category attributes and their corresponding active numeric values.
     * Only enabled attributes are included in the resulting map.
     *
     * @param category the name of the category whose sweep values are to be fetched.
     *                 It corresponds to a key in the root JSON object.
     * @return a map of attributes to lists of numeric values. The map is empty if the category
     *         does not exist or no active values are found.
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
     * Retrieves a boolean value from the configuration based on the specified section and key.
     * If the section or key does not exist, or the value is not a valid boolean representation,
     * the method returns the provided default value.
     *
     * @param section the name of the section in the configuration.
     * @param key the name of the key within the specified section.
     * @param defaultValue the value to return if the specified key is not found, or if the value cannot be resolved to a boolean.
     * @return the boolean value from the configuration if found and valid; otherwise, the provided default value.
     */
    public boolean getBoolean(String section, String key, boolean defaultValue) {
        if (!root.has(section) || !root.get(section).isJsonObject()) {
            return defaultValue;
        }
        JsonObject jsonObject = root.getAsJsonObject(section);
        if (!jsonObject.has(key)) {
            return defaultValue;
        }

        JsonElement jsonElement = jsonObject.get(key);
        if (!jsonElement.isJsonPrimitive()) {
            return defaultValue;
        }

        if (jsonElement.getAsJsonPrimitive().isBoolean()) {
            return jsonElement.getAsBoolean();
        }
        if (jsonElement.getAsJsonPrimitive().isString()) {
            String s = jsonElement.getAsString().trim();
            if ("true".equalsIgnoreCase(s)) return true;
            if ("false".equalsIgnoreCase(s)) return false;
        }
        if (jsonElement.getAsJsonPrimitive().isNumber()) {
            return jsonElement.getAsInt() != 0;
        }
        return defaultValue;
    }
}