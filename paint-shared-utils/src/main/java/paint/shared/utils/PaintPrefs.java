/******************************************************************************
 *  Class:        PaintPrefs.java
 *  Package:      paint.shared.utils
 *
 *  PURPOSE:
 *    Manages application-level preferences for the Glyco-PAINT application,
 *    stored in a macOS-style Property List (plist) file. Provides thread-safe
 *    access to get and set preference values (strings, booleans, integers),
 *    automatically saving changes and allowing reload from disk.
 *
 *  DESCRIPTION:
 *    • On class load, attempts to parse an existing plist file at
 *      '~/Library/Preferences/Glyco-PAINT.plist', falling back to an empty
 *      dictionary if missing or unreadable.
 *    • Provides synchronized getters (getString, getBoolean, getInt) and setters
 *      (putString, putBoolean, putInt) with default handling.
 *    • Persists all modifications immediately via XML save.
 *    • Supports manual reload from disk to pick up external edits.
 *
 *  RESPONSIBILITIES:
 *    • Centralised preference storage for runtime and configuration settings.
 *    • Ensures atomic and thread-safe preference access.
 *    • Abstracts away plist file handling and persistence details.
 *
 *  USAGE EXAMPLE:
 *    boolean verbose = PaintPrefs.getBoolean("Runtime", "Verbose", false);
 *    PaintPrefs.putString("Project", "RootPath", "/Users/hans/project");
 *    PaintPrefs.reload();
 *
 *  DEPENDENCIES:
 *    – com.dd.plist.NSDictionary, NSObject, PropertyListParser
 *    – java.io.File
 *    – java.lang.System
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-10-28
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
 * Provides static, thread-safe access to application preferences for Glyco-PAINT.
 * <p>
 * Preferences are stored in {@code ~/Library/Preferences/Glyco-PAINT.plist} using
 * Apple’s Property List (plist) format. The class handles reading, writing, and
 * automatic persistence of key-value pairs for various data types.
 * </p>
 * <ul>
 *   <li>Thread-safe access (synchronized methods)</li>
 *   <li>Automatic saving of preferences after updates</li>
 *   <li>Reload capability for external modifications</li>
 *   <li>Fallback to default values if keys are missing</li>
 * </ul>
 */
public final class PaintPrefs {

    // ───────────────────────────────────────────────────────────────────────────────
    // CONSTANTS AND STATE
    // ───────────────────────────────────────────────────────────────────────────────

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
            PaintLogger.errorf("Could not load plist: " + e.getMessage());
            plist = new NSDictionary();
        }
    }

    /** Private constructor to prevent instantiation. */
    private PaintPrefs() {
        // Deliberately empty
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // GET METHODS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves a boolean preference value or writes the given default if not present.
     *
     * @param section      section name (namespace)
     * @param key          preference key
     * @param defaultValue default to return and store if missing
     * @return stored boolean or default
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
     * Retrieves a string preference value or writes the given default if missing.
     *
     * @param section      section name (namespace)
     * @param key          preference key
     * @param defaultValue default to return and store if missing
     * @return stored string or default
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

    // ───────────────────────────────────────────────────────────────────────────────
    // PUT METHODS (SECTIONED)
    // ───────────────────────────────────────────────────────────────────────────────

    /** Stores a boolean value within a named section and persists the change. */
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

    /** Stores a string value within a named section and persists the change. */
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

    // ───────────────────────────────────────────────────────────────────────────────
    // PUT METHODS (TOP-LEVEL)
    // ───────────────────────────────────────────────────────────────────────────────

    /** Stores a string at the top-level (no section) and persists it. */
    public static synchronized void putString(String key, String value) {
        plist.put(key, value);
        save();
    }

    /** Stores a boolean at the top-level (no section) and persists it. */
    public static synchronized void putBoolean(String key, boolean value) {
        plist.put(key, value);
        save();
    }

    /** Stores an integer at the top-level (no section) and persists it. */
    public static synchronized void putInt(String key, int value) {
        plist.put(key, value);
        save();
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // FILE MANAGEMENT
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Saves the current preferences to the plist file.
     * Logs an error via {@link PaintLogger} if saving fails.
     */
    private static synchronized void save() {
        try {
            PropertyListParser.saveAsXML(plist, PREF_FILE);
        } catch (Exception e) {
            PaintLogger.errorf("Could not save plist: %s", e.getMessage());
        }
    }

    /**
     * Reloads preferences from disk, replacing the in-memory dictionary.
     * If loading fails, resets to a new empty dictionary.
     */
    public static synchronized void reload() {
        try {
            if (PREF_FILE.exists()) {
                plist = (NSDictionary) PropertyListParser.parse(PREF_FILE);
            } else {
                plist = new NSDictionary();
            }
        } catch (Exception e) {
            PaintLogger.errorf("Could not reload plist: %s", e.getMessage());
            plist = new NSDictionary();
        }
    }
}