/******************************************************************************
 *  Class:        TrackAttributeCalculations.java
 *  Package:      paint.fiji.tracks
 *
 *  PURPOSE:
 *    Utility class for calculating motion-related attributes for tracks
 *    in a TrackMate dataset. This class analyses trajectories of spots
 *    in tracks by computing metrics such as total travel distance,
 *    diffusion coefficients, confinement ratio, and net displacement.
 *
 *  DESCRIPTION:
 *    Includes methods to:
 *      • Compute key track attributes from the track model, track ID and time delta.
 *      • Safely retrieve feature values from individual spots.
 *
 *  RESPONSIBILITIES:
 *    • Provide a static calculation method for TrackAttributes based on a track.
 *    • Prevent instantiation (utility class).
 *
 *  USAGE EXAMPLE:
 *    TrackAttributes attrs = TrackAttributeCalculations
 *                              .calculateTrackAttributes(trackModel, trackId, dtSeconds);
 *
 *  DEPENDENCIES:
 *    - fiji.plugin.trackmate.TrackModel
 *    - fiji.plugin.trackmate.Spot
 *    - paint.shared.utils.Miscellaneous (for rounding)
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-27
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.fiji.tracks;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static paint.shared.utils.Miscellaneous.round;


public final class TrackAttributeCalculations {

    private TrackAttributeCalculations() {
        // Utility class; prevent instantiation
    }

    /**
     * Computes the motion attributes of a single track based on the provided track model,
     * track identifier, and time delta between frames.
     * Attrubutes calculated are:
     *     numberSpotsInTrack,
     *     totalDistance
     *     diffusionCoeff
     *     diffusionCoeffExt
     *     confinementRatio
     *     displacement
     *
     * @param trackModel the track model containing track data and associated spots
     * @param trackId the unique identifier of the track to analyze
     * @param dtSeconds the time interval between consecutive frames, in seconds
     * @return a {@code TrackAttributes} object containing the computed attributes
     *         such as total distance, diffusion coefficients, confinement ratio, and displacement
     */
    public static TrackAttributes calculateTrackAttributes(TrackModel trackModel, int trackId, double dtSeconds) {

        // Collect & sort spots by FRAME
        final List<Spot> spots = new ArrayList<>(trackModel.trackSpots(trackId));
        if (spots.size() < 2) {
            return new TrackAttributes();
        }
        spots.sort(Comparator.comparingInt(s -> (int) Math.round(s.getFeature(Spot.FRAME))));

        // @formatter:off
        double totalDistance       = 0.0;
        double cumMsd              = 0.0;
        double cumMsdExt           = 0.0;
        double diffusionCoeff      = Double.NaN;
        double diffusionCoeffExt   = Double.NaN;
        double confinementRatio    = Double.NaN;
        int    numberSpotsInTrack  = spots.size();
        // @formatter:on

        // Reference first point (x0, y0)
        final double x0 = get(spots.get(0), Spot.POSITION_X);
        final double y0 = get(spots.get(0), Spot.POSITION_Y);

        // Reference last point (xLast, yLast)
        final double xLast = get(spots.get(spots.size() - 1), Spot.POSITION_X);
        final double yLast = get(spots.get(spots.size() - 1), Spot.POSITION_Y);

        for (int i = 1; i < spots.size(); i++) {

            // The previous point
            final double x1 = get(spots.get(i - 1), Spot.POSITION_X);
            final double y1 = get(spots.get(i - 1), Spot.POSITION_Y);

            // The current point
            final double x2 = get(spots.get(i), Spot.POSITION_X);
            final double y2 = get(spots.get(i), Spot.POSITION_Y);

            // For MSD Ext, take the distance to the previous point
            final double dx = x2 - x1;
            final double dy = y2 - y1;

            // For MSD, take the distance to the first point
            final double dx0   = x2 - x0;
            final double dy0   = y2 - y0;

            // Calculate the cumulative values
            cumMsd            += dx0 * dx0 + dy0 * dy0;
            cumMsdExt         += dx * dx + dy * dy;

            // Also calculate the increase in total distance
            totalDistance += Math.hypot(dx, dy);
        }

        // Diffusion coefficients (remain NaN if insufficient data or dtSeconds <= 0)
        final int nSteps = spots.size() - 1;

        if (nSteps > 0 && dtSeconds > 0.0) {
            final double msd    = cumMsd / nSteps;
            final double msdExt = cumMsdExt / nSteps;

            diffusionCoeff    = round(msd / (4.0 * dtSeconds), 2);
            diffusionCoeffExt = round(msdExt / (4.0 * dtSeconds), 2);
        }

        // Calculate the displacement between the first and last spot
        double displacement = Math.hypot(xLast - x0, yLast - y0);

        // Calculate the Confinement ratio = displacement / total distance
        if (totalDistance > 0.0) {
            confinementRatio = round(displacement / totalDistance, 2);
        }

        // If totalDistance is zero, keep it as NaN for downstream consistency
        if (totalDistance == 0.0) {
            totalDistance = Double.NaN;
        }

        return new TrackAttributes(numberSpotsInTrack,
                                   totalDistance,
                                   diffusionCoeff,
                                   diffusionCoeffExt,
                                   confinementRatio,
                                   displacement
        );
    }

    // ---- helpers ----

    /**
     * Retrieves the value of a specific feature for a given spot. If the feature value is not present,
     * the method returns a default value of 0.0.
     *
     * @param s the {@code Spot} object from which the feature value is obtained
     * @param featureKey the key representing the feature to retrieve
     * @return the value associated with the specified feature key, or 0.0 if the feature value is not available
     */
    private static double get(Spot s, String featureKey) {
        final Double v = s.getFeature(featureKey);
        return v == null ? 0.0 : v;
    }

}