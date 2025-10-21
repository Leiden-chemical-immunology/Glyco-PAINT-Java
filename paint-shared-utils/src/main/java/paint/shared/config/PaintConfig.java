/**
 * ============================================================================
 *  PaintConfig.java
 *  Shared configuration manager for the Paint suite.
 *
 *  Purpose:
 *    Manages JSON-based configuration files with forgiving,
 *    case-insensitive access and automatic default population.
 *
 *  Key features:
 *    - Singleton access via PaintConfig.instance()
 *    - Thread-safe lazy initialization
 *    - Self-healing defaults for missing keys
 *    - Case-insensitive key and section lookup
 *    - Separate static and instance APIs
 *
 *  Author: Herr Doctor
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
 */
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
    public static final String SECTION_PATHS            = "Paths";            // Section name for Path configuration.
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
     * Initializes the global PaintConfig with a custom config file location.
     * If called more than once, the first call "wins".
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
     * Re-initializes the global PaintConfig, replacing any existing instance.
     * Use this when switching projects.
     */
    public static void reinitialise(Path projectPath) {
        synchronized (PaintConfig.class) {
            INSTANCE = null;
            initialise(projectPath);
            PaintLogger.infof("PaintConfig reinitialised at %s",
                              projectPath.resolve(PAINT_CONFIGURATION_JSON));
        }
    }

    /**
     * Returns the global PaintConfig instance, creating it if necessary.
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

    private synchronized void ensureLoaded() {
        if (this.configData != null) return;

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                this.configData = GSON.fromJson(reader, JsonObject.class);
                if (this.configData == null) this.configData = new JsonObject();
            } catch (IOException | JsonParseException e) {
                PaintLogger.errorf("Failed to load config file: %s", e.getMessage());
                this.configData = new JsonObject();
            }
        } else {
            PaintLogger.warningf("No config file exists at %s, default created.\n", path);
            this.configData = new JsonObject();
            loadDefaults();
            save();
        }
    }

    /** Loads default configuration values (called if no file exists). */
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
        paint.addProperty("Log Level",                                             "INFO");
        configData.add(SECTION_PAINT, paint);

        JsonObject recordingViewer = new JsonObject();
        recordingViewer.addProperty("Save Mode",                                   "Ask");
        configData.add(SECTION_RECORDING_VIEWER, recordingViewer);

        JsonObject trackMate = new JsonObject();
        trackMate.addProperty("MAX_FRAME_GAP",                                     3);
        trackMate.addProperty("ALTERNATIVE_LINKING_COST_FACTOR",                   1.05);
        trackMate.addProperty("DO_SUBPIXEL_LOCALIZATION",                          false);
        trackMate.addProperty("MIN_NR_SPOTS_IN_TRACK",                             3);
        trackMate.addProperty("LINKING_MAX_DISTANCE",                              0.6);
        trackMate.addProperty("MAX_NR_SPOTS_IN_IMAGE",                             2000000);
        trackMate.addProperty("GAP_CLOSING_MAX_DISTANCE",                          1.2);
        trackMate.addProperty("TARGET_CHANNEL",                                    1);
        trackMate.addProperty("SPLITTING_MAX_DISTANCE",                            15.0);
        trackMate.addProperty("TRACK_COLOURING",                                   "TRACK_DURATION");
        trackMate.addProperty("RADIUS",                                            0.5);
        trackMate.addProperty("ALLOW_GAP_CLOSING",                                 true);
        trackMate.addProperty("DO_MEDIAN_FILTERING",                               false);
        trackMate.addProperty("ALLOW_TRACK_SPLITTING",                             false);
        trackMate.addProperty("ALLOW_TRACK_MERGING",                               false);
        trackMate.addProperty("MERGING_MAX_DISTANCE",                              15.0);
        trackMate.addProperty("Max Seconds Per Recording",                         2000);
        configData.add(SECTION_TRACKMATE, trackMate);

        trackMate.addProperty("Debug RunTrackMateOnProject",                       false);
        trackMate.addProperty("Debug RunTrackMateOnRecording",                     false);
        configData.add(SECTION_DEBUG, trackMate);

        trackMate.addProperty("Project Root",                                      "");
        trackMate.addProperty("Images Root",                                       "");
        configData.add(SECTION_PATHS, trackMate);

        // @formatter:on
    }

    /** Saves the current configuration to disk. */
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

    /** Removes a single key (always saves). */
    public static void remove(String section, String key) {
        instance().removeValue(section, key, true);
    }

    /** Removes an entire section (always saves). */
    public static void removeSection(String section) {
        instance().removeSectionValue(section, true);
    }

    // ============================================================================
    // Removal and Listing
    // ============================================================================

    public void removeValue(String section, String key, boolean autoSave) {
        ensureLoaded();
        JsonObject sec = getSection(section);
        if (sec != null) {
            sec.remove(key);
            if (autoSave) save();
        }
    }

    public void removeSectionValue(String section, boolean autoSave) {
        ensureLoaded();
        configData.remove(section);
        if (autoSave) save();
    }

    public Set<String> keys(String section) {
        JsonObject sec = getSection(section);
        return sec != null ? sec.keySet() : Collections.emptySet();
    }

    public Set<String> sections() {
        ensureLoaded();
        return configData.keySet();
    }

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

    private String getStringInternal(String section, String key, String defaultValue) {
        ensureLoaded();
        JsonObject sec = getSection(section);
        if (sec != null) {
            String realKey = findKeyIgnoreCase(sec, key);
            if (realKey != null && sec.get(realKey).isJsonPrimitive()) {
                return sec.getAsJsonPrimitive(realKey).getAsString();
            }
        }
        PaintLogger.warningf("No value for '%s' found, default '%s' applied", key, defaultValue);
        setStringValue(section, key, defaultValue, true);
        return defaultValue;
    }

    private int getIntInternal(String section, String key, int defaultValue) {
        ensureLoaded();
        JsonObject sec = getSection(section);
        if (sec != null) {
            String realKey = findKeyIgnoreCase(sec, key);
            if (realKey != null && sec.get(realKey).isJsonPrimitive()) {
                try {
                    return sec.getAsJsonPrimitive(realKey).getAsInt();
                } catch (NumberFormatException e) {
                    PaintLogger.warningf("Invalid '%s', default %d applied", realKey, defaultValue);
                    setIntValue(section, key, defaultValue, true);
                }
            }
        }
        PaintLogger.warningf("No value for '%s', default %d applied", key, defaultValue);
        setIntValue(section, key, defaultValue, true);
        return defaultValue;
    }

    private double getDoubleInternal(String section, String key, double defaultValue) {
        ensureLoaded();
        JsonObject sec = getSection(section);
        if (sec != null) {
            String realKey = findKeyIgnoreCase(sec, key);
            if (realKey != null && sec.get(realKey).isJsonPrimitive()) {
                try {
                    return sec.getAsJsonPrimitive(realKey).getAsDouble();
                } catch (NumberFormatException e) {
                    PaintLogger.warningf("Invalid '%s', default %.2f applied", realKey, defaultValue);
                    setDoubleValue(section, key, defaultValue, true);
                }
            }
        }
        PaintLogger.warningf("No value for '%s', default %.2f applied", key, defaultValue);
        setDoubleValue(section, key, defaultValue, true);
        return defaultValue;
    }

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
        PaintLogger.warningf("No value for '%s', default %b applied", key, defaultValue);
        setBooleanValue(section, key, defaultValue, true);
        return defaultValue;
    }

    /**
     * Returns a JsonObject for the given section name (case-insensitive),
     * or {@code null} if the section does not exist.
     * <p>
     * This is useful for external modules (e.g., TrackMateHeadless) that
     * need to iterate over dynamically defined sections like "Experiments".
     */
    public JsonObject getSection(String section) {
        ensureLoaded();
        return findSectionIgnoreCase(section);
    }

    private JsonObject getOrCreateSection(String section) {
        ensureLoaded();
        JsonObject sec = findSectionIgnoreCase(section);
        if (sec != null) return sec;
        JsonObject newSec = new JsonObject();
        configData.add(section, newSec);
        return newSec;
    }

    private String findKeyIgnoreCase(JsonObject obj, String key) {
        if (obj == null || key == null) return null;
        for (String k : obj.keySet()) {
            if (k.equalsIgnoreCase(key)) return k;
        }
        return null;
    }

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