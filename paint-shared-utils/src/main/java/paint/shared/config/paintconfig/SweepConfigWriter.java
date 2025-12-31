/*=============================================================================
 *  Class:        SweepConfigWriter.java
 *  Package:      paint.shared.config.paintconfig
 *
 *  PURPOSE:
 *    Handles the generation and persistence of the "Paint Sweep Configuration.json"
 *    file.
 *
 *  DESCRIPTION:
 *    The {@code SweepConfigWriter} provides static methods to build a default
 *    sweep configuration containing predefined parameter ranges for TrackMate
 *    and Generate Squares. This allows users to perform parameter sensitivity
 *    analyses (sweeps) with a standardized starting point.
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
import paint.shared.utils.PaintLogger;

import java.io.Writer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static paint.shared.constants.PaintStringConstants.*;

/** Builds and writes the “Paint Sweep Configuration.json” file. */
class   SweepConfigWriter {

    static void writeDefaultSweepJson(Path projectPath) {
        JsonObject root = new JsonObject();

        // 1) "Sweep Settings" section with only the flag
        JsonObject sweepSettings = new JsonObject();
        sweepSettings.addProperty("Sweep", true);
        root.add("Sweep Settings", sweepSettings);

        // 2) TrackMate Sweep flags
        JsonObject trackMateSweep = new JsonObject();
        trackMateSweep.addProperty(THRESHOLD,                       true);
        trackMateSweep.addProperty(MAX_FRAME_GAP,                   false);
        trackMateSweep.addProperty(ALTERNATIVE_LINKING_COST_FACTOR, false);
        trackMateSweep.addProperty(DO_SUBPIXEL_LOCALIZATION,        false);
        trackMateSweep.addProperty(MIN_NR_SPOTS_IN_TRACK,           false);
        trackMateSweep.addProperty(LINKING_MAX_DISTANCE,            false);
        trackMateSweep.addProperty(MAX_NR_SPOTS_IN_IMAGE,           false);
        trackMateSweep.addProperty(GAP_CLOSING_MAX_DISTANCE,        false);
        trackMateSweep.addProperty(TARGET_CHANNEL,                  false);
        trackMateSweep.addProperty(SPLITTING_MAX_DISTANCE,          false);
        trackMateSweep.addProperty(TRACK_COLOURING,                 false);
        trackMateSweep.addProperty(RADIUS,                          false);
        trackMateSweep.addProperty(ALLOW_GAP_CLOSING,               false);
        trackMateSweep.addProperty(DO_MEDIAN_FILTERING,             false);
        trackMateSweep.addProperty(ALLOW_TRACK_SPLITTING,           false);
        trackMateSweep.addProperty(ALLOW_TRACK_MERGING,             false);
        trackMateSweep.addProperty(MERGING_MAX_DISTANCE,            false);
        root.add("TrackMate Sweep", trackMateSweep);

        // 3) Value ranges (top-level keys per your target JSON)
        root.add(THRESHOLD, values(obj(
                "Value 1", 30,
                "Value 2", 20,
                "Value 3", 10,
                "Value 4", 5)));

        root.add(MAX_FRAME_GAP, values(obj(
                "Value 1", 3,
                "Value 2", 4)));

        root.add(LINKING_MAX_DISTANCE, values(obj(
                "Value 0", 0.2,
                "Value 1", 0.3,
                "Value 2", 0.4,
                "Value 3", 0.5,
                "Value 4", 0.6,
                "Value 5", 0.7,
                "Value 6", 0.8,
                "Value 7", 0.9,
                "Value 8", 13)));

        root.add(ALTERNATIVE_LINKING_COST_FACTOR, values(obj(
                "Value 0", 1.02,
                "Value 1", 1.03,
                "Value 2", 1.04,
                "Value 3", 1.05,
                "Value 4", 1.06,
                "Value 5", 1.07,
                "Value 6", 1.08,
                "Value 7", 1.09,
                "Value 8", 1.10)));

        root.add(RADIUS, values(obj(
                "Value 0", 0.2,
                "Value 1", 0.3,
                "Value 2", 0.4,
                "Value 3", 0.5,
                "Value 4", 0.6,
                "Value 5", 0.7,
                "Value 6", 0.8,
                "Value 7", 0.9,
                "Value 8", 1.0,
                "Value 9", 1.1,
                "Value 10", 1.2)));

        root.add(MIN_NR_SPOTS_IN_TRACK, values(obj(
                "Value 0", 2,
                "Value 1", 3,
                "Value 2", 4,
                "Value 3", 5)));

        root.add(GAP_CLOSING_MAX_DISTANCE, values(obj(
                "Value 0", 0.7,
                "Value 1", 0.8,
                "Value 2", 0.9,
                "Value 3", 1.0,
                "Value 4", 1.1,
                "Value 5", 1.2,
                "Value 6", 1.3)));

        root.add(SPLITTING_MAX_DISTANCE, values(obj(
                "Value 0", 10.0,
                "Value 1", 11.0,
                "Value 2", 12.0,
                "Value 3", 13.0,
                "Value 4", 14.0,
                "Value 5", 15.0,
                "Value 6", 16.0,
                "Value 7", 17.0,
                "Value 8", 18.0,
                "Value 9", 19.0,
                "Value 10", 20.0)));

        root.add(MERGING_MAX_DISTANCE, values(obj(
                "Value 0", 10.0,
                "Value 1", 11.0,
                "Value 2", 12.0,
                "Value 3", 13.0,
                "Value 4", 14.0,
                "Value 5", 15.0,
                "Value 6", 16.0,
                "Value 7", 17.0,
                "Value 8", 18.0,
                "Value 9", 19.0,
                "Value 10", 20.0)));

        // 4) Generate Squares Sweep flags + ranges
        JsonObject genSquaresSweep = new JsonObject();
        genSquaresSweep.addProperty(MIN_REQUIRED_R_SQUARED,                      false);
        genSquaresSweep.addProperty(MIN_TRACKS_TO_CALCULATE_TAU,                 true);
        genSquaresSweep.addProperty(FRACTION_OF_SQUARES_TO_DETERMINE_BACKGROUND, false);
        genSquaresSweep.addProperty(NUMBER_OF_SQUARES_IN_ROW,                    false);
        genSquaresSweep.addProperty(EXCLUDE_ZERO_DC_TRACKS_FROM_TAU_CALCULATION, false);
        genSquaresSweep.addProperty(MAX_ALLOWABLE_VARIABILITY,                   false);
        genSquaresSweep.addProperty(MIN_REQUIRED_DENSITY_RATIO,                  false);
        genSquaresSweep.addProperty(NUMBER_OF_SQUARES_IN_COLUMN,                 false);
        root.add("Generate Squares Sweep", genSquaresSweep);

        root.add(MIN_REQUIRED_R_SQUARED,       values(obj("Value 0", 0.1, "Value 1", 0.2, "Value 2", 0.3, "Value 3", 0.4,
                                                              "Value 4",    0.5,       "Value 5", 0.6, "Value 6", 0.7, "Value 7", 0.8)));
        root.add(MIN_TRACKS_TO_CALCULATE_TAU,  values(obj("Value 0", 5, "Value 1", 10, "Value 2", 15, "Value 3", 20, "Value 4", 25)));

        // 5) Write to disk
        try {
            Path filePath = projectPath.resolve("Paint Sweep Configuration.json");
            try (Writer w = Files.newBufferedWriter(filePath)) {
                PaintConfig.GSON.toJson(root, w);
            }
        } catch (IOException e) {
            PaintLogger.errorf("Failed to save Paint Sweep Configuration file: %s", e.getMessage());
        }
    }

    // ---- tiny helpers to make the static blocks readable (Java 8 friendly) ----

    private static JsonObject obj(Object k1, Object v1, Object... kv) {
        JsonObject o = new JsonObject();
        put(o, k1, v1);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            put(o, kv[i], kv[i + 1]);
        }
        return o;
    }

    private static JsonObject values(JsonObject src) {
        // Already a { "Value n": X } object; return as-is for clarity.
        return src;
    }

    private static void put(JsonObject o, Object k, Object v) {
        String key = String.valueOf(k);
        if (v instanceof Number) {
            o.addProperty(key, (Number) v);
        } else if (v instanceof Boolean) {
            o.addProperty(key, (Boolean) v);
        } else {
            o.addProperty(key, String.valueOf(v));
        }
    }

    private SweepConfigWriter() {}
}