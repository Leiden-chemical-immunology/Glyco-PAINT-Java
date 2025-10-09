package paint.fiji.trackmate;   //TODO

/**
 * Configuration parameters for running the TrackMate plugin in Fiji.
 * <p>
 * This class serves as a container for the various settings used to control
 * spot detection, linking, gap closing, and track analysis. The fields
 * correspond closely to the parameters available in the TrackMate GUI.
 * </p>
 */
public class TrackMateConfig {

    /**
     * Maximum number of frames allowed for closing a gap between spots.
     */
    public int maxFrameGap;

    /**
     * Maximum distance allowed for linking spots between consecutive frames.
     */
    public double linkingMaxDistance;

    /**
     * Maximum distance allowed for closing a gap between non-consecutive frames.
     */
    public double gapClosingMaxDistance;

    /**
     * Cost factor used for alternative linking heuristics.
     */
    public double alternativeLinkingCostFactor;

    /**
     * Maximum distance allowed for splitting a track into branches.
     */
    public double splittingMaxDistance;

    /**
     * Whether to allow closing of gaps in tracks.
     */
    public boolean allowGapClosing;

    /**
     * Whether to allow merging of tracks.
     */
    public boolean allowTrackMerging;

    /**
     * Whether to allow splitting of tracks.
     */
    public boolean allowTrackSplitting;

    /**
     * Maximum distance allowed for merging two tracks.
     */
    public double mergingMaxDistance;

    /**
     * Whether to enable subpixel localization during spot detection.
     */
    public boolean doSubpixelLocalization;

    /**
     * Detection radius for spot finding (in pixels).
     */
    public double radius;

    /**
     * Target channel in the image for analysis (1-based index).
     */
    public int targetChannel;

    /**
     * Whether to apply median filtering to the image before detection.
     */
    public boolean doMedianFiltering;

    /**
     * Minimum number of spots required in a track.
     */
    public int minNumberOfSpots;

    /**
     * Maximum number of spots allowed in a single image.
     */
    public int maxNrOfSpotsInImage;

    /**
     * Method used for coloring tracks (e.g., by displacement, speed).
     */
    public String trackColouring;

    /**
     * Returns a formatted string representation of all TrackMate configuration parameters.
     *
     * @return human-readable configuration string
     */
    @Override
    public String toString() {
        return "\n\n" +
                "TrackMate Parameters\n" +
                String.format("TARGET_CHANNEL:                  %d\n", targetChannel) +
                String.format("RADIUS:                          %.1f\n", radius) +
                String.format("DO_SUBPIXEL_LOCALIZATION:        %b\n", doSubpixelLocalization) +
                String.format("DO_MEDIAN_FILTERING:             %b\n", doMedianFiltering) +
                String.format("LINKING_MAX_DISTANCE:            %.1f\n", linkingMaxDistance) +
                String.format("ALTERNATIVE_LINKING_COST_FACTOR: %.1f\n", alternativeLinkingCostFactor) +
                String.format("ALLOW_GAP_CLOSING:               %b\n", allowGapClosing) +
                String.format("GAP_CLOSING_MAX_DISTANCE:        %.1f\n", gapClosingMaxDistance) +
                String.format("MAX_FRAME_GAP:                   %d\n", maxFrameGap) +
                String.format("ALLOW_TRACK_SPLITTING:           %b\n", allowTrackSplitting) +
                String.format("SPLITTING_MAX_DISTANCE:          %.1f\n", splittingMaxDistance) +
                String.format("ALLOW_TRACK_MERGING:             %b\n", allowTrackMerging) +
                String.format("MERGING_MAX_DISTANCE:            %.1f\n", mergingMaxDistance) +
                String.format("MIN_NR_SPOTS_IN_TRACK:           %d\n", minNumberOfSpots) +
                String.format("TRACK_COLOURING:                 %s\n", trackColouring) +
                String.format("MAX_NR_SPOTS_IN_IMAGE:           %d\n", maxNrOfSpotsInImage);
    }
}