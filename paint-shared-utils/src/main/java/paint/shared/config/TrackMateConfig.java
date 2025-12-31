/*=============================================================================
 *  Class:        TrackMateConfig.java
 *  Package:      paint.shared.config
 *
 *  PURPOSE:
 *    Represents configuration parameters used in a tracking system (TrackMate).
 *
 *  DESCRIPTION:
 *    Encapsulates various settings related to frame gap, localization techniques,
 *    distance thresholds, and track management functionalities like merging and
 *    splitting. Immutable once constructed.
 *
 *  KEY FEATURES:
 *    - Immutable configuration object (all properties final)
 *    - Built either from full constructor or via reading values from PaintConfig
 *    - Provides getters for all configuration fields
 *    - Provides utility method to serialize the configuration to file
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
=============================================================================*/

package paint.shared.config;

import paint.shared.config.paintconfig.PaintConfig;

//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;

import static paint.shared.constants.PaintStringConstants.*;

/**
 * The {@code TrackMateConfig} class represents configuration parameters used in a
 * tracking system. It encapsulates various settings related to frame gap,
 * localization techniques, distance thresholds, and track management
 * functionalities like merging and splitting.
 *
 * <p>This class is immutable as all properties are final and set through
 * constructors. This allows for thread-safe usage and ensures the
 * configuration data remains consistent throughout the application.</p>
 *
 * <p>Key capabilities:
 * <ul>
 *   <li>Specification of tracking parameters such as maximum frame gap, linking
 *       costs, maximum allowed distances for linking, gap closing, splitting,
 *       and merging.</li>
 *   <li>Enabling or disabling specific track behaviors such as subpixel
 *       localization, track merging, splitting, and gap closing.</li>
 *   <li>Setting constraints on track construction, including minimum spots per
 *       track, maximum spots in an image, and other related parameters.</li>
 *   <li>Configuration of optional operations such as median filtering.</li>
 *   <li>Provides a utility method to write the configuration to a file in a
 *       structured string form.</li>
 * </ul>
 * </p>
 */
public class TrackMateConfig {

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

    /**
     * Constructs a configuration object for TrackMate using the parameters provided
     * in the given PaintConfig instance. The configuration parameters set here are
     * used for defining track-related operations such as linking, merging, splitting,
     * and visualization.
     */
    public TrackMateConfig() {
        
        this.maxFrameGap                  = PaintConfig.getInt(     TRACKMATE, MAX_FRAME_GAP, 3);
        this.alternativeLinkingCostFactor = PaintConfig.getDouble(  TRACKMATE, ALTERNATIVE_LINKING_COST_FACTOR,2.0);
        this.doSubpixelLocalization       = PaintConfig.getBoolean( TRACKMATE, DO_SUBPIXEL_LOCALIZATION,       true);
        this.minNumberOfSpotsInTrack      = PaintConfig.getInt(     TRACKMATE, MIN_NR_SPOTS_IN_TRACK,          3);
        this.linkingMaxDistance           = PaintConfig.getDouble(  TRACKMATE, LINKING_MAX_DISTANCE,           0.6);
        this.maxNumberOfSpotsInImage      = PaintConfig.getInt(     TRACKMATE, MAX_NR_SPOTS_IN_IMAGE,          2000000);
        this.maxNumberOfSecondsPerImage   = PaintConfig.getInt(     TRACKMATE, MAX_NR_SECONDS_PER_IMAGE,       2000);
        this.gapClosingMaxDistance        = PaintConfig.getDouble(  TRACKMATE, GAP_CLOSING_MAX_DISTANCE,       1.2);
        this.targetChannel                = PaintConfig.getInt(     TRACKMATE, TARGET_CHANNEL,                 1);
        this.splittingMaxDistance         = PaintConfig.getDouble(  TRACKMATE, SPLITTING_MAX_DISTANCE,         1.0);
        this.trackColouring               = PaintConfig.getString(  TRACKMATE, TRACK_COLOURING,                "TRACK_DURATION");
        this.radius                       = PaintConfig.getDouble(  TRACKMATE, RADIUS,                         1.0);
        this.allowGapClosing              = PaintConfig.getBoolean( TRACKMATE, ALLOW_GAP_CLOSING,              true);
        this.medianFiltering              = PaintConfig.getBoolean( TRACKMATE, DO_MEDIAN_FILTERING,            false);
        this.allowTrackSplitting          = PaintConfig.getBoolean( TRACKMATE, ALLOW_TRACK_SPLITTING,          false);
        this.allowTrackMerging            = PaintConfig.getBoolean( TRACKMATE, ALLOW_TRACK_MERGING,            false);
        this.mergingMaxDistance           = PaintConfig.getDouble(  TRACKMATE, MERGING_MAX_DISTANCE,           1.0);
    }


