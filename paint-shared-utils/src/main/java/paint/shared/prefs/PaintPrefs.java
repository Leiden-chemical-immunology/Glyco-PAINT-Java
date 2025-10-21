package paint.shared.prefs;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListParser;

import java.io.File;

/**
 * ============================================================================
 *  PaintPrefs.java
 *  Global preference handler for Glyco-PAINT.
 *
 *  <p>Stores preferences in ~/Library/Preferences/Glyco-PAINT.plist</p>
 *  <p>Behavior:
 *    <ul>
 *      <li>If a key exists → return its value</li>
 *      <li>If it doesn’t → create it with the provided default, save to disk, and return it</li>
 *    </ul>
 *  </p>
 * ============================================================================
 */
public final class PaintPrefs {

    private static final File PREF_FILE = new File(
            System.getProperty("user.home"),
            "Library/Preferences/Glyco-PAINT.plist"
    );

    private static NSDictionary plist = new NSDictionary();

    static {
        try {
            if (PREF_FILE.exists()) {
                plist = (NSDictionary) PropertyListParser.parse(PREF_FILE);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Could not load plist: " + e.getMessage());
            plist = new NSDictionary();
        }
    }

    private PaintPrefs() {}

    // =========================================================================
    // Core API — get or create default
    // =========================================================================

    public static synchronized String getString(String key, String defaultValue) {
        NSObject obj = plist.objectForKey(key);
        if (obj == null) {
            putString(key, defaultValue);
            return defaultValue;
        }
        return obj.toString();
    }

    public static synchronized boolean getBoolean(String key, boolean defaultValue) {
        NSObject obj = plist.objectForKey(key);
        if (obj == null) {
            putBoolean(key, defaultValue);
            return defaultValue;
        }
        return Boolean.parseBoolean(obj.toString());
    }

    public static synchronized int getInt(String key, int defaultValue) {
        NSObject obj = plist.objectForKey(key);
        if (obj == null) {
            putInt(key, defaultValue);
            return defaultValue;
        }
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // =========================================================================
    // Setters
    // =========================================================================

    public static synchronized void putString(String key, String value) {
        plist.put(key, value);
        save();
    }

    public static synchronized void putBoolean(String key, boolean value) {
        plist.put(key, value);
        save();
    }

    public static synchronized void putInt(String key, int value) {
        plist.put(key, value);
        save();
    }

    // =========================================================================
    // Save / reload
    // =========================================================================

    private static synchronized void save() {
        try {
            PropertyListParser.saveAsXML(plist, PREF_FILE);
        } catch (Exception e) {
            System.err.println("⚠️ Could not save plist: " + e.getMessage());
        }
    }

    /** Force re-read from disk (optional). */
    public static synchronized void reload() {
        try {
            if (PREF_FILE.exists()) {
                plist = (NSDictionary) PropertyListParser.parse(PREF_FILE);
            } else {
                plist = new NSDictionary();
            }
        } catch (Exception e) {
            System.err.println("⚠️ Could not reload plist: " + e.getMessage());
            plist = new NSDictionary();
        }
    }
}