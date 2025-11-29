/*=============================================================================
 *  Class:        TrackDataExporter.java
 *  Package:      paint.fiji.tracks
 *
 *  PURPOSE:
 *    Exports per-track motion and diffusion statistics derived from a TrackMate
 *    analysis into a structured CSV file. Integrates native TrackMate features
 *    with extended PAINT-calculated track attributes.
 *
 *  DESCRIPTION:
 *    • Extracts core TrackMate track-level features.
 *    • Computes extended PAINT attributes via
 *      {@link paint.fiji.tracks.TrackAttributeCalculations}.
 *    • Builds a schema-compliant {@link tech.tablesaw.api.Table} of
 *      {@link paint.shared.objects.Track} entities.
 *    • Assigns deterministic track IDs and unique recording-based keys.
 *    • Persists all track data to CSV through
 *      {@link paint.shared.io.MainDataInterface#writeSpecificTracksFile(Path, Table)}.
 *
 *  RESPONSIBILITIES:
 *    • Transform TrackMate model output into PAINT Track entities.
 *    • Compute custom diffusion and motion statistics (distance, diffusion
 *      coefficient, confinement ratio, etc.).
 *    • Populate and sort the Tracks table in a deterministic order.
 *    • Export the resulting table to a CSV file.
 *
 *  USAGE EXAMPLE:
 *    int totalSpots = TrackDataExporter.writeTracksCsv(
 *        trackmate,
 *        "ExperimentA",
 *        "Recording1",
 *        Paths.get("tracks.csv"),
 *        true
 *    );
 *
 *  DEPENDENCIES:
 *    – Fiji TrackMate (fiji.plugin.trackmate.*)
 *    – paint.shared.objects.Track
 *    – paint.fiji.tracks.TrackAttributeCalculations
 *    – paint.shared.io.TracksTableIO
 *    – paint.shared.io.MainDataInterface
 *    – tech.tablesaw.api.Table
 *    – paint.shared.utils.PaintLogger
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
=============================================================================*/

package paint.fiji.tracks;

import static paint.shared.constants.PaintStringConstants.*;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import paint.shared.constants.PaintTiming;
import paint.shared.objects.Track;
import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static paint.fiji.tracks.TrackAttributeCalculations.calculateTrackAttributes;
import static paint.shared.io.MainDataInterface.trackListToTable;
import static paint.shared.io.MainDataInterface.writeSpecificTracksFile;

/**
 * Provides functionality to export TrackMate tracks to a CSV file.
 * <p>
 * Combines native TrackMate features with custom PAINT-calculated attributes,
 * producing a structured dataset suitable for downstream statistical analysis.
 * </p>
 */
public final class TrackDataExporter {

    private TrackDataExporter() {
        // Utility class; prevent instantiation
    }

    /**
     * Writes track information from a TrackMate analysis into a CSV file.
     * <p>
     * Includes both TrackMate-calculated and PAINT-derived metrics, such as
     * total distance, diffusion coefficients, and confinement ratio.
     * </p>
     *
     * @param trackmate          TrackMate instance containing the model and features
     * @param experimentName     experiment name for metadata tagging
     * @param recordingName      recording name associated with the tracks
     * @param csvTracksFilePath  destination CSV file
     * @param visibleOnly        if true, exports only visible tracks
     * @return total number of spots across all exported tracks
     */
    public static int writeTracksCsv(final TrackMate trackmate,
                                     final String    experimentName,
                                     final String    recordingName,
                                     final Path      csvTracksFilePath,
                                     final boolean   visibleOnly) {

        // ---------------------------------------------------------------------
        // Step 1 – Extract TrackMate components
        // ---------------------------------------------------------------------
        final Model model               = trackmate.getModel();
        final TrackModel trackModel     = model.getTrackModel();
        final FeatureModel featureModel = model.getFeatureModel();

        // Collect and sort track IDs for deterministic output order
        final Set<Integer>  trackIDsSet = trackModel.trackIDs(visibleOnly);
        final List<Integer> trackIDs    = new ArrayList<>(trackIDsSet);
        Collections.sort(trackIDs);

        final List<Track> tracks = new ArrayList<>();
        int totalSpots = 0;

        // ---------------------------------------------------------------------
        // Step 2 – Process each track
        // ---------------------------------------------------------------------
        for (Integer trackId : trackIDs) {
            TrackAttributes trackAttributes = calculateTrackAttributes(trackModel, trackId, PaintTiming.TIME_INTERVAL);

            Track track = new Track();
            track.setExperimentName(experimentName);
            track.setRecordingName(recordingName);

            // Native TrackMate features
            track.setNumberOfSpots(     asInt(   featureModel.getTrackFeature(trackId, "NUMBER_SPOTS")));
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
            Table tracksTable = trackListToTable(tracks);

            tracksTable = tracksTable.sortOn(
                    RECORDING_NAME,
                    NUMBER_OF_SPOTS,
                    NUMBER_OF_GAPS,
                    LONGEST_GAP,
                    TRACK_DURATION,
                    TRACK_X_LOCATION,
                    TRACK_Y_LOCATION,
                    TRACK_DISPLACEMENT,
                    TRACK_MAX_SPEED,
                    TRACK_MEDIAN_SPEED,
                    DIFFUSION_COEFFICIENT,
                    DIFFUSION_COEFFICIENT_EXT,
                    TOTAL_DISTANCE,
                    CONFINEMENT_RATIO
            );

            // Replace Track Ids and generate unique keys
            IntColumn    newIds       = IntColumn.create(TRACK_ID);
            StringColumn newUniqueKey = StringColumn.create(UNIQUE_KEY);

            for (int i = 0; i < tracksTable.rowCount(); i++) {
                newIds.append(i);
                newUniqueKey.append(recordingName + "-" + i);
            }
            tracksTable.replaceColumn(TRACK_ID, newIds);
            tracksTable.replaceColumn(UNIQUE_KEY, newUniqueKey);

            writeSpecificTracksFile(csvTracksFilePath, tracksTable);

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