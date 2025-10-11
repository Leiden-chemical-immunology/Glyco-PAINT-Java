package paint.shared.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import paint.shared.utils.PaintLogger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import static paint.shared.constants.PaintConstants.PAINT_CONFIGURATION_JSON;

/**
 * PaintConfig manages JSON configuration files for the Paint application.
 * <p>
 * Features:
 * <ul>
 *   <li>Lazy initialisation: config file is only loaded when first accessed</li>
 *   <li>Self-healing defaults: missing keys are written back with default values</li>
 *   <li>Singleton access: a single global instance is maintained internally</li>
 *   <li>Static API for simple use with static imports (always saves)</li>
 *   <li>Instance API with {@code Value}-suffixed methods and optional autoSave</li>
 *   <li>Config file location can be set explicitly via {@link #initialise(Path)}</li>
 * </ul>
 *
 * <h3>Initialisation</h3>
 * <ul>
 *   <li>If you call {@link #initialise(Path)}, that path will be used for the config file.</li>
 *   <li>If you do not call {@code initialise}, the config file defaults to
 *       {@code <working-directory>/paint_config.json}.</li>
 * </ul>
 * <p>
 * Typical usage (static API):
 * <pre>{@code
 * // Initialise with project directory
 * PaintConfig.initialise(projectDir.resolve(PAINT_CONFIGURATION_JSON));
 *
 * import static paint.shared.config.PaintConfig.*;
 * int lw = getInt("Paint", "Line Width", 2);
 * setInt("Paint", "Line Width", 4);
 * }</pre>
 * <p>
 * Typical usage (instance API):
 * <pre>{@code
 * PaintConfig.initialise(projectDir.resolve(PAINT_CONFIGURATION_JSON));
 * PaintConfig cfg = PaintConfig.instance();
 * int lw = cfg.getIntValue("Paint", "Line Width", 2);
 * cfg.setIntValue("Paint", "Line Width", 4, true); // with autoSave
 * }</pre>
 */
public class PaintConfig {

    // --- Singleton ---
    private static volatile PaintConfig INSTANCE;

