/*=============================================================================
 *  Class:        JsonCase.java
 *  Package:      paint.shared.config.paintconfig
 *
 *  PURPOSE:
 *    Provides internal utility methods for case-insensitive key lookups
 *    within Google GSON {@link JsonObject} instances.
 *
 *  DESCRIPTION:
 *    The {@code JsonCase} class assists in retrieving keys from JSON objects
 *    when the exact casing of the key name is not guaranteed. It iterates
 *    through the object's keyset and returns the first matching key
 *    regardless of case.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *=============================================================================*/

package paint.shared.config.paintconfig;

import com.google.gson.JsonObject;

/** Case-insensitive JSON helpers kept separate for reuse. */
class JsonCase {

    String findKeyIgnoreCase(JsonObject obj, String key) {
        if (obj == null || key == null) return null;
        for (String k : obj.keySet()) {
            if (k.equalsIgnoreCase(key)) return k;
        }
        return null;
    }
}