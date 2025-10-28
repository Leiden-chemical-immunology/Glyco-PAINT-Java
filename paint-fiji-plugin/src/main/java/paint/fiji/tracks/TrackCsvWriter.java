/******************************************************************************
 *  Class:        TrackCsvWriter.java
 *  Package:      paint.fiji.tracks
 *
 *  PURPOSE:
 *    Writes per-track motion and diffusion statistics derived from a TrackMate
 *    analysis into a structured CSV file. Integrates both native TrackMate
 *    features and custom PAINT-calculated attributes.
 *
 *  DESCRIPTION:
 *    • Extracts core TrackMate features and custom diffusion metrics.
 *    • Constructs a tabular dataset of per-track attributes.
 *    • Assigns deterministic Track IDs and unique recording-based keys.
 *    • Exports all track data to CSV via {@link paint.shared.io.TrackTableIO}.
 *
 *  RESPONSIBILITIES:
 *    • Transform TrackMate results into table form.
 *    • Compute extended track attributes using
 *      {@link paint.fiji.tracks.TrackAttributeCalculations}.
 *    • Serialize and persist results to disk as CSV.
 *
 *  USAGE EXAMPLE:
 *    int totalSpots = TrackCsvWriter.writeTracksCsv(
 *        trackmate, "ExperimentA", "Recording1",
 *        new File("tracks.csv"), true);
 *
 *  DEPENDENCIES:
 *    – fiji.plugin.trackmate.*
 *    – paint.shared.io.TrackTableIO
 *    – paint.shared.objects.Track
 *    – tech.tablesaw.api.*
 *    – paint.shared.utils.PaintLogger
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

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
 * Provides functionality to export TrackMate tracks to a CSV file.
 * <p>
 * Combines native TrackMate features with custom PAINT-calculated attributes,
 * producing a structured dataset suitable for downstream statistical analysis.
 * </p>
 */
public final class TrackCsvWriter {

    private TrackCsvWriter() {
        // Utility class; prevent instantiation
    }

