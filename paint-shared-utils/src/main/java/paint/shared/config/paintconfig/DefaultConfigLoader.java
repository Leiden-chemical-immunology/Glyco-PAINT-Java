/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.shared.config.paintconfig;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

import static paint.shared.constants.PaintStringConstants.*;

/**
 * Defines and applies the factory-default configuration.
 * <p>
 * Defines the factory-default configuration and applies it in two ways: • loadDefaults — seed
 * a brand-new store with the full default set. • backfillMissing — defensively add any default
 * key that is absent from an existing store (preserving existing values).
 * </p>
 * <p>
 * Both paths share a single source of truth, {@link #buildDefaults()}, so a default value can
 * never disagree with itself. Debug flags are intentionally NOT seeded: they are internal
 * developer toggles that default to off when absent, so seeding them would only clutter every
 * user's config file.
 * </p>
 */
class DefaultConfigLoader {

    private DefaultConfigLoader() {}

    /** The single source of truth for all seeded configuration defaults. */
    private static JsonObject buildDefaults() {
        JsonObject root = new JsonObject();

        // Generate Squares
        JsonObject generateSquares = new JsonObject();
        generateSquares.addProperty(MIN_TRACKS_TO_CALCULATE,        5);
        generateSquares.addProperty(MIN_TRACKS_TO_CALCULATE_TAU,    20);
        generateSquares.addProperty(MIN_REQUIRED_R_SQUARED,         0.1);
        generateSquares.addProperty(MAX_ALLOWABLE_VARIABILITY,      10.0);
        generateSquares.addProperty(MIN_REQUIRED_DENSITY_RATIO,     2.0);
        generateSquares.addProperty(NEIGHBOUR_MODE,                 "Free");
        generateSquares.addProperty(NUMBER_OF_SQUARES_IN_RECORDING, 400);
        generateSquares.addProperty(TAU_FITTING_PLOTS,              false);
        generateSquares.addProperty(BACKGROUND_PLOTS,               false);
        root.add(GENERATE_SQUARES, generateSquares);

        // TrackMate
        JsonObject trackMate = new JsonObject();
        trackMate.addProperty(MAX_FRAME_GAP,            3);
        trackMate.addProperty(ALTERNATIVE_LINKING_COST_FACTOR, 1.05);
        trackMate.addProperty(DO_SUBPIXEL_LOCALIZATION, false);
        trackMate.addProperty(MIN_NR_SPOTS_IN_TRACK,    3);
        trackMate.addProperty(LINKING_MAX_DISTANCE,     0.6);
        trackMate.addProperty(MAX_NR_SPOTS_IN_IMAGE,    2000000);
        trackMate.addProperty(MAX_NR_SECONDS_PER_IMAGE, 2000);
        trackMate.addProperty(GAP_CLOSING_MAX_DISTANCE, 1.2);
        trackMate.addProperty(TARGET_CHANNEL,           1);
        trackMate.addProperty(SPLITTING_MAX_DISTANCE,   15.0);
        trackMate.addProperty(TRACK_COLOURING,          "TRACK_DURATION");
        trackMate.addProperty(RADIUS,                   0.5);
        trackMate.addProperty(ALLOW_GAP_CLOSING,        true);
        trackMate.addProperty(DO_MEDIAN_FILTERING,      false);
        trackMate.addProperty(ALLOW_TRACK_SPLITTING,    false);
        trackMate.addProperty(ALLOW_TRACK_MERGING,      false);
        trackMate.addProperty(MERGING_MAX_DISTANCE,     15.0);
        trackMate.addProperty(RUN_GENERATE_SQUARES_AFTER, false);
        root.add(TRACKMATE, trackMate);

        return root;
    }

    /** Seed a brand-new store with the complete default set. */
    static void loadDefaults(ConfigStore store) {
        JsonObject root = store.root();
        for (Map.Entry<String, JsonElement> section : buildDefaults().entrySet()) {
            root.add(section.getKey(), section.getValue());
        }
    }

    /**
     * Defensively adds any default key that is missing from the store, while
     * preserving every existing value. Section and key names are matched
     * case-insensitively, consistent with the store's case-insensitive lookup,
     * so no duplicate-cased keys are ever introduced.
     *
     * @param store the configuration store to complete
     * @return true if anything was added (the caller should then persist)
     */
    static boolean backfillMissing(ConfigStore store) {
        JsonObject root = store.root();
        boolean changed = false;

        for (Map.Entry<String, JsonElement> secEntry : buildDefaults().entrySet()) {
            String     sectionName = secEntry.getKey();
            JsonObject defSection  = secEntry.getValue().getAsJsonObject();

            JsonObject rootSection = findSectionIgnoreCase(root, sectionName);
            if (rootSection == null) {
                root.add(sectionName, defSection);
                changed = true;
                continue;
            }
            for (Map.Entry<String, JsonElement> keyEntry : defSection.entrySet()) {
                if (!hasKeyIgnoreCase(rootSection, keyEntry.getKey())) {
                    rootSection.add(keyEntry.getKey(), keyEntry.getValue());
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static JsonObject findSectionIgnoreCase(JsonObject root, String name) {
        for (String k : root.keySet()) {
            if (k.equalsIgnoreCase(name) && root.get(k).isJsonObject()) {
                return root.getAsJsonObject(k);
            }
        }
        return null;
    }

    private static boolean hasKeyIgnoreCase(JsonObject section, String key) {
        for (String k : section.keySet()) {
            if (k.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }
}
