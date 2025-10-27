/******************************************************************************
 *  Class:        PaintPrefs.java
 *  Package:      paint.shared.utils
 *
 *  PURPOSE:
 *    Manages application-level preferences for the Glyco-PAINT application,
 *    stored in a macOS-style Property List (plist) file. This utility class
 *    provides thread-safe access to get and set preference values (strings,
 *    booleans, integers), automatically saving changes and allowing reload
 *    from disk. The class is non-instantiable and designed for static usage.
 *
 *  DESCRIPTION:
 *    • On class load, attempts to parse existing plist file at
 *      '~/Library/Preferences/Glyco-PAINT.plist', falling back to empty if missing or
 *      unreadable.
 *    • Provides synchronized methods for retrieving defaulted values (getString,
 *      getBoolean, getInt) and for storing values (putString, putBoolean, putInt).
 *    • Persists changes immediately via XML save on each modification.
 *    • Supports optional reload from disk to reflect external changes.
 *
 *  RESPONSIBILITIES:
 *    • Centralised preference storage for runtime settings and user configurations.
 *    • Ensuring atomic and thread-safe preference access.
 *    • Abstracting away property-list file handling and preference persistence.
 *
 *  USAGE EXAMPLE:
 *    boolean verbose = PaintPrefs.getBoolean("Runtime", "Verbose", false);
 *    PaintPrefs.putString("Project", "RootPath", "/users/hans/project");
 *    PaintPrefs.reload();
 *
 *  DEPENDENCIES:
 *    – com.dd.plist.NSDictionary, NSObject, PropertyListParser
 *    – java.io.File
 *    – java.lang.System
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-27
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.shared.utils;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListParser;

import java.io.File;

/**
 * The {@code PaintPrefs} class provides a mechanism to manage application-level
 * preferences stored in a plist file specific to the Glyco-PAINT application.
 * Preferences are persisted using Apple’s Property List (plist) format and loaded/saved
 * from a predefined file location in the user’s home directory.
 *
 * <p>This class offers both retrieval and storage methods for preferences, with
 * synchronized methods to ensure thread-safety.
 *
 * <p>It supports storing and retrieving key-value pairs (string, boolean, integer),
 * with defaults provided when retrieving values not yet present.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Thread-safe access (synchronized methods).</li>
 *   <li>Automatic saving of preferences to the plist file upon modification.</li>
 *   <li>Reload capability to pick up external edits.</li>
 *   <li>Fallback defaults during retrieval if a key is absent.</li>
 * </ul>
 *
 * <p>The class initializes the preferences from the plist file defined at
 * {@code "~/Library/Preferences/Glyco-PAINT.plist"} on class load; if the file does not
 * exist, it starts with an empty dictionary.
 *
 * <p>Note:
 * <ul>
 *   <li>Any changes made using the {@code put*} methods are immediately saved.</li>
 *   <li>When reloading, if the file does not exist or an error occurs, the preferences
 *       reset to an empty state.</li>
 *   <li>This class is not intended to be instantiated, as it contains only static methods and
 *       a private constructor to prevent instantiation.</li>
 * </ul>
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
            System.err.println("️ Could not load plist: " + e.getMessage());
            plist = new NSDictionary();
        }
    }

    private PaintPrefs() {
        // Prevent instantiation
    }

    /**
     * Retrieves a boolean preference value within the specified section/key, or writes the
     * given default if not present.
     *
     * @param section      the preference section name (namespace)
     * @param key          the preference key within the section
     * @param defaultValue the default value to return (and store) when none exists
     * @return the boolean value stored for the key, or the supplied default if absent
     */
    public static synchronized boolean getBoolean(String section,
                                                  String key,
                                                  boolean defaultValue) {
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

    /**
     * Retrieves a string preference value within the specified section/key, or writes the
     * given default if none exists.
     *
     * @param section      the preference section name (namespace)
     * @param key          the preference key within the section
     * @param defaultValue the default value to return (and store) when none exists
     * @return the string value stored for the key, or the supplied default if absent
     */
    public static synchronized String getString(String section,
                                                String key,
                                                String defaultValue) {
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

    /**
     * Stores a boolean value in the specified section/key in preferences,
     * and immediately persists the change to disk.
     *
     * @param section the preference section name (namespace)
     * @param key     the preference key within the section
     * @param value   the boolean value to store
     */
    public static synchronized void putBoolean(String section,
                                               String key,
                                               boolean value) {
        NSDictionary sectionDict = (NSDictionary) plist.objectForKey(section);
        if (sectionDict == null) {
            sectionDict = new NSDictionary();
            plist.put(section, sectionDict);
        }
        sectionDict.put(key, value);
        save();
    }

    /**
     * Stores a string value in the specified section/key in preferences,
     * and immediately persists the change to disk.
     *
     * @param section the preference section name (namespace)
     * @param key     the preference key within the section
     * @param value   the string value to store
     */
    public static synchronized void putString(String section,
                                              String key,
                                              String value) {
        NSDictionary sectionDict = (NSDictionary) plist.objectForKey(section);
        if (sectionDict == null) {
            sectionDict = new NSDictionary();
            plist.put(section, sectionDict);
        }
        sectionDict.put(key, value);
        save();
    }

    /**
     * Stores a string value at top-level (no section) and persists change.
     *
     * @param key   the preference key
     * @param value the string value to store
     */
    public static synchronized void putString(String key,
                                              String value) {
        plist.put(key, value);
        save();
    }

    /**
     * Stores a boolean value at top-level (no section) and persists change.
     *
     * @param key   the preference key
     * @param value the boolean value to store
     */
    public static synchronized void putBoolean(String key,
                                               boolean value) {
        plist.put(key, value);
        save();
    }

    /**
     * Stores an integer value at top-level (no section) and persists change.
     *
     * @param key   the preference key
     * @param value the integer value to store
     */
    public static synchronized void putInt(String key,
                                           int value) {
        plist.put(key, value);
        save();
    }

    // =========================================================================
    // Helper methods: save & reload
    // =========================================================================

    /**
     * Persists the current preference dictionary to the plist file on disk.
     * If writing fails, prints error message to stderr.
     */
    private static synchronized void save() {
        try {
            PropertyListParser.saveAsXML(plist, PREF_FILE);
        } catch (Exception e) {
            PaintLogger.errorf("Could not save plist: " + e.getMessage());
        }
    }

    /**
     * Forces a reload of the preferences from disk, discarding in-memory state.
     * If loading fails, resets to empty dictionary.
     */
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