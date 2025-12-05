/*==============================================================================
 *  Class:        RecordingEntry.java
 *  Package:      paint.viewer.model
 *
 *  PURPOSE:
 *    Represents a single recording entry inside a PAINT experiment, providing
 *    structured access to metadata, TrackMate/Brightfield images, and the
 *    associated {@link paint.shared.objects.Recording} containing square data.
 *
 *  DESCRIPTION:
 *    A {@code RecordingEntry} acts as the viewer-facing wrapper around a
 *    {@link Recording}. It provides:
 *
 *      • Preloaded TrackMate and Brightfield images.
 *      • Access to all standard metadata fields (probe name/type, adjuvant,
 *        cell type, concentration, threshold, density, etc.).
 *      • Delegation to the underlying {@link Recording} object for all
 *        square-level and measurement-level data.
 *
 *    Images are loaded using standard {@code ImageIO} first, with automatic
 *    fallback to ImageJ's {@code Opener} to support scientific formats such
 *    as TIFF, JPEG2000, and some ND2 conversions.
 *
 *  KEY FEATURES:
 *    • Simple immutable wrapper around a {@link Recording}.
 *    • Automatic, robust image loading with verbosity via {@link PaintLogger}.
 *    • Supplies the ViewerFrame and its panels with pre-scaled image icons.
 *    • Does not perform filtering or control-parameter logic itself; this is
 *      delegated to the controlling UI classes.
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
 *==============================================================================*/

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


    public String getRecordingName() {
        return recording.getRecordingName();
    }

    public String getExperimentName() {
        return experimentName;
    }

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

//    public double getTau() {
//        return recording.getTau();
//    }

//    public double getDensity() {
//        return recording.getDensity();
//    }

    // =========================================================================================
    // IMAGE AND RECORDING ACCESS
    // =========================================================================================

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