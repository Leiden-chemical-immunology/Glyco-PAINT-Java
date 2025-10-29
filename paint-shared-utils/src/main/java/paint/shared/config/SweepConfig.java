/******************************************************************************
 *  Class:        SweepConfig.java
 *  Package:      paint.shared.config
 *
 *  PURPOSE:
 *    Configuration loader & accessor for sweep parameters.
 *
 *  DESCRIPTION:
 *    Reads a JSON file and provides methods to extract active sweep values
 *    for configured categories. Works with Gson to parse JSON.
 *
 *  KEY FEATURES:
 *    - Loads JSON from a file path
 *    - Case-sensitive keys in JSON structure but supports structured retrieval
 *    - Retrieves maps of numeric values for enabled sweep attributes
 *
 *  AUTHOR:
 *    Your Name (or Hans Bakker if appropriate)
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Your Name. All rights reserved.
 ******************************************************************************/

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

// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// NOTE: All `.get(...)`, `.has(...)`, and `.getAsXxx(...)` methods refer to Gson's JsonObject / JsonElement API.
// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

/**
 * The {@code SweepConfig} class provides functionality for loading, parsing,
 * and retrieving sweep-related configuration from a JSON file.
 * The configuration is stored internally as a {@link JsonObject}, and methods
 * are provided to access specific types of data.
 *
 * <p>Typical usage:
 * <pre>
 *   SweepConfig cfg = new SweepConfig("path/to/file.json");
 *   Map&lt;String, List&lt;Number&gt;&gt; values = cfg.getActiveSweepValues("CategoryName");
 * </pre>
 *
 * <p>Key behaviours:
 * <ul>
 *   <li>Loads JSON from a file path in constructor.</li>
 *   <li>Processes a category section (if present) to find attributes flagged `true`.</li>
 *   <li>For each enabled attribute, collects numeric values from its corresponding JSON object.</li>
 *   <li>Returns an ordered map (in insertion order) of attribute → list of numbers.</li>
 * </ul>
 */
public class SweepConfig {

    private final JsonObject root;

    /**
     * Constructs a SweepConfig object by parsing a JSON configuration file.
     * This constructor reads the specified file, parses its content as JSON,
     * and initializes the configuration data.
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
     * <p>
     * This method processes the JSON configuration to extract and return
     * a map containing the category attributes and their corresponding
     * active numeric values. Only enabled attributes (true) in the category
     * section are included.
     *
     * @param category the name of the category whose sweep values are to be fetched.
     *                 It corresponds to a key in the root JSON object.
     * @return a map of attributes to lists of numeric values. The map is empty
     * if the category does not exist or no active values are found.
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
}