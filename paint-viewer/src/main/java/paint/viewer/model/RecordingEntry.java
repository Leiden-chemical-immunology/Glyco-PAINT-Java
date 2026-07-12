/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.viewer.model;

import paint.shared.objects.Recording;
import paint.shared.utils.PaintLogger;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * Viewer-level wrapper for a {@link Recording} containing all metadata and
 * preloaded image resources for a single experiment recording.
 *
 * <p>This entry supplies:</p>
 * <ul>
 *   <li>TrackMate overlay image (left display)</li>
 *   <li>Brightfield image (right display)</li>
 *   <li>Delegated metadata access (probe, adjuvant, concentration, density, etc.)</li>
 *   <li>Access to the full {@link Recording} for retrieving squares</li>
 * </ul>
 *
 * <p>The entry is immutable after construction. All square-level access and
 * data manipulation occur on the underlying {@link Recording} instance.</p>
 */
public class RecordingEntry {

    private final Recording recording;
    private final String    experimentName;

    private final ImageIcon leftImage;   // TrackMate overlay
    private final ImageIcon rightImage;  // Brightfield

    /**
     * Creates a new immutable entry representing one recording inside an experiment.
     *
     * @param recording            the underlying domain object containing metadata and square data
     * @param trackmateImagePath   file path to the TrackMate image (left panel)
     * @param brightfieldImagePath file path to the Brightfield image (right panel)
     * @param experimentName       parent experiment name
     */
    public RecordingEntry(Recording recording,
            Path      trackmateImagePath,
            Path      brightfieldImagePath,
            String    experimentName) {

        this.recording      = recording;
        this.experimentName = experimentName;

        this.leftImage  = loadImage(trackmateImagePath,   "TrackMate");
        this.rightImage = loadImage(brightfieldImagePath, "Brightfield");
    }

    // =========================================================================================
    // IMAGE LOADING
    // =========================================================================================

    /**
     * Attempts to load an image using {@code ImageIO}, falling back to ImageJ’s
     * {@code Opener} for formats ImageIO cannot handle (common for scientific data).
     *
     * @param imagePath the file path to load
     * @param label     descriptive label for logging
     * @return a Swing-compatible {@link ImageIcon}, or {@code null} on failure
     */
    private static ImageIcon loadImage(Path imagePath, String label) {
        if (imagePath == null) {
            return null;
        }

        // Attempt ImageIO first
        try {
            BufferedImage img = javax.imageio.ImageIO.read(imagePath.toFile());
            if (img != null) {
                PaintLogger.debugf("[%s] Loaded via ImageIO: %s", label, imagePath);
                return new ImageIcon(img);
            }
            PaintLogger.warnf("[%s] ImageIO returned null: %s", label, imagePath);
        } catch (Exception e) {
            PaintLogger.warnf("[%s] ImageIO failed for %s (%s)", label, imagePath, e.getMessage());
        }

        // Fallback to ImageJ Opener
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

        // Failure
        PaintLogger.errorf("[%s] Failed to load image: %s", label, imagePath);
        return null;
    }

    // =========================================================================================
    // METADATA ACCESS
    // =========================================================================================


    /**
     * @return the name of the recording.
     */
    public String getRecordingName() {
        return recording.getRecordingName();
    }

    /**
     * @return the name of the experiment.
     */
    public String getExperimentName() {
        return experimentName;
    }

    /**
     * @return the name of the probe.
     */
    public String getProbeName() {
        return recording.getProbeName();
    }

    /**
     * @return the type of probe.
     */
    public String getProbeType() {
        return recording.getProbeType();
    }

    /**
     * @return the adjuvant used.
     */
    public String getAdjuvant() {
        return recording.getAdjuvant();
    }

    /**
     * @return the cell type used.
     */
    public String getCellType() {
        return recording.getCellType();
    }

    /**
     * @return the probe concentration.
     */
    public double getConcentration() {
        return recording.getConcentration();
    }

    /**
     * @return the total number of spots.
     */
    public int getNumberOfSpots() {
        return recording.getNumberOfSpots();
    }

    /**
     * @return the total number of tracks.
     */
    public int getNumberOfTracks() {
        return recording.getNumberOfTracks();
    }

    /**
     * @return the analysis threshold.
     */
    public double getThreshold() {
        return recording.getThreshold();
    }


    // =========================================================================================
    // IMAGE AND RECORDING ACCESS
    // =========================================================================================

    /**
     * @return the TrackMate preview image.
     */
    public ImageIcon getLeftImage() {
        return leftImage;
    }

    /**
     * @return the brightfield image.
     */
    public ImageIcon getRightImage() {
        return rightImage;
    }

    /**
     * @return the underlying {@link Recording} data object.
     */
    public Recording getRecording() {
        return recording;
    }
}