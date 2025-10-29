/******************************************************************************
 *  Class:        RecordingEntry.java
 *  Package:      paint.viewer.utils
 *
 *  PURPOSE:
 *    Represents a single recording entry within an experiment, encapsulating
 *    metadata, image references, and square-level visibility control parameters.
 *
 *  DESCRIPTION:
 *    Each {@code RecordingEntry} provides access to both image representations
 *    (TrackMate and Brightfield) and data-related parameters such as minimum
 *    density ratio, maximum variability, R² threshold, and neighbour mode.
 *
 *    It also provides a high-level interface for retrieving {@link paint.shared.objects.Square}
 *    data associated with a recording, as well as convenience methods for
 *    metadata access (probe type, adjuvant, cell type, etc.).
 *
 *    Images are preloaded using either {@code ImageIO} or ImageJ’s {@code Opener}
 *    for compatibility with both standard and scientific formats.
 *
 *  KEY FEATURES:
 *    • Encapsulates per-recording configuration and visibility parameters.
 *    • Loads and caches TrackMate and Brightfield images automatically.
 *    • Provides structured access to recording metadata and square data.
 *    • Performs consistency validation against expected square counts.
 *    • Fully integrated with PAINT’s logging framework ({@link paint.shared.utils.PaintLogger}).
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-10-29
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.viewer.utils;

import paint.shared.objects.Project;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.utils.PaintLogger;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single recording entry within an experiment.
 * Each {@code RecordingEntry} encapsulates metadata, image paths, and adjustable
 * visibility control parameters (e.g., density ratio, variability, and R² threshold).
 *
 * <p>Images are preloaded via ImageIO or ImageJ Opener, and associated square data
 * can be lazily fetched and cached from the experiment context.</p>
 */
public class RecordingEntry {

    // @formatter:off
    private final Recording     recording;
    private final String        experimentName;

    private       double        minRequiredDensityRatio;
    private       double        maxAllowableVariability;
    private       double        minRequiredRSquared;
    private       String        neighbourMode;

    private final ImageIcon     leftImage;
    private final ImageIcon     rightImage;

    // @formatter:on

    /**
     * Constructs a new {@code RecordingEntry} with associated images and control parameters.
     *
     * @param recording the underlying {@link Recording} metadata
     * @param trackmateImagePath path to the TrackMate overlay image
     * @param brightfieldImagePath path to the Brightfield reference image
     * @param experimentName name of the parent experiment
     * @param minRequiredDensityRatio minimum required density ratio threshold
     * @param maxAllowableVariability maximum allowable variability threshold
     * @param minRequiredRSquared minimum R² value (0.0–1.0)
     * @param neighbourMode neighbour visibility mode as string
     */
    public RecordingEntry(Recording recording,
                          Path      trackmateImagePath,
                          Path      brightfieldImagePath,
                          String    experimentName,
                          double    minRequiredDensityRatio,
                          double    maxAllowableVariability,
                          double    minRequiredRSquared,
                          String    neighbourMode) {
        // @formatter:off
        this.recording               = recording;
        this.experimentName          = experimentName;
        this.minRequiredDensityRatio = minRequiredDensityRatio;
        this.maxAllowableVariability = maxAllowableVariability;
        this.minRequiredRSquared     = minRequiredRSquared;
        this.neighbourMode           = neighbourMode;
        this.leftImage               = loadImage(trackmateImagePath, "TrackMate");
        this.rightImage              = loadImage(brightfieldImagePath, "Brightfield");
        // @formatter:on
    }

    // =========================================================================================
    // IMAGE LOADING
    // =========================================================================================

    /**
     * Attempts to load an image from disk using {@code ImageIO}, falling back to
     * ImageJ’s {@code Opener} for extended format support. Returns an {@link ImageIcon}
     * suitable for Swing rendering or {@code null} if loading fails.
     *
     * @param imagePath path to the image file
     * @param label descriptive label for logging
     * @return {@link ImageIcon} for the image, or {@code null} if load fails
     */
    private static ImageIcon loadImage(Path imagePath, String label) {
        if (imagePath == null) {
            return null;
        }
        try {
            BufferedImage img = javax.imageio.ImageIO.read(imagePath.toFile());
            if (img != null) {
                PaintLogger.debugf("[%s] Loaded via ImageIO: %s", label, imagePath);
                return new ImageIcon(img);
            }
            PaintLogger.warnf("[%s] ImageIO returned null for %s", label, imagePath);
        } catch (Exception e) {
            PaintLogger.warnf("[%s] ImageIO failed for %s (%s)", label, imagePath, e.getMessage());
        }

        try {
            ij.io.Opener opener = new ij.io.Opener();
            ij.ImagePlus imp = opener.openImage(imagePath.toString());
            if (imp != null && imp.getImage() != null) {
                PaintLogger.debugf("[%s] Loaded via ImageJ Opener: %s", label, imagePath);
                return new ImageIcon(imp.getImage());
            }
            PaintLogger.warnf("[%s] ImageJ Opener returned null for %s", label, imagePath);
        } catch (Throwable t) {
            PaintLogger.warnf("[%s] ImageJ Opener threw error for %s (%s)", label, imagePath, t.getMessage());
        }

        PaintLogger.errorf("[%s] Failed to load image: %s", label, imagePath);
        return null;
    }

    // =========================================================================================
    // GETTERS AND SETTERS
    // =========================================================================================

    public String getRecordingName() {
        return recording.getRecordingName(); }

    public String getExperimentName() {
        return experimentName; }

    public String getProbeName() {
        return recording.getProbeName();
    }

    public String getProbeType() {
        return recording.getProbeType();
    }

    public String getAdjuvant() {
        return recording.getAdjuvant();
    }

    public String getCellType() {
        return recording.getCellType();
    }

    public double getConcentration() {
        return recording.getConcentration();
    }

    public int getNumberOfSpots() {
        return recording.getNumberOfSpots();
    }

    public int getNumberOfTracks() {
        return recording.getNumberOfTracks();
    }

    public double getThreshold() {
        return recording.getThreshold();
    }

    public double getTau() {
        return recording.getTau();
    }

    public double getDensity() {
        return recording.getDensity();
    }

    public double getMinRequiredDensityRatio() {
        return minRequiredDensityRatio;
    }

    public void setMinRequiredDensityRatio(double minRequiredDensityRatio) {
        this.minRequiredDensityRatio = minRequiredDensityRatio;
    }

    public double getMaxAllowableVariability() {
        return maxAllowableVariability;
    }

    public void setMaxAllowableVariability(double maxAllowableVariability) {
        this.maxAllowableVariability = maxAllowableVariability;
    }

    public double getMinRequiredRSquared() {
        return minRequiredRSquared;
    }

    public void setMinRequiredRSquared(double minRequiredRSquared) {
        this.minRequiredRSquared = minRequiredRSquared;
    }

    public String getNeighbourMode() {
        return neighbourMode;
    }

    public void setNeighbourMode(String neighbourMode) {
        this.neighbourMode = neighbourMode;
    }

    public ImageIcon getLeftImage() {
        return leftImage;
    }

    public ImageIcon getRightImage() {
        return rightImage;
    }

    public Recording getRecording() {
        return recording;
    }
}