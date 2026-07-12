/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.shared.config.paintconfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import paint.shared.utils.PaintLogger;

import java.nio.file.Path;
import java.nio.file.Paths;

import static paint.shared.constants.PaintFileNames.PAINT_CONFIGURATION_JSON;

/**
 * Thin façade over a JSON-backed config store. Keeps the original public/static API intact.
 * <p>
 * Provides a high-level API (façade) for accessing and managing the central "Paint
 * Configuration.json" file.
 * </p>
 * <p>
 * The {@code PaintConfig} class serves as the primary entry point for reading and writing
 * application settings. It organizes configuration into sections (e.g., "Generate Squares",
 * "TrackMate") and supports case-insensitive key lookups. It ensures that default values are
 * populated if a configuration file is missing and handles reinitialization when switching
 * projects.
 * </p>
 * <ul>
 *   <li>Singleton-based access to application-wide configuration.</li>
 *   <li>Section-based organization of settings.</li>
 *   <li>Automatic default value population via {@link DefaultConfigLoader}.</li>
 *   <li>Fault-tolerant loading with automatic backup of invalid files.</li>
 *   <li>Case-insensitive key retrieval.</li>
 * </ul>
 */
public class PaintConfig {

    // ============================================================================
    // Singleton + Shared Resources
    // ============================================================================

    private static volatile PaintConfig INSTANCE;
    /* package */ static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path        path;
    private final ConfigStore store;
    private final JsonCase    jsonCase;

    private PaintConfig(Path path) {
        this.path     = path;
        this.jsonCase = new JsonCase();
        this.store    = new ConfigStore(path, GSON);
    }

    // ---------------------------------------------------------------------
    // Initialisation
    // ---------------------------------------------------------------------

