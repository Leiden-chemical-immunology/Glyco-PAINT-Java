package paint.fiji.tracks;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import paint.shared.io.TrackTableIO;
import paint.shared.objects.Track;
import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static paint.fiji.tracks.TrackAttributeCalculations.calculateTrackAttributes;
import static paint.shared.constants.PaintConstants.TIME_INTERVAL;

public class TrackCsvWriter {

    public static int writeTracksCsv(final TrackMate trackmate,
                                     final String recordingName,
                                     final File csvFile,
                                     final boolean visibleOnly) throws IOException {

        final Model model = trackmate.getModel();
        final TrackModel trackModel = model.getTrackModel();
        final FeatureModel featureModel = model.getFeatureModel();

        final Set<Integer> trackIDs = trackModel.trackIDs(visibleOnly);
        final List<Track> tracks = new ArrayList<>();
        int totalSpots = 0;

        for (Integer trackId : trackIDs) {

            TrackAttributes ca = calculateTrackAttributes(trackModel, trackId, TIME_INTERVAL);

            Track t = new Track();
            t.setUniqueKey(recordingName + "-" + trackId);
            t.setRecordingName(recordingName);
            t.setTrackId(trackId);
            t.setTrackLabel(trackModel.name(trackId) != null
                                    ? trackModel.name(trackId)
                                    : "Track-" + trackId);

            t.setNumberOfSpots(asInt(featureModel.getTrackFeature(trackId, "NUMBER_SPOTS")));
            t.setNumberOfGaps(asInt(featureModel.getTrackFeature(trackId, "NUMBER_GAPS")));
            t.setLongestGap(asInt(featureModel.getTrackFeature(trackId, "LONGEST_GAP")));
            t.setTrackDuration(roundOr(featureModel.getTrackFeature(trackId, "TRACK_DURATION"), 3, -1));
            t.setTrackXLocation(roundOr(featureModel.getTrackFeature(trackId, "TRACK_X_LOCATION"), 2, -1));
            t.setTrackYLocation(roundOr(featureModel.getTrackFeature(trackId, "TRACK_Y_LOCATION"), 2, -1));
            t.setTrackDisplacement(roundOr(featureModel.getTrackFeature(trackId, "TRACK_DISPLACEMENT"), 2, -1));
            t.setTrackMaxSpeed(roundOr(featureModel.getTrackFeature(trackId, "TRACK_MAX_SPEED"), 2, -1));
            t.setTrackMedianSpeed(roundOr(featureModel.getTrackFeature(trackId, "TRACK_MEDIAN_SPEED"), 2, -1));

            // custom calculated attributes
            t.setDiffusionCoefficient(ca.diffusionCoeff);
            t.setDiffusionCoefficientExt(ca.diffusionCoeffExt);
            t.setTotalDistance(ca.totalDistance);
            t.setConfinementRatio(ca.confinementRatio);
            t.setSquareNumber(-1);
            t.setLabelNumber(-1);

            totalSpots += ca.numberOfSpotsInTracks;
            tracks.add(t);
        }


        // delegate CSV writing to your schema-aware IO

        try {
            // TrackTableIO trackTableIO = new TrackTableIO();
            TrackTableIO trackTableIO = new paint.shared.io.TrackTableIO();
            Table tracksTable = trackTableIO.toTable(tracks);
            trackTableIO.writeCsv(tracksTable, csvFile.toPath());
        }
        catch (Exception e) {
            PaintLogger.errorf("Whoopsie");
            e.printStackTrace();
        }

        return totalSpots;
    }

    private static double roundTo(Double v, int places) {
        if (v == null) return Double.NaN;
        double scale = Math.pow(10, places);
        return Math.round(v * scale) / scale;
    }

    private static Double roundOr(Double v, int places, double ifNull) {
        return v == null ? ifNull : roundTo(v, places);
    }

    private static int asInt(Double v) {
        if (v == null || v.isNaN()) return -1;
        return (int) Math.round(v);
    }
}