    // Getters only, Setter methods are not needed either, as the attributes are final

    /**
     * @return the maximum allowed gap in frames between two spots in a track.
     */
    public int getMaxFrameGap() {
        return maxFrameGap;
    }

    /**
     * @return factor used for alternative linking cost calculations.
     */
    public double getAlternativeLinkingCostFactor() {
        return alternativeLinkingCostFactor;
    }

    /**
     * @return {@code true} if subpixel localization is enabled.
     */
    public boolean isDoSubpixelLocalization() {
        return doSubpixelLocalization;
    }

    /**
     * @return the minimum required number of spots for a valid track.
     */
    public int getMinNumberOfSpotsInTrack() {
        return minNumberOfSpotsInTrack;
    }

    /**
     * @return the maximum distance allowed for linking two spots.
     */
    public double getLinkingMaxDistance() {
        return linkingMaxDistance;
    }

    /**
     * @return the maximum number of spots allowed per image.
     */
    public int getMaxNumberOfSpotsInImage() {
        return maxNumberOfSpotsInImage;
    }

    /**
     * @return the maximum processing time allowed per image in seconds.
     */
    public int getMaxNumberOfSecondsPerImage() {
        return maxNumberOfSecondsPerImage;
    }

    /**
     * @return the maximum distance allowed for closing gaps between track segments.
     */
    public double getGapClosingMaxDistance() {
        return gapClosingMaxDistance;
    }

    /**
     * @return the target image channel for tracking.
     */
    public int getTargetChannel() {
        return targetChannel;
    }

    /**
     * @return the maximum distance allowed for track splitting.
     */
    public double getSplittingMaxDistance() {
        return splittingMaxDistance;
    }

    /**
     * @return the color mode used for track visualization.
     */
    public String getTrackColoring() {
        return trackColouring;
    }

    /**
     * @return the radius of spots to be tracked.
     */
    public double getRadius() {
        return radius;
    }

    /**
     * @return {@code true} if gap closing is allowed.
     */
    public boolean isAllowGapClosing() {
        return allowGapClosing;
    }

    /**
     * @return {@code true} if median filtering is enabled.
     */
    public boolean isMedianFiltering() {
        return medianFiltering;
    }

    /**
     * @return {@code true} if track splitting is allowed.
     */
    public boolean isAllowTrackSplitting() {
        return allowTrackSplitting;
    }

    /**
     * @return {@code true} if track merging is allowed.
     */
    public boolean isAllowTrackMerging() {
        return allowTrackMerging;
    }

    /**
     * @return the maximum distance allowed for track merging.
     */
    public double getMergingMaxDistance() {
        return mergingMaxDistance;
    }

    @Override
    public String toString() {

        return "TrackMateConfig" + "\n" +
                "                  Max Frame Gap                   = "  + maxFrameGap + "\n" +
                "                  Alternative Linking Cost Factor = "  + alternativeLinkingCostFactor + "\n" +
                "                  Do Subpixel Localization        = "  + doSubpixelLocalization + "\n" +
                "                  Min Number of Spots In Track    = "  + minNumberOfSpotsInTrack + "\n" +
                "                  Linking Max Distance            = "  + linkingMaxDistance + "\n" +
                "                  Max Number of Spots in Image    = "  + maxNumberOfSpotsInImage + "\n" +
                "                  Max Number of Seconds per Image = "  + maxNumberOfSecondsPerImage + "\n" +
                "                  Gap Closing Max Distance        = "  + gapClosingMaxDistance + "\n" +
                "                  Target Channel                  = "  + targetChannel + "\n" +
                "                  Splitting Max Distance          = "  + splittingMaxDistance + "\n" +
                "                  Track Coloring                  = '" + trackColouring + '\'' + "\n" +
                "                  Radius                          = "  + radius + "\n" +
                "                  Allow Gap Closing               = "  + allowGapClosing + "\n" +
                "                  Do Median Filtering             = "  + medianFiltering + "\n" +
                "                  Allow Track Splitting           = "  + allowTrackSplitting + "\n" +
                "                  Allow Track Merging             = "  + allowTrackMerging + "\n" +
                "                  Merging Max Distance            = "  + mergingMaxDistance + "\n";
    }
}