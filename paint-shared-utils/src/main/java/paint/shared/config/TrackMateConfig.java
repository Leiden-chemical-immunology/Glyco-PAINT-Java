package paint.shared.config;


import static paint.shared.config.PaintConfig.getInt;
import static paint.shared.config.PaintConfig.getDouble;
import static paint.shared.config.PaintConfig.getBoolean;
import static paint.shared.config.PaintConfig.getString;

/**
 * Holds the data retrieved from the TrackMate section of the Paint configuration file.
 */
public class TrackMateConfig {

    private final int     maxFrameGap;
    private final double  alternativeLinkingCostFactor;
    private final boolean doSubpixelLocalization;
    private final int     minNrSpotsInTrack;
    private final double  linkingMaxDistance;
    private final int     maxNrSpotsInImage;
    private final double  gapClosingMaxDistance;
    private final int     targetChannel;
    private final double  splittingMaxDistance;
    private final String  trackColouring;
    private final double  radius;
    private final boolean allowGapClosing;
    private final boolean medianFiltering;
    private final boolean allowTrackSplitting;
    private final boolean allowTrackMerging;
    private final double  mergingMaxDistance;

    // Full constructor with all values specified

    private TrackMateConfig(
             int     maxFrameGap,
             double  alternativeLinkingCostFactor,
             boolean doSubpixelLocalization,
             int     minNrSpotsInTrack,
             double  linkingMaxDistance,
             int     maxNrSpotsInImage,
             double  gapClosingMaxDistance,
             int     targetChannel,
             double  splittingMaxDistance,
             String  trackColouring,
             double  radius,
             boolean allowGapClosing,
             boolean doMedianFiltering,
             boolean allowTrackSplitting,
             boolean allowTrackMerging,
             double  mergingMaxDistance) {
        this.maxFrameGap = maxFrameGap;
        this.alternativeLinkingCostFactor = alternativeLinkingCostFactor;
        this.doSubpixelLocalization = doSubpixelLocalization;
        this.minNrSpotsInTrack = minNrSpotsInTrack;
        this.linkingMaxDistance = linkingMaxDistance;
        this.maxNrSpotsInImage = maxNrSpotsInImage;
        this.gapClosingMaxDistance = gapClosingMaxDistance;
        this.targetChannel = targetChannel;
        this.splittingMaxDistance = splittingMaxDistance;
        this.trackColouring = trackColouring;
        this.radius = radius;
        this.allowGapClosing = allowGapClosing;
        this.medianFiltering = doMedianFiltering;
        this.allowTrackSplitting = allowTrackSplitting;
        this.allowTrackMerging = allowTrackMerging;
        this.mergingMaxDistance = mergingMaxDistance;
    }

    private TrackMateConfig(PaintConfig paintConfig) {
        this.maxFrameGap = getInt("TrackMate", "MAX_FRAME_GAP", 3);
        this.alternativeLinkingCostFactor = getDouble("TrackMate", "ALTERNATIVE_LINKING_COST_FACTOR", 2.0);
        this.doSubpixelLocalization = getBoolean("TrackMate", "DO_SUBPIXEL_LOCALIZATION", true);
        this.minNrSpotsInTrack = getInt("TrackMate", "MIN_NR_SPOTS_IN_TRACK", 3);
        this.linkingMaxDistance = getDouble("TrackMate", "LINKING_MAX_DISTANCE", 0.6);
        this.maxNrSpotsInImage = getInt("TrackMate", "MAX_NR_SPOTS_IN_IMAGE", 10000);
        this.gapClosingMaxDistance = getDouble("TrackMate", "GAP_CLOSING_MAX_DISTANCE", 1.2);
        this.targetChannel = getInt("TrackMate", "TARGET_CHANNEL", 1);
        this.splittingMaxDistance = getDouble("TrackMate", "SPLITTING_MAX_DISTANCE", 1.0);
        this.trackColouring = getString("TrackMate", "TRACK_COLOURING", "Default");
        this.radius = getDouble("TrackMate", "RADIUS", 1.0);
        this.allowGapClosing = getBoolean("TrackMate", "ALLOW_GAP_CLOSING", true);
        this.medianFiltering = getBoolean("TrackMate", "DO_MEDIAN_FILTERING", false);
        this.allowTrackSplitting = getBoolean("TrackMate", "ALLOW_TRACK_SPLITTING", false);
        this.allowTrackMerging = getBoolean("TrackMate", "ALLOW_TRACK_MERGING", false);
        this.mergingMaxDistance = getDouble("TrackMate", "MERGING_MAX_DISTANCE", 1.0);
    }

    public static TrackMateConfig from(PaintConfig paintConfig) {
        return new TrackMateConfig(paintConfig);
    }

    // Getters only, Setter methods are not needed either, as the attributes are final

    public int getMaxFrameGap() { return maxFrameGap; }
    public double getAlternativeLinkingCostFactor() { return alternativeLinkingCostFactor; }
    public boolean isDoSubpixelLocalization() { return doSubpixelLocalization; }
    public int getMinNrSpotsInTrack() { return minNrSpotsInTrack; }
    public double getLinkingMaxDistance() { return linkingMaxDistance; }
    public int getMaxNrSpotsInImage() { return maxNrSpotsInImage; }
    public double getGapClosingMaxDistance() { return gapClosingMaxDistance; }
    public int getTargetChannel() { return targetChannel; }
    public double getSplittingMaxDistance() { return splittingMaxDistance; }
    public String getTrackColoring() { return trackColouring; }
    public double getRadius() { return radius; }
    public boolean isAllowGapClosing() { return allowGapClosing; }
    public boolean isMedianFiltering() { return medianFiltering; }
    public boolean isAllowTrackSplitting() { return allowTrackSplitting; }
    public boolean isAllowTrackMerging() { return allowTrackMerging; }
    public double getMergingMaxDistance() { return mergingMaxDistance; }    

    @Override
    public String toString() {
        return "TrackMateConfig" + "\n" +
                "   maxFrameGap=" + maxFrameGap + "\n" +
                "   alternativeLinkingCostFactor=" + alternativeLinkingCostFactor + "\n" +
                "   doSubpixelLocalization=" + doSubpixelLocalization + "\n" +
                "   minNrSpotsInTrack=" + minNrSpotsInTrack + "\n" +
                "   linkingMaxDistance=" + linkingMaxDistance + "\n" +
                "   maxNrSpotsInImage=" + maxNrSpotsInImage + "\n" +
                "   gapClosingMaxDistance=" + gapClosingMaxDistance + "\n" +
                "   targetChannel=" + targetChannel + "\n" +
                "   splittingMaxDistance=" + splittingMaxDistance + "\n" +
                "   trackColoring='" + trackColouring + '\'' + "\n" +
                "   radius=" + radius + "\n" +
                "   allowGapClosing=" + allowGapClosing + "\n" +
                "   doMedianFiltering=" + medianFiltering + "\n" +
                "   allowTrackSplitting=" + allowTrackSplitting + "\n" +
                "   allowTrackMerging=" + allowTrackMerging + "\n" +
                "   mergingMaxDistance=" + mergingMaxDistance + "\n" ;
    }
}
