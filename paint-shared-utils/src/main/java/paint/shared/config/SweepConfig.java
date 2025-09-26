package paint.shared.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class SweepConfig {

    private final JsonObject root;

    public SweepConfig(String filePath) throws IOException {
        try (FileReader reader = new FileReader(filePath)) {
            this.root = JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    /**
     * Returns the active sweep values as a map of attribute -> list of Numbers.
     * JSON integers will become Integer, decimals will become Double.
     */
    public Map<String, List<Number>> getActiveSweepValues() {
        Map<String, List<Number>> activeValues = new LinkedHashMap<>();

        JsonObject sweep = root.getAsJsonObject("TrackMate Sweep");
        if (sweep != null) {
            System.out.println("Sweep Section not found");
        }
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
        SweepConfig config = new SweepConfig("/Users/hans/JavaPaintProjects/paint-shared-utils/src/main/resources/Sweep Config.json");
        Map<String, List<Number>> sweeps = config.getActiveSweepValues();

        sweeps.forEach((key, values) -> {
            System.out.println(key + " -> " + values);
            for (Number n : values) {
                System.out.println("   as int: " + n.intValue() + ", as double: " + n.doubleValue());
            }
        });
    }
}