    /**
     * Writes track information from a TrackMate analysis into a CSV file.
     * <p>
     * Includes both TrackMate-calculated and PAINT-derived metrics, such as
     * total distance, diffusion coefficients, and confinement ratio.
     * </p>
     *
     * @param trackmate       TrackMate instance containing the model and features
     * @param experimentName  experiment name for metadata tagging
     * @param recordingName   recording name associated with the tracks
     * @param csvFile         destination CSV file
     * @param visibleOnly     if true, exports only visible tracks
     * @return total number of spots across all exported tracks
     * @throws IOException if an error occurs during file writing
     */
    public static int writeTracksCsv(final TrackMate trackmate,
                                     final String experimentName,
                                     final String recordingName,
                                     final File csvFile,
                                     final boolean visibleOnly) throws IOException {

        // ---------------------------------------------------------------------
        // Step 1 – Extract TrackMate components
        // ---------------------------------------------------------------------
        final Model model               = trackmate.getModel();
        final TrackModel trackModel     = model.getTrackModel();
        final FeatureModel featureModel = model.getFeatureModel();

        // Collect and sort track IDs for deterministic output order
        final Set<Integer> trackIDsSet = trackModel.trackIDs(visibleOnly);
        final List<Integer> trackIDs = new ArrayList<>(trackIDsSet);
        Collections.sort(trackIDs);

        final List<Track> tracks = new ArrayList<>();
        int totalSpots = 0;

        // ---------------------------------------------------------------------
        // Step 2 – Process each track
        // ---------------------------------------------------------------------
        for (Integer trackId : trackIDs) {
            TrackAttributes trackAttributes = calculateTrackAttributes(trackModel, trackId, TIME_INTERVAL);

            Track track = new Track();
            track.setExperimentName(experimentName);
            track.setRecordingName(recordingName);

            // Native TrackMate features
            track.setNumberOfSpots(     asInt(    featureModel.getTrackFeature(trackId, "NUMBER_SPOTS")));
            track.setNumberOfGaps(      asInt(   featureModel.getTrackFeature(trackId, "NUMBER_GAPS")));
            track.setLongestGap(        asInt(   featureModel.getTrackFeature(trackId, "LONGEST_GAP")));
            track.setTrackDuration(     roundOr( featureModel.getTrackFeature(trackId, "TRACK_DURATION"),     3, -1));
            track.setTrackXLocation(    roundOr( featureModel.getTrackFeature(trackId, "TRACK_X_LOCATION"),   2, -1));
            track.setTrackYLocation(    roundOr( featureModel.getTrackFeature(trackId, "TRACK_Y_LOCATION"),   2, -1));
            track.setTrackDisplacement( roundOr( featureModel.getTrackFeature(trackId, "TRACK_DISPLACEMENT"), 2, -1));
            track.setTrackMaxSpeed(     roundOr( featureModel.getTrackFeature(trackId, "TRACK_MAX_SPEED"),    2, -1));
            track.setTrackMedianSpeed(  roundOr( featureModel.getTrackFeature(trackId, "TRACK_MEDIAN_SPEED"), 2, -1));

            // Custom PAINT-calculated attributes
            track.setDiffusionCoefficient(    roundOr(trackAttributes.diffusionCoeff,    2, -1));
            track.setDiffusionCoefficientExt( roundOr(trackAttributes.diffusionCoeffExt, 2, -1));
            track.setTotalDistance(           roundOr(trackAttributes.totalDistance,     2, -1));
            track.setConfinementRatio(        roundOr(trackAttributes.confinementRatio,  2, -1));
            track.setSquareNumber(-1);
            track.setLabelNumber(-1);

            totalSpots += trackAttributes.numberOfSpotsInTracks;
            tracks.add(track);
        }

        // ---------------------------------------------------------------------
        // Step 3 – Build and export table
        // ---------------------------------------------------------------------
        try {
            TrackTableIO trackTableIO = new TrackTableIO();
            Table tracksTable = trackTableIO.toTable(tracks);

            tracksTable = tracksTable.sortOn(
                    "Recording Name",
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
                    "Confinement Ratio"
            );

            // Replace Track Ids and generate unique keys
            IntColumn newIds       = IntColumn.create("Track Id");
            StringColumn newUniqueKey = StringColumn.create("Unique Key");

            for (int i = 0; i < tracksTable.rowCount(); i++) {
                newIds.append(i);
                newUniqueKey.append(recordingName + "-" + i);
            }
            tracksTable.replaceColumn("Track Id", newIds);
            tracksTable.replaceColumn("Unique Key", newUniqueKey);

            trackTableIO.writeCsv(tracksTable, csvFile.toPath());
        } catch (Exception e) {
            PaintLogger.errorf("Failed writing track CSV: %s", e.getMessage());
            e.printStackTrace();
        }

        return totalSpots;
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Rounds a double to the specified number of decimal places.
     *
     * @param v      input value (nullable)
     * @param places number of decimal places
     * @return rounded value, or {@code Double.NaN} if null
     */
    private static double roundTo(Double v, int places) {
        if (v == null) {
            return Double.NaN;
        }
        double scale = Math.pow(10, places);
        return Math.round(v * scale) / scale;
    }

    /**
     * Rounds a double or returns a fallback if {@code v} is null.
     *
     * @param v       input value (nullable)
     * @param places  decimal precision
     * @param ifNull  fallback value when input is null
     * @return rounded value or fallback
     */
    private static Double roundOr(Double v, int places, double ifNull) {
        return v == null ? ifNull : roundTo(v, places);
    }

    /**
     * Converts a Double to an int, returning -1 for null or NaN.
     *
     * @param v Double value
     * @return integer value or -1 for invalid input
     */
    private static int asInt(Double v) {
        if (v == null || v.isNaN()) {
            return -1;
        }
        return (int) Math.round(v);
    }
}