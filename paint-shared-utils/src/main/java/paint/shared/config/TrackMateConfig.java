package paint.shared.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static paint.shared.config.PaintConfig.SECTION_TRACKMATE;
import static paint.shared.constants.PaintConstants.*;

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
 *
 * This class is designed for advanced tracking applications where precise
 * and customizable configurations are essential.
 */
public class TrackMateConfig {

    // @formatter:off
    private final int     maxFrameGap;
    private final double  alternativeLinkingCostFactor;
    private final boolean doSubpixelLocalization;
    private final int     minNumberOfSpotsInTrack;
    private final double  linkingMaxDistance;
    private final int     maxNumberOfSpotsInImage;
    private final int     maxNumberOfSecondsPerImage;
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
     * @param maxNumberOfSpotsInImage      Maximum number of spots allowed in a single image/frame.
     * @param maxNumberOfSecondsPerImage   Maximum number of seconds allowed for a single image/frame.
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
            int     maxNumberOfSpotsInImage,
            int     maxNumberOfSecondsPerImage,
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
        this.minNumberOfSpotsInTrack      = minNrSpotsInTrack;
        this.linkingMaxDistance           = linkingMaxDistance;
        this.maxNumberOfSpotsInImage      = maxNumberOfSpotsInImage;
        this.maxNumberOfSecondsPerImage   = maxNumberOfSecondsPerImage;
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
     */
    public TrackMateConfig() {
        // @formatter:off
        this.maxFrameGap                  = PaintConfig.getInt(    SECTION_TRACKMATE, MAX_FRAME_GAP, 3);
        this.alternativeLinkingCostFactor = PaintConfig.getDouble( SECTION_TRACKMATE, ALTERNATIVE_LINKING_COST_FACTOR,2.0);
        this.doSubpixelLocalization       = PaintConfig.getBoolean(SECTION_TRACKMATE, DO_SUBPIXEL_LOCALIZATION,       true);
        this.minNumberOfSpotsInTrack      = PaintConfig.getInt(    SECTION_TRACKMATE, MIN_NR_SPOTS_IN_TRACK,          3);
        this.linkingMaxDistance           = PaintConfig.getDouble( SECTION_TRACKMATE, LINKING_MAX_DISTANCE,           0.6);
        this.maxNumberOfSpotsInImage      = PaintConfig.getInt(    SECTION_TRACKMATE, MAX_NR_SPOTS_IN_IMAGE,          2000000);
        this.maxNumberOfSecondsPerImage   = PaintConfig.getInt(    SECTION_TRACKMATE, MAX_NR_SECONDS_PER_IMAGE,       2000);
        this.gapClosingMaxDistance        = PaintConfig.getDouble( SECTION_TRACKMATE, GAP_CLOSING_MAX_DISTANCE,       1.2);
        this.targetChannel                = PaintConfig.getInt(    SECTION_TRACKMATE, TARGET_CHANNEL,                 1);
        this.splittingMaxDistance         = PaintConfig.getDouble( SECTION_TRACKMATE, SPLITTING_MAX_DISTANCE,         1.0);
        this.trackColouring               = PaintConfig.getString( SECTION_TRACKMATE, TRACK_COLOURING,                "TRACK_DURATION");
        this.radius                       = PaintConfig.getDouble( SECTION_TRACKMATE, RADIUS,                         1.0);
        this.allowGapClosing              = PaintConfig.getBoolean(SECTION_TRACKMATE, ALLOW_GAP_CLOSING,              true);
        this.medianFiltering              = PaintConfig.getBoolean(SECTION_TRACKMATE, DO_MEDIAN_FILTERING,            false);
        this.allowTrackSplitting          = PaintConfig.getBoolean(SECTION_TRACKMATE, ALLOW_TRACK_SPLITTING,          false);
        this.allowTrackMerging            = PaintConfig.getBoolean(SECTION_TRACKMATE, ALLOW_TRACK_MERGING,            false);
        this.mergingMaxDistance           = PaintConfig.getDouble( SECTION_TRACKMATE, MERGING_MAX_DISTANCE,           1.0);
        // @formatter:on
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

    public int getMinNumberOfSpotsInTrack() {
        return minNumberOfSpotsInTrack;
    }

    public double getLinkingMaxDistance() {
        return linkingMaxDistance;
    }

    public int getMaxNumberOfSpotsInImage() {
        return maxNumberOfSpotsInImage;
    }

    public int getMaxNumberOfSecondsPerImage() {
        return maxNumberOfSecondsPerImage;
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
                "                  Max Frame Gap                   = "  + maxFrameGap + "\n" +
                "                  Alternative Linking Cost Factor = "  + alternativeLinkingCostFactor + "\n" +
                "                  Do Subpixel Localization        = "  + doSubpixelLocalization + "\n" +
                "                  Min Number of Spots In T        = "  + minNumberOfSpotsInTrack + "\n" +
                "                  Linking Max Distance            = "  + linkingMaxDistance + "\n" +
                "                  Max Number of Spots in Image    = "  + maxNumberOfSpotsInImage + "\n" +
                "                  Max Number of Seconds per Image = "  + maxNumberOfSecondsPerImage + "\n" +
                "                  Gap Closing Max Distance        = "  + gapClosingMaxDistance + "\n" +
                "                  Target Channel                  = "  + targetChannel + "\n" +
                "                  Splitting MaxD istance          = "  + splittingMaxDistance + "\n" +
                "                  Track Coloring                  = '" + trackColouring + '\'' + "\n" +
                "                  Radius                          = "  + radius + "\n" +
                "                  Allow Gap Closing               = "  + allowGapClosing + "\n" +
                "                  Do Median Filtering             = "  + medianFiltering + "\n" +
                "                  Allow Track Splitting           = "  + allowTrackSplitting + "\n" +
                "                  Allow Track Merging             = "  + allowTrackMerging + "\n" +
                "                  Merging Max Distance            = "  + mergingMaxDistance + "\n";
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
