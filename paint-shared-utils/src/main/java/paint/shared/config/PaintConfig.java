/******************************************************************************
 *  Class:        PaintConfig.java
 *  Package:      paint.shared.config
 *
 *  PURPOSE:
 *    Shared configuration manager for the Paint suite.
 *
 *  DESCRIPTION:
 *    Manages JSON-based configuration files with forgiving, case-insensitive
 *    access and automatic default population.
 *
 *  KEY FEATURES:
 *    - Singleton access via PaintConfig.instance()
 *    - Thread-safe lazy initialization
 *    - Self-healing defaults for missing keys
 *    - Case-insensitive key and section lookup
 *    - Separate static and instance APIs
 *
 *  AUTHOR:
 *    Hans Bakker
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

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

import static paint.shared.constants.PaintConstants.*;

/**
 * The {@code PaintConfig} class provides a configuration management system
 * for handling configuration data, organized into named sections and key-value pairs.
 * The configuration is stored as a JSON file and includes methods for reading,
 * updating, and persisting values. It supports automatic creation of default
 * configurations and offers both static and instance-level access.
 *
 * <p>This class follows a singleton pattern for managing a global configuration
 * instance, accessed via {@link #instance()}. It also supports initializing and
 * switching between different configuration files.
 *
 * <p>Key functional areas:
 * <ul>
 *   <li>Loading existing configuration or creating defaults if missing</li>
 *   <li>Saving configuration back to disk in pretty-printed JSON using Gson</li>
 *   <li>Case-insensitive lookup of section names and keys</li>
 *   <li>Typed getter and setter methods for String, int, double and boolean</li>
 *   <li>Static convenience methods for global use</li>
 * </ul>
 */
public class PaintConfig {

    // ============================================================================
    // Section Name Constants
    // ============================================================================

    public static final String SECTION_GENERATE_SQUARES = "Generate Squares"; // Section name for Generate Squares configuration
    public static final String SECTION_TRACKMATE        = "TrackMate";        // Section name for TrackMate configuration.
    public static final String SECTION_DEBUG            = "Debug";            // Section name for Debug configuration.

    // ============================================================================
    // Singleton + Shared Resources
    // ============================================================================

    private static volatile PaintConfig INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;
    private JsonObject configData; // lazily loaded

    /**
     * Private constructor to enforce singleton usage.
     *
     * @param path the file path where the JSON configuration is stored
     */
    private PaintConfig(Path path) {
        this.path = path;
        this.configData = null;
    }

    // ============================================================================
    // Initialization and Instance Access
    // ============================================================================

    /**
     * Initializes the global configuration instance with the specified project path.
     * If an instance already exists, a warning is logged if the path differs.
     *
     * @param projectPath the base project directory used to resolve the config file
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
                PaintLogger.warnf(
                        "PaintConfig already initialised at %s (attempted reinit with %s)\n",
                        INSTANCE.path, newConfigPath
                );
            }
        }
    }

    /**
     * Reinitialises the global configuration instance with a new project path.
     *
     * @param projectPath the base project directory used to resolve the config file
     */
    public static void reinitialise(Path projectPath) {
        synchronized (PaintConfig.class) {
            INSTANCE = null;
            initialise(projectPath);
            PaintLogger.debugf("PaintConfig reinitialised at %s",
                               projectPath.resolve(PAINT_CONFIGURATION_JSON));
        }
    }

    /**
     * Returns the singleton instance of {@code PaintConfig}, initializing it
     * if necessary with the default user-home based path.
     *
     * @return the singleton {@code PaintConfig} instance
     */
    public static PaintConfig instance() {
        if (INSTANCE == null) {
            Path defaultPath = Paths.get(System.getProperty("user.home"), PAINT_CONFIGURATION_JSON);
            initialise(defaultPath);
        }
        return INSTANCE;
    }

    // ============================================================================
    // Load, Save, Defaults
    // ============================================================================

