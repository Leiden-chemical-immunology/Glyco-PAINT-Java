package paint.shared.config.paintconfig;

import com.google.gson.JsonObject;

import static paint.shared.constants.PaintColumnNames.*;

/** Populates initial defaults into the store when no file exists. */
class DefaultConfigLoader {

    static void loadDefaults(ConfigStore store) {
        JsonObject root = store.root();

        // Generate Squares
        JsonObject generateSquares = new JsonObject();
        generateSquares.addProperty(MIN_TRACKS_TO_CALCULATE_TAU,    20);
        generateSquares.addProperty(MIN_REQUIRED_R_SQUARED,         0.1);
        generateSquares.addProperty(MAX_ALLOWABLE_VARIABILITY,      10.0);
        generateSquares.addProperty(MIN_REQUIRED_DENSITY_RATIO,     2.0);
        generateSquares.addProperty(MIN_TRACK_DURATION,             0);
        generateSquares.addProperty(MAX_TRACK_DURATION,             2000000);
        generateSquares.addProperty(NEIGHBOUR_MODE,                 "Free");
        generateSquares.addProperty(NUMBER_OF_SQUARES_IN_RECORDING, 400);
        generateSquares.addProperty(TAU_FITTING_PLOTS,              true);
        generateSquares.addProperty(BACKGROUND_PLOTS,               true);
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
        root.add("TrackMate", trackMate);

        // Debug
        JsonObject debug = new JsonObject();
        debug.addProperty(DEBUG_RUNTRACKMATEONPROJECT,   false);
        debug.addProperty(DEBUG_RUNTRACKMATEONRECORDING, false);
        root.add("Debug", debug);

        // Immediately saved by ConfigStore during first creation
    }

    private DefaultConfigLoader() {}
}