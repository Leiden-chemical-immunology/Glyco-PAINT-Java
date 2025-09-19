package paint.fiji.tracks;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class for computing custom attributes of a track in TrackMate.
 * <p>
 * This class extracts spots belonging to a track, sorts them by frame,
 * and calculates motion statistics such as total distance, diffusion coefficients,
 * displacement, and confinement ratio. It is invoked by the CSV writer
 * when exporting track data.
 * </p>
 */
public final class TrackAttributeCalculations {

    private TrackAttributeCalculations() {
        // Utility class; prevent instantiation
    }

    /**
     * Computes motion-related attributes for a track.
     *
     * @param trackModel the TrackMate {@link TrackModel} containing tracks and spots
     * @param trackId    the ID of the track to compute attributes for
     * @param dtSeconds  the time interval between frames (in seconds)
     * @return a {@link TrackAttributes} object containing the computed attributes,
     *         or a default instance if the track has fewer than 2 spots
     */
    public static TrackAttributes calculateTrackAttributes(TrackModel trackModel, int trackId, double dtSeconds) {

        // Collect & sort spots by FRAME
        final List<Spot> spots = new ArrayList<>(trackModel.trackSpots(trackId));
        if (spots.size() < 2) {
            return new TrackAttributes();
        }
        spots.sort(Comparator.comparingInt(s -> (int) Math.round(s.getFeature(Spot.FRAME))));

        double totalDistance = 0.0;
        double cumMsd = 0.0;
        double cumMsdExt = 0.0;
        double confinementRatio = 0.0;
        int numberSpotsInTrack = spots.size();

        // Reference first point (x0, y0)
        final double x0 = get(spots.get(0), Spot.POSITION_X);
        final double y0 = get(spots.get(0), Spot.POSITION_Y);

        final double xLast = get(spots.get(spots.size() - 1), Spot.POSITION_X);
        final double yLast = get(spots.get(spots.size() - 1), Spot.POSITION_Y);

        for (int i = 1; i < spots.size(); i++) {
            final double x1 = get(spots.get(i - 1), Spot.POSITION_X);
            final double y1 = get(spots.get(i - 1), Spot.POSITION_Y);
            final double x2 = get(spots.get(i), Spot.POSITION_X);
            final double y2 = get(spots.get(i), Spot.POSITION_Y);

            final double dx = x2 - x1;
            final double dy = y2 - y1;
            final double stepDist = Math.hypot(dx, dy);

            totalDistance += stepDist;

            // MSD relative to the first point and step-wise MSD
            final double dx0 = x2 - x0;
            final double dy0 = y2 - y0;
            cumMsd += dx0 * dx0 + dy0 * dy0;
            cumMsdExt += dx * dx + dy * dy;
        }

        // Diffusion coefficients
        final int nSteps = spots.size() - 1;
        final double msd = nSteps > 0 ? (cumMsd / nSteps) : 0.0;
        final double msdExt = nSteps > 0 ? (cumMsdExt / nSteps) : 0.0;

        // In 2D: D = MSD / (4 * dt)
        final double diffusionCoeff = round2(dtSeconds > 0 ? (msd / (4.0 * dtSeconds)) : 0.0);
        final double diffusionCoeffExt = round2(dtSeconds > 0 ? (msdExt / (4.0 * dtSeconds)) : 0.0);

        // Displacement between first and last spot
        double displacement = Math.hypot(xLast - x0, yLast - y0);

        // Confinement ratio = displacement / total distance
        if (totalDistance != 0) {
            confinementRatio = displacement / totalDistance;
        }

        return new TrackAttributes(
                numberSpotsInTrack,
                totalDistance,
                diffusionCoeff,
                diffusionCoeffExt,
                confinementRatio,
                displacement
        );
    }

    // ---- helpers ----

    /**
     * Safely extracts a numerical feature value from a {@link Spot}.
     *
     * @param s          the spot
     * @param featureKey the feature key (e.g., {@link Spot#POSITION_X})
     * @return the feature value, or 0.0 if missing
     */
    private static double get(Spot s, String featureKey) {
        final Double v = s.getFeature(featureKey);
        return v == null ? 0.0 : v;
    }

    /**
     * Rounds a number to two decimal places.
     *
     * @param v the value to round
     * @return the rounded value
     */
    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}