    /**
     * Ensures that the configuration data has been loaded into memory.
     * If not already loaded, reads the JSON file or creates defaults.
     */
    private synchronized void ensureLoaded() {
        if (this.configData != null) {
            return;
        }

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                this.configData = GSON.fromJson(reader, JsonObject.class);
                if (this.configData == null) {
                    this.configData = new JsonObject();
                }
            } catch (IOException | JsonParseException e) {
                PaintLogger.errorf("Failed to load config file: %s", e.getMessage());
                this.configData = new JsonObject();
            }
        } else {
            PaintLogger.warnf("No config file exists at %s, default created.\n", path);
            this.configData = new JsonObject();
            loadDefaults();
            save();
        }
    }

    /**
     * Loads default configuration settings into the application’s configuration store.
     * This method populates default sections and values for Generate Squares, TrackMate,
     * Debug, and any other defined configuration sections.
     */
    private void loadDefaults() {
        
        JsonObject generateSquares = new JsonObject();
        generateSquares.addProperty(MIN_TRACKS_TO_CALCULATE_TAU,                           20);
        generateSquares.addProperty(MIN_REQUIRED_R_SQUARED,                                0.1);
        generateSquares.addProperty(MAX_ALLOWABLE_VARIABILITY,                             10.0);
        generateSquares.addProperty(MIN_REQUIRED_DENSITY_RATIO,                            2.0);
        generateSquares.addProperty(MIN_TRACK_DURATION,                                    0);
        generateSquares.addProperty(MAX_TRACK_DURATION,                                    2000000);
        generateSquares.addProperty("Fraction of Squares to Determine Background", 0.1);
        generateSquares.addProperty("Exclude zero DC tracks from Tau Calculation", false);
        generateSquares.addProperty(NEIGHBOUR_MODE,                                        "Free");
        generateSquares.addProperty(NUMBER_OF_SQUARES_IN_RECORDING,                        400);
        generateSquares.addProperty("Plot Curve Fitting",                          false);
        configData.add(SECTION_GENERATE_SQUARES, generateSquares);

        JsonObject trackMate = new JsonObject();
        trackMate.addProperty(MAX_FRAME_GAP,                                             3);
        trackMate.addProperty(ALTERNATIVE_LINKING_COST_FACTOR,                           1.05);
        trackMate.addProperty(DO_SUBPIXEL_LOCALIZATION,                                  false);
        trackMate.addProperty(MIN_NR_SPOTS_IN_TRACK,                                     3);
        trackMate.addProperty(LINKING_MAX_DISTANCE,                                      0.6);
        trackMate.addProperty(MAX_NR_SPOTS_IN_IMAGE,                                     2000000);
        trackMate.addProperty(MAX_NR_SECONDS_PER_IMAGE,                                  2000);
        trackMate.addProperty(GAP_CLOSING_MAX_DISTANCE,                                  1.2);
        trackMate.addProperty(TARGET_CHANNEL,                                            1);
        trackMate.addProperty(SPLITTING_MAX_DISTANCE,                                    15.0);
        trackMate.addProperty(TRACK_COLOURING,                                           "TRACK_DURATION");
        trackMate.addProperty(RADIUS,                                                    0.5);
        trackMate.addProperty(ALLOW_GAP_CLOSING,                                         true);
        trackMate.addProperty(DO_MEDIAN_FILTERING,                                       false);
        trackMate.addProperty(ALLOW_TRACK_SPLITTING,                                     false);
        trackMate.addProperty(ALLOW_TRACK_MERGING,                                       false);
        trackMate.addProperty(MERGING_MAX_DISTANCE,                                      15.0);
        configData.add(SECTION_TRACKMATE, trackMate);

        JsonObject debugFlags = new JsonObject();
        debugFlags.addProperty("Debug RunTrackMateOnProject",                   false);
        debugFlags.addProperty("Debug RunTrackMateOnRecording",                 false);
        configData.add(SECTION_DEBUG, debugFlags);

    }

    /**
     * Saves the current configuration data to disk in pretty-printed JSON format.
     */
    public void save() {
        ensureLoaded();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(configData, writer);
            }
        } catch (IOException e) {
            PaintLogger.errorf("Failed to save config file: %s", e.getMessage());
        }
    }

    // ============================================================================
    // Instance API: Getters and Setters
    // ============================================================================

    /**
     * Returns the String value for the given section/key, or defaultValue if missing.
     */
    public String getStringValue(String section, String key, String defaultValue) {
        return getStringInternal(section, key, defaultValue);
    }

    /**
     * Returns the int value for the given section/key, or defaultValue if missing.
     */
    public int getIntValue(String section, String key, int defaultValue) {
        return getIntInternal(section, key, defaultValue);
    }

    /**
     * Returns the double value for the given section/key, or defaultValue if missing.
     */
    public double getDoubleValue(String section, String key, double defaultValue) {
        return getDoubleInternal(section, key, defaultValue);
    }

    /**
     * Returns the boolean value for the given section/key, or defaultValue if missing.
     */
    public boolean getBooleanValue(String section, String key, boolean defaultValue) {
        return getBooleanInternal(section, key, defaultValue);
    }

    /**
     * Sets a String value in the specified section/key, optionally saving immediately.
     */
    public void setStringValue(String section, String key, String value, boolean autoSave) {
        ensureLoaded();
        getOrCreateSection(section).addProperty(key, value);
        if (autoSave) {
            save();
        }
    }

    /**
     * Sets an int value in the specified section/key, optionally saving immediately.
     */
    public void setIntValue(String section, String key, int value, boolean autoSave) {
        ensureLoaded();
        getOrCreateSection(section).addProperty(key, value);
        if (autoSave) {
            save();
        }
    }

    /**
     * Sets a double value in the specified section/key, optionally saving immediately.
     */
    public void setDoubleValue(String section, String key, double value, boolean autoSave) {
        ensureLoaded();
        getOrCreateSection(section).addProperty(key, value);
        if (autoSave) {
            save();
        }
    }

    /**
     * Sets a boolean value in the specified section/key, optionally saving immediately.
     */
    public void setBooleanValue(String section, String key, boolean value, boolean autoSave) {
        ensureLoaded();
        getOrCreateSection(section).addProperty(key, value);
        if (autoSave) {
            save();
        }
    }

    // ============================================================================
    // Static API (shortcuts)
    // ============================================================================

    public static String getString(String section, String key, String defaultValue) {
        return instance().getStringInternal(section, key, defaultValue);
    }

    public static int getInt(String section, String key, int defaultValue) {
        return instance().getIntInternal(section, key, defaultValue);
    }

    public static double getDouble(String section, String key, double defaultValue) {
        return instance().getDoubleInternal(section, key, defaultValue);
    }

    public static boolean getBoolean(String section, String key, boolean defaultValue) {
        return instance().getBooleanInternal(section, key, defaultValue);
    }

    public static void setString(String section, String key, String value) {
        instance().setStringValue(section, key, value, true);
    }

    public static void setInt(String section, String key, int value) {
        instance().setIntValue(section, key, value, true);
    }

    public static void setDouble(String section, String key, double value) {
        instance().setDoubleValue(section, key, value, true);
    }

    public static void setBoolean(String section, String key, boolean value) {
        instance().setBooleanValue(section, key, value, true);
    }

    // ============================================================================
    // Static Removals (shortcuts)
    // ============================================================================

    /**
     * Removes a single key from a specified section (and auto-saves).
     *
     * @param section section name (case-insensitive)
     * @param key     key within the section to remove (case-insensitive)
     */
    public static void remove(String section, String key) {
        instance().removeValue(section, key, true);
    }

    /**
     * Removes an entire section from the configuration data (and auto-saves).
     *
     * @param section section name to remove (case-insensitive)
     */
    public static void removeSection(String section) {
        instance().removeSectionValue(section, true);
    }

    // ============================================================================
    // Removal and Listing
    // ============================================================================

    /**
     * Removes a value associated with a specified key from a given section.
     * If autoSave is true, the change is immediately persisted.
     *
     * @param section  the section from which the key-value pair is removed
     * @param key      the key identifying the value to remove
     * @param autoSave whether to immediately save changes after removal
     */
    public void removeValue(String section, String key, boolean autoSave) {
        ensureLoaded();
        JsonObject sec = getSection(section);
        if (sec != null) {
            sec.remove(key);
            if (autoSave) {
                save();
            }
        }
    }

    /**
     * Removes a section from the configuration. If autoSave is true,
     * the configuration is saved immediately after removal.
     *
     * @param section  the name of the section to remove
     * @param autoSave if true, save changes immediately after removal
     */
    public void removeSectionValue(String section, boolean autoSave) {
        ensureLoaded();
        configData.remove(section);
        if (autoSave) {
            save();
        }
    }

    /**
     * Retrieves the set of keys inside a specified configuration section.
     *
     * @param section the name of the section to read keys from
     * @return a Set of key names present in that section, or an empty set if the section does not exist
     */
    public Set<String> keys(String section) {
        JsonObject sec = getSection(section);
        return (sec != null) ? sec.keySet() : Collections.emptySet();
    }

    /**
     * Retrieves the set of all section names present in the configuration.
     *
     * @return a Set of section names (as stored, case-sensitive representation)
     */
    public Set<String> sections() {
        ensureLoaded();
        return configData.keySet();
    }

    /**
     * Retrieves the full JSON configuration data as a JsonObject.
     *
     * @return a JsonObject representing the entire configuration
     */
    public JsonObject getJson() {
        ensureLoaded();
        return configData;
    }

    @Override
    public String toString() {
        ensureLoaded();
        return GSON.toJson(configData);
    }

    // ============================================================================
    // Internal Helpers (for forgiving lookup)
    // ============================================================================

    /**
     * Retrieves a String value from the specified section and key.
     * If the key is missing or invalid, logs a warning, writes the default value,
     * and returns the default.
     *
     * @param section      the section name in the configuration
     * @param key          the key to retrieve (case-insensitive)
     * @param defaultValue default String value if missing
     * @return the String value from configuration, or defaultValue if not present/valid
     */
    private String getStringInternal(String section, String key, String defaultValue) {
        ensureLoaded();
        JsonObject sec = getSection(section);
        if (sec != null) {
            String realKey = findKeyIgnoreCase(sec, key);
            if (realKey != null && sec.get(realKey).isJsonPrimitive()) {
                return sec.getAsJsonPrimitive(realKey).getAsString();
            }
        }
        PaintLogger.warnf("No value for '%s' found, default '%s' applied", key, defaultValue);
        setStringValue(section, key, defaultValue, true);
        return defaultValue;
    }

    /**
     * Retrieves an int value from the specified section and key.
     * If missing or invalid, logs, writes, and returns the default.
     *
     * @param section      the section name
     * @param key          the key name
     * @param defaultValue default integer value
     * @return the integer from config or default if missing/invalid
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
                    PaintLogger.warnf("Invalid '%s', default %d applied", realKey, defaultValue);
                    setIntValue(section, key, defaultValue, true);
                }
            }
        }
        PaintLogger.warnf("No value for '%s', default %d applied", key, defaultValue);
        setIntValue(section, key, defaultValue, true);
        return defaultValue;
    }

    /**
     * Retrieves a double value from the specified section and key.
     * If missing or invalid, logs, writes, and returns the default.
     *
     * @param section      the section name
     * @param key          the key name
     * @param defaultValue default double value
     * @return the double from config or default if missing/invalid
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
                    PaintLogger.warnf("Invalid '%s', default %.2f applied", realKey, defaultValue);
                    setDoubleValue(section, key, defaultValue, true);
                }
            }
        }
        PaintLogger.warnf("No value for '%s', default %.2f applied", key, defaultValue);
        setDoubleValue(section, key, defaultValue, true);
        return defaultValue;
    }

    /**
     * Retrieves a boolean value from the specified section and key.
     * If missing or invalid, logs, writes, and returns the default.
     *
     * @param section      the section name
     * @param key          the key name
     * @param defaultValue default boolean value
     * @return the boolean from config or default if missing/invalid
     */
    private boolean getBooleanInternal(String section, String key, boolean defaultValue) {
        ensureLoaded();
        JsonObject sec = getSection(section);
        if (sec != null) {
            String realKey = findKeyIgnoreCase(sec, key);
            if (realKey != null && sec.get(realKey).isJsonPrimitive()) {
                try {
                    return sec.getAsJsonPrimitive(realKey).getAsBoolean();
                } catch (Exception ignored) {
                }
            }
        }
        PaintLogger.warnf("No value for '%s', default %b applied", key, defaultValue);
        setBooleanValue(section, key, defaultValue, true);
        return defaultValue;
    }

    /**
     * Searches for a key in the given JsonObject, ignoring case. Returns the actual
     * key if found, or null if not found.
     *
     * @param obj the JsonObject to search
     * @param key the key to find (case-insensitive)
     * @return the real key in obj if found; null otherwise
     */
    private String findKeyIgnoreCase(JsonObject obj, String key) {
        if (obj == null || key == null) {
            return null;
        }
        for (String k : obj.keySet()) {
            if (k.equalsIgnoreCase(key)) {
                return k;
            }
        }
        return null;
    }

    /**
     * Retrieves the specified section from the loaded configuration, ignoring case differences
     * in section names.
     *
     * @param section the section name (case-insensitive)
     * @return the JsonObject for the section if found; null if no such section exists
     */
    public JsonObject getSection(String section) {
        ensureLoaded();
        return findSectionIgnoreCase(section);
    }

    /**
     * Retrieves or creates a section in the configuration. If the section does not exist,
     * it is added to the configuration store.
     *
     * @param section the section name to retrieve or create
     * @return the existing or newly created JsonObject section
     */
    private JsonObject getOrCreateSection(String section) {
        ensureLoaded();
        JsonObject sec = findSectionIgnoreCase(section);
        if (sec != null) {
            return sec;
        }
        JsonObject newSec = new JsonObject();
        configData.add(section, newSec);
        return newSec;
    }

    /**
     * Searches for a section in the configuration data by name, ignoring case.
     *
     * @param section the name of the section (case-insensitive)
     * @return the JsonObject corresponding to the section if found; null otherwise
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