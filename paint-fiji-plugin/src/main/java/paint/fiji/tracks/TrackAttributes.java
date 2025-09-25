package paint.fiji.tracks;

/**
 * Immutable data container for motion attributes of a single track.
 * <p>
 * Instances of this class are produced by
 * {@link TrackAttributeCalculations#calculateTrackAttributes}
 * and represent computed properties such as total travel distance,
 * diffusion coefficients, confinement ratio, and net displacement.
 * </p>
 */
public class TrackAttributes {

    /**
     * Number of spots (detections) in the track.
     */
    public final int numberOfSpotsInTracks;

    /**
     * Total path length traveled by the track (in spatial units of the dataset).
     */
    public final double totalDistance;

    /**
     * Diffusion coefficient (based on mean squared displacement relative to first spot).
     */
    public final double diffusionCoeff;

    /**
     * Extended diffusion coefficient (based on step-wise mean squared displacement).
     */
    public final double diffusionCoeffExt;

    /**
     * Confinement ratio = net displacement / total distance traveled.
     */
    public final double confinementRatio;

    /**
     * Net displacement between the first and last spot in the track.
     */
    public final double displacement;

    /**
     * Creates a populated {@code TrackAttributes} object with the specified values.
     *
     * @param numberOfSpotsInTracks number of spots (detections) in the track
     * @param totalDistance         total distance traveled along the track
     * @param diffusionCoeff        diffusion coefficient from MSD relative to the first spot
     * @param diffusionCoeffExt     extended diffusion coefficient from step-wise MSD
     * @param confinementRatio      ratio of displacement to total distance
     * @param displacement          net displacement between first and last spot
     */
    public TrackAttributes(int numberOfSpotsInTracks,
                           double totalDistance,
                           double diffusionCoeff,
                           double diffusionCoeffExt,
                           double confinementRatio,
                           double displacement) {

        this.numberOfSpotsInTracks = numberOfSpotsInTracks;
        this.totalDistance = totalDistance;
        this.diffusionCoeff = diffusionCoeff;
        this.diffusionCoeffExt = diffusionCoeffExt;
        this.confinementRatio = confinementRatio;
        this.displacement = displacement;
    }

    /**
     * Creates a {@code TrackAttributes} instance with all values set to zero.
     * <p>
     * Useful as a fallback for tracks with insufficient spots.
     * </p>
     */
    public TrackAttributes() {
        this(0, 0, 0, 0, 0, 0);
    }
}