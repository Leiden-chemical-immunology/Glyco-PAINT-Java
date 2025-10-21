package viewer.utils;

import paint.shared.objects.Project;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.utils.PaintLogger;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class RecordingEntry {

    // @formatter:off
    private final Recording recording;
    private final Path      trackmateImagePath;
    private final Path      brightfieldImagePath;
    private final String    experimentName;
    private final double    minRequiredDensityRatio;
    private final double    maxAllowableVariability;
    private final double    minRequiredRSquared;
    private final String    neighbourMode;
    // @formatter:on

    private final ImageIcon leftImage;
    private final ImageIcon rightImage;
    private List<Square> squares;

    public RecordingEntry(
            Recording recording,
            Path trackmateImagePath,
            Path brightfieldImagePath,
            String experimentName,
            double minRequiredDensityRatio,
            double maxAllowableVariability,
            double minRequiredRSquared,
            String neighbourMode
    ) {
        // @formatter:off
        this.recording               = recording;
        this.trackmateImagePath      = trackmateImagePath;
        this.brightfieldImagePath    = brightfieldImagePath;
        this.experimentName          = experimentName;
        this.minRequiredDensityRatio = minRequiredDensityRatio;
        this.maxAllowableVariability = maxAllowableVariability;
        this.minRequiredRSquared     = minRequiredRSquared;
        this.neighbourMode           = neighbourMode;
        // @formatter:on

        this.leftImage = loadImage(trackmateImagePath, "TrackMate");
        this.rightImage = loadImage(brightfieldImagePath, "Brightfield");
    }

    // === Robust image loading helper ===
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

    // === Getters ===
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

    public double getTau() {
        return recording.getTau();
    }

    public double getDensity() {
        return recording.getDensity();
    }

    public double getMinRequiredDensityRatio() {
        return minRequiredDensityRatio;
    }

    public double getMaxAllowableVariability() {
        return maxAllowableVariability;
    }

    public double getMinRequiredRSquared() {
        return minRequiredRSquared;
    }

    public String getNeighbourMode() {
        return neighbourMode;
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

    // === Square loading and caching (experiment-level) ===
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