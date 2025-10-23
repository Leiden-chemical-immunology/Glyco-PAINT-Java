package paint.shared.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static paint.shared.config.PaintConfig.*;

/**
 * The TrackMateConfig class represents configuration parameters used in a
 * tracking system. It encapsulates various settings related to frame gap,
 * localization techniques, distance thresholds, and track management
 * functionalities like merging and splitting.
 *
 * The class is immutable as all properties are final and set through
 * constructors. This allows for thread-safe usage and ensures the
 * configuration data remains consistent throughout the application.
 *
 * TrackMateConfig offers the following core functionalities:
 *
 * - Specification of tracking parameters such as maximum frame gap, linking
 *   costs, maximum allowed distances for linking, gap closing, splitting,
 *   and merging.
 * - Enabling or disabling specific track behaviors such as subpixel
 *   localization, track merging, splitting, and gap closing.
 * - Setting constraints on track construction, including minimum spots per
 *   track, maximum spots in an image, and other related parameters.
 * - Configuration of optional operations such as median filtering.
 * - Enables retrieval of data in a structured manner using getter methods.
 *   Setter methods are intentionally omitted due to the immutable nature of
 *   the class.
 * - Provides a utility method to initialize the configuration from a
 *   PaintConfig object.
 * - Offers functionality to serialize the configuration to a file in the
 *   form of a structured string representation.
 *
 * Utility Methods:
 *
 * - `from(PaintConfig paintConfig)`: Creates a TrackMateConfig instance by
 *   reading parameters from a PaintConfig object.
 *
 * - `trackMateConfigToFile(TrackMateConfig trackMateConfig, Path filePath)`:
 *   Serializes the configuration object to a file for persistence.
 *
 * This class is designed for advanced tracking applications where precise
 * and customizable configurations are essential.
 */
public class TrackMateConfig {

    // @formatter:off
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
    // @formatter:on

    // Full constructor with all values specified

    /**
     * Constructs a configuration object for TrackMate with various parameters used for
     * defining track linking, merging, splitting, and other track-based operations.
     *
     * @param maxFrameGap                  Maximum allowable frame gap for linking spots in a track.
     * @param alternativeLinkingCostFactor Factor affecting the costs during alternative linking of spots.
     * @param doSubpixelLocalization       Flag indicating whether subpixel spot localization should be performed.
     * @param minNrSpotsInTrack            Minimum number of spots required for a track to be considered valid.
     * @param linkingMaxDistance           Maximum allowable distance for linking neighboring spots.
     * @param maxNrSpotsInImage            Maximum number of spots allowed in a single image/frame.
     * @param gapClosingMaxDistance        Maximum distance permitted for closing gaps between track segments.
     * @param targetChannel                The target channel to be used for track analysis.
     * @param splittingMaxDistance         Maximum allowable distance for splitting tracks.
     * @param trackColouring               Colouring scheme to use for visualizing tracks.
     * @param radius                       The radius used for spatial filtering of the spots.
     * @param allowGapClosing              Flag indicating whether gap closing in tracks is allowed.
     * @param doMedianFiltering            Flag indicating whether median filtering should be applied to the tracks.
     * @param allowTrackSplitting          Flag indicating if track splitting at branching points is allowed.
     * @param allowTrackMerging            Flag indicating if merging of separate tracks is allowed.
     * @param mergingMaxDistance           Maximum allowable distance for merging tracks.
     */
    private TrackMateConfig(

            // @formatter:off
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

        this.maxFrameGap                  = maxFrameGap;
        this.alternativeLinkingCostFactor = alternativeLinkingCostFactor;
        this.doSubpixelLocalization       = doSubpixelLocalization;
        this.minNrSpotsInTrack            = minNrSpotsInTrack;
        this.linkingMaxDistance           = linkingMaxDistance;
        this.maxNrSpotsInImage            = maxNrSpotsInImage;
        this.gapClosingMaxDistance        = gapClosingMaxDistance;
        this.targetChannel                = targetChannel;
        this.splittingMaxDistance         = splittingMaxDistance;
        this.trackColouring               = trackColouring;
        this.radius                       = radius;
        this.allowGapClosing              = allowGapClosing;
        this.medianFiltering              = doMedianFiltering;
        this.allowTrackSplitting          = allowTrackSplitting;
        this.allowTrackMerging            = allowTrackMerging;
        this.mergingMaxDistance           = mergingMaxDistance;
        // @formatter:on
    }

