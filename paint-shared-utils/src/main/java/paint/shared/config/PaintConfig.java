package paint.shared.config;

import com.google.gson.*;
import paint.shared.utils.AppLogger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

/**
 * PaintConfig manages JSON configuration files for the Paint application.
 * It provides type-safe getters/setters and CRUD-like operations on sections and keys.
 * CRUD: Create, Read, Update, Delete
 */
public class PaintConfig {

    private final Path path;
    private final Gson gson;
    private JsonObject configData;

    public PaintConfig(Path path) {
        this.path = path;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    /**
     * Factory method to create a {@link PaintConfig} instance from the given JSON file path.
     *
     * @param path the path to the JSON configuration file
     * @return a new {@code PaintConfig} instance backed by the file at the given path
     */
    public static PaintConfig from(Path path) {
        return new PaintConfig(path);
    }

    // --- Load configuration file ---
    private void load() {
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                this.configData = gson.fromJson(reader, JsonObject.class);
                if (this.configData == null) {
                    this.configData = new JsonObject();
                }
            } catch (IOException | JsonParseException e) {
                AppLogger.errorf("Failed to load config file: %s", e.getMessage());
                this.configData = new JsonObject();
            }
        } else {
            // Create a default Data/config.json if it doesn't exist yet
            this.configData = new JsonObject();
            loadDefaults();
            save(); // And save it, so that next time it does exist

        }
    }

    private void loadDefaults() {
        // === Generate Squares ===
        JsonObject generateSquares = new JsonObject();
        generateSquares.addProperty("Plot to File", false);
        generateSquares.addProperty("Min Tracks to Calculate Tau", 20);
        generateSquares.addProperty("Max Track Duration", 1000000);
        generateSquares.addProperty("Min Required R Squared", 0.1);
        generateSquares.addProperty("Min Track Duration", 0);
        generateSquares.addProperty("Fraction of Squares to Determine Background", 0.1);
        generateSquares.addProperty("Nr of Squares in Row", 30);
        generateSquares.addProperty("Exclude zero DC tracks from Tau Calculation", false);
        generateSquares.addProperty("Max Allowable Variability", 10.0);
        generateSquares.addProperty("Min Required Density Ratio", 2.0);
        generateSquares.addProperty("Plot Max", 5);
        generateSquares.addProperty("Neighbour Mode", "Free");
        generateSquares.addProperty("Last Used Directory", "");
        configData.add("Generate Squares", generateSquares);

        // === Paint ===
        JsonObject paint = new JsonObject();
        paint.addProperty("Version", "1.0");
        paint.addProperty("Image File Extension", ".nd2");
        paint.addProperty("Fiji Path", "/Applications/Fiji.app");
        paint.addProperty("Log Level", "INFO");
        configData.add("Paint", paint);

        // === Recording Viewer ===
        JsonObject recordingViewer = new JsonObject();
        recordingViewer.addProperty("Save Mode", "Ask");
        configData.add("Recording Viewer", recordingViewer);

        // === TrackMate ===
        JsonObject trackMate = new JsonObject();
        trackMate.addProperty("MAX_FRAME_GAP", 3);
        trackMate.addProperty("ALTERNATIVE_LINKING_COST_FACTOR", 1.05);
        trackMate.addProperty("DO_SUBPIXEL_LOCALIZATION", false);
        trackMate.addProperty("MIN_NR_SPOTS_IN_TRACK", 3);
        trackMate.addProperty("LINKING_MAX_DISTANCE", 0.6);
        trackMate.addProperty("MAX_NR_SPOTS_IN_IMAGE", 2000000);
        trackMate.addProperty("GAP_CLOSING_MAX_DISTANCE", 1.2);
        trackMate.addProperty("TARGET_CHANNEL", 1);
        trackMate.addProperty("SPLITTING_MAX_DISTANCE", 15.0);
        trackMate.addProperty("TRACK_COLOURING", "TRACK_DURATION");
        trackMate.addProperty("RADIUS", 0.5);
        trackMate.addProperty("ALLOW_GAP_CLOSING", true);
        trackMate.addProperty("DO_MEDIAN_FILTERING", false);
        trackMate.addProperty("ALLOW_TRACK_SPLITTING", false);
        trackMate.addProperty("ALLOW_TRACK_MERGING", false);
        trackMate.addProperty("MERGING_MAX_DISTANCE", 15.0);
        configData.add("TrackMate", trackMate);
    }

    // --- Save configuration file ---
    public void save() {
        try (Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(configData, writer);
        } catch (JsonIOException | IOException e) {
            AppLogger.errorf("Failed to save config file: %s", e.getMessage());
        }
    }

    // --- Internal: get or create section ---
    private JsonObject getOrCreateSection(String section) {
        if (!configData.has(section) || !configData.get(section).isJsonObject()) {
            JsonObject newSection = new JsonObject();
            configData.add(section, newSection);
            return newSection;
        }
        return configData.getAsJsonObject(section);
    }

    private JsonObject getSection(String section) {
        if (configData.has(section) && configData.get(section).isJsonObject()) {
            return configData.getAsJsonObject(section);
        }
        return null;
    }

    // --- Getters ---
    public String getString(String section, String key, String defaultValue) {
        JsonObject sec = getSection(section);
        if (sec != null && sec.has(key) && sec.get(key).isJsonPrimitive()) {
            return sec.getAsJsonPrimitive(key).getAsString();
        }
        return defaultValue;
    }

    public int getInt(String section, String key, int defaultValue) {
        JsonObject sec = getSection(section);
        if (sec != null && sec.has(key) && sec.get(key).isJsonPrimitive()) {
            try {
                return sec.getAsJsonPrimitive(key).getAsInt();
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public double getDouble(String section, String key, double defaultValue) {
        JsonObject sec = getSection(section);
        if (sec != null && sec.has(key) && sec.get(key).isJsonPrimitive()) {
            try {
                return sec.getAsJsonPrimitive(key).getAsDouble();
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public boolean getBoolean(String section, String key, boolean defaultValue) {
        JsonObject sec = getSection(section);
        if (sec != null && sec.has(key) && sec.get(key).isJsonPrimitive()) {
            try {
                return sec.getAsJsonPrimitive(key).getAsBoolean();
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    // --- Setters ---
    public void setString(String section, String key, String value) {
        getOrCreateSection(section).addProperty(key, value);
    }

    public void setInt(String section, String key, int value) {
        getOrCreateSection(section).addProperty(key, value);
    }

    public void setDouble(String section, String key, double value) {
        getOrCreateSection(section).addProperty(key, value);
    }

    public void setBoolean(String section, String key, boolean value) {
        getOrCreateSection(section).addProperty(key, value);
    }

    // --- Remove a key ---
    public void remove(String section, String key) {
        JsonObject sec = getSection(section);
        if (sec != null) {
            sec.remove(key);
        }
    }

    // --- Remove an entire section ---
    public void removeSection(String section) {
        configData.remove(section);
    }

    // --- List keys in a section ---
    public Set<String> keys(String section) {
        JsonObject sec = getSection(section);
        if (sec != null) {
            return sec.keySet();
        }
        return Collections.emptySet();
    }

    // --- List all section names ---
    public Set<String> sections() {
        return configData.keySet();
    }

    // --- For direct access (advanced use) ---
    public JsonObject getJson() {
        return configData;
    }

    @Override
    public String toString() {
        return gson.toJson(configData);
    }
}