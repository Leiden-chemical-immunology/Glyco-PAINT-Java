package paint.fijiUtilities;

public class TrackMateConfig {
    public int maxFrameGap;
    public double linkingMaxDistance;
    public double gapClosingMaxDistance;
    public double alternativeLinkingCostFactor;
    public double splittingMaxDistance;
    public boolean allowGapClosing;
    public boolean allowTrackMerging;
    public boolean allowTrackSplitting;
    public double mergingMaxDistance;
    public boolean doSubpixelLocalization;
    public double radius;
    public int targetChannel;
    public boolean doMedianFiltering;
    public int minNumberOfSpots;
    public int maxNrOfSpotsInImage;
    public String trackColouring;
    public double threshold;

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