    /** Initialize for a given project root (creates file if missing). */
    public static void initialise(Path projectPath) {
        Path configPath = projectPath.resolve(PAINT_CONFIGURATION_JSON);
        if (INSTANCE == null) {
            synchronized (PaintConfig.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PaintConfig(configPath);
                    INSTANCE.store.ensureLoaded(() -> DefaultConfigLoader.loadDefaults(INSTANCE.store));
                    // Defensive: an existing config file may pre-date a key or be
                    // missing one. Add any absent default (existing values are kept)
                    // and persist once, so the file is always complete.
                    if (DefaultConfigLoader.backfillMissing(INSTANCE.store)) {
                        INSTANCE.store.save();
                    }
                }
            }
        } else {
            Path expected = projectPath.resolve(PAINT_CONFIGURATION_JSON);
            if (!INSTANCE.path.equals(expected)) {
                PaintLogger.warnf("PaintConfig already initialised at %s (attempted reinit with %s)\n",
                                  INSTANCE.path, expected);
            }
        }
    }

    /** Force reinitialisation to a new project root. */
    public static void reinitialise(Path projectPath) {
        synchronized (PaintConfig.class) {
            INSTANCE = null;
            initialise(projectPath);
            PaintLogger.debugf("PaintConfig reinitialised at %s",
                               projectPath.resolve(PAINT_CONFIGURATION_JSON));
        }
    }

    /** Access singleton (lazy-inits to user-home if needed). */
    public static PaintConfig instance() {
        if (INSTANCE == null) {
            Path defaultPath = Paths.get(System.getProperty("user.home"), PAINT_CONFIGURATION_JSON);
            initialise(defaultPath);
        }
        return INSTANCE;
    }

    // ---------------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------------

    public void save() {
        store.save();
    }

    // ---------------------------------------------------------------------
    // Sweep defaults file (same behavior, new home calls writer)
    // ---------------------------------------------------------------------

    public void setSweepDefaults(Path projectPath) {
        SweepConfigWriter.writeDefaultSweepJson(projectPath);
    }

    // ---------------------------------------------------------------------
    // Instance API
    // ---------------------------------------------------------------------

    public String getStringValue(String section, String key, String def) {
        JsonObject sec = getSection(section);
        if (sec != null) {
            String real = jsonCase.findKeyIgnoreCase(sec, key);
            if (real != null && sec.get(real).isJsonPrimitive()) {
                return sec.getAsJsonPrimitive(real).getAsString();
            }
        }
        PaintLogger.warnf("No value for '%s' found, default '%s' applied", key, def);
        // A read must not touch disk (A4): cache the default in memory only.
        setStringValue(section, key, def, false);
        return def;
    }

    public int getIntValue(String section, String key, int def) {
        JsonObject sec = getSection(section);
        if (sec != null) {
            String real = jsonCase.findKeyIgnoreCase(sec, key);
            if (real != null && sec.get(real).isJsonPrimitive()) {
                try {
                    return sec.getAsJsonPrimitive(real).getAsInt();
                } catch (Exception ignored) {
                    PaintLogger.warnf("Invalid '%s', default %d applied", real, def);
                    setIntValue(section, key, def, false);
                }
            }
        }
        PaintLogger.warnf("No value for '%s', default %d applied", key, def);
        // A read must not touch disk (A4): cache the default in memory only.
        setIntValue(section, key, def, false);
        return def;
    }

    public double getDoubleValue(String section, String key, double def) {
        JsonObject sec = getSection(section);
        if (sec != null) {
            String real = jsonCase.findKeyIgnoreCase(sec, key);
            if (real != null && sec.get(real).isJsonPrimitive()) {
                try {
                    return sec.getAsJsonPrimitive(real).getAsDouble();
                } catch (Exception ignored) {
                    PaintLogger.warnf("Invalid '%s', default %.2f applied", real, def);
                    setDoubleValue(section, key, def, false);
                }
            }
        }
        PaintLogger.warnf("No value for '%s', default %.2f applied", key, def);
        // A read must not touch disk (A4): cache the default in memory only.
        setDoubleValue(section, key, def, false);
        return def;
    }

    public boolean getBooleanValue(String section, String key, boolean def) {
        JsonObject sec = getSection(section);
        if (sec != null) {
            String real = jsonCase.findKeyIgnoreCase(sec, key);
            if (real != null && sec.get(real).isJsonPrimitive()) {
                try {
                    return sec.getAsJsonPrimitive(real).getAsBoolean();
                } catch (Exception ignored) { /* fallthrough */ }
            }
        }
        PaintLogger.warnf("No value for '%s', default %b applied", key, def);
        // A read must not touch disk (A4): cache the default in memory only.
        setBooleanValue(section, key, def, false);
        return def;
    }

    public boolean getBooleanValueNoWarning(String section, String key, boolean def) {
        JsonObject sec = getSection(section);
        if (sec != null) {
            String real = jsonCase.findKeyIgnoreCase(sec, key);
            if (real != null && sec.get(real).isJsonPrimitive()) {
                try {
                    return sec.getAsJsonPrimitive(real).getAsBoolean();
                } catch (Exception ignored) { /* fallthrough */ }
            }
        }
        // A read must not touch disk (A4): cache the default in memory only.
        setBooleanValue(section, key, def, false);
        return def;
    }

    public void setStringValue(String section, String key, String value, boolean autoSave) {
        JsonObject sec = store.getOrCreateSection(section);
        sec.addProperty(key, value);
        if (autoSave) {
            save();
        }
    }

    public void setIntValue(String section, String key, int value, boolean autoSave) {
        JsonObject sec = store.getOrCreateSection(section);
        sec.addProperty(key, value);
        if (autoSave) {
            save();
        }
    }

    public void setDoubleValue(String section, String key, double value, boolean autoSave) {
        JsonObject sec = store.getOrCreateSection(section);
        sec.addProperty(key, value);
        if (autoSave) {
            save();
        }
    }

    public void setBooleanValue(String section, String key, boolean value, boolean autoSave) {
        JsonObject sec = store.getOrCreateSection(section);
        sec.addProperty(key, value);
        if (autoSave) {
            save();
        }
    }

    @Override public String toString() {
        return GSON.toJson(store.root());
    }

    public void removeValue(String section, String key, boolean autoSave) {
        JsonObject sec = getSection(section);
        if (sec != null) {
            String real = jsonCase.findKeyIgnoreCase(sec, key);
            if (real != null) {
                sec.remove(real);
                if (autoSave) save();
            }
        }
    }

    public void removeSectionValue(String section, boolean autoSave) {
        store.removeSection(section);
        if (autoSave) {
            save();
        }
    }

    public static void remove (String s, String k) {
        instance().removeValue (s, k, true);
    }

    public static void removeSection(String s) {
        instance().removeSectionValue(s, true);
    }

    // ---------------------------------------------------------------------
    // Static API (shortcuts) — unchanged
    // ---------------------------------------------------------------------

    /**
     * Retrieves a string value from the configuration.
     *
     * @param s the configuration section
     * @param k the key within the section
     * @param d the default value if not found
     * @return the configuration value or default
     */
    public static String getString(String s, String k, String d) {
        return instance().getStringValue(s, k, d);
    }

    /**
     * Retrieves an integer value from the configuration.
     *
     * @param s the configuration section
     * @param k the key within the section
     * @param d the default value if not found
     * @return the configuration value or default
     */
    public static int getInt(String s, String k, int d) {
        return instance().getIntValue(s, k, d);
    }

    /**
     * Retrieves a double value from the configuration.
     *
     * @param s the configuration section
     * @param k the key within the section
     * @param d the default value if not found
     * @return the configuration value or default
     */
    public static double getDouble(String s, String k, double d) {
        return instance().getDoubleValue(s, k, d);
    }

    /**
     * Retrieves a boolean value from the configuration.
     *
     * @param s the configuration section
     * @param k the key within the section
     * @param d the default value if not found
     * @return the configuration value or default
     */
    public static boolean getBoolean(String s, String k, boolean d) {
        return instance().getBooleanValue(s, k, d);
    }

    /**
     * Sets a string value in the configuration and saves it.
     *
     * @param s section
     * @param k key
     * @param v value
     */
    public static void setString(String s, String k, String v) {
        instance().setStringValue(s, k, v, true);
    }

    /**
     * Sets an integer value in the configuration and saves it.
     *
     * @param s section
     * @param k key
     * @param v value
     */
    public static void setInt(String s, String k, int v) {
        instance().setIntValue(s, k, v, true);
    }

    /**
     * Sets a double value in the configuration and saves it.
     *
     * @param s section
     * @param k key
     * @param v value
     */
    public static void setDouble(String s, String k, double v) {
        instance().setDoubleValue(s, k, v, true);
    }

    /**
     * Sets a boolean value in the configuration and saves it.
     *
     * @param s section
     * @param k key
     * @param v value
     */
    public static void setBoolean(String s, String k, boolean v) {
        instance().setBooleanValue(s, k, v, true);
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    public JsonObject getSection(String section) {
        return store.getSection(section);
    }
}