    /**
     * Initialises the global PaintConfig with a custom config file location.
     * If called more than once, the first call "wins".
     *
     * @param projectPath path to the prject directory
     */
    public static void initialise(Path projectPath) {
        Path configPath = projectPath.resolve(PAINT_CONFIGURATION_JSON);
        if (INSTANCE == null) {
            synchronized (PaintConfig.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PaintConfig(configPath);
                }
            }
        } else {
            Path newConfigPath = projectPath.resolve(PAINT_CONFIGURATION_JSON);
            if (!INSTANCE.path.equals(newConfigPath)) {
                PaintLogger.warningf(
                        "PaintConfig already initialised at %s (attempted reinit with %s)\n",
                        INSTANCE.path, newConfigPath
                );
            }
        }
    }


    /**
     * Re-initialises the global PaintConfig, replacing any existing instance.
     * Use this when switching projects (e.g. sweep subdirectories).
     *
     * @param projectPath path to the new project directory
     */
    public static void reinitialise(Path projectPath) {
        synchronized (PaintConfig.class) {
            INSTANCE = null; // force reset
            initialise(projectPath);
            PaintLogger.infof("PaintConfig reinitialised at %s",
                    projectPath.resolve(PAINT_CONFIGURATION_JSON));
        }
    }

    /**
     * Returns the current global PaintConfig singleton.
     * <p>
     * If not yet created, a new instance is created automatically using either:
     * <ul>
     *   <li>The path provided by {@link #initialise(Path)}, if called earlier</li>
     *   <li>Otherwise the default path: {@code <working-directory>/paint_config.json}</li>
     * </ul>
     *
     * @return the PaintConfig singleton
     */
    public static PaintConfig instance() {
        if (INSTANCE == null) {
            Path defaultPath = Paths.get(System.getProperty("user.home"), PAINT_CONFIGURATION_JSON);
            initialise(defaultPath);
        }
        return INSTANCE;
    }

    private final Path path;
    private final Gson gson;
    private JsonObject configData; // lazily loaded

    private PaintConfig(Path path) {
        this.path = path;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.configData = null;
    }

    private synchronized void ensureLoaded() {
        if (this.configData != null) return;

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                this.configData = gson.fromJson(reader, JsonObject.class);
                if (this.configData == null) {
                    this.configData = new JsonObject();
                }
            } catch (IOException | JsonParseException e) {
                PaintLogger.errorf("Failed to load config file: %s", e.getMessage());
                this.configData = new JsonObject();
            }
        } else {
            PaintLogger.warningf("No config file exists at %s, default created.\n", path);
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
        //generateSquares.addProperty("Plot to File", false);
        generateSquares.addProperty("Min Tracks to Calculate Tau", 20);
        generateSquares.addProperty("Min Required R Squared", 0.1);
        generateSquares.addProperty("Max Allowable Variability", 10.0);
        generateSquares.addProperty("Min Required Density Ratio", 2.0);
        generateSquares.addProperty("Min Track Duration", 0);
        generateSquares.addProperty("Max Track Duration", 2000000);
        generateSquares.addProperty("Fraction of Squares to Determine Background", 0.1);
        generateSquares.addProperty("Exclude zero DC tracks from Tau Calculation", false);
        generateSquares.addProperty("Neighbour Mode", "Free");
        generateSquares.addProperty("Number of Squares in Recording", 400);
        configData.add("Generate Squares", generateSquares);

        // === Paint ===
        JsonObject paint = new JsonObject();
        paint.addProperty("Version", "1.0");
        paint.addProperty("Image File Extension", ".nd2");
        //paint.addProperty("Fiji Path", "/Applications/Fiji.app");
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
     * Saves the current in-memory configuration to the config file.
     */
    public void save() {
        ensureLoaded();
        try (Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(configData, writer);
        } catch (IOException e) {
            PaintLogger.errorf("Failed to save config file: %s", e.getMessage());
        }
    }

    // ================================================================
    // Instance Getters (Value-suffixed)
    // ================================================================

    /**
     * Gets a string value, or returns and stores the default if missing.
     */
    public String getStringValue(String section, String key, String defaultValue) {
        return getStringInternal(section, key, defaultValue);
    }

    /**
     * Gets an int value, or returns and stores the default if missing.
     */
    public int getIntValue(String section, String key, int defaultValue) {
        return getIntInternal(section, key, defaultValue);
    }

    /**
     * Gets a double value, or returns and stores the default if missing.
     */
    public double getDoubleValue(String section, String key, double defaultValue) {
        return getDoubleInternal(section, key, defaultValue);
    }

    /**
     * Gets a boolean value, or returns and stores the default if missing.
     */
    public boolean getBooleanValue(String section, String key, boolean defaultValue) {
        return getBooleanInternal(section, key, defaultValue);
    }

    // ================================================================
    // Instance Setters (Value-suffixed, with autoSave overloads)
    // ================================================================

    /**
     * Sets a string value (in-memory only).
     */
    public void setStringValue(String section, String key, String value) {
        setStringValue(section, key, value, false);
    }

    /**
     * Sets a string value, with optional autoSave.
     */
    public void setStringValue(String section, String key, String value, boolean autoSave) {
        ensureLoaded();
        getOrCreateSection(section).addProperty(key, value);
        if (autoSave) save();
    }

    /**
     * Sets an int value (in-memory only).
     */
    public void setIntValue(String section, String key, int value) {
        setIntValue(section, key, value, false);
    }

    /**
     * Sets an int value, with optional autoSave.
     */
    public void setIntValue(String section, String key, int value, boolean autoSave) {
        ensureLoaded();
        getOrCreateSection(section).addProperty(key, value);
        if (autoSave) save();
    }

    /**
     * Sets a double value (in-memory only).
     */
    public void setDoubleValue(String section, String key, double value) {
        setDoubleValue(section, key, value, false);
    }

    /**
     * Sets a double value, with optional autoSave.
     */
    public void setDoubleValue(String section, String key, double value, boolean autoSave) {
        ensureLoaded();
        getOrCreateSection(section).addProperty(key, value);
        if (autoSave) save();
    }

    /**
     * Sets a boolean value (in-memory only).
     */
    public void setBooleanValue(String section, String key, boolean value) {
        setBooleanValue(section, key, value, false);
    }

    /**
     * Sets a boolean value, with optional autoSave.
     */
    public void setBooleanValue(String section, String key, boolean value, boolean autoSave) {
        ensureLoaded();
        getOrCreateSection(section).addProperty(key, value);
        if (autoSave) save();
    }

    // ================================================================
    // Static API (shortcuts, always save)
    // ================================================================

    /**
     * Static getter for strings.
     */
    public static String getString(String section, String key, String defaultValue) {
        return instance().getStringInternal(section, key, defaultValue);
    }

    /**
     * Static getter for ints.
     */
    public static int getInt(String section, String key, int defaultValue) {
        return instance().getIntInternal(section, key, defaultValue);
    }

    /**
     * Static getter for doubles.
     */
    public static double getDouble(String section, String key, double defaultValue) {
        return instance().getDoubleInternal(section, key, defaultValue);
    }

    /**
     * Static getter for booleans.
     */
    public static boolean getBoolean(String section, String key, boolean defaultValue) {
        return instance().getBooleanInternal(section, key, defaultValue);
    }

    /**
     * Static setter for strings (always saves).
     */
    public static void setString(String section, String key, String value) {
        instance().setStringValue(section, key, value, true);
    }

    /**
     * Static setter for ints (always saves).
     */
    public static void setInt(String section, String key, int value) {
        instance().setIntValue(section, key, value, true);
    }

    /**
     * Static setter for doubles (always saves).
     */
    public static void setDouble(String section, String key, double value) {
        instance().setDoubleValue(section, key, value, true);
    }

    /**
     * Static setter for booleans (always saves).
     */
    public static void setBoolean(String section, String key, boolean value) {
        instance().setBooleanValue(section, key, value, true);
    }

    /**
     * Static removal of a key (always saves).
     */
    public static void remove(String section, String key) {
        instance().removeValue(section, key, true);
    }

    /**
     * Static removal of a section (always saves).
     */
    public static void removeSection(String section) {
        instance().removeSectionValue(section, true);
    }

    // ================================================================
    // Remove (instance, Value-suffixed)
    // ================================================================

    /**
     * Removes a key (in-memory only).
     */
    public void removeValue(String section, String key) {
        removeValue(section, key, false);
    }

    /**
     * Removes a key, with optional autoSave.
     */
    public void removeValue(String section, String key, boolean autoSave) {
        ensureLoaded();
        JsonObject sec = getSection(section);
        if (sec != null) {
            sec.remove(key);
            if (autoSave) save();
        }
    }

    /**
     * Removes a section (in-memory only).
     */
    public void removeSectionValue(String section) {
        removeSectionValue(section, false);
    }

    /**
     * Removes a section, with optional autoSave.
     */
    public void removeSectionValue(String section, boolean autoSave) {
        ensureLoaded();
        configData.remove(section);
        if (autoSave) save();
    }

    // ================================================================
    // Listing / Access
    // ================================================================

    /**
     * Lists all keys in a section.
     */
    public Set<String> keys(String section) {
        JsonObject sec = getSection(section);
        if (sec != null) {
            return sec.keySet();
        }
        return Collections.emptySet();
    }

    /**
     * Lists all section names.
     */
    public Set<String> sections() {
        ensureLoaded();
        return configData.keySet();
    }

    /**
     * Returns the raw JSON object for advanced use.
     */
    public JsonObject getJson() {
        ensureLoaded();
        return configData;
    }

    @Override
    public String toString() {
        ensureLoaded();
        return gson.toJson(configData);
    }


    // ================================================================
    // Internal Helpers (with forgiving, case-insensitive lookup)
    // ================================================================

    /**
     * Retrieves a string value from the configuration, ignoring key case.
     * <p>
     * If the section or key cannot be found (case-insensitively), a warning is logged
     * and the provided default value is inserted into the configuration and saved.
     *
     * @param section       name of the config section (case-insensitive)
     * @param key           key name within the section (case-insensitive)
     * @param defaultValue  fallback value if missing or invalid
     * @return the stored or default value
     */
    private String getStringInternal(String section, String key, String defaultValue) {
        ensureLoaded();
        JsonObject sec = getSection(section);

        // Try to find an existing key, ignoring case differences
        if (sec != null) {
            String realKey = findKeyIgnoreCase(sec, key);
            if (realKey != null && sec.get(realKey).isJsonPrimitive()) {
                return sec.getAsJsonPrimitive(realKey).getAsString();
            }
        }

        // If missing, log and insert default (case-insensitive forgiving behavior)
        PaintLogger.warningf("No value for '%s' found, default value '%s' applied", key, defaultValue);
        setStringValue(section, key, defaultValue, true);
        return defaultValue;
    }

    /**
     * Retrieves an integer value from the configuration, ignoring key case.
     * Falls back to default on missing or invalid values.
     */
    private int getIntInternal(String section, String key, int defaultValue) {
        ensureLoaded();
        JsonObject sec = getSection(section);

        if (sec != null) {
            String realKey = findKeyIgnoreCase(sec, key);
            if (realKey != null && sec.get(realKey).isJsonPrimitive()) {
                try {
                    return sec.getAsJsonPrimitive(realKey).getAsInt();
                } catch (NumberFormatException e) {
                    PaintLogger.warningf("Invalid value for '%s' found, default value '%d' applied", realKey, defaultValue);
                    setIntValue(section, key, defaultValue, true);
                    return defaultValue;
                }
            }
        }

        PaintLogger.warningf("No value for '%s' found, default value '%d' applied", key, defaultValue);
        setIntValue(section, key, defaultValue, true);
        return defaultValue;
    }

    /**
     * Retrieves a double value from the configuration, ignoring key case.
     * Falls back to default on missing or invalid values.
     */
    private double getDoubleInternal(String section, String key, double defaultValue) {
        ensureLoaded();
        JsonObject sec = getSection(section);

        if (sec != null) {
            String realKey = findKeyIgnoreCase(sec, key);
            if (realKey != null && sec.get(realKey).isJsonPrimitive()) {
                try {
                    return sec.getAsJsonPrimitive(realKey).getAsDouble();
                } catch (NumberFormatException e) {
                    PaintLogger.warningf("Invalid value for '%s' found, default value '%0.2f' applied", realKey, defaultValue);
                    setDoubleValue(section, key, defaultValue, true);
                    return defaultValue;
                }
            }
        }

        PaintLogger.warningf("No value for '%s' found, default value '%0.2f' applied", key, defaultValue);
        setDoubleValue(section, key, defaultValue, true);
        return defaultValue;
    }

    /**
     * Retrieves a boolean value from the configuration, ignoring key case.
     * Falls back to default on missing or invalid values.
     */
    private boolean getBooleanInternal(String section, String key, boolean defaultValue) {
        ensureLoaded();
        JsonObject sec = getSection(section);

        if (sec != null) {
            String realKey = findKeyIgnoreCase(sec, key);
            if (realKey != null && sec.get(realKey).isJsonPrimitive()) {
                try {
                    return sec.getAsJsonPrimitive(realKey).getAsBoolean();
                } catch (Exception e) {
                    PaintLogger.warningf("Invalid value for '%s' found, default value '%b' applied", realKey, defaultValue);
                    setBooleanValue(section, key, defaultValue, true);
                    return defaultValue;
                }
            }
        }

        PaintLogger.warningf("No value for '%s' found, default value '%b' applied", key, defaultValue);
        setBooleanValue(section, key, defaultValue, true);
        return defaultValue;
    }

    /**
     * Returns a section from the configuration, performing a case-insensitive lookup.
     * <p>
     * This makes section access forgiving — e.g., both "Paint" and "paint"
     * resolve to the same section if it exists.
     *
     * @param section the section name (case-insensitive)
     * @return matching {@link JsonObject} or {@code null} if not found
     */
    private JsonObject getSection(String section) {
        ensureLoaded();
        JsonObject sec = findSectionIgnoreCase(section);
        if (sec != null) return sec;
        return null;
    }

    /**
     * Returns an existing section or creates a new one if missing.
     * Lookup is case-insensitive.
     *
     * @param section section name (case-insensitive)
     * @return the found or newly created {@link JsonObject} for that section
     */
    private JsonObject getOrCreateSection(String section) {
        ensureLoaded();
        JsonObject sec = findSectionIgnoreCase(section);
        if (sec != null) return sec;
        JsonObject newSec = new JsonObject();
        configData.add(section, newSec);
        return newSec;
    }

    // ---------------------------------------------------------------
    // Case-insensitive lookup helpers (for forgiving key access)
    // ---------------------------------------------------------------

    /**
     * Performs a case-insensitive search for a key within a given {@link JsonObject}.
     * <p>
     * Returns the actual stored key name if found, preserving its case.
     * This allows read operations to ignore user capitalization errors
     * while preserving the original key casing when writing back to disk.
     *
     * @param obj the JSON section object to search
     * @param key the key name to match (case-insensitive)
     * @return the actual stored key if found, otherwise {@code null}
     */
    private String findKeyIgnoreCase(JsonObject obj, String key) {
        if (obj == null || key == null) {
            return null;
        }
        for (String k : obj.keySet()) {
            if (k.equalsIgnoreCase(key)) {
                return k; // return the real key casing stored in JSON
            }
        }
        return null;
    }

    /**
     * Performs a case-insensitive search for a section within the root configuration.
     * <p>
     * This allows accessing "Paint", "paint", or "PAINT" interchangeably.
     *
     * @param section the section name to look up
     * @return the matching section {@link JsonObject} or {@code null} if not found
     */
    private JsonObject findSectionIgnoreCase(String section) {
        ensureLoaded();
        for (String s : configData.keySet()) {
            if (s.equalsIgnoreCase(section)) {
                return configData.getAsJsonObject(s);
            }
        }
        return null;
    }
}