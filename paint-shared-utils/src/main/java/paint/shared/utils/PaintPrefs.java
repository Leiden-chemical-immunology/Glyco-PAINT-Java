package paint.shared.utils;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListParser;

import java.io.File;

/**
 * The PaintPrefs class provides a mechanism to manage application-level preferences
 * stored in a plist file specific to the Glyco-PAINT application. Preferences are
 * persisted using Apple's Property List (plist) format and loaded/saved from a
 * predefined file location in the user's home directory. This class includes both
 * retrieval and storage methods for preferences, with synchronized methods to ensure
 * thread safety.
 *
 * This class is designed for storing and retrieving key-value pairs with support
 * for string, boolean, and integer values. Default values can be provided when
 * retrieving preferences.
 *
 * Key features:
 * - Synchronized methods to ensure thread-safe updates and retrieval.
 * - Automatic saving of preferences to the plist file upon modification.
 * - Allows reloading preferences from disk to reflect external changes.
 * - Provides fallback defaults during retrieval if a key is absent.
 *
 * Core methods:
 * - getString(String key, String defaultValue): Retrieves a string value or sets and returns the default if the key does not exist.
 * - getBoolean(String key, boolean defaultValue): Retrieves a boolean value or sets and returns the default if the key does not exist.
 * - getInt(String key, int defaultValue): Retrieves an integer value or sets and returns the default if the key does not exist.
 * - putString(String key, String value): Stores a string value for the given key.
 * - putBoolean(String key, boolean value): Stores a boolean value for the given key.
 * - putInt(String key, int value): Stores an integer value for the given key.
 * - reload(): Forces preferences to be reloaded from the plist file on disk.
 *
 * The class initializes the preferences from the plist file defined at
 * "~/Library/Preferences/Glyco-PAINT.plist" upon loading. If the file does not
 * exist, it starts with an empty set of preferences.
 *
 * Note:
 * - Any changes made using the put* methods are automatically saved to the plist file.
 * - When reloading, if the file does not exist or an error occurs, the preferences
 *   are reset to an empty state.
 * - This class is not intended to be instantiated, as it contains only static methods
 *   and a private constructor to prevent instantiation.
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
            System.err.println(" Could not load plist: " + e.getMessage());
            plist = new NSDictionary();
        }
    }

    private PaintPrefs() {}

    // =========================================================================
    // Core API — get or create default
    // =========================================================================

    // =========================================================================
    // Generic sectioned API
    // =========================================================================

    public static synchronized boolean getBoolean(String section, String key, boolean defaultValue) {
        NSDictionary sectionDict = (NSDictionary) plist.objectForKey(section);
        if (sectionDict == null) {
            sectionDict = new NSDictionary();
            plist.put(section, sectionDict);
        }
        NSObject obj = sectionDict.objectForKey(key);
        if (obj == null) {
            sectionDict.put(key, defaultValue);
            save();
            return defaultValue;
        }
        return Boolean.parseBoolean(obj.toString());
    }

    public static synchronized String getString(String section, String key, String defaultValue) {
        NSDictionary sectionDict = (NSDictionary) plist.objectForKey(section);
        if (sectionDict == null) {
            sectionDict = new NSDictionary();
            plist.put(section, sectionDict);
        }
        NSObject obj = sectionDict.objectForKey(key);
        if (obj == null) {
            sectionDict.put(key, defaultValue);
            save();
            return defaultValue;
        }
        return obj.toString();
    }

    public static synchronized void putBoolean(String section, String key, boolean value) {
        NSDictionary sectionDict = (NSDictionary) plist.objectForKey(section);
        if (sectionDict == null) {
            sectionDict = new NSDictionary();
            plist.put(section, sectionDict);
        }
        sectionDict.put(key, value);
        save();
    }

    public static synchronized void putString(String section, String key, String value) {
        NSDictionary sectionDict = (NSDictionary) plist.objectForKey(section);
        if (sectionDict == null) {
            sectionDict = new NSDictionary();
            plist.put(section, sectionDict);
        }
        sectionDict.put(key, value);
        save();
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