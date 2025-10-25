/**
 * ============================================================================
 *  PaintConfig.java
 *  Shared configuration manager for the Paint suite.
 *  <p>
 *  Purpose:
 *    Manages JSON-based configuration files with forgiving,
 *    case-insensitive access and automatic default population.
 *  <p>
 *  Key features:
 *    - Singleton access via PaintConfig.instance()
 *    - Thread-safe lazy initialization
 *    - Self-healing defaults for missing keys
 *    - Case-insensitive key and section lookup
 *    - Separate static and instance APIs
 *  <p>
 *  Author: Hans Bakker
 *  Module: paint-shared-utils
 * ============================================================================
 */

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
 * This class follows a singleton pattern for managing a global configuration instance,
 * accessed via the {@link #instance()} method. It also supports initializing and
 * switching between different configuration files.
 *
 * Fields:
 * - {@code SECTION_GENERATE_SQUARES}: Section name for configuration related to generating squares.
 * - {@code SECTION_PAINT}: Section name for configuration specific to painting operations.
 * - {@code SECTION_RECORDING_VIEWER}: Section name related to recording viewer functionality.
 * - {@code SECTION_TRACKMATE}: Section name for configurations concerning TrackMate integration.
 * - {@code SECTION_DEBUG}: Section name for debug-related configuration.
 * - {@code INSTANCE}: Singleton instance of the {@code PaintConfig} class.
 * - {@code GSON}: Gson instance for JSON parsing and serialization.
 * - {@code path}: Path to the configuration file on disk.
 * - {@code configData}: Stored configuration data represented as a {@link JsonObject}.
 *
 * Key Features:
 * - Retrieve and store configuration values of various types (string, int, double, boolean).
 * - Dynamically create and remove configuration sections and keys.
 * - Ability to specify a default value if a key or section is not found.
 * - Thread-safe loading and saving of configuration data*/
public class PaintConfig {

    // ============================================================================
    // Section Name Constants
    // ============================================================================

    // @ formatter:off
    public static final String SECTION_GENERATE_SQUARES = "Generate Squares"; // Section name for Generate Squares configuration
    public static final String SECTION_PAINT            = "Paint";            // Section name for general Paint application settings.
    public static final String SECTION_RECORDING_VIEWER = "Recording Viewer"; // Section name for the Recording Viewer configuration.
    public static final String SECTION_TRACKMATE        = "TrackMate";        // Section name for TrackMate configuration.
    public static final String SECTION_DEBUG            = "Debug";            // Section name for Debug configuration.
    // @ formatter:on

    // ============================================================================
    // Singleton + Shared Resources
    // ============================================================================

    private static volatile PaintConfig INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;
    private JsonObject configData; // lazily loaded

    private PaintConfig(Path path) {
        this.path = path;
        this.configData = null;
    }

    // ============================================================================
    // Initialization and Instance Access
    // ============================================================================

    /**
     * Initializes the global PaintConfig instance with the specified project path.
     * If an instance already exists, it logs a warning and does not reinitialize
     * unless the specified path differs from the current configuration path.
     *
     * @param projectPath the base project directory to resolve the configuration file path
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
     * Reinitializes the global PaintConfig instance with the specified project path.
     * This method clears the existing configuration instance and creates a new one
     * using the provided path.
     *
     * @param projectPath the base project directory to resolve the configuration file path
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
     * Returns the singleton instance of {@code PaintConfig}, initializing it if necessary.
     * If the instance is not yet created, it initializes with the default configuration
     * path located in the user's home directory.
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
     * Ensures that the configuration data has been loaded into memory. This method is synchronized
     * to prevent concurrent access issues. If the configuration data is already loaded, it immediately
     * returns without performing further actions.
     *
     * If a configuration file exists at the specified path, it reads and parses the file into a
     * {@link JsonObject}. If the file cannot be read or the content is invalid, an error is logged,
     * and a new default {@link JsonObject} is created.
     *
     * If no configuration file exists at the specified path, a warning is logged, default values are
     * loaded, and a new configuration file is saved to the disk.
     *
     * This method is primarily responsible for initializing the {@code configData} field with either
     * the existing configuration from the file or freshly generated defaults.
     *
     * Thread-safety: This method locks the instance to prevent simultaneous operations that could
     * interfere with loading the configuration. It must be invoked before accessing or modifying the
     * configuration data to ensure consistency and availability.
     */
    private synchronized void ensureLoaded() {
        if (this.configData != null) {
            return;
        }

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                this.configData = GSON.fromJson(reader, JsonObject.class);
                if (this.configData == null) this.configData = new JsonObject();
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
     * Loads default configuration settings into the application's configuration store.
     *
     * This method initializes several sections of configuration, including but not limited to:
     * - Generate Squares: Contains parameters for calculations and processing such as
     *   minimum tracks, R-squared thresholds, variability limits, and density ratios.
     * - Paint: Manages settings related to image file handling, such as file extension and version.
     * - Recording Viewer: Determines behavior for saving recordings, such as prompting the user for action.
     * - TrackMate: Sets up various parameters for track analysis, including frame gaps, linking costs,
     *   spot tracking, track splitting, merging, median filtering, and distance thresholds.
     * - Debug: Contains flags for enabling or disabling debug-related configurations.
     *
     * Each configuration section is encapsulated in a JsonObject and added to the global configuration store.
     * Default values are statically defined within the method for consistency across application runs.
     */
    private void loadDefaults() {

        // @formatter:off
        JsonObject generateSquares = new JsonObject();
        generateSquares.addProperty("Min Tracks to Calculate Tau",                 20);
        generateSquares.addProperty("Min Required R Squared",                      0.1);
        generateSquares.addProperty("Max Allowable Variability",                   10.0);
        generateSquares.addProperty("Min Required Density Ratio",                  2.0);
        generateSquares.addProperty("Min Track Duration",                          0);
        generateSquares.addProperty("Max Track Duration",                          2000000);
        generateSquares.addProperty("Fraction of Squares to Determine Background", 0.1);
        generateSquares.addProperty("Exclude zero DC tracks from Tau Calculation", false);
        generateSquares.addProperty("Neighbour Mode",                              "Free");
        generateSquares.addProperty("Number of Squares in Recording",              400);
        generateSquares.addProperty("Plot Curve Fitting",                          false);
        configData.add(SECTION_GENERATE_SQUARES, generateSquares);

        JsonObject paint = new JsonObject();
        paint.addProperty("Version",                                               "1.0");
        paint.addProperty("Image File Extension",                                  ".nd2");
        configData.add(SECTION_PAINT, paint);

        JsonObject recordingViewer = new JsonObject();
        recordingViewer.addProperty("Save Mode",                                   "Ask");
        configData.add(SECTION_RECORDING_VIEWER, recordingViewer);

        JsonObject trackMate = new JsonObject();
        trackMate.addProperty(MAX_FRAME_GAP,                                     3);
        trackMate.addProperty(ALTERNATIVE_LINKING_COST_FACTOR,                   1.05);
        trackMate.addProperty(DO_SUBPIXEL_LOCALIZATION,                          false);
        trackMate.addProperty(MIN_NR_SPOTS_IN_TRACK,                             3);
        trackMate.addProperty(LINKING_MAX_DISTANCE,                              0.6);
        trackMate.addProperty(MAX_NR_SPOTS_IN_IMAGE,                             2000000);
        trackMate.addProperty(MAX_NR_SECONDS_PER_IMAGE,                          2000);
        trackMate.addProperty(GAP_CLOSING_MAX_DISTANCE,                          1.2);
        trackMate.addProperty(TARGET_CHANNEL,                                    1);
        trackMate.addProperty(SPLITTING_MAX_DISTANCE,                            15.0);
        trackMate.addProperty(TRACK_COLOURING,                                   "TRACK_DURATION");
        trackMate.addProperty(RADIUS,                                            0.5);
        trackMate.addProperty(ALLOW_GAP_CLOSING,                                 true);
        trackMate.addProperty(DO_MEDIAN_FILTERING,                               false);
        trackMate.addProperty(ALLOW_TRACK_SPLITTING,                             false);
        trackMate.addProperty(ALLOW_TRACK_MERGING,                               false);
        trackMate.addProperty(MERGING_MAX_DISTANCE,                              15.0);

        configData.add(SECTION_TRACKMATE, trackMate);

        trackMate.addProperty("Debug RunTrackMateOnProject",                       false);
        trackMate.addProperty("Debug RunTrackMateOnRecording",                     false);
        configData.add(SECTION_DEBUG, trackMate);

        // @formatter:on
    }

    /**
     * Saves the current configuration data to a file specified by the path.
     * This method ensures the directory structure exists before saving the
     * configuration data in JSON format using the GSON library.
     *
     * If an error occurs during the saving process, an error message is logged.
     *
     * Pre-condition: The configuration data must be loaded and valid.
     * Post-condition: The configuration data will be written to the specified path.
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

    public String getStringValue(String section, String key, String defaultValue) {
        return getStringInternal(section, key, defaultValue);
    }

    public int getIntValue(String section, String key, int defaultValue) {
        return getIntInternal(section, key, defaultValue);
    }

    public double getDoubleValue(String section, String key, double defaultValue) {
        return getDoubleInternal(section, key, defaultValue);
    }

    public boolean getBooleanValue(String section, String key, boolean defaultValue) {
        return getBooleanInternal(section, key, defaultValue);
    }

    public void setStringValue(String section, String key, String value, boolean autoSave) {
        ensureLoaded();
        getOrCreateSection(section).addProperty(key, value);
        if (autoSave) {
            save();
        }
    }

    public void setIntValue(String section, String key, int value, boolean autoSave) {
        ensureLoaded();
        getOrCreateSection(section).addProperty(key, value);
        if (autoSave) {
            save();
        }
    }

    public void setDoubleValue(String section, String key, double value, boolean autoSave) {
        ensureLoaded();
        getOrCreateSection(section).addProperty(key, value);
        if (autoSave) {
            save();
        }
    }

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
     * Removes a single key (always saves).
     *
     * @param section section name (case-insensitive)
     * @param key     key within the section to remove (case-insensitive)
     */
    public static void remove(String section, String key) {
        instance().removeValue(section, key, true);
    }

    /**
     * Removes an entire section (always saves).
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
     * If the autoSave parameter is set to true, saves the updated data.
     *
     * @param section the section from which the key-value pair should be removed
     * @param key the key identifying the value to be removed
     * @param autoSave whether to automatically save after removing the value
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
     * Removes a section from the configuration data.
     * If autoSave is enabled, the configuration data will
     * automatically be saved after the section is removed.
     *
     * @param section the name of the section to be removed
     * @param autoSave if true, automatically saves the configuration after removing the section
     */
    public void removeSectionValue(String section, boolean autoSave) {
        ensureLoaded();
        configData.remove(section);
        if (autoSave) save();
    }

    /**
     * Retrieves the set of keys from a specific section within a JSON object.
     *
     * @param section the name of the section to retrieve keys from
     * @return a set of keys in the specified section; if the section does not exist,
     *         returns an empty set
     */
    public Set<String> keys(String section) {
        JsonObject sec = getSection(section);
        return sec != null ? sec.keySet() : Collections.emptySet();
    }

    /**
     * Retrieves the set of section names available in the configuration data.
     * Ensures that the configuration data is loaded before fetching the section names.
     *
     * @return a set of strings representing the section names in the configuration.
     */
    public Set<String> sections() {
        ensureLoaded();
        return configData.keySet();
    }

    /**
     * Retrieves the JSON configuration data.
     *
     * This method ensures that the configuration data is loaded before returning it.
     *
     * @return a JsonObject representing the configuration data.
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
     * Retrieves a string value from the specified section and key. If the value is not found,
     * the provided default value is returned, and a warning is logged.
     *
     * @param section the name of the section to look for the key
     * @param key the key to search for in the specified section
     * @param defaultValue the value to return if the key is not found or does not have a valid string value
     * @return the string value associated with the specified key, or the provided default value if the key is not found
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
     * Retrieves an integer value from a specified section and key in a JSON structure.
     * If the key does not exist or the value is not a valid integer, a default value is applied
     * and logged, and the default value is returned.
     * The method ensures the data is loaded before attempting to retrieve the value.
     *
     * @param section     the section of the JSON structure to search for the key
     * @param key         the key to look up in the specified section
     * @param defaultValue the default integer value to return and apply if the key is not found
     *                     or the value is not valid
     * @return the integer value associated with the given key, or the default value if the key
     *         does not exist or the value is not valid
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
     * Retrieves a double value from a configuration section based on the provided key.
     * If the key does not exist or contains an invalid value, the default value is returned
     * and optionally written back to the configuration.
     *
     * @param section the section name in the configuration to search for the key
     * @param key the key within the section to retrieve the double value for
     * @param defaultValue the default value to return and apply if the key does not exist
     *                     or contains an invalid value
     * @return the double value associated with the key, or the default value if the key
     *         does not exist or contains an invalid value
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
     * Retrieves a boolean value from a specific section and key in the configuration.
     * If the key is not found or cannot be converted to a boolean, it applies and returns the default value.
     *
     * @param section the configuration section from which the key is to be retrieved
     * @param key the key whose boolean value is to be fetched
     * @param defaultValue the default boolean value to be returned and applied if the key is not found or invalid
     * @return the boolean value corresponding to the specified key or the default value if not found or invalid
     */
    private boolean getBooleanInternal(String section, String key, boolean defaultValue) {
        ensureLoaded();
        JsonObject sec = getSection(section);
        if (sec != null) {
            String realKey = findKeyIgnoreCase(sec, key);
            if (realKey != null && sec.get(realKey).isJsonPrimitive()) {
                try {
                    return sec.getAsJsonPrimitive(realKey).getAsBoolean();
                } catch (Exception ignored) {}
            }
        }
        PaintLogger.warnf("No value for '%s', default %b applied", key, defaultValue);
        setBooleanValue(section, key, defaultValue, true);
        return defaultValue;
    }

    /**
     * Retrieves the specified section from the loaded configuration, ignoring case.
     *
     * @param section the name of the section to retrieve from the configuration
     * @return a JsonObject representing the requested section, or null if the section does not exist
     */
    public JsonObject getSection(String section) {
        ensureLoaded();
        return findSectionIgnoreCase(section);
    }

    /**
     * Retrieves a section from the configuration if it exists, ignoring case sensitivity,
     * or creates a new section if it does not exist.
     *
     * @param section the name of the section to retrieve or create
     * @return the existing or newly created JsonObject section
     */
    private JsonObject getOrCreateSection(String section) {
        ensureLoaded();
        JsonObject sec = findSectionIgnoreCase(section);
        if (sec != null) return sec;
        JsonObject newSec = new JsonObject();
        configData.add(section, newSec);
        return newSec;
    }

    /**
     * Searches for a key in the given JsonObject, ignoring case sensitivity.
     * If a match is found, the original key from the JsonObject is returned.
     *
     * @param obj the JsonObject to search through, must not be null
     * @param key the key to search for (case-insensitive), must not be null
     * @return the original key from the JsonObject that matches the given key
     *         case-insensitively, or null if no match is found
     */
    private String findKeyIgnoreCase(JsonObject obj, String key) {
        if (obj == null || key == null) return null;
        for (String k : obj.keySet()) {
            if (k.equalsIgnoreCase(key)) return k;
        }
        return null;
    }

    /**
     * Searches for a section in the configuration data, ignoring case differences in the section name.
     * If a matching section is found, it returns the section as a JsonObject.
     * If no matching section is found, returns null.
     *
     * @param section the name of the section to search for, case-insensitive
     * @return the JsonObject corresponding to the matched section, or null if no match is found
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