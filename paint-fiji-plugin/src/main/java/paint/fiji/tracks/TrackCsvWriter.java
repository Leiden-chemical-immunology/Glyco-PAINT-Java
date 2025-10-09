package paint.fiji.tracks;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static paint.fiji.tracks.TrackAttributeCalculations.calculateTrackAttributes;
import static paint.shared.constants.PaintConstants.TIME_INTERVAL;
import static paint.shared.constants.PaintConstants.TRACK_COLS;

/**
 * Utility class for exporting TrackMate tracks into a CSV file.
 * <p>
 * Uses the schema defined in {@link paint.shared.constants.PaintConstants#TRACK_COLS}.
 * This guarantees the CSV header is consistent across the project.
 * </p>
 */
public class TrackCsvWriter {

    /**
     * Writes TrackMate track data to a CSV file.
     *
     * @param trackmate     the {@link TrackMate} instance containing the model and features
     * @param recordingName the name of the recording; used for unique keys in the output
     * @param csvFile       the target CSV file to write into (overwritten if it exists)
     * @param visibleOnly   if {@code true}, exports only visible (filtered) tracks;
     *                      if {@code false}, exports all tracks
     * @return the total number of spots across all exported tracks
     * @throws IOException if an I/O error occurs while writing
     */
    public static int writeTracksCsv(final TrackMate trackmate,
                                     final String recordingName,
                                     final File csvFile,
                                     final boolean visibleOnly) throws IOException {
        final Model model = trackmate.getModel();
        final TrackModel trackModel = model.getTrackModel();
        final FeatureModel featureModel = model.getFeatureModel();

        int nrSpotsInAllTracks = 0;

        try (PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(csvFile, false), StandardCharsets.UTF_8))) {

            // Write header row from constants
            writeCsvRow(printWriter, (Object[]) TRACK_COLS);

            // Collect track IDs (filtered if visibleOnly == true)
            final Set<Integer> trackIDs = trackModel.trackIDs(visibleOnly);
            final int squareNumber = -1; // placeholder (not computed here)
            final int labelNumber = -1;  // placeholder (not computed here)

            for (Integer trackId : trackIDs) {

                // ---- Built-in TrackMate features ----
                Double duration = featureModel.getTrackFeature(trackId, "TRACK_DURATION");
                Double nrSpots = featureModel.getTrackFeature(trackId, "NUMBER_SPOTS");
                Double xLoc = featureModel.getTrackFeature(trackId, "TRACK_X_LOCATION");
                Double yLoc = featureModel.getTrackFeature(trackId, "TRACK_Y_LOCATION");
                Double maxSpeed = featureModel.getTrackFeature(trackId, "TRACK_MAX_SPEED");
                Double medSpeed = featureModel.getTrackFeature(trackId, "TRACK_MEDIAN_SPEED");
                Double nrGaps = featureModel.getTrackFeature(trackId, "NUMBER_GAPS");
                Double longestGap = featureModel.getTrackFeature(trackId, "LONGEST_GAP");
                Double displacement = featureModel.getTrackFeature(trackId, "TRACK_DISPLACEMENT");

                // Normalize values (rounding and null defaults)
                duration = roundOr(duration, 3, -1);
                nrSpots = roundOr(nrSpots, 0, -1);
                xLoc = roundOr(xLoc, 2, -1);
                yLoc = roundOr(yLoc, 2, -1);
                maxSpeed = roundOr(maxSpeed, 2, -1);
                medSpeed = roundOr(medSpeed, 2, -1);
                nrGaps = defaultIfNull(nrGaps, -1.0);
                longestGap = defaultIfNull(longestGap, -1.0);
                displacement = roundOr(displacement, 2, -1);

                // ---- Custom calculated attributes ----
                TrackAttributes ca = calculateTrackAttributes(trackModel, trackId, TIME_INTERVAL);

                int numberOfSpotsInTrack = ca.numberOfSpotsInTracks;
                double diffusionCoeff = ca.diffusionCoeff;
                double diffusionCoeffExt = ca.diffusionCoeffExt;
                double totalDistance = ca.totalDistance;
                double confinementRatio = ca.confinementRatio;
                // Note: ca.displacement not used here (we keep TrackMate’s displacement)

                String uniqueKey = recordingName + "-" + trackId;

                // Update total spots count
                nrSpotsInAllTracks += numberOfSpotsInTrack;

                // Track label or fallback
                String trackLabel = trackModel.name(trackId);
                if (trackLabel == null) {
                    trackLabel = "Track-" + trackId;
                }

                // Write one CSV row (aligned with TRACK_COLS)
                writeCsvRow(printWriter,
                            uniqueKey,              // 0
                            recordingName,          // 1
                            trackId,                // 2
                            trackLabel,             // 3
                            asInt(nrSpots),         // 4
                            asInt(nrGaps),          // 5
                            asInt(longestGap),      // 6
                            duration,               // 7
                            xLoc,                   // 8
                            yLoc,                   // 9
                            displacement,           // 10 (from TrackMate)
                            maxSpeed,               // 11
                            medSpeed,               // 12
                            diffusionCoeff,         // 13 (calculated)
                            diffusionCoeffExt,      // 14 (calculated)
                            totalDistance,          // 15 (calculated)
                            confinementRatio,       // 16 (calculated)
                            squareNumber,           // 17
                            labelNumber             // 18
                );
            }
        }

        return nrSpotsInAllTracks;
    }

    // --------- helper methods ----------

    private static Double defaultIfNull(Double v, Double defaultValue) {
        return v == null ? defaultValue : v;
    }

    private static double roundTo(Double v, int places) {
        if (v == null) {
            return Double.NaN;
        }
        double scale = Math.pow(10, places);
        return Math.round(v * scale) / scale;
    }

    private static Double roundOr(Double v, int places, double ifNull) {
        return v == null ? ifNull : roundTo(v, places);
    }

    private static int asInt(Double v) {
        if (v == null || v.isNaN()) {
            return -1;
        }
        return (int) Math.round(v);
    }

    private static void writeCsvRow(PrintWriter pw, Object... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapeCsv(values[i]));
        }
        pw.println(sb);
    }

    private static String escapeCsv(Object o) {
        if (o == null) {
            return "";
        }
        String s = String.valueOf(o);
        boolean needsQuotes = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (s.contains("\"")) {
            s = s.replace("\"", "\"\"");
        }
        return needsQuotes ? ("\"" + s + "\"") : s;
    }
}