    /**
     * Constructs a configuration object for TrackMate using the parameters provided
     * in the given PaintConfig instance. The configuration parameters set here are
     * used for defining track-related operations such as linking, merging, splitting,
     * and visualization.
     *
     * @param paintConfig The PaintConfig object containing parameters for configuring the TrackMate instance.
     */
    private TrackMateConfig(PaintConfig paintConfig) {
        // @formatter:off
        this.maxFrameGap                  = getInt(    "TrackMate", "MAX_FRAME_GAP", 3);
        this.alternativeLinkingCostFactor = getDouble( "TrackMate", "ALTERNATIVE_LINKING_COST_FACTOR",2.0);
        this.doSubpixelLocalization       = getBoolean("TrackMate", "DO_SUBPIXEL_LOCALIZATION",       true);
        this.minNrSpotsInTrack            = getInt(    "TrackMate", "MIN_NR_SPOTS_IN_TRACK",          3);
        this.linkingMaxDistance           = getDouble( "TrackMate", "LINKING_MAX_DISTANCE",           0.6);
        this.maxNrSpotsInImage            = getInt(    "TrackMate", "MAX_NR_SPOTS_IN_IMAGE",          2000000);
        this.gapClosingMaxDistance        = getDouble( "TrackMate", "GAP_CLOSING_MAX_DISTANCE",       1.2);
        this.targetChannel                = getInt(    "TrackMate", "TARGET_CHANNEL",                 1);
        this.splittingMaxDistance         = getDouble( "TrackMate", "SPLITTING_MAX_DISTANCE",         1.0);
        this.trackColouring               = getString( "TrackMate", "TRACK_COLOURING",                "TRACK_DURATION");
        this.radius                       = getDouble( "TrackMate", "RADIUS",                         1.0);
        this.allowGapClosing              = getBoolean("TrackMate", "ALLOW_GAP_CLOSING",              true);
        this.medianFiltering              = getBoolean("TrackMate", "DO_MEDIAN_FILTERING",            false);
        this.allowTrackSplitting          = getBoolean("TrackMate", "ALLOW_TRACK_SPLITTING",          false);
        this.allowTrackMerging            = getBoolean("TrackMate", "ALLOW_TRACK_MERGING",            false);
        this.mergingMaxDistance           = getDouble(  "TrackMate", "MERGING_MAX_DISTANCE",          1.0);
        // @formatter:on
    }

    public static TrackMateConfig from(PaintConfig paintConfig) {
        return new TrackMateConfig(paintConfig);
    }

    // Getters only, Setter methods are not needed either, as the attributes are final

    public int getMaxFrameGap() {
        return maxFrameGap;
    }

    public double getAlternativeLinkingCostFactor() {
        return alternativeLinkingCostFactor;
    }

    public boolean isDoSubpixelLocalization() {
        return doSubpixelLocalization;
    }

    public int getMinNrSpotsInTrack() {
        return minNrSpotsInTrack;
    }

    public double getLinkingMaxDistance() {
        return linkingMaxDistance;
    }

    public int getMaxNrSpotsInImage() {
        return maxNrSpotsInImage;
    }

    public double getGapClosingMaxDistance() {
        return gapClosingMaxDistance;
    }

    public int getTargetChannel() {
        return targetChannel;
    }

    public double getSplittingMaxDistance() {
        return splittingMaxDistance;
    }

    public String getTrackColoring() {
        return trackColouring;
    }

    public double getRadius() {
        return radius;
    }

    public boolean isAllowGapClosing() {
        return allowGapClosing;
    }

    public boolean isMedianFiltering() {
        return medianFiltering;
    }

    public boolean isAllowTrackSplitting() {
        return allowTrackSplitting;
    }

    public boolean isAllowTrackMerging() {
        return allowTrackMerging;
    }

    public double getMergingMaxDistance() {
        return mergingMaxDistance;
    }

    @Override
    public String toString() {
        // @formatter:off
        return "TrackMateConfig" + "\n" +
                "                  maxFrameGap                  = " + maxFrameGap + "\n" +
                "                  alternativeLinkingCostFactor = " + alternativeLinkingCostFactor + "\n" +
                "                  doSubpixelLocalization       = " + doSubpixelLocalization + "\n" +
                "                  minNrSpotsInTrack            = " + minNrSpotsInTrack + "\n" +
                "                  linkingMaxDistance           = " + linkingMaxDistance + "\n" +
                "                  maxNrSpotsInImage=           = " + maxNrSpotsInImage + "\n" +
                "                  gapClosingMaxDistance        = " + gapClosingMaxDistance + "\n" +
                "                  targetChannel                = " + targetChannel + "\n" +
                "                  splittingMaxDistance         = " + splittingMaxDistance + "\n" +
                "                  trackColoring                = '" + trackColouring + '\'' + "\n" +
                "                  radius                       = " + radius + "\n" +
                "                  allowGapClosing              = " + allowGapClosing + "\n" +
                "                  doMedianFiltering            = " + medianFiltering + "\n" +
                "                  allowTrackSplitting          = " + allowTrackSplitting + "\n" +
                "                  allowTrackMerging            = " + allowTrackMerging + "\n" +
                "                  mergingMaxDistance           = " + mergingMaxDistance + "\n";
        // @formatter:on
    }

    public static void trackMateConfigToFile(TrackMateConfig trackMateConfig, Path filePath) {
        String formattedString = trackMateConfig.toString();

        // overwrite or create
        try {
            Files.write(filePath, formattedString.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {

        }
    }
}
