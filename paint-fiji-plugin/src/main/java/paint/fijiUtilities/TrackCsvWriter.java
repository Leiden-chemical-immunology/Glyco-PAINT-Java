package paint.fijiUtilities;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.FeatureModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static constants.PaintConstants.TIME_INTERVAL;
import static paint.fijiUtilities.TrackAttributeCalculations.calculateTrackAttributes;


//
// TracksCsvWriter is called by RunTrackMate to write a CSV file with the tracks.
//

public class TrackCsvWriter {

    /**
     * Writes the TrackMate track data to a CSV file.
     *
     * @param trackmate    the {@link TrackMate} instance containing the tracks and features
     * @param recordingName the name of the recording, used in the output for identification
     * @param csvFile      the target CSV file to write the track data into
     * @param visibleOnly  if {@code true}, only visible (filtered) tracks are exported;
     *                     if {@code false}, all tracks are exported
     * @return the total number of spots across all exported tracks
     * @throws IOException if an I/O error occurs while writing the file
     */
    public static int writeTracksCsv(final TrackMate trackmate,
                                      final String recordingName,
                                      final File csvFile,
                                      final boolean visibleOnly) throws IOException {
        final Model model = trackmate.getModel();
        final TrackModel trackModel = model.getTrackModel();
        final FeatureModel featureModel = model.getFeatureModel();

        // Header
        final String[] headers = {
                "Unique Key",                      // 0
                "Recording Name",                  // 1
                "Track Id",                        // 2
                "Track Label",                     // 3
                "Number of Spots",                 // 4
                "Number of Gaps",                  // 5
                "Longest Gap",                     // 6
                "Track Duration",                  // 7
                "Track X Location",                // 8
                "Track Y Location",                // 9
                "Track Displacement",              // 10
                "Track Max Speed",                 // 11
                "Track Median Speed",              // 12
                "Diffusion Coefficient",           // 13
                "Diffusion Coefficient Ext",       // 14
                "Total Distance",                  // 15
                "Confinement Ratio",               // 16
                "Square Number",                   // 17
                "Label Number"                     // 18
        };

        int nrSpotsInAllTracks = 0;

        try (PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(csvFile, false), StandardCharsets.UTF_8))) {

            // Write header
            writeCsvRow(printWriter, (Object[]) headers);

            // Iterate tracks (visibleOnly == true mimics Python's trackIDs(True))
            final Set<Integer> trackIDs = trackModel.trackIDs(visibleOnly);
            final int squareNumber = -1;
            final int labelNumber = -1;

            for (Integer trackId : trackIDs) {

                // ---- Built-in TrackMate features ----
                Double duration     = featureModel.getTrackFeature(trackId, "TRACK_DURATION");
                Double nrSpots      = featureModel.getTrackFeature(trackId, "NUMBER_SPOTS");
                Double xLoc         = featureModel.getTrackFeature(trackId, "TRACK_X_LOCATION");
                Double yLoc         = featureModel.getTrackFeature(trackId, "TRACK_Y_LOCATION");
                Double maxSpeed     = featureModel.getTrackFeature(trackId, "TRACK_MAX_SPEED");
                Double medSpeed     = featureModel.getTrackFeature(trackId, "TRACK_MEDIAN_SPEED");
                Double nrGaps       = featureModel.getTrackFeature(trackId, "NUMBER_GAPS");
                Double longestGap   = featureModel.getTrackFeature(trackId, "LONGEST_GAP");
                Double displacement = featureModel.getTrackFeature(trackId, "TRACK_DISPLACEMENT");

                // Round/normalize like the Python code
                duration     = roundOr(duration, 3, -1);
                nrSpots      = roundOr(nrSpots, 0, -1);
                xLoc         = roundOr(xLoc, 2, -1);
                yLoc         = roundOr(yLoc, 2, -1);
                maxSpeed     = roundOr(maxSpeed, 2, -1);
                medSpeed     = roundOr(medSpeed, 2, -1);
                nrGaps       = defaultIfNull(nrGaps, -1.0);
                longestGap   = defaultIfNull(longestGap, -1.0);
                displacement = roundOr(displacement, 2, -1);

                // Calculations here
                TrackAttributes ca = calculateTrackAttributes(trackModel, trackId, TIME_INTERVAL);

                int numberOfSpotsInTrack = ca.numberOfSpotsInTracks;
                double diffusionCoeff = ca.diffusionCoeff;
                double diffusionCoeffExt = ca.diffusionCoeffExt;
                double totalDistance = ca.totalDistance;
                double confinementRatio = ca.confinementRatio;
                double displacementCalc = ca.displacement;
                String uniqueKey = recordingName + "-" + trackId;

                // Count spots in this track
                nrSpotsInAllTracks += numberOfSpotsInTrack;

                // Track label or fallback
                String trackLabel = trackModel.name(trackId);
                if (trackLabel == null) {
                    trackLabel = "Track-" + trackId;
                }

                // Write row
                writeCsvRow(printWriter,
                            uniqueKey,                                              // 0
                            recordingName,                                          // 1
                            trackId,                                                // 2
                            trackLabel,                                             // 3
                            asInt(nrSpots),                                         // 4
                            asInt(nrGaps),                                          // 5
                            asInt(longestGap),                                      // 6
                            duration,                                               // 7
                            xLoc,                                                   // 8
                            yLoc,                                                   // 9
                            displacement,                                           // 10
                            maxSpeed,                                               // 11
                            medSpeed,                                               // 12
                            diffusionCoeff,                                         // 13
                            diffusionCoeffExt,                                      // 14
                            totalDistance,                                          // 15
                            confinementRatio,                                       // 16
                            squareNumber,                                           // 17
                            labelNumber);                                           // 18
            }
        }

        return nrSpotsInAllTracks;
        // final int nrSpotsVisible = model.getSpots().getNSpots(true);
        // final int tracksAll = trackModel.nTracks(false);
        // final int tracksFiltered = trackModel.nTracks(true);

        // AppLogger.infof("Visible spots: %d | All tracks: %d | Filtered (visible) tracks: %d", nrSpotsVisible, tracksFiltered, tracksAll);
    }

    // --------- helpers ----------

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
            if (i > 0) sb.append(',');
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
