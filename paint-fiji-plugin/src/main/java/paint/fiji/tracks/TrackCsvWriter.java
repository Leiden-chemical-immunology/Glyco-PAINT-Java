package paint.fiji.tracks;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import paint.shared.io.TrackTableIO;
import paint.shared.objects.Track;
import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.IntColumn;
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

            TrackAttributes trackAttributes = calculateTrackAttributes(trackModel, trackId, TIME_INTERVAL);

            Track track = new Track();
            track.setUniqueKey(recordingName + "-" + trackId);
            track.setRecordingName(recordingName);
            track.setTrackId(trackId);
            track.setTrackLabel(trackModel.name(trackId) != null
                                    ? trackModel.name(trackId)
                                    : "Track-" + trackId);

            // @formatter:off
            track.setNumberOfSpots(       asInt(featureModel.getTrackFeature(trackId,  "NUMBER_SPOTS")));
            track.setNumberOfGaps(        asInt(featureModel.getTrackFeature(trackId,  "NUMBER_GAPS")));
            track.setLongestGap(          asInt(featureModel.getTrackFeature(trackId,  "LONGEST_GAP")));
            track.setTrackDuration(     roundOr(featureModel.getTrackFeature(trackId,  "TRACK_DURATION"), 3, -1));
            track.setTrackXLocation(    roundOr(featureModel.getTrackFeature(trackId,  "TRACK_X_LOCATION"), 2, -1));
            track.setTrackYLocation(    roundOr(featureModel.getTrackFeature(trackId,  "TRACK_Y_LOCATION"), 2, -1));
            track.setTrackDisplacement( roundOr(featureModel.getTrackFeature(trackId,  "TRACK_DISPLACEMENT"), 2, -1));
            track.setTrackMaxSpeed(      roundOr(featureModel.getTrackFeature(trackId, "TRACK_MAX_SPEED"), 2, -1));
            track.setTrackMedianSpeed(   roundOr(featureModel.getTrackFeature(trackId, "TRACK_MEDIAN_SPEED"), 2, -1));
            // @formatter:on

            // custom calculated attributes
            track.setDiffusionCoefficient(trackAttributes.diffusionCoeff);
            track.setDiffusionCoefficientExt(trackAttributes.diffusionCoeffExt);
            track.setTotalDistance(trackAttributes.totalDistance);
            track.setConfinementRatio(trackAttributes.confinementRatio);
            track.setSquareNumber(-1);
            track.setLabelNumber(-1);

            totalSpots += trackAttributes.numberOfSpotsInTracks;
            tracks.add(track);
        }

        try {
            // TrackTableIO trackTableIO = new TrackTableIO();
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


            IntColumn newIds = IntColumn.create("Track ID");
            for (int i = 0; i < tracksTable.rowCount(); i++) {
                newIds.append(i);
            }
            tracksTable.replaceColumn("Track ID", newIds);

            trackTableIO.writeCsv(tracksTable, csvFile.toPath());
        }
        catch (Exception e) {
            PaintLogger.errorf("Whoopsie");
            e.printStackTrace();
        }

        return totalSpots;
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
        if (v == null || v.isNaN()) return -1;
        return (int) Math.round(v);
    }
}