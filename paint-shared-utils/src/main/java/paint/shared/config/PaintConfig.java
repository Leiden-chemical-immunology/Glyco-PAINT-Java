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
 * <p>
 * It provides type-safe getters/setters and CRUD-like operations on sections and keys.
 * CRUD: Create, Read, Update, Delete.
 * </p>
 */
public class PaintConfig {

    private final Path path;
    private final Gson gson;
    private JsonObject configData;

    /**
     * Creates a PaintConfig instance backed by the given path.
     * If the file does not exist, a default configuration is created and saved.
     *
     * @param path the JSON configuration file path
     */
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
            this.configData = new JsonObject();
            loadDefaults();
            save(); // Always save defaults immediately
        }
    }

    /**
     * Loads default configuration values into memory.
     * This method is called when no configuration file exists yet.
     */
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

    /**
     * Saves the current configuration state to the JSON file.
     */
    public void save() {
        try (Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(configData, writer);
        } catch (JsonIOException | IOException e) {
            AppLogger.errorf("Failed to save config file: %s", e.getMessage());
        }
    }

    // --- Getters ---

    /**
     * Gets a string value from the configuration, or a default if missing.
     *
     * @param section      the section name
     * @param key          the key name
     * @param defaultValue the default value to use if not found
     * @return the string value
     */
    public String getString(String section, String key, String defaultValue) {
        JsonObject sec = getSection(section);
        if (sec != null && sec.has(key) && sec.get(key).isJsonPrimitive()) {
            return sec.getAsJsonPrimitive(key).getAsString();
        }
        setString(section, key, defaultValue);
        return defaultValue;
    }

    /**
     * Gets an int value from the configuration, or a default if missing.
     *
     * @param section      the section name
     * @param key          the key name
     * @param defaultValue the default value to use if not found
     * @return the int value
     */
    public int getInt(String section, String key, int defaultValue) {
        JsonObject sec = getSection(section);
        if (sec != null && sec.has(key) && sec.get(key).isJsonPrimitive()) {
            try {
                return sec.getAsJsonPrimitive(key).getAsInt();
            } catch (NumberFormatException e) {
                setInt(section, key, defaultValue);
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Gets a double value from the configuration, or a default if missing.
     *
     * @param section      the section name
     * @param key          the key name
     * @param defaultValue the default value to use if not found
     * @return the double value
     */
    public double getDouble(String section, String key, double defaultValue) {
        JsonObject sec = getSection(section);
        if (sec != null && sec.has(key) && sec.get(key).isJsonPrimitive()) {
            try {
                return sec.getAsJsonPrimitive(key).getAsDouble();
            } catch (NumberFormatException e) {
                setDouble(section, key, defaultValue);
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Gets a boolean value from the configuration, or a default if missing.
     *
     * @param section      the section name
     * @param key          the key name
     * @param defaultValue the default value to use if not found
     * @return the boolean value
     */
    public boolean getBoolean(String section, String key, boolean defaultValue) {
        JsonObject sec = getSection(section);
        if (sec != null && sec.has(key) && sec.get(key).isJsonPrimitive()) {
            try {
                return sec.getAsJsonPrimitive(key).getAsBoolean();
            } catch (Exception e) {
                setBoolean(section, key, defaultValue);
                return defaultValue;
            }
        }
        return defaultValue;
    }

    // --- Setters with optional autoSave ---

    /**
     * Sets a string value in the configuration.
     *
     * @param section the section name
     * @param key     the key name
     * @param value   the string value
     */
    public void setString(String section, String key, String value) {
        setString(section, key, value, false);
    }

    /**
     * Sets a string value in the configuration.
     *
     * @param section  the section name
     * @param key      the key name
     * @param value    the string value
     * @param autoSave if true, immediately saves to disk
     */
    public void setString(String section, String key, String value, boolean autoSave) {
        getOrCreateSection(section).addProperty(key, value);
        if (autoSave) save();
    }

    /**
     * Sets an int value in the configuration.
     *
     * @param section the section name
     * @param key     the key name
     * @param value   the int value
     */
    public void setInt(String section, String key, int value) {
        setInt(section, key, value, false);
    }

    /**
     * Sets an int value in the configuration.
     *
     * @param section  the section name
     * @param key      the key name
     * @param value    the int value
     * @param autoSave if true, immediately saves to disk
     */
    public void setInt(String section, String key, int value, boolean autoSave) {
        getOrCreateSection(section).addProperty(key, value);
        if (autoSave) save();
    }

    /**
     * Sets a double value in the configuration.
     *
     * @param section the section name
     * @param key     the key name
     * @param value   the double value
     */
    public void setDouble(String section, String key, double value) {
        setDouble(section, key, value, false);
    }

    /**
     * Sets a double value in the configuration.
     *
     * @param section  the section name
     * @param key      the key name
     * @param value    the double value
     * @param autoSave if true, immediately saves to disk
     */
    public void setDouble(String section, String key, double value, boolean autoSave) {
        getOrCreateSection(section).addProperty(key, value);
        if (autoSave) save();
    }

    /**
     * Sets a boolean value in the configuration.
     *
     * @param section the section name
     * @param key     the key name
     * @param value   the boolean value
     */
    public void setBoolean(String section, String key, boolean value) {
        setBoolean(section, key, value, false);
    }

    /**
     * Sets a boolean value in the configuration.
     *
     * @param section  the section name
     * @param key      the key name
     * @param value    the boolean value
     * @param autoSave if true, immediately saves to disk
     */
    public void setBoolean(String section, String key, boolean value, boolean autoSave) {
        getOrCreateSection(section).addProperty(key, value);
        if (autoSave) save();
    }

    // --- Remove a key ---

    /**
     * Removes a key from a section (in-memory only).
     *
     * @param section the section name
     * @param key     the key name
     */
    public void remove(String section, String key) {
        remove(section, key, false);
    }

    /**
     * Removes a key from a section.
     *
     * @param section  the section name
     * @param key      the key name
     * @param autoSave if true, immediately saves to disk
     */
    public void remove(String section, String key, boolean autoSave) {
        JsonObject sec = getSection(section);
        if (sec != null) {
            sec.remove(key);
            if (autoSave) save();
        }
    }

    // --- Remove an entire section ---

    /**
     * Removes an entire section (in-memory only).
     *
     * @param section the section name
     */
    public void removeSection(String section) {
        removeSection(section, false);
    }

    /**
     * Removes an entire section.
     *
     * @param section  the section name
     * @param autoSave if true, immediately saves to disk
     */
    public void removeSection(String section, boolean autoSave) {
        configData.remove(section);
        if (autoSave) save();
    }

    /**
     * Lists all keys in a section.
     *
     * @param section the section name
     * @return a set of key names, empty if section not found
     */
    public Set<String> keys(String section) {
        JsonObject sec = getSection(section);
        if (sec != null) {
            return sec.keySet();
        }
        return Collections.emptySet();
    }

    /**
     * Lists all section names in the configuration.
     *
     * @return a set of section names
     */
    public Set<String> sections() {
        return configData.keySet();
    }

    /**
     * Gets the underlying {@link JsonObject} for advanced use.
     *
     * @return the raw JSON object
     */
    public JsonObject getJson() {
        return configData;
    }

    @Override
    public String toString() {
        return gson.toJson(configData);
    }

    // --- Internal helpers ---
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
}