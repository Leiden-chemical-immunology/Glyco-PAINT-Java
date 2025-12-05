package paint.shared.config.paintconfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import paint.shared.utils.PaintLogger;

import java.nio.file.Path;
import java.nio.file.Paths;

import static paint.shared.constants.PaintFileNames.PAINT_CONFIGURATION_JSON;


/**
 * Thin façade over a JSON-backed config store.
 * Keeps the original public/static API intact.
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

    public void save() { store.save(); }

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
        setStringValue(section, key, def, true);
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
                    setIntValue(section, key, def, true);
                }
            }
        }
        PaintLogger.warnf("No value for '%s', default %d applied", key, def);
        setIntValue(section, key, def, true);
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
                    setDoubleValue(section, key, def, true);
                }
            }
        }
        PaintLogger.warnf("No value for '%s', default %.2f applied", key, def);
        setDoubleValue(section, key, def, true);
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
        setBooleanValue(section, key, def, true);
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


//    public Set<String> keys(String section) {
//        JsonObject sec = getSection(section);
//        return (sec != null) ? sec.keySet() : Collections.emptySet();
//    }


//    public JsonObject getJson() {
//        return store.root();
//    }

    @Override public String toString() {
        return GSON.toJson(store.root());
    }

    // ---------------------------------------------------------------------
    // Static API (shortcuts) — unchanged
    // ---------------------------------------------------------------------

    public static String getString(String s, String k, String d) {
        return instance().getStringValue(s, k, d);
    }

    public static int getInt(String s, String k, int d) {
        return instance().getIntValue(s, k, d);
    }

    public static double getDouble(String s, String k, double d) {
        return instance().getDoubleValue(s, k, d);
    }

    public static boolean getBoolean(String s, String k, boolean d) {
        return instance().getBooleanValue(s, k, d);
    }

    public static void setString(String s, String k, String v) {
        instance().setStringValue(s, k, v, true);
    }

    public static void setInt(String s, String k, int v) {
        instance().setIntValue(s, k, v, true);
    }

    public static void setDouble(String s, String k, double v) {
        instance().setDoubleValue(s, k, v, true);
    }

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