/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.shared.config.paintconfig;

import com.google.gson.JsonObject;

/**
 * Case-insensitive JSON helpers kept separate for reuse.
 * <p>
 * Provides internal utility methods for case-insensitive key lookups within Google GSON {@link
 * JsonObject} instances.
 * </p>
 * <p>
 * The {@code JsonCase} class assists in retrieving keys from JSON objects when the exact
 * casing of the key name is not guaranteed. It iterates through the object's keyset and
 * returns the first matching key regardless of case.
 * </p>
 */
class JsonCase {

    String findKeyIgnoreCase(JsonObject obj, String key) {
        if (obj == null || key == null) return null;
        for (String k : obj.keySet()) {
            if (k.equalsIgnoreCase(key)) return k;
        }
        return null;
    }
}