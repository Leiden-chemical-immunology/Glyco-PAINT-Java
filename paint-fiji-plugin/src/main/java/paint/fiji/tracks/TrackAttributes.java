package paint.fiji.tracks;

/**
 * Represents the attributes of a track in a dataset, such as the number of spots,
 * total distance traveled, diffusion coefficients, confinement ratio, and net displacement.
 * This class encapsulates descriptive statistics for analyzing the movement or behavior
 * of objects over time.
 */
public class TrackAttributes {

    public final int numberOfSpotsInTracks; // Number of spots (detections) in the track.
    public final double totalDistance;      // Total path length traveled by the track (in spatial units of the dataset).
    public final double diffusionCoeff;     // Diffusion coefficient (based on mean squared displacement relative to first spot).
    public final double diffusionCoeffExt;  // Extended diffusion coefficient (based on step-wise mean squared displacement).
    public final double confinementRatio;   // Confinement ratio = net displacement / total distance traveled.
    public final double displacement;       // Net displacement between the first and last spot in the track.

    /**
     * Constructs a {@code TrackAttributes} instance representing various properties
     * of a track, such as the number of spots, total distance traveled, diffusion
     * coefficients, confinement ratio, and net displacement.
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

        // @formatter:off
        this.numberOfSpotsInTracks = numberOfSpotsInTracks;
        this.totalDistance         = totalDistance;
        this.diffusionCoeff        = diffusionCoeff;
        this.diffusionCoeffExt     = diffusionCoeffExt;
        this.confinementRatio      = confinementRatio;
        this.displacement          = displacement;
        // @formatter:on
    }

    /**
     * Constructs a default {@code TrackAttributes} instance with all fields uninitialized
     * or set to default values. This constructor initializes:
     * <ul>
     * - {@code numberOfSpotsInTracks} to {@code 0}.
     * - {@code totalDistance}, {@code diffusionCoeff}, {@code diffusionCoeffExt},
     *   {@code confinementRatio}, and {@code displacement} to {@code Double.NaN}.
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