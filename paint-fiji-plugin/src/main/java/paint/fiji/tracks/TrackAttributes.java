/******************************************************************************
 *  Class:        TrackAttributes.java
 *  Package:      paint.fiji.tracks
 *
 *  PURPOSE:
 *    Immutable data container representing the motion-related attributes
 *    of a single TrackMate track.
 *
 *  DESCRIPTION:
 *    • Stores core descriptive statistics derived from a particle trajectory,
 *      such as distance, diffusion coefficients, confinement ratio, and
 *      displacement.
 *    • Used primarily as the return type from
 *      {@link paint.fiji.tracks.TrackAttributeCalculations#calculateTrackAttributes}.
 *
 *  RESPONSIBILITIES:
 *    • Encapsulate track-level quantitative metrics.
 *    • Provide a consistent structure for motion statistics.
 *    • Maintain immutability for safe multithreaded usage.
 *
 *  USAGE EXAMPLE:
 *    TrackAttributes attrs = new TrackAttributes(
 *        34, 125.7, 0.042, 0.038, 0.89, 111.3);
 *
 *  DEPENDENCIES:
 *    – None (pure data object)
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

/**
 * Immutable representation of a single track’s quantitative attributes.
 * <p>
 * Each instance encapsulates per-track motion descriptors such as total
 * distance traveled, diffusion coefficients, confinement ratio, and
 * net displacement. Primarily used for summarizing TrackMate analysis
 * results in higher-level PAINT workflows.
 * </p>
 */
public final class TrackAttributes {
    public final int    numberOfSpotsInTracks; // Number of spots (detections) in the track.
    public final double totalDistance;         // Total path length traveled by the track (in spatial units of the dataset).
    public final double diffusionCoeff;        // Diffusion coefficient (based on mean squared displacement relative to first spot).
    public final double diffusionCoeffExt;     // Extended diffusion coefficient (based on step-wise mean squared displacement).
    public final double confinementRatio;      // Confinement ratio = net displacement / total distance traveled.
    public final double displacement;          // Net displacement between the first and last spot in the track.

    /**
     * Constructs a {@code TrackAttributes} instance with all motion-related metrics.
     *
     * @param numberOfSpotsInTracks the number of spots (or detections) in the track
     * @param totalDistance the total distance traveled by the track
     * @param diffusionCoeff the diffusion coefficient calculated from the mean squared displacement relative to the first spot
     * @param diffusionCoeffExt the extended diffusion coefficient based on step-wise mean squared displacement
     * @param confinementRatio the ratio of net displacement to total distance traveled
     * @param displacement the net displacement between the first and last spot in the track
     */
    public TrackAttributes(int numberOfSpotsInTracks,
                           double totalDistance,
                           double diffusionCoeff,
                           double diffusionCoeffExt,
                           double confinementRatio,
                           double displacement) {
        this.numberOfSpotsInTracks = numberOfSpotsInTracks;
        this.totalDistance         = totalDistance;
        this.diffusionCoeff        = diffusionCoeff;
        this.diffusionCoeffExt     = diffusionCoeffExt;
        this.confinementRatio      = confinementRatio;
        this.displacement          = displacement;
    }

    /**
     * Constructs a default {@code TrackAttributes} instance with:
     * <ul>
     *   <li>{@code numberOfSpotsInTracks = 0}</li>
     *   <li>All numeric fields initialized to {@code Double.NaN}</li>
     * </ul>
     * <p>
     * Useful for representing tracks with insufficient data points or
     * failed computations.
     * </p>
     */
    public TrackAttributes() {
        this(0,
             Double.NaN,
             Double.NaN,
             Double.NaN,
             Double.NaN,
             Double.NaN);
    }
}