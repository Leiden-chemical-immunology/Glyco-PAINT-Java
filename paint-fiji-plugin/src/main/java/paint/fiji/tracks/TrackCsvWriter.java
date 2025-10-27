package paint.fiji.tracks;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import paint.shared.io.TrackTableIO;
import paint.shared.objects.Track;
import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static paint.fiji.tracks.TrackAttributeCalculations.calculateTrackAttributes;
import static paint.shared.constants.PaintConstants.TIME_INTERVAL;

/**
 * The TrackCsvWriter class is responsible for writing track-based information
 * into a CSV file. It processes track data from a TrackMate instance, extracts
 * relevant attributes, calculates additional custom features, and organizes the
 * data in a tabular format before exporting it as a CSV file.
 *
 * This class focuses on handling track data for scientific or analytical purposes,
 * such as the motion properties of detected objects.
 */
public class TrackCsvWriter {

    /**
     * Writes track information from a TrackMate analysis into a CSV file.
     *
     * @param trackmate A TrackMate instance containing the model with track data to be written.
     * @param experimentName The name of the experiment associated with the tracks.
     * @param recordingName The name of the recording associated with the tracks.
     * @param csvFile The destination file where the CSV will be written.
     * @param visibleOnly If true, only visible tracks will be included in the CSV.
     * @return The total number of spots across all tracks.
     * @throws IOException If an error occurs during the file writing process.
     */
    public static int writeTracksCsv(final TrackMate trackmate,
                                     final String experimentName,
                                     final String recordingName,
                                     final File csvFile,
                                     final boolean visibleOnly) throws IOException {

        final Model model               = trackmate.getModel();
        final TrackModel trackModel     = model.getTrackModel();
        final FeatureModel featureModel = model.getFeatureModel();

        // Collect and sort track IDs for deterministic order
        final Set<Integer> trackIDsSet = trackModel.trackIDs(visibleOnly);
        final List<Integer> trackIDs = new ArrayList<>(trackIDsSet);
        Collections.sort(trackIDs);

        final List<Track> tracks = new ArrayList<>();
        int totalSpots = 0;
        int newTrackId = 0;

        for (Integer trackId : trackIDs) {
            TrackAttributes trackAttributes = calculateTrackAttributes(trackModel, trackId, TIME_INTERVAL);

            Track track = new Track();
            track.setExperimentName(experimentName);
            track.setRecordingName(recordingName);

            track.setNumberOfSpots(     asInt(    featureModel.getTrackFeature(trackId, "NUMBER_SPOTS")));
            track.setNumberOfGaps(      asInt(    featureModel.getTrackFeature(trackId, "NUMBER_GAPS")));
            track.setLongestGap(        asInt(    featureModel.getTrackFeature(trackId, "LONGEST_GAP")));
            track.setTrackDuration(     roundOr(  featureModel.getTrackFeature(trackId, "TRACK_DURATION"),     3, -1));
            track.setTrackXLocation(    roundOr(  featureModel.getTrackFeature(trackId, "TRACK_X_LOCATION"),   2, -1));
            track.setTrackYLocation(    roundOr(  featureModel.getTrackFeature(trackId, "TRACK_Y_LOCATION"),   2, -1));
            track.setTrackDisplacement( roundOr(  featureModel.getTrackFeature(trackId, "TRACK_DISPLACEMENT"), 2, -1));
            track.setTrackMaxSpeed(     roundOr(  featureModel.getTrackFeature(trackId, "TRACK_MAX_SPEED"),    2, -1));
            track.setTrackMedianSpeed(  roundOr(  featureModel.getTrackFeature(trackId, "TRACK_MEDIAN_SPEED"), 2, -1));

            // custom computed attributes
            track.setDiffusionCoefficient(    roundOr(trackAttributes.diffusionCoeff,    2, -1));
            track.setDiffusionCoefficientExt( roundOr(trackAttributes.diffusionCoeffExt, 2, -1));
            track.setTotalDistance(           roundOr(trackAttributes.totalDistance,     2, -1));
            track.setConfinementRatio(        roundOr(trackAttributes.confinementRatio,  2, -1));
            track.setSquareNumber(-1);
            track.setLabelNumber(-1);

            totalSpots += trackAttributes.numberOfSpotsInTracks;
            tracks.add(track);
        }

        try {
            TrackTableIO trackTableIO = new paint.shared.io.TrackTableIO();
            Table tracksTable = trackTableIO.toTable(tracks);
            tracksTable = tracksTable.sortOn("Recording Name",
                                             "Number of Spots",
                                             "Number of Gaps",
                                             "Longest Gap",
                                             "Track Duration",
                                             "Track X Location",
                                             "Track Y Location",
                                             "Track Displacement",
                                             "Track Max Speed",
                                             "Track Median Speed",
                                             "Diffusion Coefficient",
                                             "Diffusion Coefficient Ext",
                                             "Total Distance",
                                             "Confinement Ratio");


            IntColumn    newIds       = IntColumn.create(    "Track Id");
            StringColumn newUniqueKey = StringColumn.create( "Unique Key");

            for (int i = 0; i < tracksTable.rowCount(); i++) {
                newIds.append(i);
                newUniqueKey.append(recordingName + "-" + i);
            }
            tracksTable.replaceColumn("Track Id", newIds);
            tracksTable.replaceColumn("Unique Key", newUniqueKey);

            trackTableIO.writeCsv(tracksTable, csvFile.toPath());
        }
        catch (Exception e) {
            PaintLogger.errorf("Failed writing track CSV: %s", e.getMessage());
            e.printStackTrace();
        }

        return totalSpots;
    }

    /**
     * Rounds a given double value to the specified number of decimal places.
     * If the input value is null, the method returns {@code Double.NaN}.
     *
     * @param v the double value to be rounded; can be null
     * @param places the number of decimal places to round the value to
     * @return the rounded value, or {@code Double.NaN} if the input value is null
     */
    private static double roundTo(Double v, int places) {
        if (v == null) {
            return Double.NaN;
        }
        double scale = Math.pow(10, places);
        return Math.round(v * scale) / scale;
    }

    /**
     * Rounds a given double value to the specified number of decimal places.
     * If the input value is null, it returns a default value specified by {@code ifNull}.
     *
     * @param v the double value to be rounded; can be null
     * @param places the number of decimal places to round the value to
     * @param ifNull the value to return if {@code v} is null
     * @return the rounded value if {@code v} is not null, or {@code ifNull} if {@code v} is null
     */
    private static Double roundOr(Double v, int places, double ifNull) {
        return v == null ? ifNull : roundTo(v, places);
    }

    /**
     * Converts a {@code Double} value into an {@code int} value.
     * If the input is {@code null} or {@code NaN}, the method returns -1.
     * Otherwise, it rounds the value to the nearest integer.
     *
     * @param v the {@code Double} value to be converted; can be {@code null} or {@code NaN}
     * @return the converted integer value, or -1 if {@code v} is {@code null} or {@code NaN}
     */
    private static int asInt(Double v) {
        if (v == null || v.isNaN()) {
            return -1;
        }
        return (int) Math.round(v);
    }
}