package paint.fiji.tracks;

public class TrackAttributes {
    public final int numberOfSpotsInTracks ;
    public final double totalDistance;
    public final double diffusionCoeff;
    public final double diffusionCoeffExt;
    public final double confinementRatio;
    public final double displacement;

    /**
     *
     * @param numberOfSpotsInTracks
     * @param totalDistance
     * @param diffusionCoeff
     * @param diffusionCoeffExt
     * @param confinementRatio
     * @param displacement
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

    public TrackAttributes() {
        this(0, 0, 0, 0, 0, 0);
    }
}
