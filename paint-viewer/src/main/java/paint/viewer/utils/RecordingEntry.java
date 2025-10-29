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
 * visibility control parameters (e.g., density ratio, variability, R² threshold).
 *
 * Images are preloaded via ImageIO or ImageJ Opener, and associated square data
 * can be lazily fetched and cached from the experiment.
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

    private       List<Square>  squares;
    // @formatter:on

    /**
     * Constructs a new {@code RecordingEntry}.
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

    public List<Square> getSquares() {
        return squares;
    }
    // =========================================================================================
    // SQUARE MANAGEMENT
    // =========================================================================================

    /**
     * Retrieves a list of Square objects associated with this recording.
     * Squares are cached per experiment for performance efficiency.
     */
    public List<Square> getSquares(Project project, int expectedNumberOfSquares) {
        if (squares == null) {
            try {
                PaintLogger.debugf("Fetching squares (cached per experiment) for recording: %s", getRecordingName());
                squares = ExperimentSquareCache.getSquaresForRecording(
                        project.getProjectRootPath(),
                        getExperimentName(),
                        getRecordingName(),
                        expectedNumberOfSquares
                );
            } catch (Exception e) {
                e.printStackTrace();
                squares = Collections.emptyList();
            }
        } else {
            PaintLogger.debugf("Returning cached squares for recording: %s", getRecordingName());
        }
        return squares;
    }
}